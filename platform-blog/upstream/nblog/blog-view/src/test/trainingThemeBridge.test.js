// Author: huangbingrui.awa
import {mount} from '@vue/test-utils'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'

vi.mock('vue-router', () => ({
	useRoute: () => ({params: {trainingPath: 'multiple'}, query: {}}),
}))

import TrainingHost from '@/views/training/TrainingHost.vue'
import {applyTheme, THEME_CHANGE_EVENT} from '@/theme'

let wrapper

beforeEach(() => {
	applyTheme('light')
	wrapper = mount(TrainingHost)
})

afterEach(() => {
	wrapper?.unmount()
	vi.restoreAllMocks()
})

describe('training iframe theme bridge', () => {
	it('sends the effective theme on iframe load with the same-origin target', async () => {
		const frame = wrapper.get('iframe')
		const postMessage = vi.fn()
		Object.defineProperty(frame.element, 'contentWindow', {configurable: true, value: {postMessage}})

		await frame.trigger('load')

		expect(postMessage).toHaveBeenLastCalledWith(
			{type: 'custacm:theme', theme: 'light'},
			window.location.origin,
		)
	})

	it('forwards a later local theme event to an already loaded iframe', () => {
		const frame = wrapper.get('iframe')
		const postMessage = vi.fn()
		Object.defineProperty(frame.element, 'contentWindow', {configurable: true, value: {postMessage}})

		window.dispatchEvent(new CustomEvent(THEME_CHANGE_EVENT, {detail: {theme: 'dark', source: 'user'}}))

		expect(postMessage).toHaveBeenLastCalledWith(
			{type: 'custacm:theme', theme: 'dark'},
			window.location.origin,
		)
	})
})
