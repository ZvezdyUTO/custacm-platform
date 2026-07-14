// Author: huangbingrui.awa
const FOCUSABLE_SELECTOR = [
	'a[href]',
	'button:not([disabled])',
	'input:not([disabled]):not([type="hidden"])',
	'select:not([disabled])',
	'textarea:not([disabled])',
	'[tabindex]:not([tabindex="-1"])',
].join(',')

export function focusDialog(container, preferredSelector) {
	if (!container) return
	const preferred = preferredSelector ? container.querySelector(preferredSelector) : null
	const target = preferred || container.querySelector(FOCUSABLE_SELECTOR) || container
	target.focus?.()
}

export function trapDialogTab(event, container) {
	if (event.key !== 'Tab' || !container) return
	const focusable = [...container.querySelectorAll(FOCUSABLE_SELECTOR)]
		.filter(element => !element.hidden && element.getAttribute('aria-hidden') !== 'true')
	if (focusable.length === 0) {
		event.preventDefault()
		container.focus?.()
		return
	}
	const first = focusable[0]
	const last = focusable[focusable.length - 1]
	if (event.shiftKey && (document.activeElement === first || !container.contains(document.activeElement))) {
		event.preventDefault()
		last.focus()
	} else if (!event.shiftKey && (document.activeElement === last || !container.contains(document.activeElement))) {
		event.preventDefault()
		first.focus()
	}
}

export function restoreDialogFocus(element) {
	if (element?.isConnected) element.focus?.()
}
