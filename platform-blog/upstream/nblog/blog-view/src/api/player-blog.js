// Author: huangbingrui.awa
import axios from '@/plugins/axios'

function bearer(token) {
	return {Authorization: `Bearer ${token}`}
}

async function dataOf(request, fallback) {
	const response = await request
	if (response.code !== 200) throw new Error(response.msg || fallback)
	return response.data
}

export function getMyBlogs(token, query = {}) {
	return dataOf(axios({
		url: 'player/blogs',
		method: 'GET',
		headers: bearer(token),
		params: query,
	}), '文章列表获取失败')
}

export function getMyBlog(token, id) {
	return dataOf(axios({
		url: 'player/blog',
		method: 'GET',
		headers: bearer(token),
		params: {id},
	}), '文章获取失败')
}

export function getInternalBlog(token, id) {
	return dataOf(axios({
		url: 'player/internal-blog',
		method: 'GET',
		headers: bearer(token),
		params: {id},
	}), '内部文章获取失败')
}

export function getPlayerCategoryAndTag(token) {
	return dataOf(axios({
		url: 'player/categoryAndTag',
		method: 'GET',
		headers: bearer(token),
	}), '分类和标签获取失败')
}

export async function createMyBlog(token, blog) {
	return dataOf(axios({
		url: 'player/blog',
		method: 'POST',
		headers: bearer(token),
		data: blog,
	}), '文章发布失败')
}

export async function updateMyBlog(token, blog) {
	await dataOf(axios({
		url: 'player/blog',
		method: 'PUT',
		headers: bearer(token),
		data: blog,
	}), '文章保存失败')
}

export function uploadMyImage(token, file, purpose) {
	const formData = new FormData()
	formData.append('file', file)
	formData.append('purpose', purpose)
	return dataOf(axios({
		url: 'player/images',
		method: 'POST',
		headers: bearer(token),
		data: formData,
	}), '图片上传失败')
}

export async function deleteMyImage(token, id) {
	await dataOf(axios({
		url: `player/images/${encodeURIComponent(id)}`,
		method: 'DELETE',
		headers: bearer(token),
	}), '图片删除失败')
}

export async function deleteMyBlog(token, id) {
	await dataOf(axios({
		url: 'player/blog',
		method: 'DELETE',
		headers: bearer(token),
		params: {id},
	}), '文章删除失败')
}
