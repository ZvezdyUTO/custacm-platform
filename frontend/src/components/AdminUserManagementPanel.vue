<template>
  <section class="admin-user-management-panel admin-reference-page" aria-label="管理用户信息">
    <header class="reference-page-header">
      <span class="reference-page-icon"><UsersRound :size="22" /></span>
      <div><h2>管理用户信息</h2><p>按 username 查询账号；在列表中直接修改角色、密码、OJ handle 和现役状态。</p></div>
    </header>
    <p v-if="notice" class="admin-notice" role="status">{{ notice }}</p>
    <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>

    <section v-if="passwords.length" class="one-time-password-result" aria-label="一次性密码结果" role="status">
      <header><div><strong>一次性密码</strong><span>请立即复制并安全交付；关闭后无法再次查看。</span></div><button v-if="!pendingRelogin" class="icon-button" type="button" @click="passwords = []"><X :size="16" /></button></header>
      <ul><li v-for="item in passwords" :key="item.username"><strong>{{ item.username }}</strong><code>{{ item.password }}</code><button class="secondary-button compact" type="button" @click="copyPassword(item)"><Copy :size="14" />复制</button></li></ul>
      <div v-if="pendingRelogin" class="one-time-password-actions"><button class="primary-button" type="button" @click="confirmedRelogin">我已保存，重新登录</button></div>
    </section>

    <label class="admin-user-search"><span>查询 username</span><span class="admin-user-search-control"><Search :size="16" /><input v-model="usernameQuery" aria-label="查询 username" placeholder="输入 username 子串"></span></label>
    <div class="reference-count"><strong>{{ filteredUsers.length }}</strong><span>个账号</span></div>
    <div class="table-shell admin-user-table-shell reference-user-table-shell">
      <table class="admin-user-table reference-user-table" aria-label="用户列表">
        <thead><tr><th>个人信息</th><th>角色 / 状态</th><th>更新时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-if="!filteredUsers.length"><td class="admin-user-empty" colspan="4">{{ usernameQuery.trim() ? '没有匹配的用户' : '暂无用户' }}</td></tr>
          <template v-for="item in filteredUsers" :key="item.user.id || item.user.username">
            <tr :class="{ 'is-expanded': expandedUsername === item.user.username }">
              <td><div class="reference-user-profile">
                <img class="reference-user-avatar" :src="item.user.avatar || '/img/default-avatar.jpg'" :alt="`${item.user.nickname || item.user.username} 的头像`">
                <div class="reference-user-identity">
                  <div class="reference-user-name"><strong>{{ item.user.nickname || '未设置姓名' }}</strong><span>{{ item.user.username }}</span></div>
                  <small>{{ item.user.email || '未设置邮箱' }}</small>
                  <div class="reference-handle-list"><span v-for="handle in handleEntries(item)" :key="handle.oj"><b>{{ handle.label }}</b>{{ handle.value }}</span><em v-if="!handleEntries(item).length">未绑定 OJ handle</em></div>
                </div>
              </div></td>
              <td><div class="reference-status-list"><span class="role-chip">{{ item.user.role === 'ROLE_admin' ? '管理员' : '队员' }}</span><span v-if="item.user.username !== 'root'" :class="['collect-chip', item.needCollect === false ? 'is-retired' : '']">{{ item.needCollect === false ? '已退役' : '现役队员' }}</span></div></td>
              <td>{{ formatTime(item.user.updateTime) }}</td>
              <td><button class="secondary-button reference-edit-button" type="button" @click="edit(item)"><UserRoundCog :size="16" />编辑</button></td>
            </tr>
            <tr v-if="expandedUsername === item.user.username && editForm" class="admin-user-edit-row"><td colspan="4">
              <form class="admin-user-edit-form" @submit.prevent="saveAccount"><div class="admin-user-edit-grid"><UserFields v-model="editForm" label-prefix="编辑" :system-account="expandedUsername === 'root'" /></div>
                <div class="admin-user-edit-actions"><button class="primary-button" type="submit"><Save :size="16" />保存修改</button><button v-if="expandedUsername !== 'root'" class="danger-button subtle" type="button" @click="removeUser"><Trash2 :size="16" />删除用户</button></div>
              </form>
            </td></tr>
          </template>
        </tbody>
      </table>
    </div>
    <div v-if="pendingDeleteUser" class="user-delete-backdrop" role="presentation" @click.self="pendingDeleteUser = null">
      <section class="user-delete-dialog" role="alertdialog" aria-modal="true" aria-labelledby="user-delete-title" aria-describedby="user-delete-description">
        <span class="user-delete-warning-icon"><TriangleAlert :size="26" /></span>
        <div><h3 id="user-delete-title">确认永久删除这个用户？</h3><p id="user-delete-description">你即将删除 <strong>{{ pendingDeleteUser.username }}<template v-if="pendingDeleteUser.nickname"> · {{ pendingDeleteUser.nickname }}</template></strong>。该用户的 OJ 绑定和对应训练数据会被清理，历史评论会匿名化；此操作无法恢复。</p></div>
        <div class="user-delete-dialog-actions"><button type="button" :disabled="deleteBusy" @click="pendingDeleteUser = null">取消</button><button class="confirm-user-delete-button" type="button" :disabled="deleteBusy" @click="confirmRemoveUser"><Trash2 :size="16" />{{ deleteBusy ? '正在删除' : '确认永久删除' }}</button></div>
      </section>
    </div>
    <div v-if="pendingHandleReplacement" class="user-delete-backdrop" role="presentation" @click.self="pendingHandleReplacement = null">
      <section class="user-delete-dialog" role="alertdialog" aria-modal="true" aria-labelledby="handle-replace-title" aria-describedby="handle-replace-description">
        <span class="user-delete-warning-icon"><TriangleAlert :size="26" /></span>
        <div><h3 id="handle-replace-title">确认更换 OJ handle？</h3><p id="handle-replace-description">更换 handle 是高危操作。系统将永久删除 <strong>{{ pendingHandleReplacement.username }}</strong> 在对应 OJ 下的全部 ODS 与数仓训练记录，并清除旧绑定和采集状态后绑定新 handle。</p>
          <ul class="handle-replacement-list"><li v-for="change in pendingHandleReplacement.changes" :key="change.ojName"><strong>{{ OJ_LABELS[change.ojName] }}</strong><code>{{ change.oldHandle }}</code><span>→</span><code>{{ change.newHandle }}</code></li></ul>
        </div>
        <div class="user-delete-dialog-actions"><button type="button" :disabled="handleReplaceBusy" @click="pendingHandleReplacement = null">取消</button><button class="confirm-user-delete-button" type="button" :disabled="handleReplaceBusy" @click="confirmHandleReplacement"><TriangleAlert :size="16" />{{ handleReplaceBusy ? '正在更换' : '确认清理并更换' }}</button></div>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { Copy, Save, Search, Trash2, TriangleAlert, UserRoundCog, UsersRound, X } from '@lucide/vue';
import UserFields from './UserFields.vue';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard';
import { OJ_LABELS, OJ_NAMES, type AdminUserMutationResponse, type OjName } from '../types';
import { handlesOf, userFormOf, type UserFormState } from '../utils/adminUsers';

// Author: huangbingrui.awa
const props = defineProps<{ dashboard: ReturnType<typeof usePlatformDashboard>; currentUsername: string | null }>();
const emit = defineEmits<{ guardChange: [value: boolean]; signOut: [] }>();
const sortedUsers = computed(() => [...props.dashboard.adminUsers.value].sort((a, b) => a.user.username.localeCompare(b.user.username)));
const usernameQuery = ref('');
const filteredUsers = computed(() => {
  const query = usernameQuery.value.trim().toLocaleLowerCase();
  return query ? sortedUsers.value.filter((item) => item.user.username.toLocaleLowerCase().includes(query)) : sortedUsers.value;
});
const expandedUsername = ref<string | null>(null); const editForm = ref<UserFormState | null>(null); const errorMessage = ref(''); const notice = ref('');
const passwords = ref<Array<{ username: string; password: string }>>([]); const pendingRelogin = ref(false);
const pendingDeleteUser = ref<{ username: string; nickname: string } | null>(null); const deleteBusy = ref(false);
type HandleChange = { ojName: OjName; oldHandle: string; newHandle: string };
const pendingHandleReplacement = ref<{ username: string; form: UserFormState; changes: HandleChange[] } | null>(null);
const handleReplaceBusy = ref(false);
watch(pendingRelogin, (value) => emit('guardChange', value));

function edit(item: AdminUserMutationResponse) { if (expandedUsername.value === item.user.username) { expandedUsername.value = null; editForm.value = null; } else { expandedUsername.value = item.user.username; editForm.value = userFormOf(item); } }
function showPasswords(results: AdminUserMutationResponse[]) { passwords.value = results.flatMap((result) => result.generatedPassword ? [{ username: result.user.username, password: result.generatedPassword }] : []); }
async function saveAccount() {
  if (!editForm.value || !expandedUsername.value) return;
  const current = props.dashboard.adminUsers.value.find((item) => item.user.username === expandedUsername.value);
  if (!current) return;
  const form = { ...editForm.value };
  const changes = handleChanges(current, form);
  if (changes.length) {
    pendingHandleReplacement.value = { username: expandedUsername.value, form, changes };
    return;
  }
  await executeSave(expandedUsername.value, form, []);
}
async function confirmHandleReplacement() {
  const pending = pendingHandleReplacement.value;
  if (!pending) return;
  handleReplaceBusy.value = true;
  await executeSave(pending.username, pending.form, pending.changes);
  if (!errorMessage.value) pendingHandleReplacement.value = null;
  handleReplaceBusy.value = false;
}
async function executeSave(original: string, form: UserFormState, changes: HandleChange[]) {
  await run(async () => {
    const accountResult = await props.dashboard.patchUser(original, { newUsername: form.username.trim(), nickname: form.nickname.trim(), email: form.email.trim(), role: form.role, ...(form.password ? { password: form.password } : {}) });
    let result = accountResult;
    if (original !== 'root') {
      const regularHandles = handlesOf(form);
      changes.forEach((change) => { delete regularHandles[change.ojName]; });
      result = await props.dashboard.updateOjHandles(accountResult.user.username, { handles: regularHandles, needCollect: form.needCollect });
      for (const change of changes) result = await props.dashboard.replaceOjHandle(result.user.username, change.ojName, change.newHandle);
    }
    showPasswords([accountResult]); expandedUsername.value = null; editForm.value = null;
    const relogin = original === props.currentUsername && accountResult.reloginRequired;
    pendingRelogin.value = relogin && Boolean(accountResult.generatedPassword);
    notice.value = changes.length ? '用户修改已保存，旧 handle 的训练数据已清理。' : relogin ? '用户修改已保存，需要重新登录。' : '用户修改已保存。';
    if (relogin && !accountResult.generatedPassword) emit('signOut');
  });
}
function handleChanges(current: AdminUserMutationResponse, form: UserFormState): HandleChange[] {
  const requested = handlesOf(form);
  return ([OJ_NAMES.CODEFORCES, OJ_NAMES.ATCODER] as OjName[]).flatMap((ojName) => {
    const oldHandle = current.handles[ojName]; const newHandle = requested[ojName];
    return oldHandle && newHandle && oldHandle !== newHandle ? [{ ojName, oldHandle, newHandle }] : [];
  });
}
function removeUser() { if (!expandedUsername.value) return; const item = props.dashboard.adminUsers.value.find((user) => user.user.username === expandedUsername.value); pendingDeleteUser.value = { username: expandedUsername.value, nickname: item?.user.nickname || '' }; }
async function confirmRemoveUser() { if (!pendingDeleteUser.value) return; const username = pendingDeleteUser.value.username; deleteBusy.value = true; await run(async () => { await props.dashboard.deleteUser(username); pendingDeleteUser.value = null; expandedUsername.value = null; editForm.value = null; notice.value = `已删除用户 ${username}。`; }); deleteBusy.value = false; }
async function copyPassword(item: { username: string; password: string }) { try { await navigator.clipboard.writeText(item.password); notice.value = `已复制 ${item.username} 的一次性密码。`; } catch { errorMessage.value = '复制密码失败。'; } }
function confirmedRelogin() { pendingRelogin.value = false; passwords.value = []; emit('signOut'); }
async function run(operation: () => Promise<void>) { errorMessage.value = ''; notice.value = ''; try { await operation(); } catch (error) { errorMessage.value = error instanceof Error ? error.message : '操作失败。'; } }
function handleEntries(item: AdminUserMutationResponse) { return Object.entries(item.handles).map(([oj, value]) => ({ oj, label: OJ_LABELS[oj as keyof typeof OJ_LABELS], value })); }
function formatTime(value: string) { const date = new Date(value); return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('zh-CN', { dateStyle: 'short', timeStyle: 'short', hour12: false }).format(date); }
</script>
