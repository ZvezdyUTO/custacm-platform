// Author: huangbingrui.awa
import {shallowMount} from '@vue/test-utils'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {getSearchBlogList} from '@/api/blog'
import Nav from '@/components/index/Nav.vue'

vi.mock('@/api/blog', () => ({
	getSearchBlogList: vi.fn(),
}))

beforeEach(() => {
	getSearchBlogList.mockReset()
	Object.defineProperty(window, 'localStorage', {
		configurable: true,
		value: {
			getItem: vi.fn(() => null),
			setItem: vi.fn(),
			removeItem: vi.fn(),
		},
	})
})

function mountNav(route = {name: 'training', path: '/training/multiple', fullPath: '/training/multiple?oj=CODEFORCES'}) {
	const push = vi.fn()
	const wrapper = shallowMount(Nav, {
		props: {categoryList: []},
		global: {
			stubs: {
				'router-link': {template: '<a><slot /></a>'},
				'el-dropdown': {template: '<div><slot /><slot name="dropdown" /></div>'},
				'el-dropdown-menu': {template: '<div><slot /></div>'},
				'el-dropdown-item': {template: '<div><slot /></div>'},
				'el-input': {
					props: ['modelValue'],
					template: '<div><input class="search-input-stub" :value="modelValue" v-bind="$attrs"><slot name="suffix" /></div>',
				},
			},
			mocks: {
				$route: route,
				$router: {push},
				$store: {state: {clientSize: {clientWidth: 1440, clientHeight: 900}}},
			},
		},
	})
	return {wrapper, push}
}

describe('training navigation dropdown', () => {
	it('uses a click-only button trigger instead of a navigation link', () => {
		const {wrapper} = mountNav()
		const dropdown = wrapper.get('.nav-training-dropdown')
		const trigger = wrapper.get('.nav-training-trigger')

		expect(dropdown.attributes('trigger')).toBe('click')
		expect(trigger.element.tagName).toBe('BUTTON')
		expect(trigger.attributes('type')).toBe('button')
		expect(trigger.attributes('href')).toBeUndefined()
	})

	it('navigates only after a training menu command is selected', async () => {
		const {wrapper, push} = mountNav()

		await wrapper.get('.nav-training-trigger').trigger('click')
		expect(push).not.toHaveBeenCalled()
		wrapper.vm.trainingRoute('single')
		expect(push).toHaveBeenCalledWith('/training/single')
	})

	it('does not mark training navigation active on administrator pages', () => {
		const queryPage = mountNav().wrapper
		const adminPage = mountNav({name: 'training', path: '/training/admin/users', fullPath: '/training/admin/users'}).wrapper

		expect(queryPage.get('.nav-training-trigger').classes()).toContain('active')
		expect(adminPage.get('.nav-training-trigger').classes()).not.toContain('active')
	})
})

describe('public article search', () => {
	it('uses the short placeholder without a clear button and never requests while typing', () => {
		const {wrapper} = mountNav()
		wrapper.vm.searchOpen = true

		wrapper.vm.handleSearchInput()

		expect(wrapper.get('.search-input-stub').attributes('placeholder')).toBe('搜索文章')
		expect(wrapper.get('.search-input-stub').attributes('aria-label')).toBe('搜索文章')
		expect(wrapper.get('.search-input-stub').attributes('clearable')).toBeUndefined()
		expect(getSearchBlogList).not.toHaveBeenCalled()
		expect(wrapper.vm.searchOpen).toBe(false)
	})

	it('requests only on submit and then exposes public article suggestions', async () => {
		const {wrapper} = mountNav()
		const results = [{id: 7, title: '公开文章', description: '这是一段文章简介'}]
		getSearchBlogList.mockResolvedValue({code: 200, data: results})
		wrapper.vm.queryString = '  训练  '

		await wrapper.vm.submitSearch()

		expect(getSearchBlogList).toHaveBeenCalledWith('训练')
		expect(wrapper.vm.queryResult).toEqual(results)
		expect(wrapper.vm.searchOpen).toBe(true)
		expect(wrapper.vm.searchLoading).toBe(false)
		expect(wrapper.get('.m-search-panel .description').text()).toBe('这是一段文章简介')
	})

	it('does not request blank or invalid submissions', async () => {
		const {wrapper} = mountNav()

		wrapper.vm.queryString = '   '
		await wrapper.vm.submitSearch()
		wrapper.vm.queryString = '100%'
		await wrapper.vm.submitSearch()

		expect(getSearchBlogList).not.toHaveBeenCalled()
	})
})

describe('login return path', () => {
	it('keeps the current Blog location when opening login from the nav', () => {
		const {wrapper} = mountNav()

		expect(wrapper.vm.loginTarget).toEqual({
			path: '/training/login',
			query: {returnTo: '/training/multiple?oj=CODEFORCES'},
		})
	})
})
