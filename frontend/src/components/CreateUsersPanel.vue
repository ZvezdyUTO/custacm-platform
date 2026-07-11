<template>
  <section class="create-users-page admin-reference-page" aria-label="创建用户">
    <header class="reference-page-header"><span class="reference-page-icon"><UserPlus :size="22" /></span><div><h2>创建用户</h2><p>文本导入会先填入信息栏；提交时创建账号，并为填写 handle 的行新增 OJ handle 绑定。</p></div></header>
    <p v-if="notice" class="admin-notice" role="status">{{ notice }}</p><p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
    <label class="create-import-field"><span>文本导入</span><textarea v-model="importText" rows="7" :placeholder="importPlaceholder" /></label>
    <button class="secondary-button import-fill-button" type="button" @click="fillRows"><FileInput :size="17" />填入信息栏</button>

    <section v-if="passwords.length" class="one-time-password-result" aria-label="一次性密码结果" role="status"><header><div><strong>一次性密码</strong><span>请立即复制并安全交付；关闭后无法再次查看。</span></div><button class="icon-button" type="button" @click="passwords = []"><X :size="16" /></button></header><ul><li v-for="item in passwords" :key="item.username"><strong>{{ item.username }}</strong><code>{{ item.password }}</code></li></ul></section>

    <form class="create-user-rows" @submit.prevent="executeCreate">
      <div v-for="(row, index) in rows" :key="index" class="create-user-row">
        <label>学号姓名<input v-model="row.username" required /></label>
        <label>nickname<input v-model="row.nickname" /></label>
        <label>角色<select v-model="row.role"><option value="ROLE_player">队员</option><option value="ROLE_admin">管理员</option></select></label>
        <label>初始密码<input v-model="row.password" placeholder="留空自动生成" /></label>
        <label>Codeforces<input v-model="row.codeforcesHandle" placeholder="可选" /></label>
        <label>AtCoder<input v-model="row.atcoderHandle" placeholder="可选" /></label>
        <button class="create-row-remove" type="button" aria-label="删除创建行" @click="removeRow(index)"><Trash2 :size="18" /></button>
      </div>
      <div class="create-user-actions"><button class="primary-button" :disabled="busy" type="submit"><UserPlus :size="17" />{{ busy ? '正在创建' : '执行创建' }}</button><button class="secondary-button" type="button" @click="addRow"><Plus :size="17" />增加一名队员</button><span>{{ rows.length }} 行待提交</span></div>
    </form>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { FileInput, Plus, Trash2, UserPlus, X } from '@lucide/vue';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard';
import { createRequestOf, emptyUserForm, parseCreateUserRows, type UserFormState } from '../utils/adminUsers';

// Author: huangbingrui.awa
const props = defineProps<{ dashboard: ReturnType<typeof usePlatformDashboard> }>();
const importText = ref(''); const rows = ref<UserFormState[]>([emptyUserForm()]); const errorMessage = ref(''); const notice = ref(''); const busy = ref(false); const passwords = ref<Array<{ username: string; password: string }>>([]);
const importPlaceholder = '每行：username,nickname,role,password,Codeforces,AtCoder\n230511213,,player,黄炳睿,Utonut-Zvezdy,Zvezdy';
function fillRows() { errorMessage.value = ''; try { rows.value = parseCreateUserRows(importText.value); notice.value = `已填入 ${rows.value.length} 行，请确认后执行创建。`; } catch (error) { errorMessage.value = error instanceof Error ? error.message : '文本导入失败。'; } }
function addRow() { rows.value.push(emptyUserForm()); }
function removeRow(index: number) { rows.value.splice(index, 1); if (!rows.value.length) addRow(); }
async function executeCreate() { errorMessage.value = ''; notice.value = ''; let requests; try { requests = rows.value.map((row, index) => { try { return createRequestOf(row); } catch (error) { const message = error instanceof Error ? error.message : '创建信息不完整。'; throw new Error(`第 ${index + 1} 行：${message}`); } }); } catch (error) { errorMessage.value = error instanceof Error ? error.message : '创建信息不完整。'; return; } if (!window.confirm(`确认创建 ${requests.length} 个用户？`)) return; busy.value = true; try { const results = await props.dashboard.batchCreateUsers(requests); passwords.value = results.flatMap((result) => result.generatedPassword ? [{ username: result.user.username, password: result.generatedPassword }] : []); notice.value = `已创建 ${results.length} 个用户，用户管理页面已同步刷新。`; rows.value = [emptyUserForm()]; importText.value = ''; } catch (error) { errorMessage.value = error instanceof Error ? error.message : '创建失败。'; } finally { busy.value = false; } }
</script>
