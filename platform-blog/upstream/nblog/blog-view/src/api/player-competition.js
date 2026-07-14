// Author: huangbingrui.awa
import axios from '@/plugins/axios'

function bearer(token) {
	return {Authorization: `Bearer ${token}`}
}

async function mutationOf(request, fallback) {
	const response = await request
	if (response.code !== 200) {
		const error = new Error(response.msg || fallback)
		error.code = response.code
		error.errorCode = response.errorCode
		throw error
	}
}

export function setAchievementProfileVisibility(token, competitionId, awardId, visible) {
	return mutationOf(axios({
		url: `player/competitions/${encodeURIComponent(competitionId)}/awards/${encodeURIComponent(awardId)}/profile-visibility`,
		method: 'PUT',
		headers: bearer(token),
		data: {visible},
	}), '奖项展示状态更新失败')
}

export function setAchievementProfileOrder(token, orderedAwardIds) {
	return mutationOf(axios({
		url: 'player/competitions/achievement-order',
		method: 'PUT',
		headers: bearer(token),
		data: {orderedAwardIds},
	}), '公开荣誉排序更新失败')
}

export function bindCompetitionArticle(token, competitionId, blogId) {
	return mutationOf(axios({
		url: `player/competitions/${encodeURIComponent(competitionId)}/articles/${encodeURIComponent(blogId)}`,
		method: 'POST',
		headers: bearer(token),
	}), '参赛文章绑定失败')
}

export function unbindCompetitionArticle(token, competitionId, blogId) {
	return mutationOf(axios({
		url: `player/competitions/${encodeURIComponent(competitionId)}/articles/${encodeURIComponent(blogId)}`,
		method: 'DELETE',
		headers: bearer(token),
	}), '参赛文章解绑失败')
}
