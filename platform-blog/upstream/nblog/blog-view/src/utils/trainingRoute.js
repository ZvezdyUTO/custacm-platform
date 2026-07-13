// Author: huangbingrui.awa
const allowedPages = new Set([
	'login',
	'multiple',
	'single',
	'problem',
	'admin',
	'admin/create-users',
	'admin/users',
	'admin/articles',
	'admin/categories',
	'admin/training',
	'admin/appearance',
])

export function buildTrainingFrameSource(rawPath, routeQuery = {}) {
	const page = allowedPages.has(rawPath) ? rawPath : 'multiple'
	const query = new URLSearchParams()
	for (const [key, value] of Object.entries(routeQuery)) {
		if (typeof value === 'string') query.set(key, value)
	}
	return `/training-app/${page}${query.size ? `?${query.toString()}` : ''}`
}

export function isAllowedTrainingRoutePath(path) {
	return /^\/(?:login|multiple|single|problem|admin(?:\/(?:create-users|users|articles|categories|training|appearance))?)(?:\?|$)/.test(path)
}
