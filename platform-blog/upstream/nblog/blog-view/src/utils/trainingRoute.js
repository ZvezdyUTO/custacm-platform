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
	'admin/competitions',
	'admin/training',
	'admin/appearance',
])

export function buildTrainingFrameSource(rawPath, routeQuery = {}, buildVersion = '') {
	const page = allowedPages.has(rawPath) ? rawPath : 'multiple'
	const query = new URLSearchParams()
	for (const [key, value] of Object.entries(routeQuery)) {
		if (key !== '_build' && typeof value === 'string') query.set(key, value)
	}
	if (buildVersion) query.set('_build', buildVersion)
	return `/training-app/${page}${query.size ? `?${query.toString()}` : ''}`
}

export function buildTrainingHostPath(path) {
	if (!isAllowedTrainingRoutePath(path)) return null
	const url = new URL(path, 'https://custacm.invalid')
	url.searchParams.delete('_build')
	return `/training${url.pathname}${url.search}${url.hash}`
}

export function isAllowedTrainingRoutePath(path) {
	return /^\/(?:login|multiple|single|problem|admin(?:\/(?:create-users|users|articles|categories|competitions|training|appearance))?)(?:\?|$)/.test(path)
}
