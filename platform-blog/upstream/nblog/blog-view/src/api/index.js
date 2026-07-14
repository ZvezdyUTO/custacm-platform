import axios from '@/plugins/axios'

export function getSite() {
	return axios({
		url: 'site',
		method: 'GET',
	})
}

export function getHomepageFeaturedImages() {
	return axios({
		url: 'homepage-featured-images',
		method: 'GET'
	})
}
