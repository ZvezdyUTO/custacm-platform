// Author: huangbingrui.awa
import {beforeEach, describe, expect, it, vi} from 'vitest'

const {request, readToken} = vi.hoisted(() => ({request: vi.fn(), readToken: vi.fn()}))
vi.mock('@/plugins/axios', () => ({default: request}))
vi.mock('@/auth/session', () => ({readToken}))

import {getCompetition, getCompetitions} from '@/api/competition'

describe('public competition API', () => {
	beforeEach(() => {
		request.mockReset()
		readToken.mockReset()
		readToken.mockReturnValue(null)
	})

	it('loads the filtered page anonymously with the complete query contract', async () => {
		const page = {pageNum: 2, pageSize: 10, total: 13, totalPages: 2, list: []}
		request.mockResolvedValue({code: 200, data: page})
		const query = {startYear: 2022, endYear: 2026, category: 'ICPC_ASIA_REGIONAL', pageNum: 2, pageSize: 10}

		await expect(getCompetitions(query)).resolves.toEqual(page)
		expect(request).toHaveBeenCalledWith({
			url: 'competitions',
			method: 'GET',
			headers: undefined,
			params: query,
		})
	})

	it('loads one public competition anonymously when no session exists', async () => {
		request.mockResolvedValue({code: 200, data: {id: 31, awards: []}})

		await expect(getCompetition(31)).resolves.toMatchObject({id: 31})
		expect(request).toHaveBeenCalledWith({
			url: 'competitions/31',
			method: 'GET',
			headers: undefined,
		})
	})

	it('explicitly attaches the shared token for login-required awards', async () => {
		readToken.mockReturnValue('member-token')
		request.mockResolvedValue({code: 200, data: {id: 31, awards: [{id: 72}]}})

		await getCompetition(31)

		expect(request).toHaveBeenCalledWith(expect.objectContaining({
			url: 'competitions/31',
			headers: {Authorization: 'Bearer member-token'},
		}))
	})

	it('rejects a non-success envelope instead of exposing partial data', async () => {
		request.mockResolvedValue({code: 400, errorCode: 'BAD_REQUEST', msg: '起始年份不能大于结束年份'})

		await expect(getCompetitions()).rejects.toMatchObject({
			message: '起始年份不能大于结束年份',
			code: 400,
			errorCode: 'BAD_REQUEST',
		})
	})
})
