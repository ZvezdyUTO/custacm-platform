// Author: huangbingrui.awa

export const THEME_STORAGE_KEY = 'custacm.theme'
export const THEME_CHANGE_EVENT = 'custacm:theme-change'

const LIGHT_THEME = 'light'
const DARK_THEME = 'dark'

let activeWindow = null

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

export function resolveTheme({storage = safeStorage()} = {}) {
	return readStoredTheme(storage) || LIGHT_THEME
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
	try {
		safeStorage(targetWindow)?.setItem(THEME_STORAGE_KEY, theme)
	} catch (_) {
		// The current page still switches when storage is unavailable.
	}
	const effectiveTheme = applyTheme(theme)
	emitThemeChange(effectiveTheme, 'user', targetWindow)
	return effectiveTheme
}

export function toggleTheme() {
	return setTheme(getCurrentTheme() === DARK_THEME ? LIGHT_THEME : DARK_THEME)
}

function handleStorageChange(event) {
	if (event.key !== null && event.key !== THEME_STORAGE_KEY) return
	const theme = applyTheme(resolveTheme({storage: safeStorage(activeWindow)}), activeWindow?.document?.documentElement)
	emitThemeChange(theme, 'storage', activeWindow)
}

export function destroyTheme() {
	if (!activeWindow) return
	activeWindow.removeEventListener('storage', handleStorageChange)
	activeWindow = null
}

export function initializeTheme(targetWindow = browserWindow()) {
	destroyTheme()
	if (!targetWindow) return destroyTheme

	activeWindow = targetWindow
	applyTheme(resolveTheme({storage: safeStorage(targetWindow)}), targetWindow.document?.documentElement)
	targetWindow.addEventListener('storage', handleStorageChange)
	return destroyTheme
}
