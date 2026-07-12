// Author: huangbingrui.awa
import {flushPromises, mount} from '@vue/test-utils'
import {afterAll, afterEach, beforeAll, describe, expect, it, vi} from 'vitest'
import LiveMarkdownEditor from '@/components/article/LiveMarkdownEditor.vue'
import ArticleEditor from '@/views/article/ArticleEditor.vue'

afterEach(() => vi.restoreAllMocks())

let originalClientRects
let originalBoundingRect
let originalStorageDescriptor

beforeAll(() => {
	originalClientRects = Range.prototype.getClientRects
	originalBoundingRect = Range.prototype.getBoundingClientRect
	originalStorageDescriptor = Object.getOwnPropertyDescriptor(window, 'localStorage')
	Range.prototype.getClientRects = () => []
	Range.prototype.getBoundingClientRect = () => ({left: 0, right: 0, top: 0, bottom: 0, width: 0, height: 0})
	const values = new Map()
	Object.defineProperty(window, 'localStorage', {
		configurable: true,
		value: {
			clear: () => values.clear(),
			getItem: key => values.has(key) ? values.get(key) : null,
			removeItem: key => values.delete(key),
			setItem: (key, value) => values.set(key, String(value)),
		},
	})
})

afterAll(() => {
	Range.prototype.getClientRects = originalClientRects
	Range.prototype.getBoundingClientRect = originalBoundingRect
	if (originalStorageDescriptor) Object.defineProperty(window, 'localStorage', originalStorageDescriptor)
})

describe('LiveMarkdownEditor', () => {
	it('renders standard math and keeps v-model content synchronized', async () => {
		const wrapper = mount(LiveMarkdownEditor, {props: {modelValue: '题解正文\n\n$x+y$\n\n| 复杂度 |\n| --- |\n| $O(n)$ |'}})
		await flushPromises()
		await new Promise(resolve => requestAnimationFrame(resolve))

		expect(wrapper.find('.cm-content').exists()).toBe(true)
		expect(wrapper.findAll('.katex').length).toBeGreaterThanOrEqual(2)
		expect(wrapper.get('button[title="在一行文字中插入公式"]').text()).toBe('行内公式')
		expect(wrapper.get('button[title="插入独占一行的公式"]').text()).toBe('块公式')

		const headingSelect = wrapper.get('select[aria-label="标题级别"]')
		await headingSelect.setValue('headingSmall')
		expect(wrapper.vm.view.state.doc.toString()).toContain('### ')

		const tableButton = wrapper.get('button[title="插入 GFM 表格"]')
		await tableButton.trigger('click')
		await flushPromises()

		expect(wrapper.emitted('update:modelValue')).toBeTruthy()
		expect(wrapper.emitted('dirty')).toBeTruthy()
		expect(wrapper.vm.view.state.doc.toString()).toContain('| --- | --- |')
		wrapper.unmount()
	})

	it('uses the native high-contrast text selection instead of a second CodeMirror selection layer', async () => {
		const wrapper = mount(LiveMarkdownEditor, {props: {modelValue: '```cpp\nint main() { return 0; }\n```\n\n正文'}})
		await flushPromises()
		wrapper.vm.view.dispatch({selection: {anchor: wrapper.vm.view.state.doc.length}})
		await new Promise(resolve => requestAnimationFrame(resolve))

		expect(wrapper.find('.cm-selectionLayer').exists()).toBe(false)
		expect(wrapper.find('.cm-codeblock-widget .hljs-keyword').exists()).toBe(true)
		wrapper.unmount()
	})

	it('shows one upload image button and keeps the native file input hidden', async () => {
		const wrapper = mount(LiveMarkdownEditor)
		await flushPromises()

		const input = wrapper.get('.markdown-toolbar input[type="file"]')
		expect(input.attributes()).toHaveProperty('hidden')
		expect(wrapper.get('button[title="上传正文图片"]').text()).toBe('上传图片')
		expect(wrapper.findAll('.markdown-toolbar input[type="file"]')).toHaveLength(1)
		wrapper.unmount()
	})

	it('renders standard Markdown images as live preview widgets', async () => {
		const wrapper = mount(LiveMarkdownEditor, {props: {modelValue: '![流程图](/api/image/assets/123e4567-e89b-12d3-a456-426614174000/thumbnail.jpg)\n\n正文'}})
		await flushPromises()
		wrapper.vm.view.dispatch({selection: {anchor: wrapper.vm.view.state.doc.length}})
		await new Promise(resolve => requestAnimationFrame(resolve))

		expect(wrapper.find('.cm-image-widget').exists()).toBe(true)
		expect(wrapper.text()).not.toContain('/api/image/assets/123e4567-e89b-12d3-a456-426614174000/thumbnail.jpg')
		wrapper.unmount()
	})

	it('does not activate the heading toolbar when the article body is clicked repeatedly', async () => {
		window.localStorage.clear()
		const replace = vi.fn()
		const wrapper = mount(ArticleEditor, {
			global: {
				mocks: {$route: {params: {}, fullPath: '/write'}, $router: {replace, push: vi.fn()}},
				stubs: {RouterLink: {template: '<a><slot /></a>'}},
			},
		})
		await wrapper.setData({loading: false})
		await flushPromises()

		const contentField = wrapper.get('.content-field')
		expect(contentField.element.tagName).toBe('DIV')
		const content = wrapper.get('.cm-content')
		for (let index = 0; index < 10; index++) await content.trigger('click')

		expect(wrapper.findComponent(LiveMarkdownEditor).vm.view.state.doc.toString()).toBe('')
		expect(replace).toHaveBeenCalledOnce()
		wrapper.unmount()
	})

	it('groups title and description to the left of the cover upload', async () => {
		window.localStorage.clear()
		const wrapper = mount(ArticleEditor, {
			global: {
				mocks: {$route: {params: {}, fullPath: '/write'}, $router: {replace: vi.fn(), push: vi.fn()}},
				stubs: {RouterLink: {template: '<a><slot /></a>'}},
			},
		})
		await wrapper.setData({loading: false})
		await flushPromises()

		const basics = wrapper.get('.article-basics')
		expect(basics.get('.article-basics-fields input[aria-label="标题"]').attributes('maxlength')).toBe('100')
		expect(basics.get('.article-basics-fields textarea[aria-label="文章简介"]').attributes('maxlength')).toBe('255')
		expect(basics.text()).toContain('0/100')
		expect(basics.text()).toContain('0/255')
		expect(basics.findComponent({name: 'ArticleCoverUpload'}).exists()).toBe(true)
		wrapper.unmount()
	})

	it('immediately deletes a temporary content asset after its Markdown image is removed', async () => {
		window.localStorage.clear()
		const wrapper = mount(ArticleEditor, {
			global: {
				mocks: {$route: {params: {}, fullPath: '/write'}, $router: {replace: vi.fn(), push: vi.fn()}},
				stubs: {RouterLink: {template: '<a><slot /></a>'}},
			},
		})
		await wrapper.setData({loading: false})
		const asset = {id: 9, publicId: '123e4567-e89b-12d3-a456-426614174000', purpose: 'ARTICLE_CONTENT'}
		await wrapper.setData({temporaryAssets: [asset]})
		const deleteTemporaryAsset = vi.spyOn(wrapper.vm, 'deleteTemporaryAsset').mockResolvedValue()

		wrapper.vm.cleanupRemovedTemporaryContentImages('', '![图](/api/image/assets/123e4567-e89b-12d3-a456-426614174000/thumbnail.jpg)')
		await flushPromises()

		expect(deleteTemporaryAsset).toHaveBeenCalledWith(asset)
		wrapper.unmount()
	})

	it('selects existing tags and creates a new tag with Enter', async () => {
		window.localStorage.clear()
		const wrapper = mount(ArticleEditor, {
			global: {
				mocks: {$route: {params: {}, fullPath: '/write'}, $router: {replace: vi.fn(), push: vi.fn()}},
				stubs: {RouterLink: {template: '<a><slot /></a>'}},
			},
		})
		await wrapper.setData({loading: false, tags: [{id: 7, name: '动态规划'}]})
		await flushPromises()

		const existingTag = wrapper.get('.tag-options button')
		await existingTag.trigger('click')
		expect(existingTag.attributes('aria-pressed')).toBe('true')
		expect(wrapper.vm.form.tagList).toEqual([7])

		await wrapper.get('.tag-add-row input').setValue('图论')
		await wrapper.get('.tag-add-row input').trigger('keydown.enter')
		expect(wrapper.vm.form.tagList).toEqual([7, '图论'])
		expect(wrapper.get('button[title="移除标签 图论"]').exists()).toBe(true)

		await existingTag.trigger('click')
		expect(wrapper.vm.form.tagList).toEqual(['图论'])
		wrapper.unmount()
	})

	it('does not create a tag when Enter confirms IME composition', async () => {
		window.localStorage.clear()
		const wrapper = mount(ArticleEditor, {
			global: {
				mocks: {$route: {params: {}, fullPath: '/write'}, $router: {replace: vi.fn(), push: vi.fn()}},
				stubs: {RouterLink: {template: '<a><slot /></a>'}},
			},
		})
		await wrapper.setData({loading: false, newTagName: 'bfs', isComposingTag: true})
		await flushPromises()

		const input = wrapper.get('.tag-add-row input')
		await input.trigger('keydown', {key: 'Enter', isComposing: true, keyCode: 229})
		expect(wrapper.vm.form.tagList).toEqual([])
		expect(wrapper.vm.newTagName).toBe('bfs')

		await input.trigger('compositionend')
		await input.trigger('keydown', {key: 'Enter'})
		expect(wrapper.vm.form.tagList).toEqual(['bfs'])
		expect(wrapper.vm.newTagName).toBe('')
		wrapper.unmount()
	})
})
