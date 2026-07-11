import {
	SAVE_COMMENT_RESULT,
	SET_PARENT_COMMENT_ID,
	RESET_COMMENT_FORM,
	SET_BLOG_PASSWORD_DIALOG_VISIBLE,
	SET_BLOG_PASSWORD_FORM
} from "./mutations-types";

import {getCommentListByQuery, submitComment} from "@/api/comment";
import {clearSession} from "@/auth/session";
import {ElMessage, ElNotification} from "element-plus";
import router from "../router";
import tvMapper from '@/plugins/tvMapper.json'
import aruMapper from '@/plugins/aruMapper.json'
import paopaoMapper from '@/plugins/paopaoMapper.json'

function escapeCommentHtml(value) {
	return String(value).replace(/[&<>"']/g, character => ({
		'&': '&amp;',
		'<': '&lt;',
		'>': '&gt;',
		'"': '&quot;',
		"'": '&#39;',
	})[character])
}

export default {
	getCommentList({commit, rootState}) {
		//密码保护的文章，需要发送密码验证通过后保存在localStorage的Token
		const blogToken = window.localStorage.getItem(`blog${rootState.commentQuery.blogId}`)
		const token = blogToken ? blogToken : ''

		function replaceEmoji(comment, emoji) {
			comment.content = comment.content.replace(new RegExp(emoji.reg, 'g'), `<img src="${emoji.src}">`)
		}

		function convertEmoji(comment) {
			tvMapper.forEach(emoji => {
				replaceEmoji(comment, emoji)
			})
			aruMapper.forEach(emoji => {
				replaceEmoji(comment, emoji)
			})
			paopaoMapper.forEach(emoji => {
				replaceEmoji(comment, emoji)
			})
		}

		getCommentListByQuery(token, rootState.commentQuery).then(res => {
			if (res.code === 200) {
				res.data.comments.list.forEach(comment => {
					//转义评论中的html
					comment.content = escapeCommentHtml(comment.content)
					//查找评论中是否有表情
					if (comment.content.indexOf('@[') != -1) {
						convertEmoji(comment)
					}
					comment.replyComments.forEach(comment => {
						//转义评论中的html
						comment.content = escapeCommentHtml(comment.content)
						//查找评论中是否有表情
						if (comment.content.indexOf('@[') != -1) {
							convertEmoji(comment)
						}
					})
				})
				commit(SAVE_COMMENT_RESULT, res.data)
			}
		}).catch(() => {
			ElMessage.error("请求失败")
		})
	},
	submitCommentForm({rootState, dispatch, commit}, token) {
		let form = {...rootState.commentForm}
		form.page = rootState.commentQuery.page
		form.blogId = rootState.commentQuery.blogId
		form.parentCommentId = rootState.parentCommentId
		submitComment(token, form).then(res => {
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
				window.location.replace('/training/login')
				return
			}
			ElNotification({
				title: '评论失败',
				message: '异常错误',
				type: 'error'
			})
		})
	},
	goBlogPage({commit}, blog) {
		if (blog.privacy) {
			const blogToken = window.localStorage.getItem(`blog${blog.id}`)
			if (blogToken) {
				return router.push(`/blog/${blog.id}`)
			}
			commit(SET_BLOG_PASSWORD_FORM, {blogId: blog.id, password: ''})
			commit(SET_BLOG_PASSWORD_DIALOG_VISIBLE, true)
		} else {
			router.push(`/blog/${blog.id}`)
		}
	},
}
