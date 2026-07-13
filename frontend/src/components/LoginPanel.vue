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
        <button class="primary-button login-submit" :disabled="loading || cooldownSeconds > 0" type="submit">
          <LogIn :size="16" aria-hidden="true" />{{ submitLabel }}
        </button>
      </form>
      <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue';
import { LogIn } from '@lucide/vue';
import { ApiError } from '../api/client';

// Author: huangbingrui.awa
const props = defineProps<{ signIn(username: string, password: string): Promise<void> }>();
const username = ref('');
const password = ref('');
const loading = ref(false);
const errorMessage = ref('');
const cooldownSeconds = ref(0);
let cooldownTimer: ReturnType<typeof window.setInterval> | null = null;
const submitLabel = computed(() => {
  if (loading.value) return '登录中';
  if (cooldownSeconds.value > 0) return `${cooldownSeconds.value} 秒后重试`;
  return '登录';
});

function startCooldown(seconds: number) {
  if (cooldownTimer !== null) window.clearInterval(cooldownTimer);
  cooldownSeconds.value = Math.max(1, Math.ceil(seconds));
  cooldownTimer = window.setInterval(() => {
    cooldownSeconds.value = Math.max(0, cooldownSeconds.value - 1);
    if (cooldownSeconds.value === 0 && cooldownTimer !== null) {
      window.clearInterval(cooldownTimer);
      cooldownTimer = null;
    }
  }, 1000);
}

async function submit() {
  if (loading.value || cooldownSeconds.value > 0) return;
  loading.value = true;
  errorMessage.value = '';
  try {
    await props.signIn(username.value, password.value);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败。';
    if (error instanceof ApiError && error.retryAfterSeconds) {
      startCooldown(error.retryAfterSeconds);
    }
    password.value = '';
  } finally {
    loading.value = false;
  }
}

onBeforeUnmount(() => {
  if (cooldownTimer !== null) window.clearInterval(cooldownTimer);
});
</script>
