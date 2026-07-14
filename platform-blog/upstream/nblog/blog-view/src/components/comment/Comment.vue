<template>
	<!--评论列表-->
	<div>
		<CommentForm v-if="isLoggedIn && parentCommentId===-1"/>
		<div class="comment-login-message" v-if="!isLoggedIn">登录后的队员可以发表评论。<router-link :to="{path: '/training/login', query: {returnTo: $route.fullPath}}">去登录</router-link></div>
		<h3 class="comment-section-heading"><span>评论</span><small>{{ allComment }} 条</small><em v-if="closeComment!==0">{{ closeComment }} 条已隐藏</em></h3>
		<h3 class="section-heading" v-if="allComment===0">快来抢沙发！</h3>
		<div class="comment" v-for="comment in comments" :key="comment.id">
			<span class="anchor" :id="`comment-${comment.id}`"></span>
			<span class="comment-avatar">
				<img :src="comment.avatar">
			</span>
			<div class="content">
				<div class="comment-header">
					<div class="comment-heading">
						<span class="comment-author"><a class="nickname" :href="comment.website!=''&&comment.website!=null?comment.website:null" target="_blank" rel="external nofollow noopener">{{ comment.nickname }}</a><small v-if="comment.username" class="comment-username">{{ comment.username }}</small></span>
						<div class="admin-comment-label" v-if="comment.adminComment">{{ $store.state.siteInfo.commentAdminFlag }}</div>
					</div>
					<div class="comment-actions">
						<div class="metadata"><time class="date">{{ $filters.dateFormat(comment.createTime, 'YYYY-MM-DD HH:mm') }}</time></div>
						<el-button v-if="isLoggedIn" class="comment-reply-button" size="small" type="primary" @click="setReply(comment.id)">回复</el-button>
					</div>
				</div>
				<div class="text" v-html="sanitizeHtml(comment.content)"></div>
			</div>
			<div class="comments" v-if="comment.replyComments.length>0">
				<div class="comment" v-for="reply in comment.replyComments" :key="reply.id">
					<span class="anchor" :id="`comment-${reply.id}`"></span>
					<span class="comment-avatar">
						<img :src="reply.avatar">
					</span>
					<div class="content">
						<div class="comment-header">
							<div class="comment-heading">
								<span class="comment-author"><a class="nickname" :href="reply.website!=''&&reply.website!=null?reply.website:null" target="_blank" rel="external nofollow noopener">{{ reply.nickname }}</a><small v-if="reply.username" class="comment-username">{{ reply.username }}</small></span>
								<div class="admin-comment-label" v-if="reply.adminComment">{{ $store.state.siteInfo.commentAdminFlag }}</div>
							</div>
							<div class="comment-actions">
								<div class="metadata"><time class="date">{{ $filters.dateFormat(reply.createTime, 'YYYY-MM-DD HH:mm') }}</time></div>
								<el-button v-if="isLoggedIn" class="comment-reply-button" size="small" type="primary" @click="setReply(reply.id)">回复</el-button>
							</div>
						</div>
						<div class="text">
							<a :href="`#comment-${reply.parentCommentId}`">@{{ reply.parentCommentNickname }}</a>
							<div v-html="sanitizeHtml(reply.content)"></div>
						</div>
					</div>
					<CommentForm v-if="isLoggedIn && parentCommentId===reply.id"/>
				</div>
			</div>
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
	.comment-login-message > a {
		margin-left: 8px;
		color: var(--anthropic-slate-dark);
		font-weight: 750;
	}

	.comment-section-heading {
		display: flex;
		align-items: baseline;
		gap: 8px;
		border-bottom: 1px solid var(--color-border);
		margin: 28px 0 14px;
		padding-bottom: 12px;
		color: var(--color-text);
		font-size: 18px;
	}

	.comment-section-heading small,
	.comment-section-heading em {
		color: var(--color-text-muted);
		font-size: 12px;
		font-style: normal;
		font-weight: 600;
	}

	.comment-section-heading em {
		margin-left: auto;
	}

	.comment {
		position: relative;
		display: grid;
		grid-template-columns: 48px minmax(0, 1fr);
		gap: 0 14px;
		margin: 12px 0;
		border: 1px solid var(--color-border);
		border-radius: 14px;
		background: var(--color-surface-subtle);
		padding: 16px !important;
	}

	.comment > .anchor {
		position: absolute;
		top: -64px;
		left: 0;
	}

	.comment .comment-avatar {
		display: block;
		grid-column: 1;
		width: 48px !important;
		height: 48px !important;
		overflow: hidden;
		border: 1px solid var(--color-border);
		border-radius: 14px;
		background: var(--color-surface);
	}

	.comment .comment-avatar img {
		display: block;
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.comment > .content {
		grid-column: 2;
		min-width: 0;
		margin: 0 !important;
	}

	.comment-header,
	.comment-heading,
	.comment-actions {
		display: flex;
		align-items: flex-start;
	}

	.comment-header {
		justify-content: space-between;
		gap: 12px;
	}

	.comment-heading {
		min-width: 0;
		gap: 8px;
	}

	.comment-actions {
		flex: 0 0 auto;
		align-items: center;
		gap: 9px;
	}

	.nickname {
		color: var(--color-text);
		font-weight: 780;
	}

	.comment-author {
		display: inline-flex;
		min-width: 0;
		flex-direction: column;
		line-height: 1.15;
	}

	.comment-username {
		margin-top: 4px;
		color: var(--color-text-muted);
		font-size: 11px;
		font-weight: 500;
		letter-spacing: .01em;
	}

	.metadata {
		color: var(--color-text-muted);
		font-size: 11px;
		font-variant-numeric: tabular-nums;
		white-space: nowrap;
	}

	.comment-reply-button.el-button--primary {
		min-height: 28px;
		border-color: var(--anthropic-dark) !important;
		background: var(--anthropic-dark) !important;
		color: var(--anthropic-ivory-light) !important;
		padding: 5px 10px !important;
		font-size: 11px;
	}

	.comment-reply-button.el-button--primary:hover,
	.comment-reply-button.el-button--primary:focus-visible {
		border-color: var(--anthropic-slate-medium) !important;
		background: var(--anthropic-slate-medium) !important;
	}

	.comment .text {
		margin-top: 12px;
		color: var(--color-text);
		font-size: 14px;
		line-height: 1.65;
		white-space: pre-wrap !important;
		word-break: break-word;
	}

	.comment .text :deep(.noto-emoji),
	.comment .text :deep(.legacy-comment-emoji) {
		display: inline-block;
		width: 1.45em;
		height: 1.45em;
		margin: 0 .06em;
		vertical-align: -.34em;
		object-fit: contain;
	}

	.comment .text a {
		margin-right: 8px;
		color: var(--anthropic-slate-dark);
		font-weight: 750;
		cursor: pointer;
	}

	.comment .text div {
		display: inline;
	}

	.admin-comment-label {
		flex: 0 0 auto;
		border-radius: 5px;
		background: var(--anthropic-dark);
		color: var(--anthropic-ivory-light);
		padding: 4px 7px !important;
		font-size: 10px;
		font-weight: 700 !important;
	}

	.comment .comments {
		display: grid;
		grid-column: 2;
		gap: 10px;
		margin: 14px 0 0;
	}

	.comment .comments .comment {
		grid-template-columns: 40px minmax(0, 1fr);
		margin: 0;
		border-color: var(--color-border);
		background: var(--color-surface);
		padding: 12px !important;
	}

	.comment .comments .comment-avatar {
		width: 40px !important;
		height: 40px !important;
		border-radius: 11px;
	}

	.comment > .form {
		grid-column: 2;
		margin-top: 16px;
	}

	@media (max-width: 640px) {
		.comment {
			grid-template-columns: 40px minmax(0, 1fr);
			gap: 0 10px;
			padding: 12px !important;
		}

		.comment .comment-avatar {
			width: 40px !important;
			height: 40px !important;
		}

		.comment-header {
			align-items: flex-start;
			flex-direction: column;
			gap: 8px;
		}

		.comment .comments {
			grid-column: 1 / -1;
		}
	}
</style>
