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
	const restoreArticle = vi.fn().mockResolvedValue(undefined);
	const backupAllArticles = vi.fn().mockResolvedValue(undefined);
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
		restoreArticle,
		backupAllArticles,
    } as unknown as ReturnType<typeof usePlatformDashboard>;

    const wrapper = mount(ArticleAdminPanel, { props: { dashboard } });
    await vi.waitFor(() => expect(loadAdminArticles).toHaveBeenCalled());
    await flushPromises();
		await wrapper.get('.article-backup-button').trigger('click');
		expect(wrapper.get('[role="alertdialog"]').text()).toContain('下载全部文章备份');
		expect(wrapper.get('[role="alertdialog"]').text()).toContain('回收站内容');
		expect(backupAllArticles).not.toHaveBeenCalled();
		await wrapper.get('.admin-confirm-primary').trigger('click');
		await flushPromises();
	expect(backupAllArticles).toHaveBeenCalledOnce();
	expect(wrapper.get('[role="status"]').text()).toContain('托管图片备份已开始下载');
    const toggle = wrapper.get('.featured-toggle');
    expect(toggle.attributes('aria-pressed')).toBe('false');
    await toggle.trigger('click');
    expect(updateArticleFeatured).toHaveBeenCalledWith(7, true);
    expect(wrapper.find('.operation-toast').exists()).toBe(false);

    await wrapper.get('.article-delete-button').trigger('click');
	expect(wrapper.get('[role="alertdialog"]').text()).toContain('固定保留 7 天');
	expect(wrapper.get('[role="alertdialog"]').text()).toContain('可以恢复');
    expect(deleteArticle).not.toHaveBeenCalled();
    await wrapper.get('.confirm-delete-button').trigger('click');
	expect(deleteArticle).toHaveBeenCalledWith(7);

	await wrapper.findAll('.article-admin-tabs button')[1].trigger('click');
	await flushPromises();
	expect(loadAdminArticles).toHaveBeenLastCalledWith(expect.any(Object), true);
	await wrapper.get('.article-restore-button').trigger('click');
	expect(restoreArticle).toHaveBeenCalledWith(7);
	});
});
