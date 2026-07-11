<template>
  <section class="admin-user-management-panel admin-reference-page" aria-label="管理用户信息">
    <header class="reference-page-header">
      <span class="reference-page-icon"><UsersRound :size="22" /></span>
      <div><h2>管理用户信息</h2><p>按学号姓名升序展示；在列表中直接修改角色、密码、OJ handle 和现役状态。</p></div>
    </header>
    <p v-if="notice" class="admin-notice" role="status">{{ notice }}</p>
    <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>

    <section v-if="passwords.length" class="one-time-password-result" aria-label="一次性密码结果" role="status">
      <header><div><strong>一次性密码</strong><span>请立即复制并安全交付；关闭后无法再次查看。</span></div><button v-if="!pendingRelogin" class="icon-button" type="button" @click="passwords = []"><X :size="16" /></button></header>
      <ul><li v-for="item in passwords" :key="item.username"><strong>{{ item.username }}</strong><code>{{ item.password }}</code><button class="secondary-button compact" type="button" @click="copyPassword(item)"><Copy :size="14" />复制</button></li></ul>
      <div v-if="pendingRelogin" class="one-time-password-actions"><button class="primary-button" type="button" @click="confirmedRelogin">我已保存，重新登录</button></div>
    </section>

    <div class="reference-count"><strong>{{ sortedUsers.length }}</strong><span>个账号</span></div>
    <div class="table-shell admin-user-table-shell reference-user-table-shell">
      <table class="admin-user-table reference-user-table" aria-label="用户列表">
        <thead><tr><th>学号姓名</th><th>OJ handle</th><th>角色 / 状态</th><th>更新时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-if="!sortedUsers.length"><td class="admin-user-empty" colspan="5">暂无用户</td></tr>
          <template v-for="item in sortedUsers" :key="item.user.id || item.user.username">
            <tr :class="{ 'is-expanded': expandedUsername === item.user.username }">
              <td><div class="reference-user-identity"><strong>{{ item.user.username }}{{ item.user.nickname || '' }}</strong><small>{{ item.user.email || '未设置邮箱' }}</small></div></td>
              <td><div class="reference-handle-list"><span v-for="handle in handleEntries(item)" :key="handle.oj"><b>{{ handle.label }}：</b>{{ handle.value }}</span><em v-if="!handleEntries(item).length">未绑定</em></div></td>
              <td><div class="reference-status-list"><span class="role-chip">{{ item.user.role === 'ROLE_admin' ? '管理员' : '队员' }}</span><span :class="['collect-chip', item.needCollect === false ? 'is-retired' : '']">{{ item.needCollect === false ? '已退役' : '现役队员' }}</span></div></td>
              <td>{{ formatTime(item.user.updateTime) }}</td>
              <td><button class="secondary-button reference-edit-button" type="button" @click="edit(item)"><UserRoundCog :size="16" />编辑</button></td>
            </tr>
            <tr v-if="expandedUsername === item.user.username && editForm" class="admin-user-edit-row"><td colspan="5">
              <form class="admin-user-edit-form" @submit.prevent="saveAccount"><div class="admin-user-edit-grid"><UserFields v-model="editForm" label-prefix="编辑" /></div>
                <div class="admin-user-edit-actions"><button class="primary-button" type="submit"><Save :size="16" />保存修改</button><button class="danger-button subtle" type="button" @click="removeUser"><Trash2 :size="16" />删除用户</button></div>
              </form>
            </td></tr>
          </template>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { Copy, Save, Trash2, UserRoundCog, UsersRound, X } from '@lucide/vue';
import UserFields from './UserFields.vue';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard';
import { OJ_LABELS, type AdminUserMutationResponse } from '../types';
import { handlesOf, userFormOf, type UserFormState } from '../utils/adminUsers';

// Author: huangbingrui.awa
const props = defineProps<{ dashboard: ReturnType<typeof usePlatformDashboard>; currentUsername: string | null }>();
const emit = defineEmits<{ guardChange: [value: boolean]; signOut: [] }>();
const sortedUsers = computed(() => [...props.dashboard.adminUsers.value].sort((a, b) => a.user.username.localeCompare(b.user.username)));
const expandedUsername = ref<string | null>(null); const editForm = ref<UserFormState | null>(null); const errorMessage = ref(''); const notice = ref('');
const passwords = ref<Array<{ username: string; password: string }>>([]); const pendingRelogin = ref(false);
watch(pendingRelogin, (value) => emit('guardChange', value));

function edit(item: AdminUserMutationResponse) { if (expandedUsername.value === item.user.username) { expandedUsername.value = null; editForm.value = null; } else { expandedUsername.value = item.user.username; editForm.value = userFormOf(item); } }
function showPasswords(results: AdminUserMutationResponse[]) { passwords.value = results.flatMap((result) => result.generatedPassword ? [{ username: result.user.username, password: result.generatedPassword }] : []); }
async function saveAccount() { if (!editForm.value || !expandedUsername.value) return; await run(async () => { const original = expandedUsername.value!; const form = editForm.value!; const accountResult = await props.dashboard.patchUser(original, { newUsername: form.username.trim(), nickname: form.nickname.trim(), email: form.email.trim(), avatar: form.avatar.trim(), role: form.role, ...(form.password ? { password: form.password } : {}) }); const handles = handlesOf(form); const result = Object.keys(handles).length ? await props.dashboard.updateOjHandles(accountResult.user.username, { handles, needCollect: form.needCollect }) : accountResult; showPasswords([accountResult]); expandedUsername.value = result.user.username; editForm.value = userFormOf(result); const relogin = original === props.currentUsername && accountResult.reloginRequired; pendingRelogin.value = relogin && Boolean(accountResult.generatedPassword); notice.value = relogin ? '用户修改已保存，需要重新登录。' : '用户修改已保存。'; if (relogin && !accountResult.generatedPassword) emit('signOut'); }); }
async function removeUser() { if (!expandedUsername.value || !window.confirm(`确认删除 ${expandedUsername.value}？此操作不可恢复。`)) return; await run(async () => { const username = expandedUsername.value!; await props.dashboard.deleteUser(username); expandedUsername.value = null; editForm.value = null; notice.value = `已删除用户 ${username}。`; }); }
async function copyPassword(item: { username: string; password: string }) { try { await navigator.clipboard.writeText(item.password); notice.value = `已复制 ${item.username} 的一次性密码。`; } catch { errorMessage.value = '复制密码失败。'; } }
function confirmedRelogin() { pendingRelogin.value = false; passwords.value = []; emit('signOut'); }
async function run(operation: () => Promise<void>) { errorMessage.value = ''; notice.value = ''; try { await operation(); } catch (error) { errorMessage.value = error instanceof Error ? error.message : '操作失败。'; } }
function handleEntries(item: AdminUserMutationResponse) { return Object.entries(item.handles).map(([oj, value]) => ({ oj, label: OJ_LABELS[oj as keyof typeof OJ_LABELS], value })); }
function formatTime(value: string) { const date = new Date(value); return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('zh-CN', { dateStyle: 'short', timeStyle: 'short', hour12: false }).format(date); }
</script>
