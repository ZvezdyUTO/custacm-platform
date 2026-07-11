// Author: huangbingrui.awa
import axios from '@/plugins/axios'

function bearer(token) {
	return {Authorization: `Bearer ${token}`}
}

export async function getCurrentProfile(token) {
	const response = await axios({
		url: 'player/me',
		method: 'GET',
		headers: bearer(token),
	})
	if (response.code !== 200) throw new Error(response.msg || '个人资料获取失败')
	return response.data
}

export async function getCurrentOjHandles(token) {
	const response = await axios({
		url: 'player/me/oj-handles',
		method: 'GET',
		headers: bearer(token),
	})
	if (response.code !== 200) throw new Error(response.msg || 'OJ 账号获取失败')
	return response.data || {}
}

export async function updateCurrentAvatar(token, blob) {
	const formData = new FormData()
	formData.append('file', blob, 'avatar.png')
	const response = await axios({
		url: 'player/me/avatar',
		method: 'POST',
		data: formData,
		headers: bearer(token),
	})
	if (response.code !== 200) throw new Error(response.msg || '头像上传失败')
	return response.data
}

export async function updateCurrentProfile(token, profile) {
	const response = await axios({
		url: 'player/me/profile',
		method: 'PATCH',
		data: profile,
		headers: bearer(token),
	})
	if (response.code !== 200) throw new Error(response.msg || '个人资料保存失败')
	return response.data
}

export async function replaceCurrentProfileLinks(token, links) {
	const response = await axios({
		url: 'player/me/profile-links',
		method: 'PUT',
		data: {links},
		headers: bearer(token),
	})
	if (response.code !== 200) throw new Error(response.msg || '友情链接保存失败')
	return response.data
}
