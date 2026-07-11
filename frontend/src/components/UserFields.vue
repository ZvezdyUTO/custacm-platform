<template>
  <label>username<input v-model="model.username" :aria-label="`${labelPrefix} username`" required /></label>
  <label>nickname<input v-model="model.nickname" :aria-label="`${labelPrefix} nickname`" /></label>
  <label>email<input v-model="model.email" :aria-label="`${labelPrefix} email`" type="email" /></label>
  <label>avatar<input v-model="model.avatar" :aria-label="`${labelPrefix} avatar`" type="url" /></label>
  <label>role<select v-model="model.role" :aria-label="`${labelPrefix} role`"><option value="ROLE_admin">管理员</option><option value="ROLE_player">队员</option></select></label>
  <label>password<input v-model="model.password" :aria-label="`${labelPrefix} password`" :placeholder="labelPrefix === '创建' ? '留空自动生成' : '填写后保存将重置'" type="password" /></label>
  <label>Codeforces handle<input v-model="model.codeforcesHandle" :aria-label="`${labelPrefix} Codeforces handle`" /></label>
  <label>AtCoder handle<input v-model="model.atcoderHandle" :aria-label="`${labelPrefix} AtCoder handle`" /></label>
  <label class="checkbox-field"><input v-model="model.needCollect" :aria-label="`${labelPrefix} needCollect`" type="checkbox" />needCollect</label>
</template>
<script setup lang="ts">
// Author: huangbingrui.awa
import type { UserFormState } from '../utils/adminUsers';
defineProps<{ labelPrefix: string }>();
const model = defineModel<UserFormState>({ required: true });
</script>
