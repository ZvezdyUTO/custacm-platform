// Author: huangbingrui.awa
export function articleDownloadFilename(title, id) {
	let safeTitle = String(title || '')
		.replace(/[\\/:*?"<>|\u0000-\u001f\u007f]/g, '_')
		.trim()
	if ([...safeTitle].length > 80) {
		safeTitle = `${[...safeTitle].slice(0, 80).join('')}-${id}`
	}
	return `${safeTitle || `article-${id}`}.zip`
}

export function saveArticleDownload(blob, filename) {
	const objectUrl = URL.createObjectURL(blob)
	const link = document.createElement('a')
	link.href = objectUrl
	link.download = filename
	link.hidden = true
	document.body.appendChild(link)
	try {
		link.click()
	} finally {
		link.remove()
		URL.revokeObjectURL(objectUrl)
	}
}

export function retryAfterSeconds(error) {
	const headers = error?.response?.headers
	const raw = headers?.['retry-after'] ?? headers?.get?.('retry-after')
	const parsed = Number.parseInt(raw, 10)
	return Number.isFinite(parsed) && parsed > 0 ? parsed : 1
}
