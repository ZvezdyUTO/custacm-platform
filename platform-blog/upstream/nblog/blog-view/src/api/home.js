import axios from '@/plugins/axios'
import {readToken} from '@/auth/session'

function optionalBearer() {
	const token = readToken()
	return token ? {Authorization: `Bearer ${token}`} : undefined
}

export function getBlogList(pageNum) {
	return axios({
		url: 'blogs',
		method: 'GET',
		headers: optionalBearer(),
		params: {
			pageNum
		}
	})
}
