// Author: huangbingrui.awa
import axios from '@/plugins/axios'

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
		params: query,
	}), '赛事档案获取失败')
}

export function getCompetition(id) {
	return dataOf(axios({
		url: `competitions/${encodeURIComponent(id)}`,
		method: 'GET',
	}), '赛事档案获取失败')
}
