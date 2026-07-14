<template>
  <section ref="loginPage" class="login-page" aria-labelledby="login-title">
    <div class="login-card">
      <header>
        <div class="login-brand-mark" :style="{ transform: `scale(${brandScale})` }" aria-hidden="true">
          <img :src="trainingLogoUrl" alt="" />
        </div>
        <h1 id="login-title">登录训练中心</h1>
        <p>使用 CUSTACM 账号继续访问训练数据</p>
      </header>
      <form @submit.prevent="submit">
        <label class="login-field">
          <span class="visually-hidden">用户名</span>
          <UserRound :size="20" aria-hidden="true" />
          <input v-model="username" autocomplete="username" placeholder="用户名" required />
        </label>
        <label class="login-field">
          <span class="visually-hidden">密码</span>
          <LockKeyhole :size="20" aria-hidden="true" />
          <input v-model="password" autocomplete="current-password" placeholder="密码" required type="password" />
        </label>
        <div class="login-submit-row">
          <button class="primary-button login-submit" :aria-label="submitLabel" :title="submitLabel" :disabled="loading || cooldownSeconds > 0" type="submit">
            <ArrowRight :size="18" aria-hidden="true" />
          </button>
          <span class="login-submit-status" aria-live="polite">{{ submitLabel }}</span>
        </div>
      </form>
      <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
    </div>
    <LoginFooter />
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { ArrowRight, LockKeyhole, UserRound } from '@lucide/vue';
import { ApiError } from '../api/client';
import LoginFooter from './LoginFooter.vue';

// Author: huangbingrui.awa
const props = defineProps<{ signIn(username: string, password: string): Promise<void> }>();
const trainingLogoUrl = `${import.meta.env.BASE_URL}img/custacm-training-logo.jpg`;
const username = ref('');
const password = ref('');
const loading = ref(false);
const errorMessage = ref('');
const cooldownSeconds = ref(0);
const brandScale = ref(1);
const loginPage = ref<ReturnType<typeof document.querySelector>>(null);
let cooldownTimer: ReturnType<typeof window.setInterval> | null = null;
let scrollTarget = loginPage.value?.closest('.training-site.is-login-page') ?? window;
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

function updateBrandScale() {
  const scrollTop = 'scrollTop' in scrollTarget ? scrollTarget.scrollTop : window.scrollY;
  brandScale.value = Math.max(0.72, 1 - scrollTop / 760);
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

onMounted(() => {
  document.documentElement.classList.add('login-scrollbar-hidden');
  scrollTarget = loginPage.value?.closest('.training-site.is-login-page') ?? window;
  updateBrandScale();
  scrollTarget.addEventListener('scroll', updateBrandScale, { passive: true });
});

onBeforeUnmount(() => {
  document.documentElement.classList.remove('login-scrollbar-hidden');
  scrollTarget.removeEventListener('scroll', updateBrandScale);
  scrollTarget = window;
  if (cooldownTimer !== null) window.clearInterval(cooldownTimer);
});
</script>
