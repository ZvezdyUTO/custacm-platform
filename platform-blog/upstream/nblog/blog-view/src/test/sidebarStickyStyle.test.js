// Author: huangbingrui.awa
import {describe, expect, it} from 'vitest'
import {readFileSync} from 'node:fs'
import {resolve} from 'node:path'

const indexSource = readFileSync(resolve(process.cwd(), 'src/views/Index.vue'), 'utf8')
const tocSource = readFileSync(resolve(process.cwd(), 'src/components/sidebar/Tocbot.vue'), 'utf8')

describe('desktop sticky sidebars', () => {
	it('keeps both sidebar stacks below the fixed navigation without trapping page scroll', () => {
		expect(indexSource.match(/class="sticky-sidebar/g)).toHaveLength(2)
		expect(indexSource).toContain('position: sticky;')
		expect(indexSource).toContain('top: var(--sidebar-sticky-top);')
		expect(indexSource).toContain('max-height: calc(100vh - var(--sidebar-sticky-top) - 16px);')
		expect(indexSource).toContain('overflow-y: auto;')
		expect(indexSource).toContain('overflow-x: clip;')
	})

	it('puts the article table of contents first and leaves positioning to the shared wrapper', () => {
		expect(indexSource.indexOf('<Tocbot')).toBeLessThan(indexSource.indexOf('<FeaturedBlog'))
		expect(tocSource).not.toContain('positionFixedSelector')
		expect(tocSource).not.toContain('.m-toc.is-position-fixed')
	})
})
