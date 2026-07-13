// Author: huangbingrui.awa
import {beforeEach, describe, expect, it, vi} from 'vitest'

const mocks = vi.hoisted(() => ({
	downloadBlog: vi.fn(),
	getBlogById: vi.fn(),
	readToken: vi.fn(),
	readUser: vi.fn(),
	clearSession: vi.fn(),
	saveArticleDownload: vi.fn(),
}))

vi.mock('@/api/blog', () => ({
	downloadBlog: mocks.downloadBlog,
	getBlogById: mocks.getBlogById,
}))
vi.mock('@/auth/session', () => ({
	clearSession: mocks.clearSession,
	readToken: mocks.readToken,
	readUser: mocks.readUser,
	SESSION_CHANGE_EVENT: 'custacm:session-change',
}))
vi.mock('@/util/articleDownload', () => ({
	articleDownloadFilename: (title, id) => `${title}-${id}.zip`,
	retryAfterSeconds: error => Number(error.response.headers['retry-after']),
	saveArticleDownload: mocks.saveArticleDownload,
}))

import Blog from '@/views/blog/Blog.vue'
import blogSource from '@/views/blog/Blog.vue?raw'

function view() {
	return {
		downloading: false,
		blogId: 9,
		blog: {title: '题解'},
		$route: {fullPath: '/blog/9'},
		$router: {push: vi.fn()},
		refreshUser: vi.fn(),
		msgSuccess: vi.fn(),
		msgError: vi.fn(),
	}
}

describe('article download UI', () => {
	beforeEach(() => {
		vi.clearAllMocks()
		mocks.readToken.mockReturnValue('token-value')
	})

	it('shows the action only for a logged-in user and saves the returned Markdown', async () => {
		expect(blogSource).toContain('v-if="authUser" class="item article-download-link"')
		const blob = new Blob(['# 题解'])
		mocks.downloadBlog.mockResolvedValue(blob)
		const target = view()

		await Blog.methods.downloadArticle.call(target)

		expect(mocks.downloadBlog).toHaveBeenCalledWith('token-value', 9)
		expect(mocks.saveArticleDownload).toHaveBeenCalledWith(blob, '题解-9.zip')
		expect(target.msgSuccess).toHaveBeenCalledWith('文章下载已开始')
		expect(target.downloading).toBe(false)
	})

	it('shows the server cooldown without clearing the session', async () => {
		mocks.downloadBlog.mockRejectedValue({response: {status: 429, headers: {'retry-after': '17'}}})
		const target = view()

		await Blog.methods.downloadArticle.call(target)

		expect(target.msgError).toHaveBeenCalledWith('下载过于频繁，请 17 秒后再试')
		expect(mocks.clearSession).not.toHaveBeenCalled()
		expect(target.downloading).toBe(false)
	})
})
