<template>
  <div class="training-site" :class="{ 'is-embedded': embedded, 'is-login-page': isLoginPage }">
    <header v-if="!embedded" class="blog-topbar">
      <div class="blog-topbar-inner">
        <a class="site-name" href="/home">{{ siteName }}</a>
        <nav aria-label="站点导航">
          <a href="/home"><Home :size="17" aria-hidden="true" />首页</a>
          <div class="top-nav-menu" @mouseenter="categoryMenuOpen = true" @mouseleave="categoryMenuOpen = false">
            <button class="top-nav-trigger" type="button" :aria-expanded="categoryMenuOpen" @click="categoryMenuOpen = !categoryMenuOpen">
              <Lightbulb :size="17" aria-hidden="true" />分类<ChevronDown :size="13" aria-hidden="true" />
            </button>
            <div v-show="categoryMenuOpen" class="top-nav-dropdown category-dropdown">
              <a v-for="category in categories" :key="category" :href="`/category/${encodeURIComponent(category)}`">{{ category }}</a>
              <span v-if="categories.length === 0" class="top-nav-empty">暂无分类</span>
            </div>
          </div>
          <div class="top-nav-menu top-training-menu" @mouseenter="trainingMenuOpen = true" @mouseleave="trainingMenuOpen = false">
            <button class="top-nav-trigger top-training-trigger" :class="{ 'is-active': isTrainingQueryPage }" type="button" :aria-expanded="trainingMenuOpen" @click="trainingMenuOpen = !trainingMenuOpen">
              <BarChart3 :size="17" aria-hidden="true" />训练中心<ChevronDown :size="13" aria-hidden="true" />
            </button>
            <div v-show="trainingMenuOpen" class="top-nav-dropdown top-training-dropdown">
              <RouterLink to="/multiple" @click="trainingMenuOpen = false">多人统计</RouterLink>
              <RouterLink to="/single" @click="trainingMenuOpen = false">单人查询</RouterLink>
              <RouterLink to="/problem" @click="trainingMenuOpen = false">题目查询</RouterLink>
              <RouterLink v-if="currentUser?.role === 'ROLE_admin'" to="/admin" @click="trainingMenuOpen = false">管理员操作</RouterLink>
            </div>
          </div>
        </nav>

        <form class="blog-nav-search" role="search" @submit.prevent="openSearchResult">
          <input v-model="searchQuery" type="search" placeholder="Search..." aria-label="搜索博客" @input="scheduleSearch" />
          <Search :size="17" aria-hidden="true" />
          <div v-if="searchResults.length" class="blog-search-results">
            <a v-for="result in searchResults" :key="result.id" :href="`/blog/${result.id}`">{{ result.title }}</a>
          </div>
        </form>

        <button
          class="blog-theme-toggle"
          type="button"
          role="switch"
          :aria-label="currentTheme === 'dark' ? '切换到日间模式' : '切换到深夜模式'"
          :aria-checked="currentTheme === 'dark'"
          :title="currentTheme === 'dark' ? '切换到日间模式' : '切换到深夜模式'"
          @click="switchTheme"
        >
          <span class="theme-switch-track" :class="{ 'is-dark': currentTheme === 'dark' }" aria-hidden="true">
            <span class="theme-switch-thumb">
              <Moon v-if="currentTheme === 'dark'" :size="13" />
              <Sun v-else :size="13" />
            </span>
          </span>
          <span class="theme-switch-status">{{ currentTheme === 'dark' ? '深夜模式' : '日间模式' }}</span>
        </button>

        <RouterLink v-if="!currentUser" class="blog-account-link" to="/login"><UserRound :size="17" aria-hidden="true" />登录</RouterLink>
        <div v-else class="blog-account">
          <button class="blog-account-summary" type="button" :aria-expanded="accountOpen" @click="toggleAccount">
            <span class="blog-avatar" aria-hidden="true">{{ avatar }}</span><span>{{ displayName }}</span><ChevronDown :size="13" aria-hidden="true" />
          </button>
          <div v-if="accountOpen" class="blog-account-menu">
            <form v-if="changingPassword" class="password-change-form" @submit.prevent="submitPassword">
              <label>旧密码<input v-model="oldPassword" required type="password" autocomplete="current-password" /></label>
              <label>新密码<input v-model="newPassword" required type="password" autocomplete="new-password" /></label>
              <label>确认新密码<input v-model="confirmPassword" required type="password" autocomplete="new-password" /></label>
              <p v-if="passwordError" role="alert">{{ passwordError }}</p>
              <div class="password-change-actions">
                <button class="account-menu-button" :disabled="submitting" type="submit"><Save :size="14" />保存</button>
                <button class="account-menu-button" type="button" @click="resetPassword">取消</button>
              </div>
            </form>
            <template v-else>
              <a class="account-menu-button" href="/profile"><UserRound :size="15" />个人主页</a>
              <button class="account-menu-button" type="button" @click="changingPassword = true"><KeyRound :size="15" />修改密码</button>
              <button class="account-menu-button" type="button" @click="emit('signOut')"><LogOut :size="15" />退出</button>
            </template>
          </div>
        </div>
      </div>
    </header>
    <main class="training-main"><slot /></main>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { BarChart3, ChevronDown, Home, KeyRound, Lightbulb, LogOut, Moon, Save, Search, Sun, UserRound } from '@lucide/vue';
import { useRoute } from 'vue-router';
import { requestData } from '../api/client';
import { currentTheme as appliedTheme, subscribeTheme, toggleTheme, type ColorTheme } from '../theme';
import type { CurrentUser } from '../types';

// Author: huangbingrui.awa
const props = defineProps<{
  currentUser: CurrentUser | null;
  changePassword(oldPassword: string, newPassword: string): Promise<void>;
}>();
const emit = defineEmits<{ signOut: [] }>();
const route = useRoute();
const embedded = window.self !== window.top;
const isLoginPage = computed(() => route.meta.page === 'login' || route.name === 'login');
const isTrainingQueryPage = computed(() => ['multiple', 'single', 'problem'].includes(String(route.meta.page || route.name || '')));
const siteName = 'CustACM';
const categories = ref<string[]>([]);
const categoryMenuOpen = ref(false);
const trainingMenuOpen = ref(false);
const searchQuery = ref('');
const searchResults = ref<Array<{ id: number; title: string }>>([]);
let searchTimer: number | undefined;
const accountOpen = ref(false);
const changingPassword = ref(false);
const oldPassword = ref('');
const newPassword = ref('');
const confirmPassword = ref('');
const passwordError = ref('');
const submitting = ref(false);
const currentTheme = ref<ColorTheme>(appliedTheme());
let stopThemeSubscription: () => void = () => undefined;
const displayName = computed(() => props.currentUser?.nickname || props.currentUser?.username || '');
const avatar = computed(() => Array.from(displayName.value.trim())[0]?.toUpperCase() ?? 'U');

onMounted(async () => {
  currentTheme.value = appliedTheme();
  stopThemeSubscription = subscribeTheme((theme) => { currentTheme.value = theme; });
  if (embedded) return;
  try {
    const result = await requestData<Array<{ name?: string }>>('/categories');
    categories.value = result.map((category) => category.name?.trim()).filter((name): name is string => Boolean(name));
  } catch {
    categories.value = [];
  }
});

onBeforeUnmount(() => {
  window.clearTimeout(searchTimer);
  stopThemeSubscription();
});

function scheduleSearch() {
  window.clearTimeout(searchTimer);
  const query = searchQuery.value.trim();
  if (!query) {
    searchResults.value = [];
    return;
  }
  searchTimer = window.setTimeout(async () => {
    try {
      searchResults.value = await requestData<Array<{ id: number; title: string }>>(`/searchBlog?query=${encodeURIComponent(query)}`);
    } catch {
      searchResults.value = [];
    }
  }, 300);
}

function openSearchResult() {
  const result = searchResults.value[0];
  if (result) window.location.assign(`/blog/${result.id}`);
}

function switchTheme() {
  currentTheme.value = toggleTheme();
}

function resetPassword() {
  oldPassword.value = '';
  newPassword.value = '';
  confirmPassword.value = '';
  passwordError.value = '';
  changingPassword.value = false;
}
function toggleAccount() {
  accountOpen.value = !accountOpen.value;
  if (!accountOpen.value) resetPassword();
}
async function submitPassword() {
  passwordError.value = '';
  if (newPassword.value !== confirmPassword.value) {
    passwordError.value = '两次输入的新密码必须一致。';
    return;
  }
  submitting.value = true;
  try {
    await props.changePassword(oldPassword.value, newPassword.value);
    accountOpen.value = false;
    resetPassword();
  } catch (error) {
    passwordError.value = error instanceof Error ? error.message : '修改密码失败。';
  } finally {
    submitting.value = false;
  }
}
</script>
