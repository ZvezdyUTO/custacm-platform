/**
 * @author huangbingrui.awa
 */
import {describe, expect, it} from 'vitest'
import {homepageBannerOpacity, homepageBannerPointerRatio} from '@/util/homepageBanner'

describe('homepage banner opacity', () => {
	it('keeps the only image visible', () => {
		expect(homepageBannerOpacity(1, 0.75, 0)).toBe(1)
	})

	it('maps the left and right edges to the first and last images', () => {
		expect(homepageBannerOpacity(5, 0, 0)).toBe(1)
		expect(homepageBannerOpacity(5, 0, 4)).toBe(0)
		expect(homepageBannerOpacity(5, 1, 0)).toBe(0)
		expect(homepageBannerOpacity(5, 1, 4)).toBe(1)
	})

	it('crossfades only the adjacent images', () => {
		expect(homepageBannerOpacity(3, 0.25, 0)).toBe(0.5)
		expect(homepageBannerOpacity(3, 0.25, 1)).toBe(0.5)
		expect(homepageBannerOpacity(3, 0.25, 2)).toBe(0)
	})

	it('keeps two images solid outside a narrow center blend band', () => {
		expect(homepageBannerOpacity(2, 0.4, 0)).toBe(1)
		expect(homepageBannerOpacity(2, 0.4, 1)).toBe(0)
		expect(homepageBannerOpacity(2, 0.5, 0)).toBeCloseTo(0.5)
		expect(homepageBannerOpacity(2, 0.5, 1)).toBeCloseTo(0.5)
		expect(homepageBannerOpacity(2, 0.6, 0)).toBe(0)
		expect(homepageBannerOpacity(2, 0.6, 1)).toBe(1)
	})

	it('maps the absolute pointer position onto the banner horizontal axis', () => {
		expect(homepageBannerPointerRatio(350, 100, 1000)).toBe(0.25)
		expect(homepageBannerPointerRatio(50, 100, 1000)).toBe(0)
		expect(homepageBannerPointerRatio(1200, 100, 1000)).toBe(1)
	})
})
