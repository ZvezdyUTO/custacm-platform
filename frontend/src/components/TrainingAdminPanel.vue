<template>
  <section class="admin-workspace" aria-label="管理员操作内容">
    <nav class="training-admin-tabs" aria-label="管理员页面"><button :class="{ 'is-active': section === 'create' }" type="button" @click="emit('sectionChange', 'create')">创建用户</button><button :class="{ 'is-active': section === 'users' }" type="button" @click="emit('sectionChange', 'users')">管理用户</button><button :class="{ 'is-active': section === 'training' }" type="button" @click="emit('sectionChange', 'training')">数据采集</button><button :class="{ 'is-active': section === 'appearance' }" type="button" @click="emit('sectionChange', 'appearance')">首页图片</button></nav>
    <CreateUsersPanel v-if="section === 'create'" :dashboard="dashboard" />
    <AdminUserManagementPanel v-else-if="section === 'users'" :dashboard="dashboard" :current-username="currentUsername" @guard-change="emit('guardChange', $event)" @sign-out="emit('signOut')" />
    <TrainingDataOpsPanel v-else-if="section === 'training'" :dashboard="dashboard" />
    <HomepageBannerAdminPanel v-else :dashboard="dashboard" />
  </section>
</template>
<script setup lang="ts">
// Author: huangbingrui.awa
import AdminUserManagementPanel from './AdminUserManagementPanel.vue'; import CreateUsersPanel from './CreateUsersPanel.vue'; import HomepageBannerAdminPanel from './HomepageBannerAdminPanel.vue'; import TrainingDataOpsPanel from './TrainingDataOpsPanel.vue';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard'; import type { AdminSection } from '../routing';
defineProps<{ dashboard: ReturnType<typeof usePlatformDashboard>; section: AdminSection; currentUsername: string | null }>();
const emit = defineEmits<{ sectionChange: [section: AdminSection]; guardChange: [value: boolean]; signOut: [] }>();
</script>
