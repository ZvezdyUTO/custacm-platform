import axios from '@/plugins/axios'
import {readToken} from '@/auth/session'

function optionalBearer() {
	const token = readToken()
	return token ? {Authorization: `Bearer ${token}`} : undefined
}

export function getBlogById(id) {
	return axios({
		url: 'blog',
		method: 'GET',
		params: {
			id
		}
	})
}

export function downloadBlog(token, id) {
	return axios({
		url: 'player/blog/download',
		method: 'GET',
		headers: {Authorization: `Bearer ${token}`},
		params: {id},
		responseType: 'blob',
	})
}

export function getSearchBlogList(query) {
	return axios({
		url: 'searchBlog',
		method: 'GET',
		headers: optionalBearer(),
		params: {
			query
		}
	})
}
