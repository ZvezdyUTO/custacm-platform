import axios from '@/plugins/axios'

export function getCommentListByQuery(query) {
	return axios({
		url: 'comments',
		method: 'GET',
		params: {
			...query
		}
	})
}

export function getInternalCommentList(token, query) {
	return axios({
		url: 'player/comments',
		method: 'GET',
		headers: {Authorization: `Bearer ${token}`},
		params: {blogId: query.blogId, pageNum: query.pageNum, pageSize: query.pageSize},
	})
}

export function submitComment(token, form) {
	return axios({
		url: 'player/comment',
		method: 'POST',
		headers: {
			Authorization: `Bearer ${token}`,
		},
		data: {
			...form
		}
	})
}
