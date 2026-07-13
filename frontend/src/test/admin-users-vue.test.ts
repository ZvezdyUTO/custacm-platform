// Author: huangbingrui.awa
import { flushPromises, mount } from '@vue/test-utils';
import { ref } from 'vue';
import { describe, expect, it, vi } from 'vitest';
import AdminUserManagementPanel from '../components/AdminUserManagementPanel.vue';
import CreateUsersPanel from '../components/CreateUsersPanel.vue';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard';
import type { AdminUserMutationResponse } from '../types';
import { parseBatchUserInput, parseCreateUserRows } from '../utils/adminUsers';

const jianglyUser: AdminUserMutationResponse = {
  user: { id: 99, username: 'ui-test-jiangly', nickname: '临时测试', email: '', avatar: '/api/image/jiangly.png', role: 'ROLE_player', createTime: '2026-07-12T00:00:00', updateTime: '2026-07-12T00:00:00' },
  handles: { CODEFORCES: 'jiangly', ATCODER: 'jiangly' }, needCollect: true, collectionStates: {}, generatedPassword: null, reloginRequired: false,
};

describe('Vue admin user import', () => {
  it('keeps role, OJ handles and collection state in batch rows', () => {
    const [request] = parseBatchUserInput('alice,队员甲,,ROLE_player,,tourist,alice_atcoder,true');

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
    const batchCreateUsers = vi.fn().mockResolvedValue([jianglyUser]);
    const dashboard = { batchCreateUsers } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(CreateUsersPanel, { props: { dashboard } });

    await wrapper.get('textarea').setValue('ui-test-jiangly,临时测试,player,test-password,jiangly,jiangly');
    await wrapper.get('.import-fill-button').trigger('click');
    expect(wrapper.findAll('.create-user-row input').map((input) => (input.element as HTMLInputElement).value)).toEqual([
      'ui-test-jiangly', '临时测试', 'test-password', 'jiangly', 'jiangly',
    ]);
    await wrapper.get('form').trigger('submit');

    expect(wrapper.get('[role="alertdialog"]').text()).toContain('即将创建 1 个账号');
    expect(batchCreateUsers).not.toHaveBeenCalled();
    await wrapper.get('.admin-confirm-primary').trigger('click');
    await flushPromises();

    expect(batchCreateUsers).toHaveBeenCalledWith([expect.objectContaining({
      username: 'ui-test-jiangly', nickname: '临时测试', handles: { CODEFORCES: 'jiangly', ATCODER: 'jiangly' },
    })]);
  });

  it('modifies and deletes the temporary jiangly user', async () => {
    const retired = { ...jianglyUser, user: { ...jianglyUser.user, nickname: '已修改' }, needCollect: false };
    const updateUser = vi.fn().mockResolvedValue(retired);
    const deleteUser = vi.fn().mockResolvedValue(undefined);
    const dashboard = { adminUsers: ref([jianglyUser]), updateUser, deleteUser } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(AdminUserManagementPanel, { props: { dashboard, currentUsername: 'administrator' } });

    await wrapper.get('.reference-edit-button').trigger('click');
    expect(wrapper.find('input[aria-label="编辑 avatar"]').exists()).toBe(false);
    await wrapper.get('input[aria-label="编辑 nickname"]').setValue('已修改');
    await wrapper.get('input[aria-label="编辑 needCollect"]').setValue(false);
    await wrapper.get('.admin-user-edit-form').trigger('submit');
    expect(updateUser).toHaveBeenCalledWith('ui-test-jiangly', expect.objectContaining({
      nickname: '已修改',
      handles: { CODEFORCES: 'jiangly', ATCODER: 'jiangly' }, needCollect: false,
    }));
    expect(wrapper.get('.admin-notice').text()).toContain('用户修改已保存');
    expect(wrapper.find('.admin-user-edit-form').exists()).toBe(false);

    await wrapper.get('.reference-edit-button').trigger('click');
    await wrapper.get('.danger-button').trigger('click');
    expect(wrapper.get('[role="alertdialog"]').text()).toContain('ui-test-jiangly · 临时测试');
    expect(wrapper.get('[role="alertdialog"]').text()).toContain('训练数据会被清理');
    expect(deleteUser).not.toHaveBeenCalled();
    await wrapper.get('.confirm-user-delete-button').trigger('click');
    expect(deleteUser).toHaveBeenCalledWith('ui-test-jiangly');
  });

  it('persists collection status even when the user has no OJ handles', async () => {
    const noHandleUser = { ...jianglyUser, handles: {}, needCollect: null };
    const retired = { ...noHandleUser, needCollect: false };
    const updateUser = vi.fn().mockResolvedValue(retired);
    const dashboard = { adminUsers: ref([noHandleUser]), updateUser } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(AdminUserManagementPanel, { props: { dashboard, currentUsername: 'administrator' } });

    await wrapper.get('.reference-edit-button').trigger('click');
    const toggle = wrapper.get('input[aria-label="编辑 needCollect"]');
    await toggle.setValue(false);
    await wrapper.get('.admin-user-edit-form').trigger('submit');

    expect(updateUser).toHaveBeenCalledWith('ui-test-jiangly', expect.objectContaining({ handles: {}, needCollect: false }));
    expect(wrapper.find('.admin-user-edit-form').exists()).toBe(false);
  });

  it('requires a danger confirmation before replacing an OJ handle', async () => {
    const replaced = { ...jianglyUser, handles: { ...jianglyUser.handles, CODEFORCES: 'Benq' } };
    const updateUser = vi.fn().mockResolvedValue(replaced);
    const dashboard = { adminUsers: ref([jianglyUser]), updateUser } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(AdminUserManagementPanel, { props: { dashboard, currentUsername: 'administrator' } });

    await wrapper.get('.reference-edit-button').trigger('click');
    await wrapper.get('input[aria-label="编辑 Codeforces handle"]').setValue('Benq');
    await wrapper.get('.admin-user-edit-form').trigger('submit');

    expect(updateUser).not.toHaveBeenCalled();
    expect(wrapper.get('[role="alertdialog"]').text()).toContain('高危操作');
    expect(wrapper.get('[role="alertdialog"]').text()).toContain('jiangly→Benq');
    expect(wrapper.get('[role="alertdialog"]').text()).toContain('全部 ODS 与数仓训练记录');

    await wrapper.get('.confirm-user-delete-button').trigger('click');
    await flushPromises();
    expect(updateUser).toHaveBeenCalledWith('ui-test-jiangly', expect.objectContaining({
      handles: { CODEFORCES: 'Benq', ATCODER: 'jiangly' }, needCollect: true,
    }));
    expect(wrapper.find('.admin-user-edit-form').exists()).toBe(false);
  });

  it('keeps the edit form expanded when saving fails', async () => {
    const updateUser = vi.fn().mockRejectedValue(new Error('保存失败'));
    const dashboard = { adminUsers: ref([jianglyUser]), updateUser } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(AdminUserManagementPanel, { props: { dashboard, currentUsername: 'administrator' } });

    await wrapper.get('.reference-edit-button').trigger('click');
    await wrapper.get('input[aria-label="编辑 nickname"]').setValue('无法保存的修改');
    await wrapper.get('.admin-user-edit-form').trigger('submit');

    expect(wrapper.get('.form-error').text()).toContain('保存失败');
    expect(wrapper.find('.admin-user-edit-form').exists()).toBe(true);
    expect((wrapper.get('input[aria-label="编辑 nickname"]').element as HTMLInputElement).value).toBe('无法保存的修改');
  });

  it('keeps avatar, account details and OJ handles together in the wide profile cell', () => {
    const dashboard = { adminUsers: ref([jianglyUser]) } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(AdminUserManagementPanel, { props: { dashboard, currentUsername: 'administrator' } });
    const identity = wrapper.get('.reference-user-identity');
    const profile = wrapper.get('.reference-user-profile');

    expect(identity.element.tagName).toBe('DIV');
    expect(profile.element.parentElement?.tagName).toBe('TD');
    expect(identity.element.parentElement).toBe(profile.element);
    expect(wrapper.get('.reference-user-avatar').attributes('src')).toBe('/api/image/jiangly.png');
    expect(wrapper.get('.reference-user-name').text()).toContain('ui-test-jiangly');
    expect(wrapper.findAll('thead th')).toHaveLength(4);
    expect(wrapper.findAll('.reference-handle-list span')).toHaveLength(2);
    expect(profile.find('.reference-handle-list').exists()).toBe(true);
  });

  it('filters the loaded user list by a username substring', async () => {
    const secondUser = {
      ...jianglyUser,
      user: { ...jianglyUser.user, id: 100, username: 'player-alice', nickname: 'Alice' },
    };
    const dashboard = { adminUsers: ref([jianglyUser, secondUser]) } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(AdminUserManagementPanel, { props: { dashboard, currentUsername: 'administrator' } });

    await wrapper.get('input[aria-label="查询 username"]').setValue('JIANG');

    expect(wrapper.findAll('.reference-user-table tbody > tr:not(.admin-user-edit-row)')).toHaveLength(1);
    expect(wrapper.get('.reference-user-name').text()).toContain('ui-test-jiangly');
    expect(wrapper.get('.reference-count').text()).toContain('1个账号');
  });

  it('renders root as a protected administrator without player status or handles', async () => {
    const root = { ...jianglyUser, user: { ...jianglyUser.user, username: 'root', role: 'ROLE_admin' as const }, handles: {}, needCollect: null };
    const updateUser = vi.fn().mockResolvedValue(root);
    const dashboard = { adminUsers: ref([root]), updateUser } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(AdminUserManagementPanel, { props: { dashboard, currentUsername: 'root' } });

    expect(wrapper.get('.reference-status-list').text()).toBe('管理员');
    await wrapper.get('.reference-edit-button').trigger('click');
    expect(wrapper.get('input[aria-label="编辑 username"]').attributes()).toHaveProperty('disabled');
    expect(wrapper.get('select[aria-label="编辑 role"]').attributes()).toHaveProperty('disabled');
    expect(wrapper.find('input[aria-label="编辑 Codeforces handle"]').exists()).toBe(false);
    expect(wrapper.find('.danger-button').exists()).toBe(false);
    await wrapper.get('.admin-user-edit-form').trigger('submit');
    expect(updateUser).toHaveBeenCalledWith('root', expect.not.objectContaining({ handles: expect.anything() }));
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
