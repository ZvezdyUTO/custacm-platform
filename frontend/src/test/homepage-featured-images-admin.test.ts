// Author: huangbingrui.awa
import { mount } from '@vue/test-utils';
import { ref } from 'vue';
import { describe, expect, it, vi } from 'vitest';
import HomepageFeaturedImagesAdminPanel from '../components/HomepageFeaturedImagesAdminPanel.vue';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard';

function image(id: number, sortOrder: number) {
  return {
    id,
    imageUrl: `/api/image/featured-${id}.jpg`,
    thumbnailUrl: `/api/image/featured-${id}-thumbnail.jpg`,
    sortOrder,
  };
}

describe('homepage featured images admin panel', () => {
  it('renders the ordered collection and sends every id when moving one image', async () => {
    const reorderHomepageFeaturedImages = vi.fn().mockResolvedValue(undefined);
    const dashboard = {
      homepageFeaturedImages: ref([image(1, 0), image(2, 1), image(3, 2)]),
      loadHomepageFeaturedImages: vi.fn().mockResolvedValue(undefined),
      reorderHomepageFeaturedImages,
    } as unknown as ReturnType<typeof usePlatformDashboard>;

    const wrapper = mount(HomepageFeaturedImagesAdminPanel, { props: { dashboard } });
    await wrapper.get('button[aria-label="将第 2 张精选图片向左移动"]').trigger('click');

    expect(reorderHomepageFeaturedImages).toHaveBeenCalledWith([2, 1, 3]);
    expect(wrapper.findAll('.homepage-featured-card')).toHaveLength(3);
    expect(wrapper.get('.homepage-featured-preview img').attributes('src')).toBe('/api/image/featured-1-thumbnail.jpg');
    expect(wrapper.text()).toContain('3 / 12');
  });

  it('hides the add card at the twelve-image limit', () => {
    const dashboard = {
      homepageFeaturedImages: ref(Array.from({ length: 12 }, (_, index) => image(index + 1, index))),
      loadHomepageFeaturedImages: vi.fn().mockResolvedValue(undefined),
    } as unknown as ReturnType<typeof usePlatformDashboard>;

    const wrapper = mount(HomepageFeaturedImagesAdminPanel, { props: { dashboard } });

    expect(wrapper.find('.homepage-featured-add').exists()).toBe(false);
    expect(wrapper.text()).toContain('最多 12 张');
  });

  it('uses the shared confirmation dialog and allows deleting the final image', async () => {
    const deleteHomepageFeaturedImage = vi.fn().mockResolvedValue(undefined);
    const dashboard = {
      homepageFeaturedImages: ref([image(1, 0)]),
      loadHomepageFeaturedImages: vi.fn().mockResolvedValue(undefined),
      deleteHomepageFeaturedImage,
    } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(HomepageFeaturedImagesAdminPanel, { props: { dashboard } });

    await wrapper.get('button[aria-label="删除第 1 张精选图片"]').trigger('click');
    expect(wrapper.get('[role="alertdialog"]').text()).toContain('删除这张精选图片');
    expect(deleteHomepageFeaturedImage).not.toHaveBeenCalled();

    await wrapper.get('.admin-confirm-primary').trigger('click');
    expect(deleteHomepageFeaturedImage).toHaveBeenCalledWith(1);
  });
});
