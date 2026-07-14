// Author: huangbingrui.awa
import {afterEach, describe, expect, it, vi} from 'vitest'
import {flushPromises, mount} from '@vue/test-utils'
import ManagedImageViewer from '@/components/article/ManagedImageViewer.vue'

describe('ManagedImageViewer', () => {
	afterEach(() => {
		document.body.innerHTML = ''
		vi.unstubAllGlobals()
	})

	it('only requests the original after the user asks for it', async () => {
		const requestedUrls = []
		class FakeImage {
			set src(value) {
				requestedUrls.push(value)
				this.onload?.()
			}
		}
		vi.stubGlobal('Image', FakeImage)
		const wrapper = mount(ManagedImageViewer, {attachTo: document.body})

		wrapper.vm.open('/api/image/assets/id/thumbnail.jpg', '/api/image/assets/id/original.jpg', '示意图')
		await wrapper.vm.$nextTick()

		expect(requestedUrls).toEqual([])
		expect(document.body.querySelector('.viewer-stage img')?.getAttribute('src')).toBe('/api/image/assets/id/thumbnail.jpg')

		document.body.querySelector('.viewer-toolbar button').click()
		await flushPromises()

		expect(requestedUrls).toEqual(['/api/image/assets/id/original.jpg'])
		expect(document.body.querySelector('.viewer-stage img')?.getAttribute('src')).toBe('/api/image/assets/id/original.jpg')
		expect(document.body.textContent).toContain('正在查看原图')
		wrapper.unmount()
	})

	it('moves focus into the viewer, closes with Escape and restores focus', async () => {
		const trigger = document.createElement('button')
		document.body.appendChild(trigger)
		trigger.focus()
		const wrapper = mount(ManagedImageViewer, {attachTo: document.body})

		wrapper.vm.open('/thumbnail.jpg', '/original.jpg', '示意图')
		await wrapper.vm.$nextTick()

		expect(document.activeElement).toBe(document.body.querySelector('.viewer-close'))
		document.body.querySelector('.managed-viewer').dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape', bubbles: true}))
		await wrapper.vm.$nextTick()

		expect(document.body.querySelector('.managed-viewer')).toBeNull()
		expect(document.activeElement).toBe(trigger)
		wrapper.unmount()
	})
})
