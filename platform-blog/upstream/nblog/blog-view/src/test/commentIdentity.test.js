// Author: huangbingrui.awa
import {mount} from '@vue/test-utils'
import {createStore} from 'vuex'
import {describe, expect, it, vi} from 'vitest'
import Comment from '@/components/comment/Comment.vue'
import commentSource from '@/components/comment/Comment.vue?raw'
import commentFormSource from '@/components/comment/CommentForm.vue?raw'

describe('comment identity', () => {
	it('shows a small username for account comments and omits it for anonymous comments', () => {
		Object.defineProperty(window, 'localStorage', {configurable: true, value: {getItem: vi.fn(() => null), setItem: vi.fn(), removeItem: vi.fn()}})
		const store = createStore({state: {
			allComment: 2, closeComment: 0, parentCommentId: -1, siteInfo: {}, comments: [
				{id: 1, nickname: '夏依冰', username: 'player-24', avatar: '', website: null, adminComment: false, createTime: '', content: '评论', replyComments: []},
				{id: 2, nickname: '游客', username: null, avatar: '', website: null, adminComment: false, createTime: '', content: '游客评论', replyComments: []},
			],
		}})
		const wrapper = mount(Comment, {global: {plugins: [store], mocks: {$route: {fullPath: '/blog/1'}, $filters: {dateFormat: () => ''}}, stubs: {CommentForm: true, RouterLink: true, ElButton: true}}})

		expect(wrapper.findAll('.comment-username')).toHaveLength(1)
		expect(wrapper.get('.comment-username').text()).toBe('player-24')
	})

	it('uses a stable comment grid and Anthropic charcoal actions', () => {
		expect(commentSource).toContain('class="comment-header"')
		expect(commentSource).toContain('grid-template-columns: 48px minmax(0, 1fr);')
		expect(commentSource).toContain('class="comment-reply-button"')
		expect(commentSource).not.toContain('<div class="border"></div>')
		expect(commentFormSource).toContain('class="comment-form-layout"')
		expect(commentFormSource).toContain('class="comment-submit-button"')
		expect(commentFormSource).toContain('background: var(--anthropic-dark);')
		expect(commentFormSource.match(/font-size: 12px;/g)?.length).toBeGreaterThanOrEqual(2)
	})
})
