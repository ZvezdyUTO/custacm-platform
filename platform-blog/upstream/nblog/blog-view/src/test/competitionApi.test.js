// Author: huangbingrui.awa
import {beforeEach, describe, expect, it, vi} from 'vitest'

const {request} = vi.hoisted(() => ({request: vi.fn()}))
vi.mock('@/plugins/axios', () => ({default: request}))

import {getCompetition, getCompetitions} from '@/api/competition'

describe('public competition API', () => {
	beforeEach(() => request.mockReset())

	it('loads the filtered page anonymously with the complete query contract', async () => {
		const page = {pageNum: 2, pageSize: 10, total: 13, totalPages: 2, list: []}
		request.mockResolvedValue({code: 200, data: page})
		const query = {startYear: 2022, endYear: 2026, category: 'ICPC_ASIA_REGIONAL', pageNum: 2, pageSize: 10}

		await expect(getCompetitions(query)).resolves.toEqual(page)
		expect(request).toHaveBeenCalledWith({
			url: 'competitions',
			method: 'GET',
			params: query,
		})
		expect(request.mock.calls[0][0]).not.toHaveProperty('headers')
	})

	it('loads one public competition without attaching shared credentials', async () => {
		request.mockResolvedValue({code: 200, data: {id: 31, awards: []}})

		await expect(getCompetition(31)).resolves.toMatchObject({id: 31})
		expect(request).toHaveBeenCalledWith({
			url: 'competitions/31',
			method: 'GET',
		})
		expect(request.mock.calls[0][0]).not.toHaveProperty('headers')
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
