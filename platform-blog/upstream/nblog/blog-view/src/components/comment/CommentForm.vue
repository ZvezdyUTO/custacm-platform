<template>
	<!-- 评论输入表单 -->
	<div class="form">
		<h3>
			发表评论
			<el-button class="m-small" size="small" type="primary" @click="$store.commit(SET_PARENT_COMMENT_ID, -1)" v-show="parentCommentId!==-1">取消回复</el-button>
		</h3>
		<el-form class="comment-form-layout" :inline="true" :model="commentForm" size="small">
			<el-input ref="commentInput" :class="'textarea'" type="textarea" :rows="5" v-model="commentForm.content" placeholder="评论千万条，友善第一条"
			          maxlength="250" show-word-limit :validate-event="false"></el-input>
			<div class="el-form-item el-form-item--small emoji">
				<button class="emoji-trigger" type="button" :aria-expanded="emojiShow" aria-haspopup="dialog"
				        aria-label="选择 Noto emoji" @mousedown.prevent="captureSelection" @click="showEmojiBox">
					<img :src="emojiTriggerSrc" alt="" draggable="false"><span>表情</span>
				</button>
				<div class="emoji-mask" v-show="emojiShow" @click="hideEmojiBox"></div>
				<transition name="emoji-popover">
					<section ref="emojiBox" class="emoji-box" v-if="emojiShow" role="dialog" aria-label="Noto emoji 选择器"
					         tabindex="-1" @keydown.esc.stop="hideEmojiBox" @keydown.tab="trapEmojiTab">
						<header class="emoji-title">
							<div><strong>Noto emoji</strong><small>Google 圆形表情</small></div>
							<button type="button" aria-label="关闭表情选择器" @click="hideEmojiBox">×</button>
						</header>
						<nav class="emoji-tabs" aria-label="表情分类">
							<button v-for="category in emojiCategories" :key="category.id" type="button"
							        :class="{on: activeEmojiTab === category.id}" @click="activeEmojiTab = category.id">
								{{ category.label }}
							</button>
						</nav>
						<div class="emoji-wrap" role="group" :aria-label="`${activeEmojiCategory.label}表情`">
							<button class="emoji-list" v-for="emoji in activeEmojis" :key="emoji.unicode" type="button"
							        :title="emoji.label" :aria-label="emoji.label" @click="insertEmoji(emoji.unicode)">
								<img :src="notoEmojiUrl(emoji)" :alt="emoji.unicode" draggable="false">
							</button>
						</div>
					</section>
				</transition>
			</div>
			<el-form-item>
					<el-button class="comment-submit-button" type="primary" :disabled="submitting" v-throttle="[postForm,`click`,3000]">{{ submitting ? '发送中…' : '发表评论' }}</el-button>
			</el-form-item>
		</el-form>
	</div>
</template>

<script>
	import {mapState} from 'vuex'
	import {readToken} from "@/auth/session";
	import {SET_PARENT_COMMENT_ID} from "@/store/mutations-types";
	import {notoEmojiCategories, notoEmojiUrl} from '@/plugins/notoEmoji'
	import {trapDialogTab} from '@/util/dialogFocus'

	export default {
		name: "CommentForm",
		computed: {
			...mapState(['parentCommentId', 'commentForm', 'commentQuery']),
			activeEmojiCategory() {
				return this.emojiCategories.find(category => category.id === this.activeEmojiTab) || this.emojiCategories[0]
			},
			activeEmojis() {
				return this.activeEmojiCategory.emojis
			},
			emojiTriggerSrc() {
				return notoEmojiUrl(this.emojiCategories[0].emojis[0])
			},
		},
		data() {
			return {
				SET_PARENT_COMMENT_ID,
				emojiShow: false,
				activeEmojiTab: 'frequent',
				emojiCategories: notoEmojiCategories,
				textarea: null,
				submitting: false,
				start: 0,
				end: 0,
			}
		},
		mounted() {
			const input = this.$refs.commentInput
			this.textarea = input?.textarea
				|| (input?.$el?.matches?.('textarea') ? input.$el : input?.$el?.querySelector?.('textarea'))
				|| this.$el?.querySelector?.('textarea')
		},
		methods: {
			notoEmojiUrl,
			captureSelection() {
				if (!this.textarea) return
				this.start = this.textarea.selectionStart
				this.end = this.textarea.selectionEnd
			},
			showEmojiBox() {
				this.captureSelection()
				this.emojiShow = !this.emojiShow
				if (this.emojiShow) this.$nextTick(() => this.$refs.emojiBox?.focus())
			},
			trapEmojiTab(event) {
				trapDialogTab(event, this.$refs.emojiBox)
			},
			insertEmoji(unicode) {
				let str = this.commentForm.content
				const nextContent = str.substring(0, this.start) + unicode + str.substring(this.end)
				if (nextContent.length > 250) {
					return this.$notify({title: '无法插入表情', message: '评论最多 250 个字符', type: 'warning'})
				}
				this.commentForm.content = nextContent
				this.start += unicode.length
				this.end = this.start
				this.emojiShow = false
				this.$nextTick(() => {
					this.textarea.focus()
					this.textarea.setSelectionRange(this.start, this.end)
				})
			},
			hideEmojiBox() {
				this.emojiShow = false
				this.$nextTick(() => {
					this.textarea?.focus()
					this.textarea?.setSelectionRange(this.start, this.end)
				})
			},
				async postForm() {
					const token = readToken()
					if (this.submitting || !token || this.commentForm.content === '' || this.commentForm.content.length > 250) {
						return this.$notify({title: '评论失败', message: token ? '评论内容有误' : '请先登录', type: 'warning'})
					}
					this.submitting = true
					try { await this.$store.dispatch('submitCommentForm', token) }
					finally { this.submitting = false }
				}
		}
	}
</script>

<style>
	.form {
		position: relative;
		background: transparent;
	}

	.form h3 {
		margin: 0 0 12px;
		color: var(--color-text);
		font-size: 16px;
		font-weight: 750 !important;
	}

	.form .m-small {
		margin-left: 5px;
		padding: 4px 5px;
	}

	.comment-form-layout {
		display: flex !important;
		align-items: center;
		flex-wrap: wrap;
		gap: 10px;
	}

	.comment-form-layout .textarea {
		flex: 0 0 100%;
		margin: 0 0 4px;
	}

	.comment-form-layout > .el-form-item {
		margin: 0 !important;
	}

	.el-form textarea {
		padding: 6px 8px;
	}

	.el-form textarea, .el-form input {
		color: black;
	}

	.el-form .el-form-item__label {
		padding-right: 3px;
	}

	.emoji {
		position: relative;
		margin-right: 8px;
		user-select: none;
	}

	.emoji-trigger {
		display: inline-flex;
		align-items: center;
		gap: 7px;
		height: 32px;
		border: 1px solid var(--anthropic-dark);
		border-radius: 7px;
		background: var(--anthropic-dark);
		color: var(--anthropic-ivory-light);
		padding: 3px 10px 3px 7px;
		font: inherit;
		font-size: 12px;
		font-weight: 700;
		line-height: 1;
		cursor: pointer;
		transition: border-color 160ms ease, background-color 160ms ease, transform 160ms ease;
	}

	.emoji-trigger:hover,
	.emoji-trigger[aria-expanded="true"] {
		border-color: var(--anthropic-slate-medium);
		background: var(--anthropic-slate-medium);
		transform: translateY(-1px);
	}

	.emoji-trigger:focus-visible,
	.emoji-box button:focus-visible {
		outline: 2px solid var(--anthropic-clay);
		outline-offset: 2px;
	}

	.emoji-trigger img { width: 24px; height: 24px; }

	.comment-submit-button.el-button--primary {
		min-height: 32px;
		border-color: var(--anthropic-dark) !important;
		background: var(--anthropic-dark) !important;
		color: var(--anthropic-ivory-light) !important;
		padding: 6px 14px !important;
		font-size: 12px;
		font-weight: 700;
		line-height: 1;
	}

	.comment-submit-button.el-button--primary:hover,
	.comment-submit-button.el-button--primary:focus-visible {
		border-color: var(--anthropic-slate-medium) !important;
		background: var(--anthropic-slate-medium) !important;
	}

	.emoji-box {
		position: absolute;
		z-index: 101;
		top: 41px;
		left: 0;
		width: 360px;
		overflow: hidden;
		border: 1px solid var(--anthropic-cloud-light);
		border-radius: 12px;
		background: var(--anthropic-ivory-light);
		box-shadow: 0 18px 42px color-mix(in srgb, var(--anthropic-slate-dark) 18%, transparent);
		color: var(--anthropic-slate-dark);
	}

	.emoji-box * { box-sizing: border-box; }

	.emoji-title {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 13px 14px 10px;
		border-bottom: 1px solid var(--anthropic-cloud-light);
	}

	.emoji-title div { display: grid; gap: 2px; }
	.emoji-title strong { font-size: 13px; letter-spacing: .01em; }
	.emoji-title small { color: var(--anthropic-cloud-dark); font-size: 10px; }

	.emoji-title > button {
		width: 28px;
		height: 28px;
		border: 0;
		border-radius: 7px;
		background: transparent;
		color: var(--anthropic-slate-light);
		font-size: 20px;
		line-height: 1;
		cursor: pointer;
	}

	.emoji-title > button:hover { background: var(--anthropic-ivory-medium); color: var(--anthropic-slate-dark); }

	.emoji-tabs {
		display: flex;
		gap: 3px;
		padding: 8px 10px 5px;
	}

	.emoji-tabs button {
		border: 0;
		border-radius: 6px;
		background: transparent;
		color: var(--anthropic-slate-light);
		padding: 6px 10px;
		font-size: 11px;
		font-weight: 700;
		cursor: pointer;
	}

	.emoji-tabs button:hover { background: var(--anthropic-ivory-medium); color: var(--anthropic-slate-medium); }
	.emoji-tabs button.on { background: var(--anthropic-ivory-dark); color: var(--anthropic-slate-dark); }

	.emoji-wrap {
		display: grid;
		grid-template-columns: repeat(8, 1fr);
		gap: 3px;
		min-height: 152px;
		max-height: 218px;
		overflow-y: auto;
		padding: 7px 10px 12px;
		scrollbar-width: thin;
		scrollbar-color: var(--anthropic-cloud-medium) transparent;
	}

	.emoji-list {
		display: grid;
		place-items: center;
		width: 38px;
		height: 38px;
		border: 0;
		border-radius: 8px;
		background: transparent;
		cursor: pointer;
		transition: background-color 140ms ease, transform 140ms ease;
	}

	.emoji-list:hover { background: #e3dacc; transform: translateY(-2px) scale(1.06); }
	.emoji-list img { width: 29px; height: 29px; pointer-events: none; }

	.emoji-mask {
		position: fixed;
		z-index: 100;
		inset: 0;
		pointer-events: auto;
	}

	.emoji-popover-enter-active,
	.emoji-popover-leave-active {
		transition: opacity 150ms ease, transform 150ms ease;
		transform-origin: top left;
	}

	.emoji-popover-enter-from,
	.emoji-popover-leave-to {
		opacity: 0;
		transform: translateY(-5px) scale(.98);
	}

	@media (prefers-reduced-motion: reduce) {
		.emoji-trigger,
		.emoji-list,
		.emoji-popover-enter-active,
		.emoji-popover-leave-active { transition: none; }
	}

	@media (max-width: 520px) {
		.emoji-box { width: min(340px, calc(100vw - 42px)); }
		.emoji-wrap { grid-template-columns: repeat(7, 1fr); }
	}
</style>
