// Author: huangbingrui.awa
import { mount } from '@vue/test-utils';
import { createMemoryHistory, createRouter } from 'vue-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AppShell from '../components/AppShell.vue';
import { applyTheme, THEME_STORAGE_KEY } from '../theme';

function memoryLocalStorage(): Storage {
  const values = new Map<string, string>();
  return {
    clear: () => values.clear(),
    getItem: (key) => values.get(key) ?? null,
    key: (index) => Array.from(values.keys())[index] ?? null,
    get length() { return values.size; },
    removeItem: (key) => values.delete(key),
    setItem: (key, value) => values.set(key, value),
  };
}

beforeEach(() => vi.stubGlobal('localStorage', memoryLocalStorage()));

afterEach(() => {
  localStorage.clear();
  applyTheme('light');
  vi.unstubAllGlobals();
});

describe('Vue training app shell', () => {
  it('marks the login route so the embedded mobile shell can drop its desktop minimum width', async () => {
    const router = createRouter({
      history: createMemoryHistory('/training-app/'),
      routes: [
        { path: '/login', name: 'login', meta: { page: 'login' }, component: { template: '<div />' } },
      ],
    });
    await router.push('/login');
    await router.isReady();

    const wrapper = mount(AppShell, {
      props: {
        currentUser: null,
        changePassword: async () => undefined,
      },
      global: { plugins: [router] },
    });

    expect(wrapper.get('.training-site').classes()).toContain('is-login-page');
  });

  it('places every training route in the top navigation menu', async () => {
    const router = createRouter({
      history: createMemoryHistory('/training-app/'),
      routes: [
        { path: '/multiple', component: { template: '<div />' } },
        { path: '/single', component: { template: '<div />' } },
        { path: '/problem', component: { template: '<div />' } },
        { path: '/admin', component: { template: '<div />' } },
        { path: '/login', component: { template: '<div />' } },
      ],
    });
    const wrapper = mount(AppShell, {
      props: {
        currentUser: { username: 'admin', nickname: '管理员', avatar: '', email: '', role: 'ROLE_admin' },
        changePassword: async () => undefined,
      },
      global: { plugins: [router] },
    });

    await wrapper.get('.top-training-trigger').trigger('click');

    const links = wrapper.findAll('.top-training-dropdown a').map((link) => link.text());
    expect(links).toEqual(['多人统计', '单人查询', '题目查询', '管理员操作']);
    expect(wrapper.get('.site-name').text()).toBe('CustACM');
    expect(wrapper.findAll('nav > a, nav > .top-nav-menu > button').map((item) => item.text())).toEqual([
      '首页', '分类', '训练中心',
    ]);
    expect(wrapper.get('.blog-nav-search input').attributes('placeholder')).toBe('Search...');
    await wrapper.get('.blog-account-summary').trigger('click');
    expect(wrapper.get('.blog-account-menu a[href="/profile"]').text()).toBe('个人主页');
  });

  it('keeps the training navigation inactive on administrator pages', async () => {
    const router = createRouter({
      history: createMemoryHistory('/training-app/'),
      routes: [
        { path: '/multiple', name: 'multiple', meta: { page: 'multiple' }, component: { template: '<div />' } },
        { path: '/admin/users', name: 'admin-users', meta: { page: 'admin' }, component: { template: '<div />' } },
      ],
    });
    await router.push('/admin/users');
    await router.isReady();
    const wrapper = mount(AppShell, {
      props: {
        currentUser: { username: 'admin', nickname: '管理员', avatar: '', email: '', role: 'ROLE_admin' },
        changePassword: async () => undefined,
      },
      global: { plugins: [router] },
    });

    expect(wrapper.get('.top-training-trigger').classes()).not.toContain('is-active');
    await router.push('/multiple');
    expect(wrapper.get('.top-training-trigger').classes()).toContain('is-active');
  });

  it('offers an accessible persisted theme toggle in the standalone shell', async () => {
    const router = createRouter({
      history: createMemoryHistory('/training-app/'),
      routes: [{ path: '/multiple', component: { template: '<div />' } }],
    });
    await router.push('/multiple');
    await router.isReady();
    applyTheme('light');
    const wrapper = mount(AppShell, {
      props: { currentUser: null, changePassword: async () => undefined },
      global: { plugins: [router] },
    });

    const toggle = wrapper.get('.blog-theme-toggle');
    expect(toggle.attributes('aria-label')).toBe('切换到深夜模式');
    expect(toggle.attributes('role')).toBe('switch');
    expect(toggle.attributes('aria-checked')).toBe('false');
    expect(toggle.get('.theme-switch-thumb svg').classes()).toContain('lucide-sun');
    await toggle.trigger('click');

    expect(document.documentElement.dataset.theme).toBe('dark');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark');
    expect(toggle.attributes('aria-label')).toBe('切换到日间模式');
    expect(toggle.attributes('aria-checked')).toBe('true');
    expect(toggle.get('.theme-switch-thumb svg').classes()).toContain('lucide-moon');
    expect(toggle.text()).toContain('深夜模式');
  });

  it('does not render a second theme toggle inside the Blog-owned frame', async () => {
    vi.stubGlobal('self', {} as Window);
    const router = createRouter({
      history: createMemoryHistory('/training-app/'),
      routes: [{ path: '/multiple', component: { template: '<div />' } }],
    });
    await router.push('/multiple');
    await router.isReady();
    const wrapper = mount(AppShell, {
      props: { currentUser: null, changePassword: async () => undefined },
      global: { plugins: [router] },
    });

    expect(wrapper.find('.blog-theme-toggle').exists()).toBe(false);
    expect(wrapper.find('.blog-topbar').exists()).toBe(false);
  });
});
