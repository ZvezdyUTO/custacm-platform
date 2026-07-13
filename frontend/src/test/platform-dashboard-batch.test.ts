// Author: huangbingrui.awa
import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent, h, ref } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getAcceptedSummaries, listTrainingUsers } from '../api/training';
import { usePlatformDashboard } from '../composables/usePlatformDashboard';
import { OJ_NAMES, type CurrentUser, type TrainingQueryMode } from '../types';
import type { AdminSection } from '../routing';

vi.mock('../api/training', async (importOriginal) => ({
  ...await importOriginal<typeof import('../api/training')>(),
  getAcceptedSummaries: vi.fn(),
  listTrainingUsers: vi.fn(),
}));

const users = [
  { username: 'player-a', nickname: '队员甲', ojNames: [OJ_NAMES.CODEFORCES] },
  { username: 'player-b', nickname: '队员乙', ojNames: [OJ_NAMES.CODEFORCES] },
];

function mountDashboard() {
  let dashboard!: ReturnType<typeof usePlatformDashboard>;
  const wrapper = mount(defineComponent({
    setup() {
      dashboard = usePlatformDashboard({
        token: ref('token'),
        user: ref<CurrentUser>({
          username: 'player-a', nickname: '队员甲', avatar: '', email: '', role: 'ROLE_player',
        }),
        mode: ref<TrainingQueryMode>('multiple'),
        adminSection: ref<AdminSection | null>(null),
        onUnauthorized: vi.fn(),
      });
      return () => h('div');
    },
  }));
  return { dashboard, wrapper };
}

describe('multi-user batch summary state', () => {
  beforeEach(() => {
    vi.mocked(listTrainingUsers).mockResolvedValue(users);
  });

  it('finishes progress and exposes retryable rows when the batch request fails', async () => {
    vi.mocked(getAcceptedSummaries).mockRejectedValue(new Error('批量查询失败'));
    const { dashboard, wrapper } = mountDashboard();

    await flushPromises();

    expect(dashboard.status.value).toBe('error');
    expect(dashboard.multiUserProgress.value).toEqual({ completed: 2, total: 2, active: false, failed: 2 });
    expect(dashboard.multiUserRows.value).toEqual(users.map((user) => ({
      user, status: 'error', summary: null, message: '批量查询失败',
    })));
    wrapper.unmount();
  });

  it('marks a missing backend result as an error instead of forging a zero summary', async () => {
    vi.mocked(getAcceptedSummaries).mockResolvedValue([{
      username: 'player-a', authorHandle: 'tourist', totalAcceptedProblemCount: 3, ratingCounts: [],
    }]);
    const { dashboard, wrapper } = mountDashboard();

    await flushPromises();

    expect(dashboard.status.value).toBe('ready');
    expect(dashboard.multiUserProgress.value).toEqual({ completed: 2, total: 2, active: false, failed: 1 });
    expect(dashboard.multiUserRows.value.find((row) => row.user.username === 'player-b')).toEqual({
      user: users[1], status: 'error', summary: null, message: '批量汇总结果缺失，请重试。',
    });
    expect(dashboard.errorMessage.value).toBe('1 名队员的汇总结果缺失，请重试。');
    wrapper.unmount();
  });
});
