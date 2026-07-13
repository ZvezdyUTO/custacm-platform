// Author: huangbingrui.awa
import {afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi} from 'vitest'
import {
	applyTheme,
	destroyTheme,
	getCurrentTheme,
	initializeTheme,
	readStoredTheme,
	resolveTheme,
	setTheme,
	THEME_CHANGE_EVENT,
	THEME_STORAGE_KEY,
	toggleTheme,
} from '@/theme'

let originalMatchMedia
let originalStorage
let storageValues

function mediaQuery(initialMatches) {
	let changeListener = null
	return {
		matches: initialMatches,
		addEventListener: vi.fn((type, listener) => { if (type === 'change') changeListener = listener }),
		removeEventListener: vi.fn((type, listener) => { if (type === 'change' && changeListener === listener) changeListener = null }),
		change(matches) {
			this.matches = matches
			changeListener?.({matches})
		},
	}
}

beforeAll(() => {
	originalMatchMedia = Object.getOwnPropertyDescriptor(window, 'matchMedia')
	originalStorage = Object.getOwnPropertyDescriptor(window, 'localStorage')
})

beforeEach(() => {
	destroyTheme()
	storageValues = new Map()
	Object.defineProperty(window, 'localStorage', {
		configurable: true,
		value: {
			clear: () => storageValues.clear(),
			getItem: key => storageValues.has(key) ? storageValues.get(key) : null,
			removeItem: key => storageValues.delete(key),
			setItem: (key, value) => storageValues.set(key, String(value)),
		},
	})
	document.documentElement.classList.remove('dark')
	delete document.documentElement.dataset.theme
	document.documentElement.style.removeProperty('color-scheme')
})

afterEach(() => {
	destroyTheme()
	vi.restoreAllMocks()
})

afterAll(() => {
	if (originalMatchMedia) Object.defineProperty(window, 'matchMedia', originalMatchMedia)
	else delete window.matchMedia
	if (originalStorage) Object.defineProperty(window, 'localStorage', originalStorage)
})

describe('shared theme service', () => {
	it('prefers a valid stored theme and applies every root theme contract', () => {
		const media = mediaQuery(false)
		Object.defineProperty(window, 'matchMedia', {configurable: true, value: vi.fn(() => media)})
		window.localStorage.setItem(THEME_STORAGE_KEY, 'dark')

		initializeTheme()

		expect(readStoredTheme()).toBe('dark')
		expect(getCurrentTheme()).toBe('dark')
		expect(document.documentElement.dataset.theme).toBe('dark')
		expect(document.documentElement.classList.contains('dark')).toBe(true)
		expect(document.documentElement.style.colorScheme).toBe('dark')
	})

	it('follows system changes when storage is missing or invalid', () => {
		const media = mediaQuery(true)
		Object.defineProperty(window, 'matchMedia', {configurable: true, value: vi.fn(() => media)})
		window.localStorage.setItem(THEME_STORAGE_KEY, 'sepia')
		const listener = vi.fn()
		window.addEventListener(THEME_CHANGE_EVENT, listener)
		initializeTheme()

		expect(getCurrentTheme()).toBe('dark')
		media.change(false)

		expect(getCurrentTheme()).toBe('light')
		expect(listener).toHaveBeenLastCalledWith(expect.objectContaining({detail: {theme: 'light', source: 'system'}}))
		window.removeEventListener(THEME_CHANGE_EVENT, listener)
	})

	it('keeps an explicit preference stable when the system changes', () => {
		const media = mediaQuery(true)
		Object.defineProperty(window, 'matchMedia', {configurable: true, value: vi.fn(() => media)})
		window.localStorage.setItem(THEME_STORAGE_KEY, 'light')
		initializeTheme()

		media.change(false)
		media.change(true)

		expect(getCurrentTheme()).toBe('light')
	})

	it('persists direct changes, emits a local event, and toggles from the effective state', () => {
		applyTheme('light')
		const listener = vi.fn()
		window.addEventListener(THEME_CHANGE_EVENT, listener)

		expect(toggleTheme()).toBe('dark')
		expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark')
		expect(listener).toHaveBeenLastCalledWith(expect.objectContaining({detail: {theme: 'dark', source: 'user'}}))
		expect(setTheme('light')).toBe('light')
		expect(document.documentElement.classList.contains('dark')).toBe(false)

		window.removeEventListener(THEME_CHANGE_EVENT, listener)
	})

	it('applies cross-context storage updates and removes listeners on destroy', () => {
		const media = mediaQuery(false)
		Object.defineProperty(window, 'matchMedia', {configurable: true, value: vi.fn(() => media)})
		const remove = vi.spyOn(window, 'removeEventListener')
		const cleanup = initializeTheme()
		window.localStorage.setItem(THEME_STORAGE_KEY, 'dark')

		window.dispatchEvent(new StorageEvent('storage', {key: THEME_STORAGE_KEY}))

		expect(getCurrentTheme()).toBe('dark')
		cleanup()
		expect(remove).toHaveBeenCalledWith('storage', expect.any(Function))
		expect(media.removeEventListener).toHaveBeenCalledWith('change', expect.any(Function))
	})

	it('falls back to light when storage and media access both throw', () => {
		const hostileWindow = {
			get localStorage() { throw new Error('blocked') },
			matchMedia() { throw new Error('blocked') },
		}

		expect(resolveTheme({targetWindow: hostileWindow})).toBe('light')
	})
})
