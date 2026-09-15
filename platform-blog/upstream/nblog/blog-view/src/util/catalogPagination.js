const CARD_MIN_WIDTH = 300
const CARD_GAP = 24

export function catalogColumns(width) {
	return Math.max(1, Math.min(6, Math.floor((width + CARD_GAP) / (CARD_MIN_WIDTH + CARD_GAP))))
}

export function catalogPageSize(columns, layout = 'grid') {
	return layout === 'list' ? 12 : columns * Math.max(4, Math.ceil(12 / columns))
}

// The API uses fixed-size batches. Keep display pages independent of that size,
// fetching only their intersecting batches plus the last batch for an exact count.
// A pager belongs to one filter and one authentication state; never share it globally.
export function createCatalogPager(fetchPage) {
	const batches = new Map()
	function batch(page) {
		if (!batches.has(page)) {
			const request = Promise.resolve().then(() => fetchPage(page)).then(response => {
				const data = response?.data
				if (response?.code !== 200 || !Array.isArray(data?.list) || !Number.isInteger(data.totalPage) || data.totalPage < 0) {
					throw new Error('无法加载文章')
				}
				return data
			}).catch(error => {
				batches.delete(page)
				throw error
			})
			batches.set(page, request)
		}
		return batches.get(page)
	}

	return {
		async readPage(requestedPage, pageSize) {
			const first = await batch(1)
			if (!first.list.length) return {list: [], total: 0, totalPage: 0, page: 1}
			const batchSize = first.list.length
			const last = first.totalPage > 1 ? await batch(first.totalPage) : first
			const total = (Math.max(1, first.totalPage) - 1) * batchSize + last.list.length
			const totalPage = Math.ceil(total / pageSize)
			const page = Math.max(1, Math.min(requestedPage, totalPage))
			const start = (page - 1) * pageSize
			const end = Math.min(start + pageSize, total)
			const firstBatch = Math.floor(start / batchSize) + 1
			const lastBatch = Math.ceil(end / batchSize)
			const results = await Promise.all(Array.from({length: lastBatch - firstBatch + 1}, (_, index) => batch(firstBatch + index)))
			const offset = start % batchSize
			return {list: results.flatMap(result => result.list).slice(offset, offset + end - start), total, totalPage, page}
		},
	}
}
