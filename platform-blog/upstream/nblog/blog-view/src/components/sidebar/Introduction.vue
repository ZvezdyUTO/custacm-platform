<template>
	<div class="ui segments m-box profile-card">
		<div v-if="displayProfile" class="ui card">
			<div class="profile-avatar-shell">
				<button
					type="button"
					class="profile-avatar-button"
					:aria-label="authorUsername ? '文章作者头像' : (isProfilePage ? '更换头像' : '进入个人页面')"
					:disabled="Boolean(authorUsername)"
					@click="handleAvatarClick"
				>
					<img :src="avatarSrc" :alt="`${displayProfile.nickname || displayProfile.username} 的头像`">
					<span v-if="!authorUsername" class="avatar-action">{{ isProfilePage ? '更换头像' : '个人页面' }}</span>
				</button>
				<input ref="fileInput" class="visually-hidden" type="file" accept="image/png,image/jpeg" @change="selectAvatar">
			</div>
			<div class="content profile-identity" align="center">
				<div class="header">{{ displayProfile.nickname || displayProfile.username }}</div>
				<div class="profile-username">@{{ displayProfile.username }}</div>
				<div v-if="displayProfile.email" class="profile-username profile-email">{{ displayProfile.email }}</div>
				<p class="profile-signature" :class="{'is-empty': !displayProfile.signature}">
					{{ displayProfile.signature || '还没有个性签名' }}
				</p>
			</div>
		</div>
		<div v-else class="ui card guest-card">
			<div class="content" align="center">
				<div class="guest-mark" aria-hidden="true">C</div>
				<div class="header">尚未登录</div>
				<p>登录后显示你的头像和个人资料。</p>
				<router-link to="/training/login?returnTo=/profile" class="ui button profile-login">登录训练中心</router-link>
			</div>
		</div>
		<div v-if="displayProfile" class="ui segment profile-notes">
			<div class="profile-links-heading">
				<span>友情链接</span>
				<small>{{ profileLinks.length }}/8</small>
			</div>
			<nav v-if="profileLinks.length" class="profile-links" aria-label="个人友情链接">
				<a v-for="link in profileLinks" :key="link.id || link.url" :href="link.url" target="_blank" rel="external nofollow noopener">
					<span class="profile-link-icon" aria-hidden="true">
						<img
							v-if="faviconUrl(link.url) && !faviconFailures[link.url]"
							:src="faviconUrl(link.url)"
							alt=""
							loading="lazy"
							@error="faviconFailures[link.url] = true"
						>
						<i v-else class="globe icon"></i>
					</span>
					<span>{{ link.label }}</span>
					<i class="external alternate icon external-mark" aria-hidden="true"></i>
				</a>
			</nav>
			<div v-else class="empty-profile-links">还没有友情链接</div>
		</div>
		<AvatarCropDialog ref="cropDialog" :saving="saving" :error-message="errorMessage" @save="saveAvatar"/>
	</div>
</template>

<script>
	// Author: huangbingrui.awa
	import AvatarCropDialog from '@/components/profile/AvatarCropDialog.vue'
	import {getCurrentProfile, getPublicProfile, updateCurrentAvatar} from '@/api/profile'
	import {readToken, readUser, SESSION_CHANGE_EVENT, writeUser} from '@/auth/session'

	export default {
		name: 'Introduction',
		components: {AvatarCropDialog},
		props: {
			authorUsername: {type: String, default: ''},
			authorSummary: {type: Object, default: null},
		},
		data() {
			return {
				authUser: readUser(),
				authorProfile: null,
				saving: false,
				errorMessage: '',
				faviconFailures: {},
			}
		},
		computed: {
			displayProfile() {
				return this.authorUsername ? (this.authorProfile || this.authorSummary) : this.authUser
			},
			isProfilePage() {
				return this.$route.name === 'profile'
			},
			avatarSrc() {
				return this.displayProfile?.avatarOriginalUrl || this.displayProfile?.avatar || '/img/default-avatar.jpg'
			},
			profileLinks() {
				return Array.isArray(this.displayProfile?.links) ? this.displayProfile.links : []
			},
		},
		watch: {
			authorUsername: {
				immediate: true,
				handler() { this.loadAuthorProfile() },
			},
		},
		mounted() {
			window.addEventListener('storage', this.refreshUser)
			window.addEventListener(SESSION_CHANGE_EVENT, this.refreshUser)
			this.loadProfile()
		},
		beforeUnmount() {
			window.removeEventListener('storage', this.refreshUser)
			window.removeEventListener(SESSION_CHANGE_EVENT, this.refreshUser)
		},
		methods: {
			faviconUrl(url) {
				try {
					return `${new URL(url, window.location.origin).origin}/favicon.ico`
				} catch {
					return ''
				}
			},
			async loadAuthorProfile() {
				const username = this.authorUsername
				this.authorProfile = null
				if (!username) return
				try {
					const profile = await getPublicProfile(username)
					if (this.authorUsername === username) this.authorProfile = profile
				} catch {
					// The article remains readable if its author profile is temporarily unavailable.
				}
			},
			async loadProfile() {
				const token = readToken()
				if (!token) return
				try {
					const profile = await getCurrentProfile(token)
					writeUser(profile)
					this.authUser = readUser()
				} catch {
					// Keep the last valid session summary while the API is temporarily unavailable.
				}
			},
			refreshUser() {
				this.authUser = readUser()
			},
			handleAvatarClick() {
				if (this.authorUsername) return
				if (!this.isProfilePage) {
					this.$router.push('/profile')
					return
				}
				this.$refs.fileInput.click()
			},
			selectAvatar(event) {
				const [file] = event.target.files || []
				event.target.value = ''
				if (file) {
					this.errorMessage = ''
					this.$refs.cropDialog.open(file)
				}
			},
			async saveAvatar(blob) {
				const token = readToken()
				if (!token) {
					this.errorMessage = '登录状态已失效，请重新登录。'
					return
				}
				this.saving = true
				this.errorMessage = ''
				let saved = false
				try {
					const profile = await updateCurrentAvatar(token, blob)
					writeUser(profile)
					this.authUser = readUser()
					saved = true
					this.msgSuccess('头像已更新')
				} catch (error) {
					this.errorMessage = error?.response?.data?.msg || '头像上传失败，请稍后重试。'
				} finally {
					this.saving = false
					if (saved) {
						await this.$nextTick()
						this.$refs.cropDialog.close()
					}
				}
			},
		},
	}
</script>

<style scoped>
	.profile-card,
	.profile-card > .ui.card {
		width: 100%;
	}

	.profile-notes {
		margin: 0 !important;
		border: 0 !important;
		border-top: 1px solid #e0e5ea !important;
		border-radius: 0 0 4px 4px !important;
		box-shadow: none !important;
		padding: 16px !important;
		font-family: inherit;
	}

	.profile-links-heading {
		display: flex;
		align-items: center;
		justify-content: space-between;
		color: #5f6b76;
		font-size: 11px;
		font-weight: 700;
		letter-spacing: .08em;
	}

	.profile-signature {
		margin: 16px 0 0;
		border-top: 1px solid #e4e8ec;
		color: #596570;
		padding: 14px 4px 0;
		font-size: 12px;
		line-height: 1.65;
		word-break: break-word;
	}

	.profile-signature.is-empty {
		color: #9ba4ad;
	}

	.profile-links-heading small {
		color: #a0a8b0;
		font-size: 10px;
		font-weight: 500;
		letter-spacing: normal;
	}

	.profile-links {
		display: grid;
		gap: 7px;
		margin-top: 10px;
	}

	.profile-links a {
		display: grid;
		grid-template-columns: 18px minmax(0, 1fr) 14px;
		align-items: center;
		border: 1px solid #e0e5ea;
		border-left: 2px solid #17324d;
		background: #f6f8fa;
		color: #3b4650;
		padding: 9px 9px;
		font-size: 12px;
		font-weight: 600;
		text-decoration: none;
		transition: border-color 150ms ease, background 150ms ease;
	}

	.profile-links a:hover,
	.profile-links a:focus-visible {
		border-color: #17324d;
		background: #eef2f5;
	}

	.profile-links a span {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.profile-links a i {
		margin: 0 !important;
		color: #687785;
	}

	.profile-link-icon {
		display: inline-flex;
		width: 18px;
		height: 18px;
		align-items: center;
		justify-content: center;
		border-radius: 4px;
		background: #e8edf2;
		overflow: hidden;
	}

	.profile-link-icon img {
		display: block;
		width: 14px;
		height: 14px;
		object-fit: contain;
	}

	.profile-link-icon i {
		width: auto !important;
		font-size: 11px;
	}

	.profile-links .external-mark {
		color: #a0a8b0;
		font-size: 10px;
	}

	.empty-profile-links {
		display: block;
		margin-top: 10px;
		border: 1px dashed #d4dae0;
		color: #89939d;
		padding: 10px;
		font-size: 12px;
		text-align: center;
	}

	.visually-hidden {
		position: absolute;
		width: 1px;
		height: 1px;
		overflow: hidden;
		clip-path: inset(50%);
		white-space: nowrap;
	}

	.profile-card > .ui.card {
		margin: 0;
		border: 0;
		border-radius: 4px;
		box-shadow: none;
	}

	.profile-avatar-shell {
		aspect-ratio: 1;
		overflow: hidden;
		background: #e8edf2;
	}

	.profile-avatar-button {
		position: relative;
		display: block;
		width: 100%;
		height: 100%;
		border: 0;
		background: transparent;
		padding: 0;
		cursor: pointer;
	}

	.profile-avatar-button img {
		display: block;
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.avatar-action {
		position: absolute;
		right: 10px;
		bottom: 10px;
		border-radius: 3px;
		background: rgba(23, 50, 77, .88);
		color: #fff;
		padding: 5px 8px;
		font-size: 12px;
		letter-spacing: .04em;
		opacity: 0;
		transform: translateY(4px);
		transition: opacity 150ms ease, transform 150ms ease;
	}

	.profile-avatar-button:hover .avatar-action,
	.profile-avatar-button:focus-visible .avatar-action {
		opacity: 1;
		transform: translateY(0);
	}

	.profile-avatar-button:focus-visible {
		outline: 3px solid #17324d;
		outline-offset: -3px;
	}

	.profile-identity {
		padding: 18px 12px 20px !important;
	}

	.profile-identity .header {
		color: #20252b !important;
		font-size: 24px !important;
		font-weight: 700 !important;
		line-height: 1.25 !important;
	}

	.profile-username {
		margin-top: 6px;
		color: #89939f;
		font-size: 13px;
		letter-spacing: .06em;
	}

	.profile-email {
		overflow-wrap: anywhere;
	}

	.guest-card .content {
		padding: 24px 16px !important;
	}

	.guest-mark {
		display: grid;
		width: 68px;
		height: 68px;
		place-items: center;
		margin: 0 auto 14px;
		border-radius: 50%;
		background: #17324d;
		color: #fff;
		font-size: 28px;
		font-weight: 700;
	}

	.guest-card p {
		color: #89939f;
		font-size: 13px;
	}

	.profile-login {
		margin-top: 8px !important;
		background: #17324d !important;
		color: #fff !important;
	}
</style>
