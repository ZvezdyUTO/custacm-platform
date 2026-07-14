// Author: huangbingrui.awa
import {beforeEach, describe, expect, it, vi} from 'vitest'

const mocks = vi.hoisted(() => ({
	confirm: vi.fn(),
	deleteMyBlog: vi.fn(),
	restoreMyBlog: vi.fn(),
	readToken: vi.fn(),
	readUser: vi.fn(),
}))

vi.mock('element-plus', () => ({ElMessageBox: {confirm: mocks.confirm}}))
vi.mock('@/api/player-blog', () => ({
	deleteMyBlog: mocks.deleteMyBlog,
	getMyBlogs: vi.fn(),
	getMyDeletedBlogs: vi.fn(),
	restoreMyBlog: mocks.restoreMyBlog,
}))
vi.mock('@/auth/session', () => ({clearSession: vi.fn(), readToken: mocks.readToken, readUser: mocks.readUser}))

import MyArticles from '@/components/profile/MyArticles.vue'
import source from '@/components/profile/MyArticles.vue?raw'

describe('article recycle bin UI', () => {
	beforeEach(() => {
		vi.clearAllMocks()
		mocks.readToken.mockReturnValue('token-value')
		mocks.confirm.mockResolvedValue(undefined)
		mocks.deleteMyBlog.mockResolvedValue(undefined)
		mocks.restoreMyBlog.mockResolvedValue(undefined)
	})

	it('explains the fixed retention and never presents deletion as permanent', () => {
		expect(source).toContain('当前文章')
		expect(source).toContain('回收站')
		expect(source).toContain('固定保留')
		expect(source).not.toContain('无法恢复')
	})

	it('moves an article to the recycle bin and allows the owner to restore it', async () => {
		const target = {
			deletingId: null,
			restoringId: null,
			load: vi.fn().mockResolvedValue(undefined),
			msgSuccess: vi.fn(),
			goLogin: vi.fn(),
			handleError: vi.fn(),
		}
		const blog = {id: 9, title: '区间 DP'}

		await MyArticles.methods.remove.call(target, blog)
		expect(mocks.confirm).toHaveBeenCalledWith(expect.stringContaining('保留 7 天'), '移入回收站', expect.any(Object))
		expect(mocks.deleteMyBlog).toHaveBeenCalledWith('token-value', 9)
		expect(target.msgSuccess).toHaveBeenCalledWith('文章已移入回收站')

		await MyArticles.methods.restore.call(target, blog)
		expect(mocks.restoreMyBlog).toHaveBeenCalledWith('token-value', 9)
		expect(target.msgSuccess).toHaveBeenCalledWith('文章已恢复')
	})
})
