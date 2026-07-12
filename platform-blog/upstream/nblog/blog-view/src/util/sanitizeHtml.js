import DOMPurify from 'dompurify'

export function sanitizeHtml(html) {
	return DOMPurify.sanitize(html || '', {
		USE_PROFILES: {html: true},
		FORBID_TAGS: ['style'],
		FORBID_ATTR: ['style'],
	})
}
