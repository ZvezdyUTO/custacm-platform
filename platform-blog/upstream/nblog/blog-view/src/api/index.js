import axios from '@/plugins/axios'
import {readToken} from '@/auth/session'

function optionalBearer() {
	const token = readToken()
	return token ? {Authorization: `Bearer ${token}`} : undefined
}

export function getSite() {
	return axios({
		url: 'site',
		method: 'GET',
		headers: optionalBearer(),
	})
}

export function getHomepageBanners() {
	return axios({
		url: 'homepage-banners',
		method: 'GET'
	})
}
