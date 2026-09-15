import {flushPromises, shallowMount} from '@vue/test-utils'
import {reactive} from 'vue'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import Index from '@/views/Index.vue'
import {getSite} from '@/api/index'
import {getCategories} from '@/api/category'

vi.mock('@/api/index', () => ({getSite: vi.fn()}))
vi.mock('@/api/category', () => ({getCategories: vi.fn()}))
vi.mock('@/auth/session', async importOriginal => ({...await importOriginal(), readUser: () => null}))

let wrapper
beforeEach(() => {
	getSite.mockResolvedValue({code: 200, data: {
		siteInfo: {}, categoryList: [{name: '训练经验', description: '旧说明'}], tagList: [],
	}})
	getCategories.mockReset()
})
afterEach(() => wrapper?.unmount())

function mountShell() {
	const route = reactive({name: 'training', path: '/training/admin/categories', params: {trainingPath: 'admin/categories'}, meta: {}})
	wrapper = shallowMount(Index, {global: {
		mocks: {$route: route, $store: {state: {}, commit: vi.fn()}, scrollToTop: vi.fn()},
		stubs: {'router-view': true, 'router-link': true, 'el-backtop': true, AppIcon: true},
	}})
	return route
}

describe('category metadata freshness', () => {
	it('reloads category descriptions when returning from administration without a full page reload', async () => {
		const route = mountShell()
		await flushPromises()
		expect(wrapper.vm.categoryList[0].description).toBe('旧说明')
		getCategories.mockResolvedValue({code: 200, data: [{name: '训练经验', description: '保存后的说明'}]})
		Object.assign(route, {name: 'category', path: '/category/训练经验', params: {name: '训练经验'}})
		await flushPromises()
		expect(getCategories).toHaveBeenCalledOnce()
		expect(wrapper.vm.taxonomyPageProps.categoryList[0].description).toBe('保存后的说明')
	})

	it('keeps existing categories if the metadata refresh fails', async () => {
		const route = mountShell()
		await flushPromises()
		getCategories.mockRejectedValue(new Error('网络不可用'))
		Object.assign(route, {name: 'category', path: '/category/训练经验', params: {name: '训练经验'}})
		await flushPromises()
		expect(wrapper.vm.categoryList[0].description).toBe('旧说明')
	})
})
