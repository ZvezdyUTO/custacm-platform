import {
	SAVE_COMMENT_RESULT,
	SET_PARENT_COMMENT_ID,
	RESET_COMMENT_FORM
} from "./mutations-types";

import {getCommentListByQuery, getInternalCommentList, submitComment} from "@/api/comment";
import {clearSession, readToken} from "@/auth/session";
import {ElMessage, ElNotification} from "element-plus";
import router from "../router";
import {formatCommentContent} from '@/util/commentContent'

export default {
	getCommentList({commit, rootState}) {
		const request = rootState.commentQuery.internal
			? getInternalCommentList(readToken(), rootState.commentQuery)
			: getCommentListByQuery(rootState.commentQuery)
		request.then(res => {
			if (res.code === 200) {
				res.data.comments.list.forEach(comment => {
					comment.content = formatCommentContent(comment.content)
					comment.replyComments.forEach(comment => {
						comment.content = formatCommentContent(comment.content)
					})
				})
				commit(SAVE_COMMENT_RESULT, res.data)
			}
		}).catch(() => {
			ElMessage.error("请求失败")
		})
	},
	submitCommentForm({rootState, dispatch, commit}, token) {
		const form = {
			content: rootState.commentForm.content,
			blogId: rootState.commentQuery.blogId,
			parentCommentId: rootState.parentCommentId,
		}
		return submitComment(token, form).then(res => {
			if (res.code === 200) {
				ElNotification({
					title: res.msg,
					type: 'success'
				})
				commit(SET_PARENT_COMMENT_ID, -1)
				commit(RESET_COMMENT_FORM)
				dispatch('getCommentList')
			} else {
				ElNotification({
					title: '评论失败',
					message: res.msg,
					type: 'error'
				})
			}
		}).catch(error => {
			if (error && error.response && error.response.status === 401) {
				clearSession()
				ElNotification({
					title: '评论失败',
					message: '登录状态已失效，请重新登录',
					type: 'error'
				})
					const returnTo = `${window.location.pathname}${window.location.search}${window.location.hash}`
					window.location.replace(`/training/login?returnTo=${encodeURIComponent(returnTo)}`)
					return
				}
				ElNotification({
					title: '评论失败',
					message: error?.response?.data?.msg || (error?.response?.status === 403 ? '当前文章不允许评论' : '评论发送失败'),
					type: 'error'
			})
		})
	},
	goBlogPage(context, blog) {
		return router.push(`/blog/${blog.id}`)
	},
}
