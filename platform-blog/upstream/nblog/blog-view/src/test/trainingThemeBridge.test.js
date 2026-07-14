// Author: huangbingrui.awa
import {mount} from '@vue/test-utils'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'

vi.mock('vue-router', () => ({
	useRoute: () => ({params: {trainingPath: 'multiple'}, query: {}}),
}))

import TrainingHost from '@/views/training/TrainingHost.vue'
import trainingHostSource from '@/views/training/TrainingHost.vue?raw'
import indexSource from '@/views/Index.vue?raw'
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
	it('removes the login footer and scopes a fallback outer scrollbar style', () => {
		expect(indexSource).toContain("trainingPath !== 'login' && !trainingPath.startsWith('admin')")
		expect(document.documentElement.classList.contains('training-host-active')).toBe(true)
		wrapper.unmount()
		wrapper = null
		expect(document.documentElement.classList.contains('training-host-active')).toBe(false)
	})

	it('keeps the outer host within one viewport after reserving navigation space', () => {
		expect(trainingHostSource).toMatch(/\.training-host[\s\S]*box-sizing: border-box;[\s\S]*height: 100vh;[\s\S]*padding-top: 51px;/)
		expect(trainingHostSource).toContain('height: calc(100vh - 51px);')
	})

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
