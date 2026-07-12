// Author: huangbingrui.awa
export const ARTICLE_CONTENT_MAX_BYTES = 15 * 1024 * 1024
export const ARTICLE_COVER_MAX_BYTES = 10 * 1024 * 1024

export function validateArticleImage(file, maxBytes = ARTICLE_CONTENT_MAX_BYTES) {
	if (!file || !['image/jpeg', 'image/png'].includes(file.type)) {
		throw new Error('请选择 JPEG 或 PNG 图片')
	}
	if (file.size > maxBytes) {
		throw new Error(`图片不能超过 ${Math.round(maxBytes / 1024 / 1024)}MB`)
	}
}

export function imageAltFromFilename(name = '') {
	return name.replace(/\.[^.]+$/, '').trim() || '文章图片'
}

export function markdownForImage(asset, alt) {
	return `![${String(alt || '文章图片').replace(/[\[\]]/g, '')}](${asset.thumbnailUrl})`
}

export function contentUsesAsset(content, asset) {
	return Boolean(asset?.publicId && String(content || '').includes(`/assets/${asset.publicId}/`))
}

export function originalUrlForManagedThumbnail(url) {
	const value = String(url || '')
	if (!/\/api\/image\/assets\/[0-9a-f-]{36}\/thumbnail\.(?:jpg|png)$/i.test(value)) return ''
	return value.replace(/\/thumbnail\.(jpg|png)$/i, '/original.$1')
}

export function deleteManagedImageBackward(view) {
	const selection = view?.state?.selection?.main
	if (!selection || selection.from !== selection.to) return false
	const content = view.state.doc.toString()
	const pattern = /!\[[^\]\n]*\]\(\/api\/image\/assets\/[0-9a-f-]{36}\/thumbnail\.(?:jpg|png)\)/gi
	let match
	while ((match = pattern.exec(content)) !== null) {
		const from = match.index
		const to = from + match[0].length
		if (selection.from < from || selection.from > to) continue
		view.dispatch({changes: {from, to, insert: ''}, selection: {anchor: from}, scrollIntoView: true})
		return true
	}
	return false
}
