import {describe, expect, it} from 'vitest'
import {sanitizeHtml} from '@/util/sanitizeHtml'

describe('sanitizeHtml', () => {
	it('removes scripts, event handlers, and dangerous URLs', () => {
		const result = sanitizeHtml('<img src="x" onerror="alert(1)"><script>alert(2)</script><a href="javascript:alert(3)">bad</a>')

		expect(result).not.toContain('onerror')
		expect(result).not.toContain('<script')
		expect(result).not.toContain('javascript:')
	})

	it('keeps safe article markup', () => {
		const result = sanitizeHtml('<h2 id="heading">Title</h2><a href="https://example.com" rel="noopener">safe</a>')

		expect(result).toContain('<h2 id="heading">Title</h2>')
		expect(result).toContain('href="https://example.com"')
	})
})
