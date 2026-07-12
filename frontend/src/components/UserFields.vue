<template>
  <label>username<input v-model="model.username" :aria-label="`${labelPrefix} username`" :disabled="systemAccount" required /></label>
  <label>nickname<input v-model="model.nickname" :aria-label="`${labelPrefix} nickname`" /></label>
  <label>email<input v-model="model.email" :aria-label="`${labelPrefix} email`" type="email" /></label>
  <label>role<select v-model="model.role" :aria-label="`${labelPrefix} role`" :disabled="systemAccount"><option value="ROLE_admin">管理员</option><option value="ROLE_player">队员</option></select></label>
  <label>password<input v-model="model.password" :aria-label="`${labelPrefix} password`" :placeholder="labelPrefix === '创建' ? '留空自动生成' : '填写后保存将重置'" type="password" /></label>
  <label v-if="!systemAccount">Codeforces handle<input v-model="model.codeforcesHandle" :aria-label="`${labelPrefix} Codeforces handle`" /></label>
  <label v-if="!systemAccount">AtCoder handle<input v-model="model.atcoderHandle" :aria-label="`${labelPrefix} AtCoder handle`" /></label>
  <label v-if="!systemAccount" class="collect-toggle-field">
    <input v-model="model.needCollect" :aria-label="`${labelPrefix} needCollect`" role="switch" type="checkbox" />
    <strong>现役队员</strong>
    <span class="collect-toggle-track" aria-hidden="true"><span /></span>
    <small>继续自动收集数据</small>
  </label>
</template>
<script setup lang="ts">
// Author: huangbingrui.awa
import type { UserFormState } from '../utils/adminUsers';
withDefaults(defineProps<{ labelPrefix: string; systemAccount?: boolean }>(), { systemAccount: false });
const model = defineModel<UserFormState>({ required: true });
</script>
