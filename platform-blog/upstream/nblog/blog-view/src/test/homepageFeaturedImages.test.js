/**
 * @author huangbingrui.awa
 */
import {describe, expect, it, vi, afterEach} from 'vitest'
import {readFileSync} from 'node:fs'
import {resolve} from 'node:path'

vi.mock('@/plugins/axios', () => ({default: vi.fn()}))

import axios from '@/plugins/axios'
import {getHomepageFeaturedImages} from '@/api/index'

const source = readFileSync(
	resolve(process.cwd(), 'src/components/index/FeaturedImageMarquee.vue'),
	'utf8'
)
const indexSource = readFileSync(resolve(process.cwd(), 'src/views/Index.vue'), 'utf8')

describe('homepage featured image marquee', () => {
	afterEach(() => vi.clearAllMocks())

	it('loads the complete public ordered collection without a bearer token', () => {
		getHomepageFeaturedImages()

		expect(axios).toHaveBeenCalledWith({
			url: 'homepage-featured-images',
			method: 'GET'
		})
	})

	it('sits directly after the homepage hero and renders three loop copies', () => {
		expect(indexSource).toMatch(/<Header v-if="\$route\.name==='home'"\/>\s*<FeaturedImageMarquee/)
		expect(source).toContain('v-for="copyIndex in 3"')
		expect(source).toContain('centerOnMiddleCopy')
		expect(source).toContain('normalizeLoopPosition')
	})

	it('pauses for hover, focus, drag and reduced-motion preferences', () => {
		expect(source).toContain('@mouseenter="hovered = true"')
		expect(source).toContain('!this.hovered && !this.focused && !this.dragging')
		expect(source).toContain("matchMedia('(prefers-reduced-motion: reduce)')")
		expect(source).toContain('autoScrollRemainder')
		expect(source).toContain('Math.trunc(this.autoScrollRemainder)')
	})

	it('supports pointer, touchpad, keyboard and blurred edge interaction', () => {
		expect(source).toContain('@pointerdown="startDrag"')
		expect(source).toContain('@keydown.left.prevent="nudge(-1)"')
		expect(source).toContain('overflow-x: auto')
		expect(source).toContain('backdrop-filter: blur(7px)')
		expect(source.indexOf('this.dragMoved = true')).toBeLessThan(source.indexOf('setPointerCapture?.(event.pointerId)'))
	})

	it('renders compressed thumbnails and opens the shared on-demand original viewer', () => {
		expect(source).toContain(':src="image.thumbnailUrl || image.imageUrl"')
		expect(source).toContain('<ManagedImageViewer ref="imageViewer" />')
		expect(source).toContain('this.$refs.imageViewer?.open(thumbnailUrl, image.imageUrl')
		expect(source).toContain('class="featured-image-open"')
	})

	it('moves into the hero fade instead of leaving a large white gap', () => {
		expect(source).toContain('margin-top: clamp(-14px, -.8vw, -6px)')
		expect(source).toContain('padding: 0 0 clamp(42px, 5vw, 72px)')
	})
})
