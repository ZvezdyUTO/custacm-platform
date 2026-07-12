<template>
	<!--评论列表-->
	<div>
		<CommentForm v-if="isLoggedIn && parentCommentId===-1"/>
		<div class="ui info message" v-if="!isLoggedIn">登录后的队员可以发表评论。<router-link :to="{path: '/training/login', query: {returnTo: $route.fullPath}}">去登录</router-link></div>
		<h3 class="ui dividing header">Comments | 共 {{ allComment }} 条评论<span v-if="closeComment!==0">（{{ closeComment }} 条评论被隐藏）</span></h3>
		<h3 class="ui header" v-if="allComment===0">快来抢沙发！</h3>
		<div class="comment" v-for="comment in comments" :key="comment.id">
			<span class="anchor" :id="`comment-${comment.id}`"></span>
			<a class="ui circular image avatar">
				<img :src="comment.avatar">
			</a>
			<div class="content">
				<span class="comment-author"><a class="nickname" :href="comment.website!=''&&comment.website!=null?comment.website:null" target="_blank" rel="external nofollow noopener">{{ comment.nickname }}</a><small v-if="comment.username" class="comment-username">{{ comment.username }}</small></span>
				<div class="ui black left pointing label" v-if="comment.adminComment">{{ $store.state.siteInfo.commentAdminFlag }}</div>
				<div class="metadata">
					<strong class="date">{{ $filters.dateFormat(comment.createTime, 'YYYY-MM-DD HH:mm') }}</strong>
				</div>
				<el-button v-if="isLoggedIn" size="small" type="primary" @click="setReply(comment.id)">回复</el-button>
				<div class="text" v-html="sanitizeHtml(comment.content)"></div>
			</div>
			<div class="comments" v-if="comment.replyComments.length>0">
				<div class="comment" v-for="reply in comment.replyComments" :key="reply.id">
					<span class="anchor" :id="`comment-${reply.id}`"></span>
					<a class="ui circular image avatar">
						<img :src="reply.avatar">
					</a>
					<div class="content">
						<span class="comment-author"><a class="nickname" :href="reply.website!=''&&reply.website!=null?reply.website:null" target="_blank" rel="external nofollow noopener">{{ reply.nickname }}</a><small v-if="reply.username" class="comment-username">{{ reply.username }}</small></span>
						<div class="ui black left pointing label" v-if="reply.adminComment">{{ $store.state.siteInfo.commentAdminFlag }}</div>
						<div class="metadata">
							<strong class="date">{{ $filters.dateFormat(reply.createTime, 'YYYY-MM-DD HH:mm') }}</strong>
						</div>
            <el-button v-if="isLoggedIn" size="small" type="primary" @click="setReply(reply.id)">回复</el-button>
						<div class="text">
							<a :href="`#comment-${reply.parentCommentId}`">@{{ reply.parentCommentNickname }}</a>
							<div v-html="sanitizeHtml(reply.content)"></div>
						</div>
					</div>
					<CommentForm v-if="isLoggedIn && parentCommentId===reply.id"/>
				</div>
			</div>
			<div class="border"></div>
			<CommentForm v-if="isLoggedIn && parentCommentId===comment.id"/>
		</div>
	</div>
</template>

<script>
	import {mapState} from 'vuex'
	import {readToken, SESSION_CHANGE_EVENT} from "@/auth/session";
	import CommentForm from "./CommentForm";
	import {SET_PARENT_COMMENT_ID} from "@/store/mutations-types";
	import {sanitizeHtml} from '@/util/sanitizeHtml'

	export default {
		name: "Comment",
		components: {CommentForm},
		data() {
			return {
				isLoggedIn: !!readToken()
			}
		},
		computed: {
			...mapState(['allComment', 'closeComment', 'comments', 'parentCommentId'])
		},
		mounted() {
			window.addEventListener('storage', this.handleStorage)
			window.addEventListener(SESSION_CHANGE_EVENT, this.refreshLoginState)
		},
		beforeUnmount() {
			window.removeEventListener('storage', this.handleStorage)
			window.removeEventListener(SESSION_CHANGE_EVENT, this.refreshLoginState)
		},
		methods: {
			sanitizeHtml,
			refreshLoginState() {
				this.isLoggedIn = !!readToken()
			},
			handleStorage(event) {
				if (
					event.key === null
					|| event.key === 'custacm.accessToken'
					|| event.key === 'custacm.user'
				) {
					this.refreshLoginState()
				}
			},
			setReply(id) {
				this.$store.commit(SET_PARENT_COMMENT_ID, id)
			}
		}
	}
</script>

<style scoped>
	.comments + .border {
		position: absolute;
		left: 34px;
		top: 47px;
		bottom: 0;
		border-style: solid;
		border-width: 0 0 0 1px;
		border-color: #e0e0e0;
	}

	.ui.threaded.comments .comment .comments {
		box-shadow: none;
		margin-top: -2em;
	}

	.ui.info.message > a { margin-left: 8px; color: #17324d; font-weight: 700; }

	.comment {
		padding-right: 1em !important;
		padding-left: 1em !important;
	}

	.nickname {
		font-weight: bolder;
		color: #000;
	}

	.comment-author {
		display: inline-flex;
		flex-direction: column;
		vertical-align: top;
		line-height: 1.15;
	}

	.comment-username {
		margin-top: 4px;
		color: #9a9a9a;
		font-size: 11px;
		font-weight: 500;
		letter-spacing: .01em;
	}

	.comment .el-button {
		margin-left: 5px;
		padding: 4px 5px;
	}

	.comment > .anchor {
		position: absolute;
		left: 0;
		top: -48px;
	}

	.comments .comment:first-child {
		margin-top: 0 !important;
	}

	.comment .comments .comment {
		box-shadow: 0 0 5px rgb(0, 0, 0, 0.1);
		border-radius: 5px;
		margin-top: 12px;
		padding-top: 10px !important;
		padding-bottom: 10px !important;
	}

	.comment .comments .comment > .anchor {
		top: -55px;
	}

	.ui.comments .comment .avatar {
		width: 56px !important;
		height: 56px !important;
		margin: 0;
	}

	.ui.comments .comment > .content {
		margin-left: 72px !important;
		min-height: 56px;
	}

	.ui.comments .comment .text {
		white-space: pre-wrap !important;
		line-height: 1.5;
	}

	.ui.comments .comment .text a {
		cursor: pointer;
		margin-right: 8px;
		font-weight: bolder;
		color: rgba(0, 0, 0, .87);
	}

	.ui.comments .comment .text div {
		display: inline;
	}

	.label {
		cursor: default;
		padding: 4px 6px !important;
		font-weight: 500 !important;
	}

	.comment .form {
		margin-top: 20px;
	}
</style>
