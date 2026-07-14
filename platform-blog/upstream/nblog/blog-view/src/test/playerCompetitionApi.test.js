// Author: huangbingrui.awa
import {beforeEach, describe, expect, it, vi} from 'vitest'

const {request} = vi.hoisted(() => ({request: vi.fn()}))
vi.mock('@/plugins/axios', () => ({default: request}))

import {
	bindCompetitionArticle,
	setAchievementProfileOrder,
	setAchievementProfileVisibility,
	unbindCompetitionArticle,
} from '@/api/player-competition'

describe('player competition API', () => {
	beforeEach(() => request.mockReset().mockResolvedValue({code: 200}))

	it('updates only the current recipient visibility with an explicit bearer token', async () => {
		await setAchievementProfileVisibility('token-value', 31, 71, true)

		expect(request).toHaveBeenCalledWith({
			url: 'player/competitions/31/awards/71/profile-visibility',
			method: 'PUT',
			headers: {Authorization: 'Bearer token-value'},
			data: {visible: true},
		})
	})

	it('reorders the complete public award id list with an explicit bearer token', async () => {
		await setAchievementProfileOrder('token-value', [73, 71, 72])

		expect(request).toHaveBeenCalledWith({
			url: 'player/competitions/achievement-order',
			method: 'PUT',
			headers: {Authorization: 'Bearer token-value'},
			data: {orderedAwardIds: [73, 71, 72]},
		})
	})

	it('binds and unbinds the current users article with explicit bearer authentication', async () => {
		await bindCompetitionArticle('token-value', 31, 9)
		await unbindCompetitionArticle('token-value', 31, 9)

		expect(request.mock.calls.map(([config]) => [config.method, config.url])).toEqual([
			['POST', 'player/competitions/31/articles/9'],
			['DELETE', 'player/competitions/31/articles/9'],
		])
		expect(request.mock.calls.every(([config]) => config.headers.Authorization === 'Bearer token-value')).toBe(true)
	})

	it('surfaces a failed Result envelope instead of treating it as success', async () => {
		request.mockResolvedValue({
			code: 401,
			msg: '登录已过期',
			errorCode: 'AUTH_UNAUTHORIZED',
		})

		await expect(bindCompetitionArticle('token-value', 31, 9))
			.rejects.toMatchObject({
				message: '登录已过期',
				code: 401,
				errorCode: 'AUTH_UNAUTHORIZED',
			})
	})
})
