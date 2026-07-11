// Author: huangbingrui.awa
import {beforeEach, describe, expect, it, vi} from 'vitest'

const {request} = vi.hoisted(() => ({request: vi.fn()}))

vi.mock('@/plugins/axios', () => ({default: request}))

import {
	getCurrentProfile,
	replaceCurrentProfileLinks,
	updateCurrentProfile,
} from '@/api/profile'

describe('profile API', () => {
	beforeEach(() => request.mockReset())

	it('loads the current user with an explicit bearer token', async () => {
		request.mockResolvedValue({code: 200, data: {username: 'alice', links: []}})

		await expect(getCurrentProfile('token-value')).resolves.toMatchObject({username: 'alice'})
		expect(request).toHaveBeenCalledWith(expect.objectContaining({
			url: 'player/me',
			method: 'GET',
			headers: {Authorization: 'Bearer token-value'},
		}))
	})

	it('updates nickname and signature through the profile endpoint', async () => {
		request.mockResolvedValue({code: 200, data: {nickname: 'Alice', signature: 'Hello'}})

		await updateCurrentProfile('token-value', {nickname: 'Alice', signature: 'Hello'})

		expect(request).toHaveBeenCalledWith(expect.objectContaining({
			url: 'player/me/profile',
			method: 'PATCH',
			data: {nickname: 'Alice', signature: 'Hello'},
		}))
	})

	it('replaces the complete ordered link list', async () => {
		const links = [{label: 'GitHub', url: 'https://github.com/alice'}]
		request.mockResolvedValue({code: 200, data: {links}})

		await replaceCurrentProfileLinks('token-value', links)

		expect(request).toHaveBeenCalledWith(expect.objectContaining({
			url: 'player/me/profile-links',
			method: 'PUT',
			data: {links},
		}))
	})
})
