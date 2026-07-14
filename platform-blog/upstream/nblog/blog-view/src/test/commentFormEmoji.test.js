// Author: huangbingrui.awa
import {mount} from '@vue/test-utils'
import {createStore} from 'vuex'
import {describe, expect, it, vi} from 'vitest'
import CommentForm from '@/components/comment/CommentForm.vue'

const ElInput = {
	props: ['modelValue'],
	emits: ['update:modelValue'],
	template: '<textarea :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)"></textarea>',
}
const ElForm = {template: '<form><slot /></form>'}
const ElFormItem = {template: '<div><slot /></div>'}
const ElButton = {template: '<button type="button"><slot /></button>'}

describe('Noto emoji picker', () => {
	it('opens an accessible local picker and inserts Unicode at the caret', async () => {
		const store = createStore({
			state: {parentCommentId: -1, commentForm: {content: 'AB'}, commentQuery: {}},
		})
		const wrapper = mount(CommentForm, {
			global: {
				plugins: [store],
				stubs: {ElInput, ElForm, ElFormItem, ElButton},
				directives: {throttle: () => {}},
				mocks: {$notify: vi.fn()},
			},
		})
		const textarea = wrapper.get('textarea').element
		textarea.focus()
		textarea.setSelectionRange(1, 1)

		await wrapper.get('.emoji-trigger').trigger('mousedown')
		await wrapper.get('.emoji-trigger').trigger('click')

		expect(wrapper.get('.emoji-box').attributes('role')).toBe('dialog')
		expect(wrapper.get('.emoji-list img').attributes('src')).toMatch(/^\/emoji\/noto\//)
		await wrapper.get('.emoji-list').trigger('click')
		expect(store.state.commentForm.content).toBe('A😀B')
	})

	it('keeps keyboard focus inside the picker and returns it to the textarea on Escape', async () => {
		const store = createStore({state: {parentCommentId: -1, commentForm: {content: ''}, commentQuery: {}}})
		const wrapper = mount(CommentForm, {
			attachTo: document.body,
			global: {
				plugins: [store],
				stubs: {ElInput, ElForm, ElFormItem, ElButton},
				directives: {throttle: () => {}},
				mocks: {$notify: vi.fn()},
			},
		})
		const textarea = wrapper.get('textarea').element
		textarea.focus()
		await wrapper.get('.emoji-trigger').trigger('click')
		const box = wrapper.get('.emoji-box')
		const lastEmoji = wrapper.findAll('.emoji-list').at(-1)
		lastEmoji.element.focus()
		await lastEmoji.trigger('keydown', {key: 'Tab'})
		expect(document.activeElement).toBe(box.find('button').element)

		await box.trigger('keydown', {key: 'Escape'})
		expect(wrapper.find('.emoji-box').exists()).toBe(false)
		expect(document.activeElement).toBe(textarea)
		wrapper.unmount()
	})
})
