import axios from '@/plugins/axios'

export function getSite() {
	return axios({
		url: 'site',
		method: 'GET'
	})
}

export function getHomepageBanners() {
	return axios({
		url: 'homepage-banners',
		method: 'GET'
	})
}
