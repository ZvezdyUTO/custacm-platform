// Author: huangbingrui.awa

export const THEME_STORAGE_KEY = 'custacm.theme'
export const THEME_CHANGE_EVENT = 'custacm:theme-change'

const LIGHT_THEME = 'light'
const DARK_THEME = 'dark'
const DARK_MEDIA_QUERY = '(prefers-color-scheme: dark)'

let activeWindow = null
let activeMediaQuery = null

export function isTheme(value) {
	return value === LIGHT_THEME || value === DARK_THEME
}

function browserWindow() {
	return typeof window === 'undefined' ? null : window
}

function browserDocument() {
	return typeof document === 'undefined' ? null : document
}

function safeStorage(targetWindow = browserWindow()) {
	try {
		return targetWindow?.localStorage || null
	} catch (_) {
		return null
	}
}

export function readStoredTheme(storage = safeStorage()) {
	try {
		const value = storage?.getItem(THEME_STORAGE_KEY)
		return isTheme(value) ? value : null
	} catch (_) {
		return null
	}
}

export function readSystemTheme(targetWindow = browserWindow(), mediaQuery = null) {
	try {
		const query = mediaQuery || targetWindow?.matchMedia?.(DARK_MEDIA_QUERY)
		return query?.matches ? DARK_THEME : LIGHT_THEME
	} catch (_) {
		return LIGHT_THEME
	}
}

export function resolveTheme({targetWindow = browserWindow(), storage = safeStorage(targetWindow), mediaQuery = null} = {}) {
	return readStoredTheme(storage) || readSystemTheme(targetWindow, mediaQuery)
}

export function applyTheme(theme, root = browserDocument()?.documentElement) {
	const effectiveTheme = theme === DARK_THEME ? DARK_THEME : LIGHT_THEME
	if (!root) return effectiveTheme

	root.dataset.theme = effectiveTheme
	root.classList.toggle('dark', effectiveTheme === DARK_THEME)
	root.style.colorScheme = effectiveTheme
	return effectiveTheme
}

export function getCurrentTheme() {
	const current = browserDocument()?.documentElement?.dataset?.theme
	return isTheme(current) ? current : resolveTheme()
}

function emitThemeChange(theme, source, targetWindow = browserWindow()) {
	if (!targetWindow?.dispatchEvent) return
	const EventConstructor = targetWindow.CustomEvent || (typeof CustomEvent === 'undefined' ? null : CustomEvent)
	if (!EventConstructor) return
	targetWindow.dispatchEvent(new EventConstructor(THEME_CHANGE_EVENT, {detail: {theme, source}}))
}

export function setTheme(theme) {
	if (!isTheme(theme)) return getCurrentTheme()
	const targetWindow = browserWindow()
	const storage = safeStorage(targetWindow)
	try {
		storage?.setItem(THEME_STORAGE_KEY, theme)
	} catch (_) {
		// Storage may be unavailable in privacy-restricted contexts; the current page still switches.
	}
	const effectiveTheme = applyTheme(theme)
	emitThemeChange(effectiveTheme, 'user', targetWindow)
	return effectiveTheme
}

export function toggleTheme() {
	return setTheme(getCurrentTheme() === DARK_THEME ? LIGHT_THEME : DARK_THEME)
}

function addMediaListener(mediaQuery, listener) {
	if (mediaQuery?.addEventListener) mediaQuery.addEventListener('change', listener)
	else mediaQuery?.addListener?.(listener)
}

function removeMediaListener(mediaQuery, listener) {
	if (mediaQuery?.removeEventListener) mediaQuery.removeEventListener('change', listener)
	else mediaQuery?.removeListener?.(listener)
}

function handleStorageChange(event) {
	if (event.key !== null && event.key !== THEME_STORAGE_KEY) return
	const theme = applyTheme(
		resolveTheme({targetWindow: activeWindow, mediaQuery: activeMediaQuery}),
		activeWindow?.document?.documentElement,
	)
	emitThemeChange(theme, 'storage', activeWindow)
}

function handleSystemThemeChange() {
	if (readStoredTheme(safeStorage(activeWindow))) return
	const theme = applyTheme(readSystemTheme(activeWindow, activeMediaQuery), activeWindow?.document?.documentElement)
	emitThemeChange(theme, 'system', activeWindow)
}

export function destroyTheme() {
	if (!activeWindow) return
	activeWindow.removeEventListener('storage', handleStorageChange)
	removeMediaListener(activeMediaQuery, handleSystemThemeChange)
	activeWindow = null
	activeMediaQuery = null
}

export function initializeTheme(targetWindow = browserWindow()) {
	destroyTheme()
	if (!targetWindow) return destroyTheme

	activeWindow = targetWindow
	try {
		activeMediaQuery = targetWindow.matchMedia?.(DARK_MEDIA_QUERY) || null
	} catch (_) {
		activeMediaQuery = null
	}
	applyTheme(resolveTheme({targetWindow, mediaQuery: activeMediaQuery}), targetWindow.document?.documentElement)
	targetWindow.addEventListener('storage', handleStorageChange)
	addMediaListener(activeMediaQuery, handleSystemThemeChange)
	return destroyTheme
}
