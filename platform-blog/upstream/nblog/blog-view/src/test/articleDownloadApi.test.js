// Author: huangbingrui.awa
import {beforeEach, describe, expect, it, vi} from 'vitest'

const {request} = vi.hoisted(() => ({request: vi.fn()}))
vi.mock('@/plugins/axios', () => ({default: request}))

import {downloadBlog} from '@/api/blog'

describe('article download API', () => {
	beforeEach(() => request.mockReset())

	it('uses the protected blob endpoint with an explicit bearer token', async () => {
		const blob = new Blob(['markdown'])
		request.mockResolvedValue(blob)

		expect(await downloadBlog('token-value', 9)).toBe(blob)
		expect(request).toHaveBeenCalledWith({
			url: 'player/blog/download',
			method: 'GET',
			headers: {Authorization: 'Bearer token-value'},
			params: {id: 9},
			responseType: 'blob',
		})
	})
})
