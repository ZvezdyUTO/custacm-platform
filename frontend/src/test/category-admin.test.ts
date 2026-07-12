// Author: huangbingrui.awa
import { flushPromises, mount } from '@vue/test-utils';
import { ref } from 'vue';
import { describe, expect, it, vi } from 'vitest';
import CategoryAdminPanel from '../components/CategoryAdminPanel.vue';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard';

describe('category admin panel', () => {
  it('loads, creates, renames and deletes categories', async () => {
    vi.stubGlobal('confirm', vi.fn(() => true));
    const dashboard = {
      adminCategories: ref({ list: [{ id: 3, name: '题解', color: '#8B1E3F' }], pageNum: 1, pageSize: 10, pages: 1, total: 1 }),
      adminTags: ref({ list: [{ id: 8, name: 'DP', color: '#245A73' }], pageNum: 1, pageSize: 10, pages: 1, total: 1 }),
      loadAdminCategories: vi.fn().mockResolvedValue(undefined),
      loadAdminTags: vi.fn().mockResolvedValue(undefined),
      createCategory: vi.fn().mockResolvedValue(undefined),
      updateCategory: vi.fn().mockResolvedValue(undefined),
      deleteCategory: vi.fn().mockResolvedValue(undefined),
      createTag: vi.fn().mockResolvedValue(undefined),
      deleteTag: vi.fn().mockResolvedValue(undefined),
    } as unknown as ReturnType<typeof usePlatformDashboard>;
    const wrapper = mount(CategoryAdminPanel, { props: { dashboard } });
    await vi.waitFor(() => expect(dashboard.loadAdminCategories).toHaveBeenCalledWith(1, 10));
    await flushPromises();

    await wrapper.get('.category-create-form input').setValue('算法');
    await wrapper.get('.category-create-form').trigger('submit');
    await flushPromises();
    expect(dashboard.createCategory).toHaveBeenCalledWith('算法', '#8B1E3F');

    await wrapper.get('.category-admin-row input').setValue('赛事题解');
    await wrapper.get('.category-admin-row').trigger('submit');
    await flushPromises();
    expect(dashboard.updateCategory).toHaveBeenCalledWith(3, '赛事题解', '#8B1E3F');

    await wrapper.get('.danger-button').trigger('click');
    await flushPromises();
    expect(dashboard.deleteCategory).toHaveBeenCalledWith(3);

    expect(wrapper.find('.tag-grid input').exists()).toBe(false);
    expect(wrapper.find('.tag-grid button[type="submit"]').exists()).toBe(false);
    await wrapper.get('.tag-create-form input').setValue('图论');
    await wrapper.get('.tag-create-form').trigger('submit');
    await flushPromises();
    expect(dashboard.createTag).toHaveBeenCalledWith('图论');

    await wrapper.get('.tag-grid .danger-button').trigger('click');
    await flushPromises();
    expect(dashboard.deleteTag).toHaveBeenCalledWith(8);
  });
});
