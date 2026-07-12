import axios from '@/plugins/axios'
import {readToken} from '@/auth/session'

function optionalBearer() {
	const token = readToken()
	return token ? {Authorization: `Bearer ${token}`} : undefined
}

export function getBlogListByTagName(tagName, pageNum) {
	return axios({
		url: 'tag',
		method: 'GET',
		headers: optionalBearer(),
		params: {
			tagName,
			pageNum
		}
	})
}
