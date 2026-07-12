// Author: huangbingrui.awa
import { flushPromises, mount } from '@vue/test-utils';
import { ref } from 'vue';
import { describe, expect, it, vi } from 'vitest';
import ArticleAdminPanel from '../components/ArticleAdminPanel.vue';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard';

describe('article admin panel', () => {
  it('loads articles and allows an administrator to feature a published article', async () => {
    const loadAdminArticles = vi.fn().mockResolvedValue(undefined);
    const updateArticleFeatured = vi.fn().mockResolvedValue(undefined);
    const deleteArticle = vi.fn().mockResolvedValue(undefined);
    const dashboard = {
      adminArticles: ref({
        categories: [{ id: 2, name: '题解' }],
        blogs: { pageNum: 1, pageSize: 10, pages: 1, total: 1, list: [{
          id: 7, title: '区间 DP', firstPicture: '', createTime: '2026-07-01T10:00:00',
          updateTime: '2026-07-12T10:00:00', published: true, recommend: false, top: false,
          category: { id: 2, name: '题解' },
        }] },
      }),
      loadAdminArticles,
      updateArticleFeatured,
      deleteArticle,
    } as unknown as ReturnType<typeof usePlatformDashboard>;

    const wrapper = mount(ArticleAdminPanel, { props: { dashboard } });
    await vi.waitFor(() => expect(loadAdminArticles).toHaveBeenCalled());
    await flushPromises();
    const toggle = wrapper.get('.featured-toggle');
    expect(toggle.attributes('aria-pressed')).toBe('false');
    await toggle.trigger('click');
    expect(updateArticleFeatured).toHaveBeenCalledWith(7, true);
    expect(wrapper.find('.operation-toast').exists()).toBe(false);

    await wrapper.get('.article-delete-button').trigger('click');
    expect(wrapper.get('[role="alertdialog"]').text()).toContain('无法恢复');
    expect(deleteArticle).not.toHaveBeenCalled();
    await wrapper.get('.confirm-delete-button').trigger('click');
    expect(deleteArticle).toHaveBeenCalledWith(7);
  });
});
