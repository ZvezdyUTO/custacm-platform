// Author: huangbingrui.awa
import {beforeEach, describe, expect, it, vi} from 'vitest'

const {request, readToken} = vi.hoisted(() => ({request: vi.fn(), readToken: vi.fn()}))

vi.mock('@/plugins/axios', () => ({default: request}))
vi.mock('@/auth/session', () => ({readToken}))

import {
	changeCurrentPassword,
	getCurrentProfile,
	getPublicProfile,
	replaceCurrentProfileLinks,
	updateCurrentProfile,
} from '@/api/profile'

describe('profile API', () => {
	beforeEach(() => {
		request.mockReset()
		readToken.mockReset()
		readToken.mockReturnValue(null)
	})

	it('loads the current user with an explicit bearer token', async () => {
		request.mockResolvedValue({code: 200, data: {username: 'alice', links: []}})

		await expect(getCurrentProfile('token-value')).resolves.toMatchObject({username: 'alice'})
		expect(request).toHaveBeenCalledWith(expect.objectContaining({
			url: 'player/me',
			method: 'GET',
			headers: {Authorization: 'Bearer token-value'},
		}))
	})

	it('loads an article author profile anonymously with an encoded username', async () => {
		request.mockResolvedValue({code: 200, data: {username: 'alice smith', links: []}})

		await expect(getPublicProfile('alice smith')).resolves.toMatchObject({username: 'alice smith'})
		expect(request).toHaveBeenCalledWith({
			url: 'profiles/alice%20smith',
			method: 'GET',
			headers: undefined,
		})
	})

	it('loads login-required public achievements with an explicit shared token', async () => {
		readToken.mockReturnValue('member-token')
		request.mockResolvedValue({code: 200, data: {username: 'alice', achievements: [{awardId: 72}]}})

		await getPublicProfile('alice')

		expect(request).toHaveBeenCalledWith({
			url: 'profiles/alice',
			method: 'GET',
			headers: {Authorization: 'Bearer member-token'},
		})
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

	it('changes the current password with an explicit bearer token', async () => {
		request.mockResolvedValue({code: 200})

		await changeCurrentPassword('token-value', 'old-password', 'new-password')

		expect(request).toHaveBeenCalledWith({
			url: 'player/me/password',
			method: 'PATCH',
			data: {oldPassword: 'old-password', newPassword: 'new-password'},
			headers: {Authorization: 'Bearer token-value'},
		})
	})
})
