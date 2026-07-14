<template>
  <AppShell :current-user="currentUser" :change-password="auth.changePassword" @sign-out="signOut">
    <Transition name="route-fade" mode="out-in">
      <div :key="routeAnimationKey" class="route-view">
        <p v-if="navigationNotice" class="admin-notice navigation-guard-notice" role="status">{{ navigationNotice }}</p>
        <LoginPanel v-if="page === 'login' || auth.status.value === 'anonymous'" :sign-in="signIn" />
        <section v-else-if="auth.status.value === 'restoring'" class="route-status" aria-live="polite">
          <h1>正在恢复登录状态</h1><p>正在通过 Blog API 校验当前账号。</p>
        </section>
        <section v-else-if="page === 'admin' && currentUser?.role !== 'ROLE_admin'" class="admin-gate">
          <h1>需要管理员权限</h1><p>当前账号可以访问训练查询，但不能进入管理员操作页面。</p>
        </section>
        <div v-else class="dashboard-main">
          <TrainingQueryPanel
            v-if="page !== 'admin'"
            :dashboard="dashboard"
            :mode="mode"
          />
          <TrainingAdminPanel
            v-else
            :dashboard="dashboard"
            :section="adminSection"
            :current-username="currentUser?.username ?? null"
            @guard-change="navigationGuarded = $event"
            @section-change="changeAdminSection"
            @sign-out="auth.signOut"
          />
          <p v-if="dashboard.errorMessage.value" class="form-error" role="alert">
            接口提示：{{ dashboard.errorMessage.value }}
          </p>
        </div>
      </div>
    </Transition>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '../components/AppShell.vue';
import LoginPanel from '../components/LoginPanel.vue';
import TrainingAdminPanel from '../components/TrainingAdminPanel.vue';
import TrainingQueryPanel from '../components/TrainingQueryPanel.vue';
import { useAuthSession } from '../composables/useAuthSession';
import { usePlatformDashboard } from '../composables/usePlatformDashboard';
import { navigateToBlogReturnPath, safeReturnPath, type AdminSection, type TrainingPage } from '../routing';
import type { TrainingQueryMode } from '../types';

// Author: huangbingrui.awa
const route = useRoute();
const router = useRouter();
const auth = useAuthSession();
const navigationGuarded = ref(false);
const navigationNotice = ref('');
const page = computed<TrainingPage>(() => (route.meta.page as TrainingPage | undefined) ?? 'multiple');
const mode = computed<TrainingQueryMode>(() => page.value === 'single' || page.value === 'problem' ? page.value : 'multiple');
const adminSection = computed<AdminSection>(() => (route.meta.adminSection as AdminSection | undefined) ?? 'users');
const currentUser = computed(() => auth.status.value === 'anonymous' ? null : auth.user.value);
const activeAdminSection = computed<AdminSection | null>(() => page.value === 'admin' ? adminSection.value : null);
const routeAnimationKey = computed(() => `${page.value}:${page.value === 'admin' ? adminSection.value : mode.value}`);
const dashboard = usePlatformDashboard({
  token: auth.token,
  user: auth.user,
  mode,
  adminSection: activeAdminSection,
  onUnauthorized: auth.signOut,
});

watch([() => auth.status.value, page], ([status, nextPage]) => {
  if (status !== 'anonymous' || nextPage === 'login') return;
  const returnTo = safeReturnPath(`/training${route.fullPath}`);
  void router.replace({ path: '/login', query: { returnTo } });
}, { immediate: true });

watch(navigationGuarded, (guarded) => {
  if (!guarded) navigationNotice.value = '';
});

watch(() => route.fullPath, (path) => {
  if (window.self !== window.top) {
    window.parent.postMessage({ type: 'custacm:training-route', path }, window.location.origin);
  }
}, { immediate: true });

async function signIn(username: string, password: string) {
  const returnTo = safeReturnPath(typeof route.query.returnTo === 'string' ? route.query.returnTo : null);
  await auth.signIn(username, password);
	if (!returnTo.startsWith('/training')) {
		navigateToBlogReturnPath(returnTo);
		return;
	}
  await router.replace(returnTo.replace(/^\/training/, '') || '/multiple');
}

function signOut() {
  if (navigationGuarded.value) {
    navigationNotice.value = '请先保存一次性密码，再点击“我已保存，重新登录”。';
    return;
  }
  const returnTo = safeReturnPath(`/training${route.fullPath}`);
  auth.signOut();
  void router.replace({ path: '/login', query: { returnTo } });
}

function changeAdminSection(section: AdminSection) {
  if (navigationGuarded.value) {
    navigationNotice.value = '请先保存一次性密码，再点击“我已保存，重新登录”。';
    return;
  }
  void router.push(section === 'create'
    ? '/admin/create-users'
    : section === 'articles'
      ? '/admin/articles'
    : section === 'categories'
      ? '/admin/categories'
    : section === 'training'
      ? '/admin/training'
      : section === 'appearance'
        ? '/admin/appearance'
        : '/admin/users');
}
</script>
