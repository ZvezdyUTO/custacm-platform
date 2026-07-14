// Author: huangbingrui.awa
import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent, h, ref } from 'vue';
import { describe, expect, it, vi } from 'vitest';
import { listCompetitions, listCompetitionRecycleBin } from '../api/admin';
import CompetitionAdminPanel from '../components/CompetitionAdminPanel.vue';
import { usePlatformDashboard } from '../composables/usePlatformDashboard';
import type { AdminSection } from '../routing';
import type {
  Competition,
  CompetitionPageResponse,
  CurrentUser,
  TrainingQueryMode,
} from '../types';

vi.mock('../api/admin', async (importOriginal) => ({
  ...await importOriginal<typeof import('../api/admin')>(),
  listCompetitions: vi.fn(),
  listCompetitionRecycleBin: vi.fn(),
}));

function deferred<T>() {
  let resolve!: (value: T | PromiseLike<T>) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, reject, resolve };
}

function competitionFixture(deletedAt: string | null = null): Competition {
  return {
    id: 7,
    fullName: '2026 ICPC 亚洲区域赛（合肥）',
    year: 2026,
    category: 'ICPC_ASIA_REGIONAL',
    categoryLabel: 'ICPC 亚洲区域赛',
    participationMode: 'TEAM',
    participationModeLabel: '团队',
    types: [{ code: 'ICPC', label: 'ICPC' }, { code: 'ASIA_REGIONAL', label: 'ICPC 亚洲区域赛' }],
    createTime: '2026-07-14T08:00:00Z',
    deletedAt,
    participants: [
      { id: 11, username: 'alice', displayName: 'Alice', articles: [] },
      { id: 12, username: 'bob', displayName: 'Bob', articles: [{ id: 3, title: '赛后复盘' }] },
    ],
    awards: [{
      id: 21,
      awardMode: 'TEAM',
      awardModeLabel: '团队',
      teamName: 'CUST ACM',
      awardTier: 'MEDAL_GOLD',
      awardTierLabel: '金牌',
      rankPosition: 3,
      rankTotal: 280,
      rank: '(3/280)',
      recipients: [
        { participantId: 11, username: 'alice', displayName: 'Alice' },
        { participantId: 12, username: 'bob', displayName: 'Bob' },
      ],
    }],
  };
}

function page(list: Competition[]): CompetitionPageResponse {
  return { pageNum: 1, pageSize: 10, total: list.length, totalPages: list.length ? 1 : 0, list };
}

function dashboardStub() {
  const active = ref(page([competitionFixture()]));
  const recycle = ref(page([competitionFixture(new Date().toISOString())]));
  return {
    adminCompetitions: active,
    adminCompetitionRecycleBin: recycle,
    loadAdminCompetitions: vi.fn().mockResolvedValue(active.value),
    loadAdminCompetitionRecycleBin: vi.fn().mockResolvedValue(recycle.value),
    createCompetition: vi.fn().mockResolvedValue(competitionFixture()),
    addCompetitionParticipants: vi.fn().mockResolvedValue(competitionFixture()),
    deleteCompetitionParticipant: vi.fn().mockResolvedValue(undefined),
    addCompetitionAward: vi.fn().mockResolvedValue(competitionFixture()),
    deleteCompetitionAward: vi.fn().mockResolvedValue(undefined),
    moveCompetitionToRecycleBin: vi.fn().mockResolvedValue(undefined),
    restoreCompetition: vi.fn().mockResolvedValue(competitionFixture()),
  };
}

function mountPanel(stub = dashboardStub()) {
  const wrapper = mount(CompetitionAdminPanel, {
    props: { dashboard: stub as unknown as ReturnType<typeof usePlatformDashboard> },
    attachTo: document.body,
  });
  return { wrapper, stub };
}

function mountCompetitionDashboard() {
  let dashboard!: ReturnType<typeof usePlatformDashboard>;
  const wrapper = mount(defineComponent({
    setup() {
      dashboard = usePlatformDashboard({
        token: ref('token'),
        user: ref<CurrentUser | null>({
          username: 'root', nickname: 'Root', avatar: '', email: '', role: 'ROLE_admin',
        }),
        mode: ref<TrainingQueryMode>('multiple'),
        adminSection: ref<AdminSection | null>('competitions'),
        onUnauthorized: vi.fn(),
      });
      return () => h('div');
    },
  }));
  return { dashboard, wrapper };
}

describe('CompetitionAdminPanel', () => {
  it('loads active competitions on demand and keeps recycle-bin loading behind its tab', async () => {
    const { wrapper, stub } = mountPanel();
    await flushPromises();

    expect(stub.loadAdminCompetitions).toHaveBeenCalledWith({
      startYear: null, endYear: null, category: null, pageNum: 1, pageSize: 10,
    });
    expect(wrapper.text()).toContain('ICPC 亚洲区域赛');
    expect(stub.loadAdminCompetitionRecycleBin).not.toHaveBeenCalled();

    await wrapper.get('.competition-scope-tabs button:nth-child(2)').trigger('click');
    await flushPromises();
    expect(stub.loadAdminCompetitionRecycleBin).toHaveBeenCalledWith({
      startYear: null, endYear: null, category: null, pageNum: 1, pageSize: 10,
    });
    wrapper.unmount();
  });

  it('uses the normalized category parameter for list filtering', async () => {
    const { wrapper, stub } = mountPanel();
    await flushPromises();

    await wrapper.get('select[aria-label="筛选规范分类"]').setValue('EC_FINAL');
    await wrapper.get('[data-test="competition-filters"]').trigger('submit');
    await flushPromises();

    expect(stub.loadAdminCompetitions).toHaveBeenLastCalledWith({
      startYear: null,
      endYear: null,
      category: 'EC_FINAL',
      pageNum: 1,
      pageSize: 10,
    });
    wrapper.unmount();
  });

  it('keeps unclassified historical records readable without allowing ambiguous new awards', async () => {
    const stub = dashboardStub();
    stub.adminCompetitions.value = page([{
      ...competitionFixture(),
      category: null,
      categoryLabel: null,
      awards: [{
        ...competitionFixture().awards[0],
        awardTier: null,
        awardTierLabel: null,
      }],
    }]);
    const { wrapper } = mountPanel(stub);
    await flushPromises();

    expect(wrapper.text()).toContain('待归类历史记录');
    await wrapper.get('.competition-detail-trigger').trigger('click');
    expect(wrapper.text()).toContain('待归类历史奖项');
    expect(wrapper.text()).toContain('暂不能添加新奖项');
    expect(wrapper.get('.competition-award-submit button').attributes('disabled')).toBeDefined();
    wrapper.unmount();
  });

  it('keeps the latest list request busy when older tab requests settle first', async () => {
    const stub = dashboardStub();
    const initialActive = deferred<CompetitionPageResponse>();
    const recycleRequest = deferred<CompetitionPageResponse>();
    const latestActive = deferred<CompetitionPageResponse>();
    stub.loadAdminCompetitions
      .mockReset()
      .mockReturnValueOnce(initialActive.promise)
      .mockReturnValueOnce(latestActive.promise);
    stub.loadAdminCompetitionRecycleBin
      .mockReset()
      .mockReturnValueOnce(recycleRequest.promise);

    const { wrapper } = mountPanel(stub);
    const tabs = wrapper.findAll('.competition-scope-tabs button');
    expect(tabs).toHaveLength(2);
    await tabs[1].trigger('click');
    await tabs[0].trigger('click');

    expect(wrapper.get('.competition-list').attributes('aria-busy')).toBe('true');
    initialActive.reject(new Error('旧的当前比赛请求失败'));
    recycleRequest.resolve(page([]));
    await flushPromises();

    expect(wrapper.get('.competition-list').attributes('aria-busy')).toBe('true');
    expect(wrapper.find('.form-error').exists()).toBe(false);

    latestActive.resolve(stub.adminCompetitions.value);
    await flushPromises();
    expect(wrapper.get('.competition-list').attributes('aria-busy')).toBe('false');
    wrapper.unmount();
  });

  it('creates an immutable competition and refreshes the active list', async () => {
    const { wrapper, stub } = mountPanel();
    await flushPromises();
    await wrapper.get('.competition-create-trigger').trigger('click');
    await wrapper.get('.competition-name-field input').setValue('2026 CCPC 全国邀请赛');
    await wrapper.get('select[aria-label="比赛规范分类"]').setValue('CCPC_NATIONAL_INVITATIONAL');
    expect(wrapper.find('select[aria-label="比赛参赛形态"]').exists()).toBe(false);
    await wrapper.get('[data-test="competition-create-form"]').trigger('submit');
    await flushPromises();

    expect(stub.createCompetition).toHaveBeenCalledWith({
      fullName: '2026 CCPC 全国邀请赛',
      year: new Date().getFullYear(),
      category: 'CCPC_NATIONAL_INVITATIONAL',
      participationMode: 'TEAM',
    });
    expect(stub.loadAdminCompetitions).toHaveBeenCalledTimes(2);
    expect(wrapper.text()).toContain('比赛已创建');
    wrapper.unmount();
  });

  it('offers exactly ten grouped categories without a redundant participation-mode field', async () => {
    const { wrapper } = mountPanel();
    await flushPromises();
    await wrapper.get('.competition-create-trigger').trigger('click');

    const category = wrapper.get('select[aria-label="比赛规范分类"]');
    const categoryOptions = category.findAll('option').filter((option) => Boolean(option.attributes('value')));
    expect(categoryOptions.map((option) => option.attributes('value'))).toEqual([
      'PROVINCIAL',
      'ICPC_NATIONAL_INVITATIONAL',
      'CCPC_NATIONAL_INVITATIONAL',
      'ICPC_ASIA_REGIONAL',
      'CCPC_REGIONAL',
      'EC_FINAL',
      'CCPC_FINAL',
      'BAIDU_STAR',
      'GPLT_NATIONAL',
      'LANQIAO_CUP_NATIONAL',
    ]);
    expect(categoryOptions.map((option) => option.text())).toEqual([
      '省赛',
      'ICPC 全国邀请赛',
      'CCPC 全国邀请赛',
      'ICPC 亚洲区域赛',
      'CCPC 区域赛',
      'EC-Final',
      'CCPC-Final',
      '百度之星',
      'GPLT 团体程序设计天梯赛（国赛）',
      '蓝桥杯程序设计竞赛（国奖）',
    ]);
    expect(wrapper.find('select[aria-label="比赛参赛形态"]').exists()).toBe(false);
    wrapper.unmount();
  });

  it.each([
    { category: 'PROVINCIAL' as const, participationMode: 'TEAM' as const },
    { category: 'BAIDU_STAR' as const, participationMode: 'INDIVIDUAL' as const },
    { category: 'GPLT_NATIONAL' as const, participationMode: 'MIXED' as const },
  ])('derives $participationMode participation from $category on submit', async ({ category, participationMode }) => {
    const { wrapper, stub } = mountPanel();
    await flushPromises();
    await wrapper.get('.competition-create-trigger').trigger('click');
    await wrapper.get('.competition-name-field input').setValue(`2026 ${category}`);
    await wrapper.get('select[aria-label="比赛规范分类"]').setValue(category);
    await wrapper.get('[data-test="competition-create-form"]').trigger('submit');
    await flushPromises();

    expect(stub.createCompetition).toHaveBeenCalledWith({
      fullName: `2026 ${category}`,
      year: new Date().getFullYear(),
      category,
      participationMode,
    });
    wrapper.unmount();
  });

  it('blocks missing categories, invalid recipient shape, and invalid medal ranks before calling APIs', async () => {
    const stub = dashboardStub();
    stub.adminCompetitions.value = page([{
      ...competitionFixture(),
      category: 'PROVINCIAL',
      categoryLabel: '省赛',
      participationMode: 'MIXED',
      participationModeLabel: '混合',
    }]);
    const { wrapper } = mountPanel(stub);
    await flushPromises();

    await wrapper.get('.competition-create-trigger').trigger('click');
    await wrapper.get('.competition-name-field input').setValue('2026 待分类比赛');
    expect(wrapper.text()).toContain('请选择唯一的规范分类');
    await wrapper.get('[data-test="competition-create-form"]').trigger('submit');
    expect(stub.createCompetition).not.toHaveBeenCalled();

    await wrapper.get('.competition-detail-trigger').trigger('click');
    expect(wrapper.get('select[aria-label="奖项档位"]').findAll('option').map((option) => option.text())).toEqual([
      '金牌', '银牌', '铜牌', '优胜奖',
    ]);
    expect(wrapper.text()).toContain('团队奖项至少需要选择一名获奖人');
    await wrapper.get('.competition-award-form').trigger('submit');

    const recipients = wrapper.findAll('.competition-recipient-picker input');
    await recipients[0].setValue(true);
    await wrapper.get('select[aria-label="奖项归属形态"]').setValue('INDIVIDUAL');
    await recipients[1].setValue(true);
    expect(wrapper.text()).toContain('个人奖项必须且只能选择一名获奖人');
    await wrapper.get('.competition-award-form').trigger('submit');

    await wrapper.get('select[aria-label="奖项归属形态"]').setValue('TEAM');
    await wrapper.get('input[aria-label="排名名次"]').setValue(4);
    await wrapper.get('input[aria-label="排名总数"]').setValue(3);
    expect(wrapper.text()).toContain('排名必须满足 1 ≤ 名次 ≤ 总排名数');
    await wrapper.get('.competition-award-form').trigger('submit');

    expect(stub.addCompetitionAward).not.toHaveBeenCalled();
    wrapper.unmount();
  });

  it('uses Baidu tiers, fixes individual awards, and submits ordinary awards without ranks', async () => {
    const stub = dashboardStub();
    stub.adminCompetitions.value = page([{
      ...competitionFixture(),
      category: 'BAIDU_STAR',
      categoryLabel: '百度之星',
      participationMode: 'INDIVIDUAL',
      participationModeLabel: '个人',
      awards: [],
    }]);
    const { wrapper } = mountPanel(stub);
    await flushPromises();
    await wrapper.get('.competition-detail-trigger').trigger('click');

    const mode = wrapper.get('select[aria-label="奖项归属形态"]');
    expect((mode.element as HTMLSelectElement).value).toBe('INDIVIDUAL');
    expect(mode.attributes('disabled')).toBeDefined();
    expect(wrapper.find('input[aria-label="排名名次"]').exists()).toBe(false);
    const tiers = wrapper.get('select[aria-label="奖项档位"]').findAll('option');
    expect(tiers.map((option) => option.attributes('value'))).toEqual([
      'BAIDU_NATIONAL_FIRST',
      'BAIDU_NATIONAL_SECOND',
      'BAIDU_NATIONAL_THIRD',
      'BAIDU_NATIONAL_FOURTH',
      'BAIDU_PROVINCIAL_FIRST',
      'BAIDU_PROVINCIAL_SECOND',
      'BAIDU_PROVINCIAL_THIRD',
    ]);
    expect(tiers.map((option) => option.text())).toEqual([
      '国赛一等奖', '国赛二等奖', '国赛三等奖', '国赛四等奖',
      '省赛一等奖', '省赛二等奖', '省赛三等奖',
    ]);

    await wrapper.get('select[aria-label="奖项档位"]').setValue('BAIDU_NATIONAL_SECOND');
    await wrapper.findAll('.competition-recipient-picker input')[0].setValue(true);
    await wrapper.get('.competition-award-form').trigger('submit');
    await flushPromises();

    expect(stub.addCompetitionAward).toHaveBeenCalledWith(7, {
      awardMode: 'INDIVIDUAL',
      teamName: null,
      awardTier: 'BAIDU_NATIONAL_SECOND',
      rankPosition: null,
      rankTotal: null,
      recipientUsernames: ['alice'],
    });
    wrapper.unmount();
  });

  it.each([
    { category: 'GPLT_NATIONAL' as const, mode: 'MIXED' as const, locked: false },
    { category: 'LANQIAO_CUP_NATIONAL' as const, mode: 'INDIVIDUAL' as const, locked: true },
  ])('uses the three prize tiers and $mode award-mode rule for $category', async ({ category, mode, locked }) => {
    const stub = dashboardStub();
    stub.adminCompetitions.value = page([{
      ...competitionFixture(),
      category,
      categoryLabel: category,
      participationMode: mode,
      participationModeLabel: mode,
      awards: [],
    }]);
    const { wrapper } = mountPanel(stub);
    await flushPromises();
    await wrapper.get('.competition-detail-trigger').trigger('click');

    const awardMode = wrapper.get('select[aria-label="奖项归属形态"]');
    expect(awardMode.attributes('disabled') !== undefined).toBe(locked);
    expect(wrapper.find('input[aria-label="排名名次"]').exists()).toBe(false);
    const tiers = wrapper.get('select[aria-label="奖项档位"]').findAll('option');
    expect(tiers.map((option) => option.attributes('value'))).toEqual([
      'FIRST_PRIZE', 'SECOND_PRIZE', 'THIRD_PRIZE',
    ]);
    expect(tiers.map((option) => option.text())).toEqual(['一等奖', '二等奖', '三等奖']);
    wrapper.unmount();
  });

  it('adds participants and awards, and confirms destructive actions in the shared dialog', async () => {
    const { wrapper, stub } = mountPanel();
    await flushPromises();
    await wrapper.get('.competition-detail-trigger').trigger('click');
    expect(wrapper.text()).toContain('金牌');

    await wrapper.get('.competition-participant-form textarea').setValue('carol\ndave');
    await wrapper.get('.competition-participant-form').trigger('submit');
    await flushPromises();
    expect(stub.addCompetitionParticipants).toHaveBeenCalledWith(7, { usernames: ['carol', 'dave'] });

    const recipients = wrapper.findAll('.competition-recipient-picker input');
    await recipients[0].setValue(true);
    await wrapper.get('.competition-award-form').trigger('submit');
    await flushPromises();
    expect(stub.addCompetitionAward).toHaveBeenCalledWith(7, expect.objectContaining({
      awardMode: 'TEAM',
      awardTier: 'MEDAL_GOLD',
      rankPosition: 1,
      rankTotal: 1,
      recipientUsernames: ['alice'],
    }));

    await wrapper.get('button[aria-label^="删除奖项"]').trigger('click');
    expect(document.body.textContent).toContain('删除这项奖项？');
    await wrapper.get('.admin-confirm-primary').trigger('click');
    await flushPromises();
    expect(stub.deleteCompetitionAward).toHaveBeenCalledWith(7, 21);

    await wrapper.get('.competition-danger-action').trigger('click');
    expect(document.body.textContent).toContain('将整场比赛移入回收站？');
    await wrapper.get('.admin-confirm-primary').trigger('click');
    await flushPromises();
    expect(stub.moveCompetitionToRecycleBin).toHaveBeenCalledWith(7);
    wrapper.unmount();
  });

  it('does not impose a one-hundred-recipient limit on team awards', async () => {
    const stub = dashboardStub();
    stub.adminCompetitions.value = page([{
      ...competitionFixture(),
      participants: Array.from({ length: 101 }, (_, index) => ({
        id: index + 1,
        username: `player_${index + 1}`,
        displayName: `队员 ${index + 1}`,
        articles: [],
      })),
      awards: [],
    }]);
    const { wrapper } = mountPanel(stub);
    await flushPromises();
    await wrapper.get('.competition-detail-trigger').trigger('click');

    const recipients = wrapper.findAll('.competition-recipient-picker input');
    expect(recipients).toHaveLength(101);
    for (const recipient of recipients) await recipient.setValue(true);
    await wrapper.get('.competition-award-form').trigger('submit');
    await flushPromises();

    const request = stub.addCompetitionAward.mock.calls[0]?.[1];
    expect(request.recipientUsernames).toHaveLength(101);
    expect(wrapper.text()).not.toContain('最多选择 100 名获奖人');
    wrapper.unmount();
  });

  it('confirms restoring a retained competition and refreshes both lists', async () => {
    const { wrapper, stub } = mountPanel();
    await flushPromises();
    await wrapper.get('.competition-scope-tabs button:nth-child(2)').trigger('click');
    await flushPromises();
    await wrapper.get('.competition-restore-action').trigger('click');
    expect(document.body.textContent).toContain('恢复这场比赛？');
    await wrapper.get('.admin-confirm-primary').trigger('click');
    await flushPromises();

    expect(stub.restoreCompetition).toHaveBeenCalledWith(7);
    expect(stub.loadAdminCompetitionRecycleBin).toHaveBeenCalledTimes(2);
    expect(stub.loadAdminCompetitions).toHaveBeenCalledTimes(2);
    expect(wrapper.text()).toContain('比赛已恢复');
    wrapper.unmount();
  });
});

describe('competition dashboard list sequencing', () => {
  it('does not let older active or recycle-bin responses overwrite the latest page', async () => {
    const oldActiveRequest = deferred<CompetitionPageResponse>();
    const latestActiveRequest = deferred<CompetitionPageResponse>();
    const oldRecycleRequest = deferred<CompetitionPageResponse>();
    const latestRecycleRequest = deferred<CompetitionPageResponse>();
    vi.mocked(listCompetitions)
      .mockReset()
      .mockReturnValueOnce(oldActiveRequest.promise)
      .mockReturnValueOnce(latestActiveRequest.promise);
    vi.mocked(listCompetitionRecycleBin)
      .mockReset()
      .mockReturnValueOnce(oldRecycleRequest.promise)
      .mockReturnValueOnce(latestRecycleRequest.promise);
    const { dashboard, wrapper } = mountCompetitionDashboard();
    await flushPromises();

    const oldActivePage = page([{ ...competitionFixture(), id: 1, fullName: '旧筛选结果' }]);
    const latestActivePage = page([{ ...competitionFixture(), id: 2, fullName: '最新筛选结果' }]);
    const firstActive = dashboard.loadAdminCompetitions({ startYear: 2025, pageNum: 1, pageSize: 10 });
    const secondActive = dashboard.loadAdminCompetitions({ startYear: 2026, pageNum: 2, pageSize: 10 });
    latestActiveRequest.resolve(latestActivePage);
    await secondActive;
    oldActiveRequest.resolve(oldActivePage);
    await firstActive;
    expect(dashboard.adminCompetitions.value).toEqual(latestActivePage);

    const oldRecyclePage = page([{ ...competitionFixture(), id: 3, fullName: '旧回收站结果' }]);
    const latestRecyclePage = page([{ ...competitionFixture(), id: 4, fullName: '最新回收站结果' }]);
    const firstRecycle = dashboard.loadAdminCompetitionRecycleBin({ pageNum: 1, pageSize: 10 });
    const secondRecycle = dashboard.loadAdminCompetitionRecycleBin({ pageNum: 2, pageSize: 10 });
    latestRecycleRequest.resolve(latestRecyclePage);
    await secondRecycle;
    oldRecycleRequest.resolve(oldRecyclePage);
    await firstRecycle;
    expect(dashboard.adminCompetitionRecycleBin.value).toEqual(latestRecyclePage);
    wrapper.unmount();
  });
});
