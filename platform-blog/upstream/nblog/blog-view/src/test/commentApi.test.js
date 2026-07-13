// Author: huangbingrui.awa
import {describe, expect, it, vi} from 'vitest'

const {request} = vi.hoisted(() => ({request: vi.fn()}))
vi.mock('@/plugins/axios', () => ({default: request}))

import {getCommentListByQuery, submitComment} from '@/api/comment'

describe('authenticated comment API', () => {
	it('reads comments only for an article page', async () => {
		request.mockResolvedValue({code: 200})
		await getCommentListByQuery({blogId: 9, pageNum: 2, pageSize: 5})
		expect(request).toHaveBeenCalledWith({
			url: 'comments',
			method: 'GET',
			params: {blogId: 9, pageNum: 2, pageSize: 5},
		})
	})

	it('submits only through the player route with an explicit bearer token', async () => {
		request.mockResolvedValue({code: 200})
		await submitComment('token-value', {
			content: '学到了',
			blogId: 9,
			parentCommentId: -1,
			nickname: '伪造昵称',
			email: 'fake@example.com',
			website: 'https://example.com',
			notice: true,
		})
		expect(request).toHaveBeenCalledWith({
			url: 'player/comment',
			method: 'POST',
			headers: {Authorization: 'Bearer token-value'},
			data: {content: '学到了', blogId: 9, parentCommentId: -1},
		})
	})
})
