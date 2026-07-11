import axios from '@/util/request'

export function changeAccount(account) {
	return axios({
		url: 'account',
		method: 'POST',
		data: {
			...account
		}
	})
}

export function getPlayerProfile() {
	return axios({baseURL: 'http://localhost:8090/player/', url: 'me', method: 'GET'})
}

export function changeNickname(nickname) {
	return axios({
		baseURL: 'http://localhost:8090/player/',
		url: 'me/nickname',
		method: 'PATCH',
		data: {nickname}
	})
}

export function changePassword(oldPassword, newPassword) {
	return axios({
		baseURL: 'http://localhost:8090/player/',
		url: 'me/password',
		method: 'PATCH',
		data: {oldPassword, newPassword}
	})
}
