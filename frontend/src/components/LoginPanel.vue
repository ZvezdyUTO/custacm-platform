<template>
  <section class="login-page" aria-labelledby="login-title">
    <div class="login-card">
      <header>
        <span class="login-eyebrow">TRAINING CENTER</span>
        <h1 id="login-title">登录训练中心</h1>
        <p>使用 custacm wiki 账号继续访问训练数据。</p>
      </header>
      <form @submit.prevent="submit">
        <label>用户名<input v-model="username" autocomplete="username" placeholder="输入用户名" required /></label>
        <label>密码<input v-model="password" autocomplete="current-password" placeholder="输入密码" required type="password" /></label>
        <button class="primary-button login-submit" :disabled="loading" type="submit">
          <LogIn :size="16" aria-hidden="true" />{{ loading ? '登录中' : '登录' }}
        </button>
      </form>
      <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { LogIn } from '@lucide/vue';

// Author: huangbingrui.awa
const props = defineProps<{ signIn(username: string, password: string): Promise<void> }>();
const username = ref('');
const password = ref('');
const loading = ref(false);
const errorMessage = ref('');
async function submit() {
  loading.value = true;
  errorMessage.value = '';
  try {
    await props.signIn(username.value, password.value);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败。';
    password.value = '';
  } finally {
    loading.value = false;
  }
}
</script>
