// Author: huangbingrui.awa
import {readFileSync} from 'node:fs'
import {flushPromises, mount} from '@vue/test-utils'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'

const SESSION_CHANGE_EVENT = 'custacm:session-change'

const mocks = vi.hoisted(() => ({
	bindCompetitionArticle: vi.fn(),
	clearSession: vi.fn(),
	deleteMyBlog: vi.fn(),
	getCompetitions: vi.fn(),
	getMyBlogs: vi.fn(),
	getMyDeletedBlogs: vi.fn(),
	readToken: vi.fn(),
	readUser: vi.fn(),
	restoreMyBlog: vi.fn(),
	unbindCompetitionArticle: vi.fn(),
}))

vi.mock('element-plus', () => ({ElMessageBox: {confirm: vi.fn()}}))
vi.mock('@/api/competition', () => ({getCompetitions: mocks.getCompetitions}))
vi.mock('@/api/player-blog', () => ({
	deleteMyBlog: mocks.deleteMyBlog,
	getMyBlogs: mocks.getMyBlogs,
	getMyDeletedBlogs: mocks.getMyDeletedBlogs,
	restoreMyBlog: mocks.restoreMyBlog,
}))
vi.mock('@/api/player-competition', () => ({
	bindCompetitionArticle: mocks.bindCompetitionArticle,
	unbindCompetitionArticle: mocks.unbindCompetitionArticle,
}))
vi.mock('@/auth/session', () => ({
	clearSession: mocks.clearSession,
	readToken: mocks.readToken,
	readUser: mocks.readUser,
	SESSION_CHANGE_EVENT: 'custacm:session-change',
}))

import MyArticles from '@/components/profile/MyArticles.vue'

const mountedWrappers = []

function deferred() {
	let resolve
	let reject
	const promise = new Promise((resolvePromise, rejectPromise) => {
		resolve = resolvePromise
		reject = rejectPromise
	})
	return {promise, reject, resolve}
}

function article(id, title, overrides = {}) {
	return {
		id,
		title,
		published: true,
		internal: false,
		category: null,
		commentEnabled: true,
		...overrides,
	}
}

function articlePage(list, pages = 1, categories = []) {
	return {blogs: {list, pages}, categories}
}

function competition(id, username = 'alice', articles = []) {
	return {
		id,
		fullName: `比赛 ${id}`,
		year: 2026,
		participants: [{id: id * 10, username, displayName: username, articles}],
	}
}

function competitionPage(list, totalPages = 1, pageNum = 1) {
	return {pageNum, totalPages, list}
}

function mountArticles() {
	const routerPush = vi.fn()
	const msgSuccess = vi.fn()
	const wrapper = mount(MyArticles, {
		global: {
			mocks: {
				$filters: {dateFormat: value => value || ''},
				$router: {push: routerPush},
				msgSuccess,
			},
			stubs: {AppIcon: true, ElPagination: true, RouterLink: true},
		},
	})
	mountedWrappers.push(wrapper)
	return {msgSuccess, routerPush, wrapper}
}

function relativeLuminance(hex) {
	const channels = [1, 3, 5]
		.map(index => Number.parseInt(hex.slice(index, index + 2), 16) / 255)
		.map(value => value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4)
	return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722
}

function contrastRatio(first, second) {
	const lighter = Math.max(relativeLuminance(first), relativeLuminance(second))
	const darker = Math.min(relativeLuminance(first), relativeLuminance(second))
	return (lighter + 0.05) / (darker + 0.05)
}

describe('article competition bindings', () => {
	afterEach(() => {
		while (mountedWrappers.length) mountedWrappers.pop().unmount()
	})

	beforeEach(() => {
		vi.clearAllMocks()
		mocks.readToken.mockReturnValue('token-value')
		mocks.readUser.mockReturnValue({username: 'alice'})
		mocks.bindCompetitionArticle.mockResolvedValue(undefined)
		mocks.unbindCompetitionArticle.mockResolvedValue(undefined)
		mocks.getMyBlogs.mockResolvedValue(articlePage([
			article(9, '公开题解'),
			article(10, '草稿', {published: false}),
			article(11, '内部复盘', {internal: true}),
		]))
		mocks.getMyDeletedBlogs.mockResolvedValue(articlePage([]))
		mocks.getCompetitions.mockResolvedValue(competitionPage([
			competition(31, 'alice', [{id: 9, title: '公开题解'}]),
		]))
	})

	it('uses article titles as accessible names and lazily reveals binding controls only for public articles', async () => {
		const {wrapper} = mountArticles()
		await flushPromises()

		expect(mocks.getCompetitions).not.toHaveBeenCalled()
		const tabs = wrapper.findAll('.article-view-tabs button')
		expect(tabs[0].attributes('aria-pressed')).toBe('true')
		expect(tabs[1].attributes('aria-pressed')).toBe('false')

		const rows = wrapper.findAll('article.article-row')
		expect(rows).toHaveLength(3)
		expect(rows[0].attributes('aria-labelledby')).toBe('my-article-title-9')
		expect(rows[0].get('#my-article-title-9').text()).toBe('公开题解')
		expect(rows[0].get('[aria-label="查看文章“公开题解”"]').exists()).toBe(true)
		expect(rows[0].get('[aria-label="删除文章“公开题解”"]').exists()).toBe(true)
		expect(wrapper.find('.new-article-button').exists()).toBe(false)
		expect(wrapper.findAll('.competition-panel-toggle')).toHaveLength(1)

		const toggle = wrapper.get('.competition-panel-toggle')
		expect(toggle.attributes('aria-label')).toContain('公开题解')
		expect(toggle.attributes('aria-expanded')).toBe('false')
		await toggle.trigger('click')
		await flushPromises()

		expect(mocks.getCompetitions).toHaveBeenCalledOnce()
		expect(toggle.attributes('aria-expanded')).toBe('true')
		const binding = wrapper.get('.article-competition-bindings > div > button')
		expect(binding.classes()).toContain('is-bound')
		expect(binding.attributes('aria-pressed')).toBe('true')
		expect(binding.attributes('aria-label')).toContain('公开题解')
		expect(binding.attributes('aria-label')).toContain('比赛 31')

		await binding.trigger('click')
		await flushPromises()
		expect(mocks.unbindCompetitionArticle).toHaveBeenCalledWith('token-value', 31, 9)
		expect(wrapper.get('.article-competition-bindings > div > button').attributes('aria-pressed')).toBe('false')
	})

	it('loads every public page only on first expansion and reuses the lifecycle cache after mode switches', async () => {
		mocks.getCompetitions
			.mockResolvedValueOnce(competitionPage([competition(31)], 2, 1))
			.mockResolvedValueOnce(competitionPage([competition(32, 'bob')], 2, 2))
		const {wrapper} = mountArticles()
		await flushPromises()

		expect(mocks.getCompetitions).not.toHaveBeenCalled()
		await wrapper.get('.competition-panel-toggle').trigger('click')
		await flushPromises()
		expect(mocks.getCompetitions).toHaveBeenNthCalledWith(1, {pageNum: 1, pageSize: 100})
		expect(mocks.getCompetitions).toHaveBeenNthCalledWith(2, {pageNum: 2, pageSize: 100})
		expect(wrapper.vm.competitions.map(item => item.id)).toEqual([31])

		await wrapper.get('.competition-panel-toggle').trigger('click')
		await wrapper.get('.competition-panel-toggle').trigger('click')
		await flushPromises()
		expect(mocks.getCompetitions).toHaveBeenCalledTimes(2)

		await wrapper.findAll('.article-view-tabs button')[1].trigger('click')
		await flushPromises()
		await wrapper.findAll('.article-view-tabs button')[0].trigger('click')
		await flushPromises()
		await wrapper.get('.competition-panel-toggle').trigger('click')
		await flushPromises()
		expect(mocks.getCompetitions).toHaveBeenCalledTimes(2)
	})

	it('does not scan competitions when the current page has no bindable public article', async () => {
		mocks.getMyBlogs.mockResolvedValue(articlePage([
			article(10, '草稿', {published: false}),
			article(11, '内部复盘', {internal: true}),
		]))
		const {wrapper} = mountArticles()
		await flushPromises()

		expect(wrapper.find('.competition-panel-toggle').exists()).toBe(false)
		await wrapper.vm.loadCompetitionBindings({force: true})
		expect(mocks.getCompetitions).not.toHaveBeenCalled()
	})

	it('keeps the latest filter and pagination response when older article requests finish later', async () => {
		const initial = deferred()
		const filtered = deferred()
		const latestPage = deferred()
		mocks.getMyBlogs
			.mockReset()
			.mockImplementationOnce(() => initial.promise)
			.mockImplementationOnce(() => filtered.promise)
			.mockImplementationOnce(() => latestPage.promise)
		const {wrapper} = mountArticles()

		wrapper.vm.filters.title = '目标文章'
		wrapper.vm.search()
		wrapper.vm.changePage(2)
		expect(mocks.getMyBlogs).toHaveBeenNthCalledWith(2, 'token-value', {title: '目标文章', pageNum: 1, pageSize: 6})
		expect(mocks.getMyBlogs).toHaveBeenNthCalledWith(3, 'token-value', {title: '目标文章', pageNum: 2, pageSize: 6})

		latestPage.resolve(articlePage([article(203, '最新分页结果')], 3))
		await flushPromises()
		filtered.resolve(articlePage([article(202, '过时筛选结果')], 2))
		initial.resolve(articlePage([article(201, '过时初始结果')]))
		await flushPromises()

		expect(wrapper.vm.pageNum).toBe(2)
		expect(wrapper.vm.totalPages).toBe(3)
		expect(wrapper.vm.blogs.map(item => item.title)).toEqual(['最新分页结果'])
	})

	it('keeps the latest active or recycle mode response when the previous mode finishes later', async () => {
		const active = deferred()
		const recycle = deferred()
		mocks.getMyBlogs.mockReset().mockImplementationOnce(() => active.promise)
		mocks.getMyDeletedBlogs.mockReset().mockImplementationOnce(() => recycle.promise)
		const {wrapper} = mountArticles()

		wrapper.vm.switchMode('recycle')
		recycle.resolve(articlePage([article(302, '回收站最新结果', {deletedAt: '2026-07-01T00:00:00Z'})]))
		await flushPromises()
		active.resolve(articlePage([article(301, '当前文章过时结果')]))
		await flushPromises()

		expect(wrapper.vm.viewMode).toBe('recycle')
		expect(wrapper.vm.blogs.map(item => item.title)).toEqual(['回收站最新结果'])
	})

	it('invalidates old requests on account changes, reloads the new account, and clears on logout', async () => {
		let token = 'alice-token'
		let user = {username: 'alice'}
		mocks.readToken.mockImplementation(() => token)
		mocks.readUser.mockImplementation(() => user)
		const alice = deferred()
		const bob = deferred()
		mocks.getMyBlogs
			.mockReset()
			.mockImplementationOnce(() => alice.promise)
			.mockImplementationOnce(() => bob.promise)
		const {wrapper} = mountArticles()

		token = 'bob-token'
		user = {username: 'bob'}
		window.dispatchEvent(new Event(SESSION_CHANGE_EVENT))
		expect(mocks.getMyBlogs).toHaveBeenNthCalledWith(2, 'bob-token', {title: '', pageNum: 1, pageSize: 6})

		bob.resolve(articlePage([article(402, 'Bob 的文章')]))
		await flushPromises()
		alice.resolve(articlePage([article(401, 'Alice 的过时文章')]))
		await flushPromises()
		expect(wrapper.vm.currentUsername).toBe('bob')
		expect(wrapper.vm.blogs.map(item => item.title)).toEqual(['Bob 的文章'])

		token = ''
		user = null
		window.dispatchEvent(new Event(SESSION_CHANGE_EVENT))
		await flushPromises()
		expect(wrapper.vm.blogs).toEqual([])
		expect(wrapper.vm.competitions).toEqual([])
		expect(wrapper.vm.loading).toBe(false)
	})

	it('does not let an in-flight refresh overwrite a successful binding mutation', async () => {
		mocks.getMyBlogs.mockResolvedValue(articlePage([article(9, '公开题解')]))
		const staleRefresh = deferred()
		mocks.getCompetitions
			.mockReset()
			.mockResolvedValueOnce(competitionPage([competition(31)]))
			.mockImplementationOnce(() => staleRefresh.promise)
		const {wrapper} = mountArticles()
		await flushPromises()
		await wrapper.get('.competition-panel-toggle').trigger('click')
		await flushPromises()

		await wrapper.get('.article-competition-bindings > header button').trigger('click')
		await wrapper.get('.article-competition-bindings > div > button').trigger('click')
		await flushPromises()
		expect(mocks.bindCompetitionArticle).toHaveBeenCalledWith('token-value', 31, 9)
		expect(wrapper.get('.article-competition-bindings > div > button').attributes('aria-pressed')).toBe('true')

		staleRefresh.resolve(competitionPage([competition(31)]))
		await flushPromises()
		expect(wrapper.get('.article-competition-bindings > div > button').attributes('aria-pressed')).toBe('true')
	})

	it('clears an expired session when a protected binding returns an envelope 401', async () => {
		mocks.getMyBlogs.mockResolvedValue(articlePage([article(9, '公开题解')]))
		mocks.getCompetitions.mockResolvedValue(competitionPage([competition(31)]))
		mocks.bindCompetitionArticle.mockRejectedValue({code: 401})
		const {routerPush, wrapper} = mountArticles()
		await flushPromises()
		await wrapper.get('.competition-panel-toggle').trigger('click')
		await flushPromises()
		await wrapper.get('.article-competition-bindings > div > button').trigger('click')
		await flushPromises()

		expect(mocks.clearSession).toHaveBeenCalledOnce()
		expect(routerPush).toHaveBeenCalledWith({path: '/training/login', query: {returnTo: '/profile'}})
	})

	it('clears an expired session when the article list returns an envelope 401', async () => {
		mocks.getMyBlogs.mockRejectedValue({code: 401})
		const {routerPush} = mountArticles()
		await flushPromises()

		expect(mocks.clearSession).toHaveBeenCalledOnce()
		expect(routerPush).toHaveBeenCalledWith({path: '/training/login', query: {returnTo: '/profile'}})
	})

	it('keeps the session and displays the backend message when a protected binding returns 403', async () => {
		mocks.getMyBlogs.mockResolvedValue(articlePage([article(9, '公开题解')]))
		mocks.getCompetitions.mockResolvedValue(competitionPage([competition(31)]))
		mocks.bindCompetitionArticle.mockRejectedValue({response: {status: 403, data: {msg: '当前账号不再是该比赛参赛用户'}}})
		const {routerPush, wrapper} = mountArticles()
		await flushPromises()
		await wrapper.get('.competition-panel-toggle').trigger('click')
		await flushPromises()
		await wrapper.get('.article-competition-bindings > div > button').trigger('click')
		await flushPromises()

		expect(mocks.clearSession).not.toHaveBeenCalled()
		expect(routerPush).not.toHaveBeenCalled()
		expect(wrapper.get('.competition-panel-message[role="alert"]').text()).toBe('当前账号不再是该比赛参赛用户')
	})

	it('uses an AA bound state in the fixed light application palette', () => {
		const source = readFileSync('src/components/profile/MyArticles.vue', 'utf8')
		expect(contrastRatio('#126244', '#eaf4ef')).toBeGreaterThanOrEqual(4.5)
		expect(source).toContain('.competition-binding-list > button.is-bound { background: #eaf4ef; color: #126244;')
		expect(source).not.toContain('data-theme="dark"')
	})
})
