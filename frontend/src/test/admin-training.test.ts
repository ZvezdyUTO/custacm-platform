// Author: huangbingrui.awa
import { flushPromises, mount } from '@vue/test-utils';
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
    let finishCodeforces!: () => void;
    const codeforcesJob = new Promise<void>((resolve) => { finishCodeforces = resolve; });
    const batchCollectSubmissions = vi.fn()
      .mockImplementationOnce(() => codeforcesJob)
      .mockRejectedValueOnce(new Error('AtCoder 采集失败'));
    const dashboard = {
      adminUsers: ref([{
        user: { id: 99, username: 'ui-test-jiangly', nickname: '临时测试', email: '', avatar: '', role: 'ROLE_player', createTime: '2026-07-12T00:00:00', updateTime: '2026-07-12T00:00:00' },
        handles: { CODEFORCES: 'jiangly', ATCODER: 'jiangly' }, needCollect: true,
        collectionStates: {
          CODEFORCES: { lastCollectedAt: '2026-07-12T08:30:00Z' },
          ATCODER: { lastCollectedAt: null },
        },
        generatedPassword: null, reloginRequired: false,
      }]),
      collectionJobs: ref([]), batchCollectSubmissions,
    } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(TrainingDataOpsPanel, { props: { dashboard } });

    expect(wrapper.get('.collection-member-identity strong').text()).toBe('ui-test-jiangly·临时测试');
    expect(wrapper.get('.collection-member-state').text()).toContain('已建立增量采集游标');
    expect(wrapper.get('.collection-member-state').text()).toContain('最近成功窗口结束：2026/7/12 16:30');

    await wrapper.get('.collection-member-row .primary-button').trigger('click');
    expect(wrapper.get('[role="alertdialog"]').text()).toContain('ui-test-jiangly · 临时测试');
    expect(batchCollectSubmissions).not.toHaveBeenCalled();
    await wrapper.get('.confirm-collection-button').trigger('click');
    await wrapper.get('.collection-reference-controls select').setValue('ATCODER');
    expect(wrapper.get('.collection-member-state').text()).toContain('首次采集将抓取全部历史');
    expect(wrapper.get('.collection-member-state').text()).toContain('最近成功窗口结束：尚无记录');
    await wrapper.get('.collection-member-row .primary-button').trigger('click');
    await wrapper.get('.confirm-collection-button').trigger('click');

    expect(batchCollectSubmissions).toHaveBeenNthCalledWith(1, {
      usernames: ['ui-test-jiangly'], lookbackHours: 1440, ojName: 'CODEFORCES', refreshWarehouse: true,
    });
    expect(batchCollectSubmissions).toHaveBeenNthCalledWith(2, {
      usernames: ['ui-test-jiangly'], lookbackHours: 1440, ojName: 'ATCODER', refreshWarehouse: true,
    });
    await flushPromises();
    expect(wrapper.get('[role="alert"]').text()).toBe('AtCoder 采集失败');

    await wrapper.get('.collection-reference-controls select').setValue('CODEFORCES');
    expect(wrapper.find('[role="alert"]').exists()).toBe(false);

    finishCodeforces();
    await flushPromises();
  });

  it('accepts the numeric value emitted by the all-users rollback input', async () => {
    const batchCollectSubmissions = vi.fn().mockResolvedValue(undefined);
    const dashboard = {
      adminUsers: ref([{
        user: { id: 99, username: 'ui-test-jiangly', nickname: '临时测试', email: '', avatar: '', role: 'ROLE_player', createTime: '2026-07-12T00:00:00', updateTime: '2026-07-12T00:00:00' },
        handles: { CODEFORCES: 'jiangly' }, needCollect: true, collectionStates: {}, generatedPassword: null, reloginRequired: false,
      }]),
      collectionJobs: ref([]), batchCollectSubmissions,
    } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(TrainingDataOpsPanel, { props: { dashboard } });

    await wrapper.get('.collection-reference-controls input').setValue(14400);
    await wrapper.get('.collect-all-button').trigger('click');

    expect(wrapper.get('[role="alertdialog"]').text()).toContain('全部 1 名队员');
    expect(wrapper.get('[role="alertdialog"]').text()).toContain('向前倒退 14400 小时');
    expect(wrapper.get('[role="alertdialog"]').text()).toContain('尚无成功记录的账号会采集全部历史数据');
    expect(batchCollectSubmissions).not.toHaveBeenCalled();
    await wrapper.get('.confirm-collection-button').trigger('click');

    expect(batchCollectSubmissions).toHaveBeenCalledWith({
      usernames: ['ui-test-jiangly'], lookbackHours: 14400, ojName: 'CODEFORCES', refreshWarehouse: true,
    });
    expect(wrapper.find('[role="alert"]').exists()).toBe(false);
  });
});
