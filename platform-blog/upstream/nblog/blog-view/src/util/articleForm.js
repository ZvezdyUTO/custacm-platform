// Author: huangbingrui.awa
const MAX_MARKDOWN_BYTES = 2 * 1024 * 1024

export function isArticleAuthor(blog, user) {
	return Boolean(blog?.authorUsername && user?.username && blog.authorUsername === user.username)
}

export function articleRequest(form) {
	const content = form.content.trim()
	return {
		...(form.id ? {id: Number(form.id)} : {}),
		title: form.title.trim(),
		firstPicture: (form.firstPicture || '').trim(),
		firstPictureAssetId: form.firstPictureAssetId ? Number(form.firstPictureAssetId) : null,
		description: form.description.trim(),
		content,
		words: Array.from(content).length,
		cate: Number(form.categoryId),
		tagList: (form.tagList || []).map(tag => typeof tag === 'number' ? tag : String(tag).trim()).filter(tag => tag !== ''),
		published: Boolean(form.published),
		internal: Boolean(form.published && form.internal),
		commentEnabled: Boolean(form.commentEnabled),
	}
}

export async function markdownTextFromFile(file) {
	if (!file || !/\.(?:md|markdown)$/i.test(file.name || '')) {
		throw new Error('请选择 .md 或 .markdown 文件')
	}
	if (file.size > MAX_MARKDOWN_BYTES) {
		throw new Error('Markdown 文件不能超过 2MB')
	}
	return (await file.text()).replace(/^\uFEFF/, '')
}
