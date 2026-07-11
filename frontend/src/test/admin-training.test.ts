// Author: huangbingrui.awa
import { mount } from '@vue/test-utils';
import { ref } from 'vue';
import { describe, expect, it, vi } from 'vitest';
import TrainingDataOpsPanel from '../components/TrainingDataOpsPanel.vue';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard';
import { collectionRequest } from '../utils/adminTraining';

describe('admin training collection', () => {
  it('always refreshes the warehouse after collection', () => {
    expect(collectionRequest(['ui-test-jiangly'], 1440, 'CODEFORCES')).toEqual({
      usernames: ['ui-test-jiangly'], lookbackHours: 1440, ojName: 'CODEFORCES', refreshWarehouse: true,
    });
  });

  it('collects the jiangly handle on both OJs and always refreshes the warehouse', async () => {
    const batchCollectSubmissions = vi.fn().mockResolvedValue(undefined);
    const dashboard = {
      adminUsers: ref([{
        user: { id: 99, username: 'ui-test-jiangly', nickname: '临时测试', email: '', avatar: '', role: 'ROLE_player', createTime: '2026-07-12T00:00:00', updateTime: '2026-07-12T00:00:00' },
        handles: { CODEFORCES: 'jiangly', ATCODER: 'jiangly' }, needCollect: true, generatedPassword: null, reloginRequired: false,
      }]),
      collectionJobs: ref([]), batchCollectSubmissions,
    } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(TrainingDataOpsPanel, { props: { dashboard } });

    await wrapper.get('.collection-member-row .primary-button').trigger('click');
    await wrapper.get('.collection-reference-controls select').setValue('ATCODER');
    await wrapper.get('.collection-member-row .primary-button').trigger('click');

    expect(batchCollectSubmissions).toHaveBeenNthCalledWith(1, {
      usernames: ['ui-test-jiangly'], lookbackHours: 1_000_000_000, ojName: 'CODEFORCES', refreshWarehouse: true,
    });
    expect(batchCollectSubmissions).toHaveBeenNthCalledWith(2, {
      usernames: ['ui-test-jiangly'], lookbackHours: 1_000_000_000, ojName: 'ATCODER', refreshWarehouse: true,
    });
  });

  it('accepts the numeric value emitted by the all-users lookback input', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const batchCollectSubmissions = vi.fn().mockResolvedValue(undefined);
    const dashboard = {
      adminUsers: ref([{
        user: { id: 99, username: 'ui-test-jiangly', nickname: '临时测试', email: '', avatar: '', role: 'ROLE_player', createTime: '2026-07-12T00:00:00', updateTime: '2026-07-12T00:00:00' },
        handles: { CODEFORCES: 'jiangly' }, needCollect: true, generatedPassword: null, reloginRequired: false,
      }]),
      collectionJobs: ref([]), batchCollectSubmissions,
    } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(TrainingDataOpsPanel, { props: { dashboard } });

    await wrapper.get('.collection-reference-controls input').setValue(14400);
    await wrapper.get('.collect-all-button').trigger('click');

    expect(batchCollectSubmissions).toHaveBeenCalledWith({
      usernames: ['ui-test-jiangly'], lookbackHours: 14400, ojName: 'CODEFORCES', refreshWarehouse: true,
    });
    expect(wrapper.find('[role="alert"]').exists()).toBe(false);
  });
});
