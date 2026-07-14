// Author: huangbingrui.awa
import { flushPromises, mount } from '@vue/test-utils';
import { nextTick, ref } from 'vue';
import { describe, expect, it, vi } from 'vitest';
import HomepageFeaturedGroupsPanel from '../components/HomepageFeaturedGroupsPanel.vue';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard';
import type {
  HomepageFeaturedArticle,
  HomepageFeaturedArticleCandidate,
  HomepageFeaturedGroup,
  HomepageFeaturedGroupUpsertRequest,
} from '../types';

function article(id: number, sortOrder: number): HomepageFeaturedArticle {
  return {
    id,
    title: `文章 ${id}`,
    description: '',
    firstPicture: `/image/${id}.jpg`,
    createTime: '2026-07-14T10:00:00',
    categoryName: '训练经验',
    authorUsername: `player-${id}`,
    authorNickname: `队员 ${id}`,
    authorAvatar: null,
    sortOrder,
    available: true,
  };
}

function group(id: number, sortOrder: number): HomepageFeaturedGroup {
  return {
    id,
    title: `精选组 ${id}`,
    sortOrder,
    complete: true,
    articles: [0, 1, 2].map((slot) => article(id * 10 + slot, slot)),
  };
}

function candidate(
  id: number,
  featuredGroupId: number | null = null,
  sortOrder: number | null = null,
): HomepageFeaturedArticleCandidate {
  return { ...article(id, sortOrder ?? 0), sortOrder, featuredGroupId };
}

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((resolvePromise) => {
    resolve = resolvePromise;
  });
  return { promise, resolve };
}

describe('homepage featured groups admin', () => {
  it('creates only after all three article slots are selected', async () => {
    const groups = ref<HomepageFeaturedGroup[]>([group(1, 0)]);
    const candidates = [candidate(41), candidate(42), candidate(43)];
    const createHomepageFeaturedGroup = vi.fn().mockResolvedValue(undefined);
    const dashboard = {
      homepageFeaturedGroups: groups,
      loadHomepageFeaturedGroups: vi.fn().mockResolvedValue(undefined),
      searchHomepageFeaturedArticleCandidates: vi.fn().mockResolvedValue(candidates),
      createHomepageFeaturedGroup,
    } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(HomepageFeaturedGroupsPanel, { props: { dashboard } });
    await flushPromises();

    await wrapper.get('.featured-group-add').trigger('click');
    expect(createHomepageFeaturedGroup).not.toHaveBeenCalled();
    const newGroup = () => wrapper.findAll('.featured-group-editor')[1];

    for (let slot = 0; slot < 3; slot += 1) {
      await newGroup().findAll('.featured-article-slot')[slot].trigger('click');
      await flushPromises();
      await wrapper.findAll('.featured-candidate-list > button')[slot].trigger('click');
    }

    await newGroup().get('.featured-group-editor-footer .primary-button').trigger('click');
    await flushPromises();

    expect(createHomepageFeaturedGroup).toHaveBeenCalledWith({
      title: '精选文章',
      articleIds: [41, 42, 43],
    });
  });

  it('sends the complete group order and confirms destructive deletion', async () => {
    const groups = ref<HomepageFeaturedGroup[]>([group(1, 0), group(2, 1)]);
    const reorderHomepageFeaturedGroups = vi.fn().mockResolvedValue(undefined);
    const deleteHomepageFeaturedGroup = vi.fn().mockResolvedValue(undefined);
    const dashboard = {
      homepageFeaturedGroups: groups,
      loadHomepageFeaturedGroups: vi.fn().mockResolvedValue(undefined),
      reorderHomepageFeaturedGroups,
      deleteHomepageFeaturedGroup,
    } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(HomepageFeaturedGroupsPanel, { props: { dashboard } });
    await flushPromises();

    await wrapper.get('button[aria-label="将第 2 组上移"]').trigger('click');
    expect(reorderHomepageFeaturedGroups).toHaveBeenCalledWith([2, 1]);

    await wrapper.get('button[aria-label="删除第 1 组"]').trigger('click');
    expect(wrapper.get('[role="alertdialog"]').text()).toContain('立即从首页移除');
    expect(deleteHomepageFeaturedGroup).not.toHaveBeenCalled();
    await wrapper.get('.admin-confirm-primary').trigger('click');
    expect(deleteHomepageFeaturedGroup).toHaveBeenCalledWith(1);
  });

  it('preserves other dirty drafts after saving one group', async () => {
    const groups = ref<HomepageFeaturedGroup[]>([group(1, 0), group(2, 1)]);
    const updateHomepageFeaturedGroup = vi.fn(async (
      id: number,
      request: HomepageFeaturedGroupUpsertRequest,
    ) => {
      groups.value = groups.value.map((item) => item.id === id
        ? { ...item, title: request.title }
        : item);
    });
    const dashboard = {
      homepageFeaturedGroups: groups,
      loadHomepageFeaturedGroups: vi.fn().mockResolvedValue(undefined),
      updateHomepageFeaturedGroup,
    } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(HomepageFeaturedGroupsPanel, { props: { dashboard } });
    await flushPromises();

    const titleInputs = wrapper.findAll<HTMLInputElement>('.featured-group-editor-header input');
    await titleInputs[0].setValue('已保存标题');
    await titleInputs[1].setValue('第二组未保存标题');
    await wrapper.findAll('.featured-group-editor-footer .primary-button')[0].trigger('click');
    await flushPromises();

    expect(updateHomepageFeaturedGroup).toHaveBeenCalledWith(1, {
      title: '已保存标题',
      articleIds: [10, 11, 12],
    });
    expect(wrapper.findAll<HTMLInputElement>('.featured-group-editor-header input').map((input) => input.element.value))
      .toEqual(['已保存标题', '第二组未保存标题']);
  });

  it('preserves dirty and unsaved groups after deleting another group', async () => {
    const groups = ref<HomepageFeaturedGroup[]>([group(1, 0), group(2, 1)]);
    const deleteHomepageFeaturedGroup = vi.fn(async (id: number) => {
      groups.value = groups.value
        .filter((item) => item.id !== id)
        .map((item, sortOrder) => ({ ...item, sortOrder }));
    });
    const dashboard = {
      homepageFeaturedGroups: groups,
      loadHomepageFeaturedGroups: vi.fn().mockResolvedValue(undefined),
      deleteHomepageFeaturedGroup,
    } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(HomepageFeaturedGroupsPanel, { props: { dashboard } });
    await flushPromises();

    await wrapper.findAll('.featured-group-editor-header input')[1].setValue('第二组草稿');
    await wrapper.get('.featured-group-add').trigger('click');
    await wrapper.findAll('.featured-group-editor-header input')[2].setValue('新建组草稿');
    await wrapper.get('button[aria-label="删除第 1 组"]').trigger('click');
    await wrapper.get('.admin-confirm-primary').trigger('click');
    await flushPromises();

    expect(deleteHomepageFeaturedGroup).toHaveBeenCalledWith(1);
    expect(wrapper.findAll<HTMLInputElement>('.featured-group-editor-header input').map((input) => input.element.value))
      .toEqual(['第二组草稿', '新建组草稿']);
  });

  it('swaps articles already selected in the same group and keeps cross-group duplicates disabled', async () => {
    const groups = ref<HomepageFeaturedGroup[]>([group(1, 0), group(2, 1)]);
    const currentGroupCandidates = groups.value[0].articles.map((item) => (
      candidate(item.id, 1, item.sortOrder)
    ));
    const updateHomepageFeaturedGroup = vi.fn().mockResolvedValue(undefined);
    const dashboard = {
      homepageFeaturedGroups: groups,
      loadHomepageFeaturedGroups: vi.fn().mockResolvedValue(undefined),
      searchHomepageFeaturedArticleCandidates: vi.fn().mockResolvedValue(currentGroupCandidates),
      updateHomepageFeaturedGroup,
    } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(HomepageFeaturedGroupsPanel, { props: { dashboard } });
    await flushPromises();

    await wrapper.findAll('.featured-article-slot')[1].trigger('click');
    await flushPromises();
    const sameGroupCandidate = wrapper.findAll<HTMLButtonElement>('.featured-candidate-list > button')[0];
    expect(sameGroupCandidate.element.disabled).toBe(false);
    await sameGroupCandidate.trigger('click');
    expect(wrapper.findAll('.featured-slot-copy strong').slice(0, 3).map((title) => title.text()))
      .toEqual(['文章 11', '文章 10', '文章 12']);
    await wrapper.findAll('.featured-group-editor-footer .primary-button')[0].trigger('click');
    await flushPromises();
    expect(updateHomepageFeaturedGroup).toHaveBeenCalledWith(1, {
      title: '精选组 1',
      articleIds: [11, 10, 12],
    });

    await wrapper.findAll('.featured-article-slot')[3].trigger('click');
    await flushPromises();
    const crossGroupCandidate = wrapper.findAll<HTMLButtonElement>('.featured-candidate-list > button')[0];
    expect(crossGroupCandidate.element.disabled).toBe(true);
    expect(crossGroupCandidate.text()).toContain('已在其他分组');
  });

  it('keeps keyboard focus inside the picker and restores it after Escape', async () => {
    const groups = ref<HomepageFeaturedGroup[]>([group(1, 0)]);
    const dashboard = {
      homepageFeaturedGroups: groups,
      loadHomepageFeaturedGroups: vi.fn().mockResolvedValue(undefined),
      searchHomepageFeaturedArticleCandidates: vi.fn().mockResolvedValue([candidate(41)]),
    } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(HomepageFeaturedGroupsPanel, {
      props: { dashboard },
      attachTo: document.body,
    });
    await flushPromises();

    const opener = wrapper.get<HTMLButtonElement>('.featured-article-slot');
    opener.element.focus();
    await opener.trigger('click');
    await flushPromises();
    const searchInput = wrapper.get<HTMLInputElement>('.featured-picker-search input');
    const closeButton = wrapper.get<HTMLButtonElement>('.featured-picker-dialog > header button');
    const dialog = wrapper.get('.featured-picker-dialog');
    const focusable = Array.from(dialog.element.querySelectorAll<HTMLElement>(
      'button:not(:disabled), input:not(:disabled), [tabindex]:not([tabindex="-1"])',
    ));
    const lastFocusable = focusable.at(-1);
    expect(document.activeElement).toBe(searchInput.element);
    expect(searchInput.attributes('aria-label')).toBe('按文章标题搜索');

    lastFocusable?.focus();
    await dialog.trigger('keydown', { key: 'Tab' });
    expect(document.activeElement).toBe(closeButton.element);
    closeButton.element.focus();
    await dialog.trigger('keydown', { key: 'Tab', shiftKey: true });
    expect(document.activeElement).toBe(lastFocusable);

    await dialog.trigger('keydown', { key: 'Escape' });
    await nextTick();
    expect(wrapper.find('.featured-picker-dialog').exists()).toBe(false);
    expect(document.activeElement).toBe(opener.element);
    wrapper.unmount();
  });

  it('ignores stale candidate responses after a newer search', async () => {
    const groups = ref<HomepageFeaturedGroup[]>([group(1, 0)]);
    const initial = deferred<HomepageFeaturedArticleCandidate[]>();
    const older = deferred<HomepageFeaturedArticleCandidate[]>();
    const newer = deferred<HomepageFeaturedArticleCandidate[]>();
    const searchHomepageFeaturedArticleCandidates = vi.fn()
      .mockReturnValueOnce(initial.promise)
      .mockReturnValueOnce(older.promise)
      .mockReturnValueOnce(newer.promise);
    const dashboard = {
      homepageFeaturedGroups: groups,
      loadHomepageFeaturedGroups: vi.fn().mockResolvedValue(undefined),
      searchHomepageFeaturedArticleCandidates,
    } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(HomepageFeaturedGroupsPanel, { props: { dashboard } });
    await flushPromises();

    await wrapper.get('.featured-article-slot').trigger('click');
    initial.resolve([candidate(40)]);
    await flushPromises();
    const searchInput = wrapper.get('.featured-picker-search input');
    await searchInput.setValue('旧查询');
    await wrapper.get('.featured-picker-search').trigger('submit');
    await searchInput.setValue('新查询');
    await wrapper.get('.featured-picker-search').trigger('submit');

    newer.resolve([candidate(43)]);
    await flushPromises();
    expect(wrapper.get('.featured-candidate-list').text()).toContain('文章 43');
    older.resolve([candidate(42)]);
    await flushPromises();
    expect(wrapper.get('.featured-candidate-list').text()).toContain('文章 43');
    expect(wrapper.get('.featured-candidate-list').text()).not.toContain('文章 42');
  });

  it('does not refill a reopened picker from a request that was closed', async () => {
    const groups = ref<HomepageFeaturedGroup[]>([group(1, 0)]);
    const closedRequest = deferred<HomepageFeaturedArticleCandidate[]>();
    const currentRequest = deferred<HomepageFeaturedArticleCandidate[]>();
    const dashboard = {
      homepageFeaturedGroups: groups,
      loadHomepageFeaturedGroups: vi.fn().mockResolvedValue(undefined),
      searchHomepageFeaturedArticleCandidates: vi.fn()
        .mockReturnValueOnce(closedRequest.promise)
        .mockReturnValueOnce(currentRequest.promise),
    } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(HomepageFeaturedGroupsPanel, { props: { dashboard } });
    await flushPromises();

    await wrapper.get('.featured-article-slot').trigger('click');
    await nextTick();
    await wrapper.get('.featured-picker-dialog').trigger('keydown', { key: 'Escape' });
    await nextTick();
    await wrapper.get('.featured-article-slot').trigger('click');
    await nextTick();
    closedRequest.resolve([candidate(41)]);
    await flushPromises();
    expect(wrapper.findAll('.featured-candidate-list > button')).toHaveLength(0);

    currentRequest.resolve([candidate(42)]);
    await flushPromises();
    expect(wrapper.get('.featured-candidate-list').text()).toContain('文章 42');
  });
});
