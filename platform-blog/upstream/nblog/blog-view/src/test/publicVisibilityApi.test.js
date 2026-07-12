// Author: huangbingrui.awa
import {beforeEach, describe, expect, it, vi} from 'vitest'

const {request, readToken} = vi.hoisted(() => ({request: vi.fn(), readToken: vi.fn()}))

vi.mock('@/plugins/axios', () => ({default: request}))
vi.mock('@/auth/session', () => ({readToken}))

import {getBlogList} from '@/api/home'
import {getBlogListByCategoryName} from '@/api/category'
import {getBlogListByTagName} from '@/api/tag'
import {getSearchBlogList} from '@/api/blog'
import {getSite} from '@/api/index'

describe('visibility-aware public Blog reads', () => {
	beforeEach(() => {
		request.mockReset()
		readToken.mockReset()
		readToken.mockReturnValue(null)
	})

	it('stays anonymous without a shared session', () => {
		getBlogList(1)
		expect(request).toHaveBeenCalledWith(expect.objectContaining({url: 'blogs', headers: undefined}))
	})

	it('explicitly attaches the shared bearer token to every visibility-aware read', () => {
		readToken.mockReturnValue('token-value')
		getBlogList(1)
		getBlogListByCategoryName('训练', 1)
		getBlogListByTagName('动态规划', 1)
		getSearchBlogList('内部')
		getSite()

		for (const call of request.mock.calls) {
			expect(call[0].headers).toEqual({Authorization: 'Bearer token-value'})
		}
	})
})
