// Author: huangbingrui.awa
import {shallowMount} from '@vue/test-utils'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import Nav from '@/components/index/Nav.vue'

beforeEach(() => {
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
		global: {
			stubs: {
				AppIcon: {
					props: ['name'],
					template: '<span class="app-icon" :data-icon="name"></span>',
				},
				'router-link': {template: '<a><slot /></a>'},
				'el-dropdown': {template: '<div><slot /><slot name="dropdown" /></div>'},
				'el-dropdown-menu': {template: '<div><slot /></div>'},
				'el-dropdown-item': {template: '<div><slot /></div>'},
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

	it('renders the compact theme switch before the account entry', async () => {
		const {wrapper} = mountNav()
		const toggle = wrapper.get('.nav-theme-toggle')

		expect(toggle.attributes('role')).toBe('switch')
		expect(toggle.attributes('aria-checked')).toBe('false')
		await toggle.trigger('click')
		expect(toggle.attributes('aria-checked')).toBe('true')
		expect(window.localStorage.setItem).toHaveBeenCalledWith('custacm.theme', 'dark')
	})
})

describe('article search placement', () => {
	it('does not render the retired top-right search box', () => {
		const {wrapper} = mountNav()

		expect(wrapper.find('.m-search').exists()).toBe(false)
		expect(wrapper.find('input[type="search"]').exists()).toBe(false)
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

describe('account navigation', () => {
	it('opens the retained profile page without using the retired About route', () => {
		const {wrapper, push} = mountNav()

		wrapper.vm.accountCommand('profile')

		expect(push).toHaveBeenCalledWith('/profile')
	})
})
