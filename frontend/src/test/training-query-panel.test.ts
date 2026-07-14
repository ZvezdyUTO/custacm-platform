// Author: huangbingrui.awa
import { mount } from '@vue/test-utils';
import { computed, ref } from 'vue';
import { afterEach, describe, expect, it, vi } from 'vitest';
import TrainingQueryPanel from '../components/TrainingQueryPanel.vue';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard';
import { OJ_NAMES } from '../types';

function dashboardFixture() {
  const applyTrainingQuery = vi.fn().mockResolvedValue(undefined);
  return {
    dashboard: {
      status: ref('ready'), trainingUsers: ref([]), selectedTrainingUser: computed(() => null),
      includeRetiredUsers: ref(false),
      selectedUsername: ref(null), selectedOjName: ref(OJ_NAMES.CODEFORCES),
      trainingQuery: ref({ acceptedFromDateUtcPlus8: '2026-07-01', acceptedToDateUtcPlus8: '2026-07-12', minProblemRating: '', maxProblemRating: '' }),
      multiUserRows: ref([]), multiUserProgress: ref({ completed: 0, total: 0, active: false, failed: 0 }),
      acceptedSummary: ref(null), submissions: ref(null), firstAccepted: ref(null), problemKey: ref(''),
      problemSubmissions: ref(null), problemFirstAccepted: ref(null), submissionPage: ref(1), submissionLimit: ref(15),
      firstAcceptedPage: ref(1), firstAcceptedLimit: ref(15), problemSubmissionPage: ref(1), problemSubmissionLimit: ref(15),
      problemFirstAcceptedPage: ref(1), problemFirstAcceptedLimit: ref(15), applyTrainingQuery,
      refreshDashboard: vi.fn(), chooseUsername: vi.fn(), setIncludeRetiredUsers: vi.fn(), chooseOjName: vi.fn(), retryMultiUserSummary: vi.fn(),
      changeSubmissionPage: vi.fn(), changeFirstAcceptedPage: vi.fn(), changeProblemSubmissionPage: vi.fn(), changeProblemFirstAcceptedPage: vi.fn(),
    } as unknown as ReturnType<typeof usePlatformDashboard>,
    applyTrainingQuery,
  };
}

afterEach(() => vi.useRealTimers());

describe('training query automatic filters', () => {
  it('leaves the single-user search empty until the user chooses a player', () => {
    const { dashboard } = dashboardFixture();
    dashboard.trainingUsers.value = [{ username: 'player-a', nickname: '队员甲', ojNames: [OJ_NAMES.CODEFORCES] }];
    const wrapper = mount(TrainingQueryPanel, { props: { dashboard, mode: 'single' } });

    const search = wrapper.get('input[aria-label="队员"]');
    expect((search.element as HTMLInputElement).value).toBe('');
    expect(search.attributes('placeholder')).toBe('输入用户名或姓名');
    expect(search.attributes('role')).toBe('combobox');
    expect(dashboard.chooseUsername).not.toHaveBeenCalled();
  });

  it('matches username or nickname substrings and selects from a descending result list', async () => {
    const { dashboard } = dashboardFixture();
    dashboard.trainingUsers.value = [
      { username: '25000002', nickname: '队员乙', ojNames: [OJ_NAMES.CODEFORCES] },
      { username: '25000010', nickname: '队员甲', ojNames: [OJ_NAMES.CODEFORCES] },
    ];
    const wrapper = mount(TrainingQueryPanel, { props: { dashboard, mode: 'single' } });

    const search = wrapper.get('input[aria-label="队员"]');
    await search.setValue('250000');
    expect(wrapper.findAll('.player-search-options button').map((option) => option.text())).toEqual([
      '25000010队员甲',
      '25000002队员乙',
    ]);

    await search.setValue('队员乙');
    expect(wrapper.findAll('.player-search-options button')).toHaveLength(1);
    await wrapper.get('.player-search-options button').trigger('mousedown');
    expect(dashboard.chooseUsername).toHaveBeenCalledWith('25000002');
    expect((search.element as HTMLInputElement).value).toBe('25000002 · 队员乙');
    expect(wrapper.find('.player-search-options').exists()).toBe(false);
  });

  it('queries the first highlighted player when the search icon is clicked', async () => {
    const { dashboard } = dashboardFixture();
    dashboard.trainingUsers.value = [
      { username: '25000002', nickname: '队员乙', ojNames: [OJ_NAMES.CODEFORCES] },
      { username: '25000010', nickname: '队员甲', ojNames: [OJ_NAMES.CODEFORCES] },
    ];
    const wrapper = mount(TrainingQueryPanel, { props: { dashboard, mode: 'single' } });

    await wrapper.get('input[aria-label="队员"]').setValue('250000');
    const submit = wrapper.get('button[aria-label="搜索队员"]');
    await submit.trigger('mousedown');
    await submit.trigger('click');

    expect(dashboard.chooseUsername).toHaveBeenCalledWith('25000010');
  });

  it('uses a site-style OJ menu and applies the selected platform', async () => {
    const { dashboard } = dashboardFixture();
    const wrapper = mount(TrainingQueryPanel, { props: { dashboard, mode: 'multiple' } });

    expect(wrapper.find('select[aria-label="选择 OJ"]').exists()).toBe(false);
    const trigger = wrapper.get('.oj-select-trigger');
    expect(trigger.text()).toBe('Codeforces');
    await trigger.trigger('click');
    expect(wrapper.findAll('.oj-select-options button').map((option) => option.text())).toEqual(['Codeforces', 'AtCoder']);
    await wrapper.findAll('.oj-select-options button')[1]!.trigger('mousedown');

    expect(dashboard.selectedOjName.value).toBe(OJ_NAMES.ATCODER);
    expect(dashboard.chooseOjName).toHaveBeenCalledWith(OJ_NAMES.ATCODER);
    expect(wrapper.find('.oj-select-options').exists()).toBe(false);
  });

  it('keeps the retired-user toggle only on the multiple-user view', async () => {
    const multiple = dashboardFixture().dashboard;
    const multipleWrapper = mount(TrainingQueryPanel, { props: { dashboard: multiple, mode: 'multiple' } });
    const multipleToggle = multipleWrapper.get('label.query-retired-toggle');
    expect(multipleToggle.text()).toBe('显示退役队员');
    await multipleToggle.get('input').setValue(true);
    expect(multiple.setIncludeRetiredUsers).toHaveBeenCalledWith(true);

    const single = dashboardFixture().dashboard;
    const singleWrapper = mount(TrainingQueryPanel, { props: { dashboard: single, mode: 'single' } });
    expect(singleWrapper.find('label.query-retired-toggle').exists()).toBe(false);
    expect(single.setIncludeRetiredUsers).not.toHaveBeenCalled();
  });

  it('removes the query button and applies a valid filter after a short debounce', async () => {
    vi.useFakeTimers();
    const { dashboard, applyTrainingQuery } = dashboardFixture();
    const wrapper = mount(TrainingQueryPanel, { props: { dashboard, mode: 'multiple' } });

    expect(wrapper.find('button[type="submit"]').exists()).toBe(false);
    expect(wrapper.get('.query-auto-refresh-hint').text()).toBe('筛选后自动刷新');
    expect(wrapper.get('.query-filter-hint').text()).toBe('筛选后自动刷新 · 日期或 Rating 的任一边界留空时，不限制对应方向的范围。');
    await wrapper.findAll('input[type="number"]')[0]!.setValue('1200');
    await vi.advanceTimersByTimeAsync(249);
    expect(applyTrainingQuery).not.toHaveBeenCalled();
    await vi.advanceTimersByTimeAsync(1);
    expect(applyTrainingQuery).toHaveBeenCalledOnce();
    expect(applyTrainingQuery).toHaveBeenCalledWith(expect.objectContaining({ minProblemRating: '1200' }), 'multiple');
  });

  it('shows the training guidance without a redundant multi-user heading', () => {
    const { dashboard } = dashboardFixture();
    const wrapper = mount(TrainingQueryPanel, { props: { dashboard, mode: 'multiple' } });

    expect(wrapper.get('.multi-summary-panel header p').text()).toBe('思考质量 > 难度 > 数量');
    expect(wrapper.find('.multi-summary-panel h2').exists()).toBe(false);
  });

  it('waits for an invalid range to be corrected before refreshing', async () => {
    vi.useFakeTimers();
    const { dashboard, applyTrainingQuery } = dashboardFixture();
    const wrapper = mount(TrainingQueryPanel, { props: { dashboard, mode: 'multiple' } });
    const ratingInputs = wrapper.findAll('input[type="number"]');

    await ratingInputs[0]!.setValue('1800');
    await ratingInputs[1]!.setValue('1200');
    await vi.advanceTimersByTimeAsync(300);
    expect(wrapper.get('[role="alert"]').text()).toContain('最低 rating 不能大于最高 rating');
    expect(applyTrainingQuery).not.toHaveBeenCalled();

    await ratingInputs[1]!.setValue('2000');
    await vi.advanceTimersByTimeAsync(250);
    expect(applyTrainingQuery).toHaveBeenCalledOnce();
  });

  it('keeps an explicit darker query button for problem lookup', async () => {
    vi.useFakeTimers();
    const { dashboard, applyTrainingQuery } = dashboardFixture();
    const wrapper = mount(TrainingQueryPanel, { props: { dashboard, mode: 'problem' } });

    await wrapper.get('input[placeholder="例如 2242:C"]').setValue('2242:C');
    await vi.advanceTimersByTimeAsync(300);
    expect(applyTrainingQuery).not.toHaveBeenCalled();
    expect(wrapper.get('.query-problem-apply-button').text()).toBe('查询');
    expect(wrapper.get('.query-form').classes()).toContain('problem-query-form');
    expect(wrapper.get('.query-filter-hint').text()).toBe('日期的任一边界留空时，不限制对应方向的范围。');

    await wrapper.get('.query-form').trigger('submit');
    expect(applyTrainingQuery).toHaveBeenCalledOnce();
  });

  it('uses five vivid AtCoder rating tones in the multi-user table', () => {
    const { dashboard } = dashboardFixture();
    dashboard.selectedOjName.value = OJ_NAMES.ATCODER;
    dashboard.multiUserRows.value = [{
      user: { username: 'player-a', nickname: '队员甲', ojNames: [OJ_NAMES.ATCODER] }, status: 'ready', message: null,
      summary: { username: 'player-a', authorHandle: 'atcoder', totalAcceptedProblemCount: 5, ratingCounts: [
        { problemRating: '0', acceptedProblemCount: 1 }, { problemRating: '400', acceptedProblemCount: 1 },
        { problemRating: '800', acceptedProblemCount: 1 }, { problemRating: '1200', acceptedProblemCount: 1 },
        { problemRating: '1600', acceptedProblemCount: 1 },
      ] },
    }];

    const wrapper = mount(TrainingQueryPanel, { props: { dashboard, mode: 'multiple' } });
    expect(wrapper.findAll('.auto-summary-rating-col').map((item) => item.classes())).toEqual([
      expect.arrayContaining(['rating-tone-gray']), expect.arrayContaining(['rating-tone-green']),
      expect.arrayContaining(['rating-tone-blue']), expect.arrayContaining(['rating-tone-yellow']),
      expect.arrayContaining(['rating-tone-red']),
    ]);
    expect(wrapper.findAll('.auto-rating-count')).toHaveLength(5);
  });

  it('gives numerous training columns their own horizontal scroll width', () => {
    const { dashboard } = dashboardFixture();
    dashboard.multiUserRows.value = [{
      user: { username: '25000001', nickname: '队员甲', ojNames: [OJ_NAMES.CODEFORCES] }, status: 'ready', message: null,
      summary: {
        username: '25000001', authorHandle: 'player-a', totalAcceptedProblemCount: 20,
        ratingCounts: Array.from({ length: 20 }, (_, index) => ({ problemRating: String(800 + index * 100), acceptedProblemCount: 1 })),
      },
    }];

    const wrapper = mount(TrainingQueryPanel, { props: { dashboard, mode: 'multiple' } });
    expect(wrapper.get('.auto-summary-table').attributes('style')).toContain('min-width: 1396px');
    expect(wrapper.get('.auto-summary-player-cell').text()).toContain('25000001');
  });
});
