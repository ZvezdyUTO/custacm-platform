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
      selectedUsername: ref(null), selectedOjName: ref(OJ_NAMES.CODEFORCES),
      trainingQuery: ref({ acceptedFromDateUtcPlus8: '2026-07-01', acceptedToDateUtcPlus8: '2026-07-12', minProblemRating: '', maxProblemRating: '' }),
      multiUserRows: ref([]), multiUserProgress: ref({ completed: 0, total: 0, active: false, failed: 0 }),
      acceptedSummary: ref(null), submissions: ref(null), firstAccepted: ref(null), problemKey: ref(''),
      problemSubmissions: ref(null), problemFirstAccepted: ref(null), submissionPage: ref(1), submissionLimit: ref(15),
      firstAcceptedPage: ref(1), firstAcceptedLimit: ref(15), problemSubmissionPage: ref(1), problemSubmissionLimit: ref(15),
      problemFirstAcceptedPage: ref(1), problemFirstAcceptedLimit: ref(15), applyTrainingQuery,
      refreshDashboard: vi.fn(), chooseUsername: vi.fn(), chooseOjName: vi.fn(), retryMultiUserSummary: vi.fn(),
      changeSubmissionPage: vi.fn(), changeFirstAcceptedPage: vi.fn(), changeProblemSubmissionPage: vi.fn(), changeProblemFirstAcceptedPage: vi.fn(),
    } as unknown as ReturnType<typeof usePlatformDashboard>,
    applyTrainingQuery,
  };
}

afterEach(() => vi.useRealTimers());

describe('training query automatic filters', () => {
  it('leaves the single-user selector empty until the user chooses a player', () => {
    const { dashboard } = dashboardFixture();
    dashboard.trainingUsers.value = [{ username: 'player-a', nickname: '队员甲', ojNames: [OJ_NAMES.CODEFORCES] }];
    const wrapper = mount(TrainingQueryPanel, { props: { dashboard, mode: 'single' } });

    const selector = wrapper.get('select[aria-label="队员"]');
    expect((selector.element as HTMLSelectElement).value).not.toBe('player-a');
    expect(selector.find('option:checked').text()).toBe('请选择队员');
    expect(dashboard.chooseUsername).not.toHaveBeenCalled();
  });

  it('removes the query button and applies a valid filter after a short debounce', async () => {
    vi.useFakeTimers();
    const { dashboard, applyTrainingQuery } = dashboardFixture();
    const wrapper = mount(TrainingQueryPanel, { props: { dashboard, mode: 'multiple' } });

    expect(wrapper.find('button[type="submit"]').exists()).toBe(false);
    expect(wrapper.get('.query-auto-refresh-hint').text()).toBe('筛选后自动刷新');
    await wrapper.findAll('input[type="number"]')[0]!.setValue('1200');
    await vi.advanceTimersByTimeAsync(249);
    expect(applyTrainingQuery).not.toHaveBeenCalled();
    await vi.advanceTimersByTimeAsync(1);
    expect(applyTrainingQuery).toHaveBeenCalledOnce();
    expect(applyTrainingQuery).toHaveBeenCalledWith(expect.objectContaining({ minProblemRating: '1200' }), 'multiple');
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
});
