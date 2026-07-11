// Author: huangbingrui.awa
import { mount } from '@vue/test-utils';
import { createMemoryHistory, createRouter } from 'vue-router';
import { describe, expect, it } from 'vitest';
import AppShell from '../components/AppShell.vue';

describe('Vue training app shell', () => {
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
    expect(wrapper.get('.site-name').text()).toBe("Naccl's Blog");
    expect(wrapper.findAll('nav > a, nav > .top-nav-menu > button').map((item) => item.text())).toEqual([
      '首页', '分类', '归档', '动态', '友人帐', '关于我', '训练中心',
    ]);
    expect(wrapper.get('.blog-nav-search input').attributes('placeholder')).toBe('Search...');
  });
});
