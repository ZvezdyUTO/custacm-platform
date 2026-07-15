// Author: huangbingrui.awa
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {
	applyTheme,
	destroyTheme,
	getCurrentTheme,
	initializeTheme,
	resolveTheme,
	THEME_CHANGE_EVENT,
	THEME_STORAGE_KEY,
	toggleTheme,
} from '@/theme'

let values

beforeEach(() => {
	destroyTheme()
	values = new Map()
	Object.defineProperty(window, 'localStorage', {
		configurable: true,
		value: {
			getItem: key => values.get(key) ?? null,
			setItem: (key, value) => values.set(key, String(value)),
			removeItem: key => values.delete(key),
		},
	})
	document.documentElement.classList.remove('dark')
	delete document.documentElement.dataset.theme
})

afterEach(() => {
	destroyTheme()
	vi.restoreAllMocks()
})

describe('shared Blog theme', () => {
	it('defaults to light without following the operating system', () => {
		Object.defineProperty(window, 'matchMedia', {configurable: true, value: vi.fn(() => ({matches: true}))})
		expect(resolveTheme()).toBe('light')
		initializeTheme()
		expect(getCurrentTheme()).toBe('light')
	})

	it('restores, persists and announces an explicit choice', () => {
		values.set(THEME_STORAGE_KEY, 'dark')
		initializeTheme()
		expect(document.documentElement.classList.contains('dark')).toBe(true)

		const listener = vi.fn()
		window.addEventListener(THEME_CHANGE_EVENT, listener)
		expect(toggleTheme()).toBe('light')
		expect(values.get(THEME_STORAGE_KEY)).toBe('light')
		expect(listener).toHaveBeenLastCalledWith(expect.objectContaining({detail: {theme: 'light', source: 'user'}}))
		window.removeEventListener(THEME_CHANGE_EVENT, listener)
	})

	it('applies cross-tab storage updates and resets an invalid value to light', () => {
		initializeTheme()
		values.set(THEME_STORAGE_KEY, 'dark')
		window.dispatchEvent(new StorageEvent('storage', {key: THEME_STORAGE_KEY}))
		expect(getCurrentTheme()).toBe('dark')

		values.set(THEME_STORAGE_KEY, 'sepia')
		window.dispatchEvent(new StorageEvent('storage', {key: THEME_STORAGE_KEY}))
		expect(getCurrentTheme()).toBe('light')
	})

	it('keeps root attribute, class and native control scheme aligned', () => {
		applyTheme('dark')
		expect(document.documentElement.dataset.theme).toBe('dark')
		expect(document.documentElement.classList.contains('dark')).toBe(true)
		expect(document.documentElement.style.colorScheme).toBe('dark')
	})
})
