import axios from '@/util/request'

function contentRequest(config) {
	const user = JSON.parse(window.localStorage.getItem('user') || '{}')
	if (user.role === 'ROLE_player') {
		config.baseURL = 'http://localhost:8090/player/'
	}
	return axios(config)
}

export function getDataByQuery(queryInfo) {
	return contentRequest({
		url: 'blogs',
		method: 'GET',
		params: {
			...queryInfo
		}
	})
}

export function deleteBlogById(id) {
	return contentRequest({
		url: 'blog',
		method: 'DELETE',
		params: {
			id
		}
	})
}

export function getCategoryAndTag() {
	return contentRequest({
		url: 'categoryAndTag',
		method: 'GET'
	})
}

export function saveBlog(blog) {
	return contentRequest({
		url: 'blog',
		method: 'POST',
		data: {
			...blog
		}
	})
}

export function updateTop(id, top) {
	return axios({
		url: 'blog/top',
		method: 'PUT',
		params: {
			id,
			top
		}
	})
}

export function updateRecommend(id, recommend) {
	return axios({
		url: 'blog/recommend',
		method: 'PUT',
		params: {
			id,
			recommend
		}
	})
}

export function updateVisibility(id, form) {
	return axios({
		url: `blog/${id}/visibility`,
		method: 'PUT',
		data: {
			...form
		}
	})
}

export function getBlogById(id) {
	return contentRequest({
		url: 'blog',
		method: 'GET',
		params: {
			id
		}
	})
}

export function updateBlog(blog) {
	return contentRequest({
		url: 'blog',
		method: 'PUT',
		data: {
			...blog
		}
	})
}
