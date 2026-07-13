// Author: huangbingrui.awa
import {afterEach, describe, expect, it, vi} from 'vitest'
import {articleDownloadFilename, retryAfterSeconds, saveArticleDownload} from '@/util/articleDownload'

describe('article downloads', () => {
	afterEach(() => {
		vi.restoreAllMocks()
		document.body.innerHTML = ''
	})

	it('creates a safe archive filename', () => {
		expect(articleDownloadFilename('训练/A:*?', 9)).toBe('训练_A___.zip')
		expect(articleDownloadFilename('   ', 9)).toBe('article-9.zip')
		expect(articleDownloadFilename('题'.repeat(255), 9)).toBe(`${'题'.repeat(80)}-9.zip`)
	})

	it('starts a browser download and releases the object URL', () => {
		const blob = new Blob(['zip-data'], {type: 'application/zip'})
		const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
		vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:article')
		const revoke = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})

		saveArticleDownload(blob, '题解.md')

		expect(click).toHaveBeenCalledOnce()
		expect(revoke).toHaveBeenCalledWith('blob:article')
		expect(document.querySelector('a')).toBeNull()
	})

	it('reads Retry-After and falls back to one second', () => {
		expect(retryAfterSeconds({response: {headers: {'retry-after': '17'}}})).toBe(17)
		expect(retryAfterSeconds({response: {headers: {}}})).toBe(1)
	})
})
