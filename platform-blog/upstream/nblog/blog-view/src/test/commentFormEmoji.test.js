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
})
