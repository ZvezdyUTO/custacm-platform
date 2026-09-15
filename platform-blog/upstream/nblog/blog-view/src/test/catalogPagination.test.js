import {describe, expect, it, vi} from 'vitest'
import {catalogColumns, catalogPageSize, createCatalogPager} from '@/util/catalogPagination'

function source(total, batchSize) {
	const rows = Array.from({length: total}, (_, index) => ({id: index + 1}))
	return vi.fn(async page => ({code: 200, data: {
		list: rows.slice((page - 1) * batchSize, page * batchSize),
		totalPage: Math.ceil(total / batchSize),
	}}))
}

describe('responsive catalog pagination', () => {
	it.each([358, 600, 624, 947, 948, 1100, 1272, 1596, 1920, 2400])('fills complete display rows at content width %i', async width => {
		const columns = catalogColumns(width)
		const pageSize = catalogPageSize(columns)
		const pager = createCatalogPager(source(83, 6))
		const first = await pager.readPage(1, pageSize)
		const second = await pager.readPage(2, pageSize)
		expect(pageSize).toBeGreaterThanOrEqual(12)
		expect(first.list.length % columns).toBe(0)
		expect(second.list.length % columns).toBe(0)
		expect(second.list[0].id).toBe(first.list.at(-1).id + 1)
		expect(first.total).toBe(83)
	})

	it.each([5, 6])('reads all articles once across display pages despite API batches of %i', async batchSize => {
		const fetchPage = source(53, batchSize)
		const pager = createCatalogPager(fetchPage)
		const results = []
		for (let page = 1; page <= 4; page += 1) results.push(await pager.readPage(page, 16))
		expect(results.map(result => result.list.length)).toEqual([16, 16, 16, 5])
		expect(results.flatMap(result => result.list.map(item => item.id))).toEqual(Array.from({length: 53}, (_, index) => index + 1))
		expect(fetchPage).toHaveBeenCalledTimes(Math.ceil(53 / batchSize))
		expect((await pager.readPage(99, 16)).page).toBe(4)
	})

	it('does not download all remaining articles just to fill the first display page', async () => {
		const fetchPage = source(1000, 6)
		const pager = createCatalogPager(fetchPage)
		const result = await pager.readPage(1, 12)
		expect(result.total).toBe(1000)
		expect(fetchPage.mock.calls.map(([page]) => page).sort((a, b) => a - b)).toEqual([1, 2, 167])
		await pager.readPage(1, 12)
		expect(fetchPage).toHaveBeenCalledTimes(3)
	})

	it('allows a partial final row only when there are no remaining articles', async () => {
		const pager = createCatalogPager(source(11, 6))
		const result = await pager.readPage(1, 12)
		expect(result).toMatchObject({total: 11, totalPage: 1, page: 1})
		expect(result.list).toHaveLength(11)
	})

	it('handles an empty catalog without requesting another batch', async () => {
		const fetchPage = source(0, 6)
		expect(await createCatalogPager(fetchPage).readPage(1, 12)).toEqual({list: [], total: 0, totalPage: 0, page: 1})
		expect(fetchPage).toHaveBeenCalledTimes(1)
	})

	it('retries a failed batch instead of caching a partial display page', async () => {
		const fetchPage = source(53, 6)
		const normalFetch = fetchPage.getMockImplementation()
		let failed = false
		fetchPage.mockImplementation(page => {
			if (page === 2 && !failed) { failed = true; return Promise.reject(new Error('offline')) }
			return normalFetch(page)
		})
		const pager = createCatalogPager(fetchPage)
		await expect(pager.readPage(1, 12)).rejects.toThrow('offline')
		expect((await pager.readPage(1, 12)).list).toHaveLength(12)
		expect(fetchPage.mock.calls.filter(([page]) => page === 2)).toHaveLength(2)
	})
})
