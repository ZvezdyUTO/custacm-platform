// Author: huangbingrui.awa
import { mount } from '@vue/test-utils';
import { ref } from 'vue';
import { describe, expect, it, vi } from 'vitest';
import HomepageBannerAdminPanel from '../components/HomepageBannerAdminPanel.vue';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard';

describe('homepage banner admin panel', () => {
  it('renders banners left-to-right and sends the complete order when moving one', async () => {
    const reorderHomepageBanners = vi.fn().mockResolvedValue(undefined);
    const dashboard = {
      homepageBanners: ref([
        { id: 1, imageUrl: '/api/image/one.jpg', sortOrder: 0 },
        { id: 2, imageUrl: '/api/image/two.jpg', sortOrder: 1 },
      ]),
      loadHomepageBanners: vi.fn().mockResolvedValue(undefined),
      uploadHomepageBanner: vi.fn(),
      reorderHomepageBanners,
      deleteHomepageBanner: vi.fn(),
    } as unknown as ReturnType<typeof usePlatformDashboard>;

    const wrapper = mount(HomepageBannerAdminPanel, { props: { dashboard } });
    await wrapper.get('button[aria-label="将第 2 张向左移动"]').trigger('click');

    expect(reorderHomepageBanners).toHaveBeenCalledWith([2, 1]);
    expect(wrapper.findAll('.homepage-banner-card')).toHaveLength(2);
    expect(wrapper.text()).not.toContain('选择图片');
  });

  it('disables deletion when only one banner remains', () => {
    const dashboard = {
      homepageBanners: ref([{ id: 1, imageUrl: '/api/image/one.jpg', sortOrder: 0 }]),
      loadHomepageBanners: vi.fn().mockResolvedValue(undefined),
    } as unknown as ReturnType<typeof usePlatformDashboard>;

    const wrapper = mount(HomepageBannerAdminPanel, { props: { dashboard } });

    expect(wrapper.get('button[aria-label="删除第 1 张首页图片"]').attributes('disabled')).toBeDefined();
    expect(wrapper.find('.homepage-banner-list').element.lastElementChild?.classList)
      .toContain('homepage-banner-add');
    expect(wrapper.find('.homepage-banner-add input[type="file"]').attributes('multiple')).toBeDefined();
  });

  it('hides the add card when two banners already exist', () => {
    const dashboard = {
      homepageBanners: ref([
        { id: 1, imageUrl: '/api/image/one.jpg', sortOrder: 0 },
        { id: 2, imageUrl: '/api/image/two.jpg', sortOrder: 1 },
      ]),
      loadHomepageBanners: vi.fn().mockResolvedValue(undefined),
    } as unknown as ReturnType<typeof usePlatformDashboard>;

    const wrapper = mount(HomepageBannerAdminPanel, { props: { dashboard } });

    expect(wrapper.find('.homepage-banner-add').exists()).toBe(false);
    expect(wrapper.text()).toContain('首页最多保留两张图片');
  });

  it('uses the shared confirmation dialog before deleting a banner', async () => {
    const deleteHomepageBanner = vi.fn().mockResolvedValue(undefined);
    const dashboard = {
      homepageBanners: ref([
        { id: 1, imageUrl: '/api/image/one.jpg', sortOrder: 0 },
        { id: 2, imageUrl: '/api/image/two.jpg', sortOrder: 1 },
      ]),
      loadHomepageBanners: vi.fn().mockResolvedValue(undefined),
      deleteHomepageBanner,
    } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(HomepageBannerAdminPanel, { props: { dashboard } });

    await wrapper.get('button[aria-label="删除第 1 张首页图片"]').trigger('click');
    expect(wrapper.get('[role="alertdialog"]').text()).toContain('删除这张首页图片');
    expect(deleteHomepageBanner).not.toHaveBeenCalled();

    await wrapper.get('.admin-confirm-primary').trigger('click');
    expect(deleteHomepageBanner).toHaveBeenCalledWith(1);
  });
});
