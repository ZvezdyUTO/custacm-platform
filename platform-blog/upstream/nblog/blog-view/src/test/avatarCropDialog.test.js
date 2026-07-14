// Author: huangbingrui.awa
import {afterEach, describe, expect, it} from 'vitest'
import {mount} from '@vue/test-utils'
import AvatarCropDialog from '@/components/profile/AvatarCropDialog.vue'

describe('AvatarCropDialog focus lifecycle', () => {
	afterEach(() => { document.body.innerHTML = '' })

	it('focuses the close button and restores the opener after Escape', async () => {
		const trigger = document.createElement('button')
		document.body.appendChild(trigger)
		trigger.focus()
		const wrapper = mount(AvatarCropDialog, {attachTo: document.body})

		wrapper.vm.open({type: 'text/plain', size: 10})
		await wrapper.vm.$nextTick()
		const dialog = document.body.querySelector('.crop-dialog')
		expect(document.activeElement).toBe(dialog.querySelector('.crop-close'))
		dialog.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape', bubbles: true}))
		await wrapper.vm.$nextTick()

		expect(document.body.querySelector('.crop-dialog')).toBeNull()
		expect(document.activeElement).toBe(trigger)
		wrapper.unmount()
	})
})
