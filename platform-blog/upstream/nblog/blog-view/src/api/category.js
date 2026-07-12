import axios from '@/plugins/axios'
import {readToken} from '@/auth/session'

function optionalBearer() {
	const token = readToken()
	return token ? {Authorization: `Bearer ${token}`} : undefined
}

export function getBlogListByCategoryName(categoryName, pageNum) {
	return axios({
		url: 'category',
		method: 'GET',
		headers: optionalBearer(),
		params: {
			categoryName,
			pageNum
		}
	})
}
