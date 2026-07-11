// Author: huangbingrui.awa
import { mount } from '@vue/test-utils';
import { ref } from 'vue';
import { describe, expect, it, vi } from 'vitest';
import AdminUserManagementPanel from '../components/AdminUserManagementPanel.vue';
import CreateUsersPanel from '../components/CreateUsersPanel.vue';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard';
import type { AdminUserMutationResponse } from '../types';
import { parseBatchUserInput, parseCreateUserRows } from '../utils/adminUsers';

const jianglyUser: AdminUserMutationResponse = {
  user: { id: 99, username: 'ui-test-jiangly', nickname: '临时测试', email: '', avatar: '', role: 'ROLE_player', createTime: '2026-07-12T00:00:00', updateTime: '2026-07-12T00:00:00' },
  handles: { CODEFORCES: 'jiangly', ATCODER: 'jiangly' }, needCollect: true, generatedPassword: null, reloginRequired: false,
};

describe('Vue admin user import', () => {
  it('keeps role, OJ handles and collection state in batch rows', () => {
    const [request] = parseBatchUserInput('alice,队员甲,,,ROLE_player,,tourist,alice_atcoder,true');

    expect(request).toMatchObject({
      username: 'alice',
      role: 'ROLE_player',
      handles: { CODEFORCES: 'tourist', ATCODER: 'alice_atcoder' },
      needCollect: true,
    });
  });

  it('fills editable creation rows from the compact import format', () => {
    const [row] = parseCreateUserRows('ui-test-jiangly,临时测试,player,test-password,jiangly,jiangly');
    expect(row).toMatchObject({
      username: 'ui-test-jiangly', role: 'ROLE_player', password: 'test-password',
      codeforcesHandle: 'jiangly', atcoderHandle: 'jiangly',
    });
  });

  it('fills the creation form and submits both jiangly handles', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const batchCreateUsers = vi.fn().mockResolvedValue([jianglyUser]);
    const dashboard = { batchCreateUsers } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(CreateUsersPanel, { props: { dashboard } });

    await wrapper.get('textarea').setValue('ui-test-jiangly,临时测试,player,test-password,jiangly,jiangly');
    await wrapper.get('.import-fill-button').trigger('click');
    expect(wrapper.findAll('.create-user-row input').map((input) => (input.element as HTMLInputElement).value)).toEqual([
      'ui-test-jiangly', '临时测试', 'test-password', 'jiangly', 'jiangly',
    ]);
    await wrapper.get('form').trigger('submit');

    expect(batchCreateUsers).toHaveBeenCalledWith([expect.objectContaining({
      username: 'ui-test-jiangly', nickname: '临时测试', handles: { CODEFORCES: 'jiangly', ATCODER: 'jiangly' },
    })]);
  });

  it('modifies and deletes the temporary jiangly user', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const updated = { ...jianglyUser, user: { ...jianglyUser.user, nickname: '已修改' } };
    const patchUser = vi.fn().mockResolvedValue(updated);
    const retired = { ...updated, needCollect: false };
    const updateOjHandles = vi.fn().mockResolvedValue(retired);
    const deleteUser = vi.fn().mockResolvedValue(undefined);
    const dashboard = { adminUsers: ref([jianglyUser]), patchUser, updateOjHandles, deleteUser } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(AdminUserManagementPanel, { props: { dashboard, currentUsername: 'administrator' } });

    await wrapper.get('.reference-edit-button').trigger('click');
    await wrapper.get('input[aria-label="编辑 nickname"]').setValue('已修改');
    await wrapper.get('input[aria-label="编辑 needCollect"]').setValue(false);
    await wrapper.get('.admin-user-edit-form').trigger('submit');
    expect(patchUser).toHaveBeenCalledWith('ui-test-jiangly', expect.objectContaining({ nickname: '已修改' }));
    expect(updateOjHandles).toHaveBeenCalledWith('ui-test-jiangly', {
      handles: { CODEFORCES: 'jiangly', ATCODER: 'jiangly' }, needCollect: false,
    });
    expect(wrapper.get('.admin-notice').text()).toContain('用户修改已保存');

    await wrapper.get('.danger-button').trigger('click');
    expect(deleteUser).toHaveBeenCalledWith('ui-test-jiangly');
  });

  it('keeps identity layout inside a normal table cell when a row has multiple OJ handles', () => {
    const dashboard = { adminUsers: ref([jianglyUser]) } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(AdminUserManagementPanel, { props: { dashboard, currentUsername: 'administrator' } });
    const identity = wrapper.get('.reference-user-identity');

    expect(identity.element.tagName).toBe('DIV');
    expect(identity.element.parentElement?.tagName).toBe('TD');
    expect(wrapper.findAll('.reference-handle-list span')).toHaveLength(2);
  });

  it('identifies the row when an imported password is too short', async () => {
    const batchCreateUsers = vi.fn();
    const dashboard = { batchCreateUsers } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(CreateUsersPanel, { props: { dashboard } });

    await wrapper.get('textarea').setValue('240212224苏可航,1,admin,skh,Apeiron_24,Apeiron_24');
    await wrapper.get('.import-fill-button').trigger('click');
    await wrapper.get('form').trigger('submit');

    expect(wrapper.get('[role="alert"]').text()).toBe('第 1 行：初始密码长度需为 6 到 128 个字符。');
    expect(batchCreateUsers).not.toHaveBeenCalled();
  });
});
