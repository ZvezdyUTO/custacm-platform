// Author: huangbingrui.awa
import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent, ref } from 'vue';
import { describe, expect, it, vi } from 'vitest';
import ArticleAdminPanel from '../components/ArticleAdminPanel.vue';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard';

describe('article admin panel', () => {
  it('opens homepage composition first and keeps article backup and recycle-bin operations', async () => {
    const loadAdminArticles = vi.fn().mockResolvedValue(undefined);
	const deleteArticle = vi.fn().mockResolvedValue(undefined);
	const restoreArticle = vi.fn().mockResolvedValue(undefined);
	const backupAllArticles = vi.fn().mockResolvedValue(undefined);
    const dashboard = {
      adminArticles: ref({
        categories: [{ id: 2, name: '题解' }],
        blogs: { pageNum: 1, pageSize: 10, pages: 1, total: 1, list: [{
          id: 7, title: '区间 DP', firstPicture: '', createTime: '2026-07-01T10:00:00',
          updateTime: '2026-07-12T10:00:00', published: true, recommend: false, top: false,
          category: { id: 2, name: '题解' },
        }] },
      }),
      loadAdminArticles,
		deleteArticle,
		restoreArticle,
		backupAllArticles,
    } as unknown as ReturnType<typeof usePlatformDashboard>;

    const wrapper = mount(ArticleAdminPanel, {
      props: { dashboard },
      global: { stubs: { HomepageFeaturedGroupsPanel: true } },
    });
    expect(wrapper.text()).toContain('每组固定三篇文章，最多展示三组');
    expect(wrapper.find('.article-backup-button').exists()).toBe(false);
    await wrapper.findAll('.article-admin-tabs button')[1].trigger('click');
    await vi.waitFor(() => expect(loadAdminArticles).toHaveBeenCalled());
    await flushPromises();
		await wrapper.get('.article-backup-button').trigger('click');
		expect(wrapper.get('[role="alertdialog"]').text()).toContain('下载全部文章备份');
		expect(wrapper.get('[role="alertdialog"]').text()).toContain('回收站内容');
		expect(backupAllArticles).not.toHaveBeenCalled();
		await wrapper.get('.admin-confirm-primary').trigger('click');
		await flushPromises();
	expect(backupAllArticles).toHaveBeenCalledOnce();
	expect(wrapper.get('[role="status"]').text()).toContain('托管图片备份已开始下载');
    await wrapper.get('.article-delete-button').trigger('click');
	expect(wrapper.get('[role="alertdialog"]').text()).toContain('固定保留 7 天');
	expect(wrapper.get('[role="alertdialog"]').text()).toContain('可以恢复');
    expect(deleteArticle).not.toHaveBeenCalled();
    await wrapper.get('.confirm-delete-button').trigger('click');
	expect(deleteArticle).toHaveBeenCalledWith(7);

	await wrapper.findAll('.article-admin-tabs button')[2].trigger('click');
	await flushPromises();
	expect(loadAdminArticles).toHaveBeenLastCalledWith(expect.any(Object), true);
	await wrapper.get('.article-restore-button').trigger('click');
	expect(restoreArticle).toHaveBeenCalledWith(7);
		});

  it('keeps an unsaved homepage-composition draft when switching article subviews', async () => {
    const dashboard = {
      adminArticles: ref(null),
      loadAdminArticles: vi.fn().mockResolvedValue(undefined),
    } as unknown as ReturnType<typeof usePlatformDashboard>;
    const FeaturedDraftStub = defineComponent({
      data: () => ({ title: '' }),
      template: '<div class="featured-draft-stub"><input v-model="title"></div>',
    });
    const wrapper = mount(ArticleAdminPanel, {
      props: { dashboard },
      global: { stubs: { HomepageFeaturedGroupsPanel: FeaturedDraftStub } },
    });

    const input = wrapper.get<HTMLInputElement>('.featured-draft-stub input');
    await input.setValue('未保存的精选标题');
    await wrapper.findAll('.article-admin-tabs button')[1].trigger('click');
    await flushPromises();
    expect(wrapper.find('.featured-draft-stub').exists()).toBe(true);
    expect(wrapper.get('.featured-draft-stub').attributes('style')).toContain('display: none');

    await wrapper.findAll('.article-admin-tabs button')[0].trigger('click');
    expect(wrapper.get<HTMLInputElement>('.featured-draft-stub input').element.value)
      .toBe('未保存的精选标题');
  });
});
