// Author: huangbingrui.awa
import axios from '@/plugins/axios'
import {readToken} from '@/auth/session'

function optionalBearer() {
	const token = readToken()
	return token ? {Authorization: `Bearer ${token}`} : undefined
}

async function dataOf(request, fallback) {
	const response = await request
	if (response?.code !== 200) {
		const error = new Error(response?.msg || fallback)
		error.code = response?.code
		error.errorCode = response?.errorCode
		throw error
	}
	return response.data
}

export function getCompetitions(query = {}) {
	return dataOf(axios({
		url: 'competitions',
		method: 'GET',
		headers: optionalBearer(),
		params: query,
	}), '赛事档案获取失败')
}

export function getCompetition(id) {
	return dataOf(axios({
		url: `competitions/${encodeURIComponent(id)}`,
		method: 'GET',
		headers: optionalBearer(),
	}), '赛事档案获取失败')
}
