import {mount} from '@vue/test-utils'
import {afterEach, describe, expect, it, vi} from 'vitest'
import Header from '@/components/index/Header.vue'

describe('homepage banner loading', () => {
	afterEach(() => vi.restoreAllMocks())

	it('keeps the blurred preview on failure and reveals the banner when it loads', async () => {
		const wrapper = mount(Header)
		const media = wrapper.get('.home-hero-media')
		const banner = media.get(':scope > img')

		expect(media.classes()).not.toContain('is-loaded')
		await banner.trigger('error')
		expect(media.classes()).not.toContain('is-loaded')
		await banner.trigger('load')
		expect(media.classes()).toContain('is-loaded')
		wrapper.unmount()
	})

	it.each([1920, 0])('reveals a cached image only when its natural width is positive (%i)', async naturalWidth => {
		vi.spyOn(HTMLImageElement.prototype, 'complete', 'get').mockReturnValue(true)
		vi.spyOn(HTMLImageElement.prototype, 'naturalWidth', 'get').mockReturnValue(naturalWidth)
		const wrapper = mount(Header)
		await wrapper.vm.$nextTick()

		expect(wrapper.get('.home-hero-media').classes().includes('is-loaded')).toBe(naturalWidth > 0)
		wrapper.unmount()
	})
})
