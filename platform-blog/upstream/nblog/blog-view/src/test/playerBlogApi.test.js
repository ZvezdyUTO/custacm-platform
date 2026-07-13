// Author: huangbingrui.awa
import {beforeEach, describe, expect, it, vi} from 'vitest'

const {request} = vi.hoisted(() => ({request: vi.fn()}))
vi.mock('@/plugins/axios', () => ({default: request}))

import {createMyBlog, deleteMyBlog, deleteMyImage, getMyBlog, getMyBlogs, getMyDeletedBlogs, restoreMyBlog, updateMyBlog, uploadMyImage} from '@/api/player-blog'

describe('player blog API', () => {
	beforeEach(() => request.mockReset())

	it('uses the current-user list endpoint with an explicit bearer token', async () => {
		request.mockResolvedValue({code: 200, data: {blogs: {list: []}}})
		await getMyBlogs('token-value', {pageNum: 2})
		expect(request).toHaveBeenCalledWith(expect.objectContaining({
			url: 'player/blogs', method: 'GET', headers: {Authorization: 'Bearer token-value'}, params: {pageNum: 2},
		}))
	})

	it('keeps all mutations on the player-owned endpoint', async () => {
		request.mockResolvedValue({code: 200})
		await getMyBlog('token-value', 9)
		await createMyBlog('token-value', {title: '题解'})
		await updateMyBlog('token-value', {id: 9, title: '题解'})
		await deleteMyBlog('token-value', 9)
		expect(request.mock.calls.map(([config]) => [config.method, config.url])).toEqual([
			['GET', 'player/blog'], ['POST', 'player/blog'], ['PUT', 'player/blog'], ['DELETE', 'player/blog'],
		])
		expect(request.mock.calls.every(([config]) => config.headers.Authorization === 'Bearer token-value')).toBe(true)
	})

	it('lists and restores only through the authenticated player recycle bin routes', async () => {
		request.mockResolvedValue({code: 200, data: {blogs: {list: []}}})
		await getMyDeletedBlogs('token-value', {pageNum: 2})
		await restoreMyBlog('token-value', 9)
		expect(request.mock.calls.map(([config]) => [config.method, config.url])).toEqual([
			['GET', 'player/blogs/recycle-bin'], ['PUT', 'player/blog/restore'],
		])
		expect(request.mock.calls.every(([config]) => config.headers.Authorization === 'Bearer token-value')).toBe(true)
	})

	it('uploads and deletes player-owned image assets with an explicit bearer token', async () => {
		request.mockResolvedValue({code: 200, data: {id: 12}})
		const file = new File(['image'], 'diagram.png', {type: 'image/png'})
		await uploadMyImage('token-value', file, 'ARTICLE_CONTENT')
		await deleteMyImage('token-value', 12)
		const upload = request.mock.calls[0][0]
		expect(upload.url).toBe('player/images')
		expect(upload.headers).toEqual({Authorization: 'Bearer token-value'})
		expect(upload.data.get('file')).toBe(file)
		expect(upload.data.get('purpose')).toBe('ARTICLE_CONTENT')
		expect(request.mock.calls[1][0]).toMatchObject({url: 'player/images/12', method: 'DELETE'})
	})
})
