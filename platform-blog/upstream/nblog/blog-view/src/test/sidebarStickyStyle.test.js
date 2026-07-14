// Author: huangbingrui.awa
import {describe, expect, it} from 'vitest'
import {readFileSync} from 'node:fs'
import {resolve} from 'node:path'

const indexSource = readFileSync(resolve(process.cwd(), 'src/views/Index.vue'), 'utf8')
const featuredSource = readFileSync(resolve(process.cwd(), 'src/components/sidebar/FeaturedBlog.vue'), 'utf8')
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

	it('keeps a page gutter and extra space above the home featured section', () => {
		expect(indexSource).toContain('padding-inline: 2rem;')
		expect(indexSource).toContain('padding-inline: 1rem;')
		expect(indexSource).toContain('--home-canvas: #f2ede3;')
		expect(indexSource).toContain('--home-canvas: #171513;')
		expect(indexSource).toContain(':global(html[data-theme="dark"] .site.is-home)')
		expect(featuredSource).toContain('margin: 1.5rem 0 2rem;')
	})

	it('keeps the author sidebar on article pages and moves featured articles into the home content', () => {
		expect(indexSource).toContain('v-if="$route.name === \'blog\'" class="m-mobile-hide sidebar-column"')
		expect(indexSource).toContain('<FeaturedBlog v-if="$route.name === \'home\'"')
		expect(indexSource).toContain('<router-view v-if="$route.name !== \'home\'"')
		expect(indexSource.indexOf('<FeaturedBlog')).toBeLessThan(indexSource.indexOf('<router-view v-if="$route.name !== \'home\'"'))
		expect(indexSource).not.toContain('<Tags')
		expect(indexSource).not.toContain('import Tags')
		expect(tocSource).not.toContain('positionFixedSelector')
		expect(tocSource).not.toContain('.m-toc.is-position-fixed')
	})
})
