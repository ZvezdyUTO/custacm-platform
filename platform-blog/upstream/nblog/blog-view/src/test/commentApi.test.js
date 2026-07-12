// Author: huangbingrui.awa
import {describe, expect, it, vi} from 'vitest'

const {request} = vi.hoisted(() => ({request: vi.fn()}))
vi.mock('@/plugins/axios', () => ({default: request}))

import {submitComment} from '@/api/comment'

describe('authenticated comment API', () => {
	it('submits only through the player route with an explicit bearer token', async () => {
		request.mockResolvedValue({code: 200})
		await submitComment('token-value', {content: '学到了', page: 0, blogId: 9, parentCommentId: -1})
		expect(request).toHaveBeenCalledWith({
			url: 'player/comment',
			method: 'POST',
			headers: {Authorization: 'Bearer token-value'},
			data: {content: '学到了', page: 0, blogId: 9, parentCommentId: -1},
		})
	})
})
