import { createRouter, createWebHistory } from 'vue-router';
import TrainingView from '../views/TrainingView.vue';

// Author: huangbingrui.awa
export default createRouter({
  history: createWebHistory('/training-app/'),
  routes: [
    { path: '/', redirect: '/multiple' },
    { path: '/login', name: 'login', component: TrainingView, meta: { page: 'login' } },
    { path: '/multiple', name: 'multiple', component: TrainingView, meta: { page: 'multiple' } },
    { path: '/single', name: 'single', component: TrainingView, meta: { page: 'single' } },
    { path: '/problem', name: 'problem', component: TrainingView, meta: { page: 'problem' } },
    { path: '/admin', redirect: '/admin/users' },
    { path: '/admin/create-users', name: 'admin-create-users', component: TrainingView, meta: { page: 'admin', adminSection: 'create' } },
    { path: '/admin/users', name: 'admin-users', component: TrainingView, meta: { page: 'admin', adminSection: 'users' } },
    { path: '/admin/articles', name: 'admin-articles', component: TrainingView, meta: { page: 'admin', adminSection: 'articles' } },
    { path: '/admin/categories', name: 'admin-categories', component: TrainingView, meta: { page: 'admin', adminSection: 'categories' } },
    { path: '/admin/competitions', name: 'admin-competitions', component: TrainingView, meta: { page: 'admin', adminSection: 'competitions' } },
    { path: '/admin/training', name: 'admin-training', component: TrainingView, meta: { page: 'admin', adminSection: 'training' } },
    { path: '/admin/appearance', name: 'admin-appearance', component: TrainingView, meta: { page: 'admin', adminSection: 'appearance' } },
    { path: '/:pathMatch(.*)*', redirect: '/multiple' },
  ],
});
