<template>
	<section class="profile-page content-panel">
		<template v-if="authUser">
			<header class="profile-heading">
				<div class="profile-heading-identity">
					<div class="profile-avatar-shell">
						<button type="button" class="profile-avatar-button" aria-label="更换头像" @click="openAvatarPicker">
							<img :src="profileAvatarSrc" :alt="`${profileName} 的头像`" @error="handleAvatarError">
							<span aria-hidden="true">更换头像</span>
						</button>
						<input ref="avatarInput" class="visually-hidden" type="file" accept="image/png,image/jpeg" @change="selectAvatar">
					</div>
					<div>
						<p class="profile-eyebrow">MY HOMEPAGE</p>
						<h1>{{ profileName }}</h1>
						<p class="profile-handle">@{{ authUser.username }}</p>
					</div>
				</div>
				<button v-if="!editing" type="button" class="edit-profile-button" @click="startEditing">
					<AppIcon name="edit" />编辑资料
				</button>
			</header>

			<AchievementsPanel
				:achievements="achievements"
				:updating="achievementUpdating"
				:reordering="achievementOrderSaving"
				:error-message="profileLoadError || achievementError"
				:loading="profileLoading"
				:retryable="Boolean(profileLoadError)"
				:show-empty="profileLoaded"
				:editable="profileLoaded && !profileLoading"
				@retry="loadProfile"
				@order-change="changeAchievementOrder"
				@visibility-change="changeAchievementVisibility"
			/>

				<form v-if="editing" class="profile-editor" @submit.prevent="saveProfile">
				<header class="editor-heading">
					<div>
						<p class="profile-eyebrow">EDIT PROFILE</p>
						<h2>编辑个人资料</h2>
					</div>
					<span>保存后会同步到左侧名片</span>
				</header>

				<div class="editor-fields">
					<label>
						<span>昵称</span>
						<input ref="nicknameInput" v-model="draft.nickname" maxlength="30" required autocomplete="nickname" placeholder="输入昵称">
						<small>{{ draft.nickname.length }}/30</small>
					</label>
					<label>
						<span>个性签名</span>
						<textarea v-model="draft.signature" :maxlength="signatureMaxLength" rows="3" placeholder="写一句想留在个人卡片上的话"></textarea>
						<small>{{ draft.signature.length }}/{{ signatureMaxLength }}</small>
					</label>
				</div>

				<div class="links-editor">
					<div class="links-heading">
						<div><strong>友情链接</strong><span>最多 8 条，仅支持 HTTP 或 HTTPS 地址</span></div>
						<button type="button" :disabled="draft.links.length >= 8" @click="addLink"><AppIcon name="plus" />添加链接</button>
					</div>
					<div v-if="draft.links.length" class="link-rows">
						<div v-for="(link, index) in draft.links" :key="link.key" class="link-row">
							<span class="link-index">{{ String(index + 1).padStart(2, '0') }}</span>
							<input v-model="link.label" maxlength="30" required :aria-label="`第 ${index + 1} 条链接名称`" placeholder="名称，例如 GitHub">
							<input v-model="link.url" maxlength="2048" required type="url" :aria-label="`第 ${index + 1} 条链接地址`" placeholder="https://example.com">
							<div class="link-actions">
								<button type="button" :disabled="index === 0" aria-label="上移" @click="moveLink(index, -1)"><AppIcon name="arrow-up" /></button>
								<button type="button" :disabled="index === draft.links.length - 1" aria-label="下移" @click="moveLink(index, 1)"><AppIcon name="arrow-down" /></button>
								<button type="button" class="remove-link" aria-label="删除" @click="removeLink(index)"><AppIcon name="trash" /></button>
							</div>
						</div>
					</div>
					<button v-else type="button" class="empty-links" @click="addLink"><AppIcon name="link" />添加第一条友情链接</button>
				</div>

				<section class="password-editor" aria-labelledby="password-editor-title">
					<div class="links-heading">
						<div><strong id="password-editor-title">修改密码</strong><span>新密码至少 6 个字符</span></div>
					</div>
					<div class="password-fields">
						<label><span>旧密码</span><input v-model="passwordDraft.oldPassword" type="password" autocomplete="current-password"></label>
						<label><span>新密码</span><input v-model="passwordDraft.newPassword" type="password" minlength="6" autocomplete="new-password"></label>
						<label><span>确认新密码</span><input v-model="passwordDraft.confirmPassword" type="password" minlength="6" autocomplete="new-password"></label>
					</div>
					<p v-if="passwordError" class="save-error" role="alert">{{ passwordError }}</p>
					<div class="password-actions"><button type="button" class="primary" :disabled="passwordSaving" @click="savePassword">{{ passwordSaving ? '修改中…' : '修改密码' }}</button></div>
				</section>

				<p v-if="saveError" class="save-error" role="alert">{{ saveError }}</p>
				<footer class="editor-actions">
					<button type="button" :disabled="saving" @click="cancelEditing">取消</button>
					<button type="submit" class="primary" :disabled="saving">{{ saving ? '保存中…' : '保存修改' }}</button>
				</footer>
				</form>
				<MyArticles :key="sessionGeneration"/>
				<AvatarCropDialog ref="avatarCropDialog" :saving="avatarSaving" :error-message="avatarError" @save="saveAvatar"/>
			</template>
			<div v-else class="profile-empty">
				<p class="profile-eyebrow">MY HOMEPAGE</p>
				<h1>登录后查看我的主页</h1>
				<p>你的个人资料、赛事荣誉和已发布内容会集中在这里。</p>
			<router-link to="/training/login?returnTo=/profile" class="secondary-button profile-login-link">登录训练中心</router-link>
		</div>
	</section>
</template>

<script>
		// Author: huangbingrui.awa
		import {changeCurrentPassword, getCurrentProfile, replaceCurrentProfileLinks, updateCurrentAvatar, updateCurrentProfile} from '@/api/profile'
		import {setAchievementProfileOrder, setAchievementProfileVisibility} from '@/api/player-competition'
		import {clearSession, readToken, readUser, SESSION_CHANGE_EVENT, writeUser} from '@/auth/session'
		import AchievementsPanel from '@/components/profile/AchievementsPanel.vue'
		import AvatarCropDialog from '@/components/profile/AvatarCropDialog.vue'
		import MyArticles from '@/components/profile/MyArticles.vue'

	let nextLinkKey = 1
	const MAX_SIGNATURE_LENGTH = 40

		export default {
			name: 'Profile',
			components: {AchievementsPanel, AvatarCropDialog, MyArticles},
		data() {
			const authUser = readUser()
			const sessionToken = authUser ? readToken() : null
			return {
				authUser,
				profile: authUser,
				signatureMaxLength: MAX_SIGNATURE_LENGTH,
				sessionToken: sessionToken || '',
				sessionUsername: authUser?.username || '',
				sessionGeneration: 0,
				profileRequestId: 0,
				profileSaveRequestId: 0,
				profileLoading: Boolean(sessionToken && authUser),
				profileLoaded: false,
				profileLoadError: '',
				avatarMutationId: 0,
				avatarSaving: false,
				avatarError: '',
				editing: false,
				saving: false,
				saveError: '',
				draft: {nickname: '', signature: '', links: []},
				passwordDraft: {oldPassword: '', newPassword: '', confirmPassword: ''},
				passwordSaving: false,
				passwordError: '',
				achievementUpdating: {},
				achievementError: '',
				achievementMutationSequence: 0,
				achievementOrderMutationId: 0,
				achievementOrderSaving: false,
			}
		},
		computed: {
			currentProfile() { return this.profile || this.authUser || {} },
			profileName() { return this.currentProfile.nickname || this.authUser?.username },
			profileAvatarSrc() { return this.currentProfile.avatarOriginalUrl || this.currentProfile.avatar || '/img/default-avatar.jpg' },
			achievements() { return Array.isArray(this.currentProfile.achievements) ? this.currentProfile.achievements : [] },
		},
		mounted() {
			window.addEventListener('storage', this.refreshUser)
			window.addEventListener(SESSION_CHANGE_EVENT, this.refreshUser)
			this.loadProfile()
		},
		beforeUnmount() {
			window.removeEventListener('storage', this.refreshUser)
			window.removeEventListener(SESSION_CHANGE_EVENT, this.refreshUser)
			this.sessionGeneration += 1
			this.profileRequestId += 1
			this.profileSaveRequestId += 1
			this.avatarMutationId += 1
			this.achievementOrderMutationId += 1
		},
		methods: {
			isSessionCurrent(token, username, generation) {
				const user = readUser()
				return generation === this.sessionGeneration
					&& readToken() === token
					&& user?.username === username
			},
			isProfileRequestCurrent(requestId, token, username, generation) {
				return requestId === this.profileRequestId
					&& this.isSessionCurrent(token, username, generation)
			},
			errorStatus(error) {
				return Number(error?.response?.status ?? error?.response?.data?.code ?? error?.code)
			},
			expireCurrentSession(token, username, generation) {
				if (!this.isSessionCurrent(token, username, generation)) return false
				this.sessionGeneration += 1
				this.sessionToken = ''
				this.sessionUsername = ''
				this.profileRequestId += 1
				this.profileSaveRequestId += 1
				this.avatarMutationId += 1
				this.achievementOrderMutationId += 1
				this.avatarSaving = false
				this.avatarError = ''
				this.authUser = null
				this.profile = null
				this.profileLoading = false
				this.profileLoaded = false
				this.profileLoadError = ''
				this.achievementUpdating = {}
				this.achievementOrderSaving = false
				clearSession()
				this.$router.push({path: '/training/login', query: {returnTo: '/profile'}})
				return true
			},
			async loadProfile() {
				const user = readUser()
				const token = user ? readToken() : null
				const username = user?.username || ''
				const generation = this.sessionGeneration
				const requestId = ++this.profileRequestId
				if (!token || !username) {
					this.profileLoading = false
					this.profileLoaded = false
					this.profileLoadError = ''
					return
				}
				this.profileLoading = true
				this.profileLoaded = false
				this.profileLoadError = ''
				try {
					const profile = await getCurrentProfile(token)
					if (!this.isProfileRequestCurrent(requestId, token, username, generation)) return
					if (profile?.username !== username) {
						this.profileLoadError = '个人资料身份校验失败，请重试。'
						return
					}
					this.profile = profile
					this.profileLoaded = true
					writeUser(profile)
				} catch (error) {
					if (!this.isProfileRequestCurrent(requestId, token, username, generation)) return
					if (this.errorStatus(error) === 401) {
						this.expireCurrentSession(token, username, generation)
					} else {
						this.profileLoadError = this.errorMessage(error, '个人资料读取失败，请重试。')
					}
				} finally {
					if (this.isProfileRequestCurrent(requestId, token, username, generation)) this.profileLoading = false
				}
			},
			openAvatarPicker() {
				this.$refs.avatarInput?.click()
			},
			selectAvatar(event) {
				const [file] = event.target.files || []
				event.target.value = ''
				if (!file) return
				this.avatarError = ''
				this.$refs.avatarCropDialog?.open(file)
			},
			handleAvatarError(event) {
				if (event.currentTarget.getAttribute('src') === '/img/default-avatar.jpg') return
				event.currentTarget.src = '/img/default-avatar.jpg'
			},
			async saveAvatar(blob) {
				const user = readUser()
				const token = user ? readToken() : null
				if (!token || !user?.username) {
					this.avatarError = '登录状态已失效，请重新登录。'
					return
				}
				const username = user.username
				const generation = this.sessionGeneration
				const mutationId = ++this.avatarMutationId
				this.avatarSaving = true
				this.avatarError = ''
				let saved = false
				try {
					const profile = await updateCurrentAvatar(token, blob)
					if (mutationId !== this.avatarMutationId
						|| !this.isSessionCurrent(token, username, generation)
						|| profile?.username !== username) return
					const nextProfile = {
						...this.currentProfile,
						...profile,
						achievements: Array.isArray(profile.achievements) ? profile.achievements : this.achievements,
					}
					this.profile = nextProfile
					writeUser(nextProfile)
					this.authUser = readUser()
					saved = true
					this.msgSuccess('头像已更新')
				} catch (error) {
					if (mutationId !== this.avatarMutationId || !this.isSessionCurrent(token, username, generation)) return
					if (this.errorStatus(error) === 401) this.expireCurrentSession(token, username, generation)
					else this.avatarError = this.errorMessage(error, '头像上传失败，请稍后重试。')
				} finally {
					if (mutationId === this.avatarMutationId) this.avatarSaving = false
					if (saved && mutationId === this.avatarMutationId) {
						await this.$nextTick()
						this.$refs.avatarCropDialog?.close()
					}
				}
			},
			startEditing() {
				this.draft = {
					nickname: this.profileName || '',
					signature: Array.from(this.currentProfile.signature || '').slice(0, MAX_SIGNATURE_LENGTH).join(''),
					links: (this.currentProfile.links || []).map(link => ({...link, key: nextLinkKey++})),
				}
				this.saveError = ''
				this.editing = true
				this.$nextTick(() => this.$refs.nicknameInput?.focus())
			},
			cancelEditing() { this.editing = false; this.saveError = ''; this.resetPasswordDraft() },
			addLink() { if (this.draft.links.length < 8) this.draft.links.push({key: nextLinkKey++, label: '', url: ''}) },
			removeLink(index) { this.draft.links.splice(index, 1) },
			moveLink(index, offset) {
				const target = index + offset
				if (target < 0 || target >= this.draft.links.length) return
				const [link] = this.draft.links.splice(index, 1)
				this.draft.links.splice(target, 0, link)
			},
			async saveProfile() {
				const user = readUser()
				const token = user ? readToken() : null
				if (!token) return this.saveError = '登录状态已失效，请重新登录。'
				const username = user.username
				const generation = this.sessionGeneration
				const requestId = ++this.profileSaveRequestId
				this.saving = true
				this.saveError = ''
				try {
					await updateCurrentProfile(token, {nickname: this.draft.nickname, signature: this.draft.signature})
					if (requestId !== this.profileSaveRequestId || !this.isSessionCurrent(token, username, generation)) return
					const profile = await replaceCurrentProfileLinks(token, this.draft.links.map(({label, url}) => ({label, url})))
					if (requestId !== this.profileSaveRequestId || !this.isSessionCurrent(token, username, generation)) return
					if (profile?.username !== username) return this.saveError = '个人资料身份校验失败，请重试。'
					this.profile = profile
					this.profileLoaded = true
					writeUser(profile)
					this.editing = false
					this.msgSuccess('个人资料已保存')
				} catch (error) {
					if (requestId !== this.profileSaveRequestId || !this.isSessionCurrent(token, username, generation)) return
					if (this.errorStatus(error) === 401) this.expireCurrentSession(token, username, generation)
					else this.saveError = this.errorMessage(error, '保存失败，请检查填写内容。')
				} finally {
					if (requestId === this.profileSaveRequestId) this.saving = false
				}
			},
			resetPasswordDraft() {
				this.passwordDraft = {oldPassword: '', newPassword: '', confirmPassword: ''}
				this.passwordError = ''
			},
			async savePassword() {
				this.passwordError = ''
				if (!this.passwordDraft.oldPassword || !this.passwordDraft.newPassword) return this.passwordError = '请填写旧密码和新密码。'
				if (this.passwordDraft.newPassword.length < 6) return this.passwordError = '新密码至少需要 6 个字符。'
				if (this.passwordDraft.newPassword !== this.passwordDraft.confirmPassword) return this.passwordError = '两次输入的新密码必须一致。'
				const token = readToken()
				if (!token) return this.passwordError = '登录状态已失效，请重新登录。'
				this.passwordSaving = true
				try {
					await changeCurrentPassword(token, this.passwordDraft.oldPassword, this.passwordDraft.newPassword)
					this.resetPasswordDraft()
					this.msgSuccess('密码修改成功')
				} catch (error) { this.passwordError = this.errorMessage(error, '密码修改失败。') }
				finally { this.passwordSaving = false }
			},
			achievementKey(achievement) { return `${achievement.competitionId}:${achievement.awardId}` },
			async changeAchievementVisibility(achievement, visible) {
				const key = this.achievementKey(achievement)
				if (this.achievementOrderSaving || this.achievementUpdating[key] || achievement.profileVisible === visible) return
				const user = readUser()
				const token = user ? readToken() : null
				if (!token) return this.achievementError = '登录状态已失效，请重新登录。'
				const username = user.username
				const generation = this.sessionGeneration
				const mutationId = ++this.achievementMutationSequence
				this.achievementError = ''
				this.achievementUpdating = {...this.achievementUpdating, [key]: mutationId}
				try {
					await setAchievementProfileVisibility(token, achievement.competitionId, achievement.awardId, visible)
					if (!this.isSessionCurrent(token, username, generation)
						|| this.achievementUpdating[key] !== mutationId
						|| this.currentProfile.username !== username) return
					const maxProfileOrder = this.achievements.reduce((maximum, item) => {
						if (this.achievementKey(item) === key || !item.profileVisible) return maximum
						const order = Number(item.profileOrder)
						return Number.isFinite(order) ? Math.max(maximum, order) : maximum
					}, 0)
					const profile = {
						...this.currentProfile,
						achievements: this.achievements.map(item => this.achievementKey(item) === key
							? {...item, profileVisible: visible, profileOrder: visible ? maxProfileOrder + 1 : null}
							: item),
					}
					this.profile = profile
					writeUser(profile)
					this.msgSuccess(visible ? '该奖项已在公开名片展示' : '该奖项已从公开名片隐藏')
				} catch (error) {
					if (!this.isSessionCurrent(token, username, generation)
						|| this.achievementUpdating[key] !== mutationId) return
					if (this.errorStatus(error) === 401) {
						this.expireCurrentSession(token, username, generation)
					} else {
						this.achievementError = this.errorMessage(error, '奖项展示状态更新失败。')
					}
				} finally {
					if (this.achievementUpdating[key] === mutationId) {
						const next = {...this.achievementUpdating}
						delete next[key]
						this.achievementUpdating = next
					}
				}
			},
			async changeAchievementOrder(orderedAwardIds) {
				if (this.achievementOrderSaving || Object.keys(this.achievementUpdating).length > 0) return
				const visibleAchievements = this.achievements.filter(item => item.profileVisible)
				const visibleAwardIds = visibleAchievements.map(item => item.awardId)
				const normalizedOrder = Array.isArray(orderedAwardIds) ? orderedAwardIds : []
				if (normalizedOrder.length !== visibleAwardIds.length
					|| new Set(normalizedOrder).size !== normalizedOrder.length
					|| normalizedOrder.some(id => !visibleAwardIds.includes(id))) {
					this.achievementError = '公开荣誉排序已变化，请重试。'
					return
				}
				const user = readUser()
				const token = user ? readToken() : null
				if (!token || !user?.username) {
					this.achievementError = '登录状态已失效，请重新登录。'
					return
				}
				const username = user.username
				const generation = this.sessionGeneration
				const mutationId = ++this.achievementOrderMutationId
				const previousOrderByAwardId = new Map(this.achievements.map(item => [item.awardId, item.profileOrder]))
				const orderByAwardId = new Map(normalizedOrder.map((awardId, index) => [awardId, index + 1]))
				this.achievementError = ''
				this.achievementOrderSaving = true
				this.profile = {
					...this.currentProfile,
					achievements: this.achievements.map(item => item.profileVisible
						? {...item, profileOrder: orderByAwardId.get(item.awardId)}
						: item),
				}
				try {
					await setAchievementProfileOrder(token, normalizedOrder)
					if (mutationId !== this.achievementOrderMutationId
						|| !this.isSessionCurrent(token, username, generation)
						|| this.currentProfile.username !== username) return
					const profile = {
						...this.currentProfile,
						achievements: this.achievements.map(item => item.profileVisible && orderByAwardId.has(item.awardId)
							? {...item, profileOrder: orderByAwardId.get(item.awardId)}
							: item),
					}
					this.profile = profile
					writeUser(profile)
					this.msgSuccess('公开荣誉排序已保存')
				} catch (error) {
					if (mutationId !== this.achievementOrderMutationId
						|| !this.isSessionCurrent(token, username, generation)) return
					if (this.errorStatus(error) === 401) {
						this.expireCurrentSession(token, username, generation)
					} else {
						this.profile = {
							...this.currentProfile,
							achievements: this.achievements.map(item => item.profileVisible && previousOrderByAwardId.has(item.awardId)
								? {...item, profileOrder: previousOrderByAwardId.get(item.awardId)}
								: item),
						}
						this.achievementError = this.errorMessage(error, '公开荣誉排序保存失败。')
					}
				} finally {
					if (mutationId === this.achievementOrderMutationId) this.achievementOrderSaving = false
				}
			},
			errorMessage(error, fallback) { return error?.response?.data?.msg || error?.message || fallback },
			refreshUser() {
				const user = readUser()
				const token = user ? readToken() : null
				const username = user?.username || ''
				const normalizedToken = token || ''
				const identityChanged = normalizedToken !== this.sessionToken || username !== this.sessionUsername
				this.authUser = user
				if (!identityChanged) {
					if (!user) this.profile = null
					else if (!this.editing) {
						this.profile = this.profile?.username === username
							? {...this.profile, ...user, achievements: this.profile.achievements || []}
							: user
					}
					return
				}

				this.sessionToken = normalizedToken
				this.sessionUsername = username
				this.sessionGeneration += 1
				this.profileRequestId += 1
				this.profileSaveRequestId += 1
				this.avatarMutationId += 1
				this.achievementOrderMutationId += 1
				this.profile = user
				this.profileLoading = Boolean(token && user)
				this.profileLoaded = false
				this.profileLoadError = ''
				this.avatarSaving = false
				this.avatarError = ''
				this.achievementUpdating = {}
				this.achievementError = ''
				this.achievementOrderSaving = false
				this.editing = false
				this.saving = false
				this.saveError = ''
				this.passwordSaving = false
				this.passwordError = ''
				if (token && user) {
					this.loadProfile()
				}
			},
		},
	}
</script>

<style scoped>
	.profile-page { min-height: 420px; border-top: 3px solid #17324d !important; padding: 42px 48px !important; font-family: inherit; }
	.profile-heading, .editor-heading, .links-heading, .editor-actions { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; }
	.profile-heading { align-items: center; margin-bottom: 30px; }
	.profile-heading-identity { display: flex; min-width: 0; align-items: center; gap: 18px; }
	.profile-eyebrow { margin: 0 0 8px; color: #6d7a87; font-size: 11px; font-weight: 700; letter-spacing: .16em; }
	.profile-page h1 { margin: 0; color: #20252b; font-size: 34px; font-weight: 700; }
	.profile-handle { margin: 7px 0 0; color: #929ca6; font-size: 14px; letter-spacing: .06em; }
	.profile-avatar-shell { width: 82px; height: 82px; flex: 0 0 82px; }
	.profile-avatar-button { position: relative; width: 100%; height: 100%; overflow: hidden; border: 2px solid #fff; border-radius: 50%; outline: 1px solid #cbd3da; background: #eef1f4; padding: 0; box-shadow: 0 6px 18px rgba(24, 42, 57, .12); cursor: pointer; }
	.profile-avatar-button img { display: block; width: 100%; height: 100%; object-fit: cover; }
	.profile-avatar-button span { position: absolute; inset: auto 0 0; transform: translateY(100%); background: rgba(18, 38, 56, .78); color: #fff; padding: 6px 2px 7px; font-size: 10px; font-weight: 700; transition: transform 150ms ease; }
	.profile-avatar-button:hover span, .profile-avatar-button:focus-visible span { transform: translateY(0); }
	.profile-avatar-button:focus-visible { outline: 2px solid #17324d; outline-offset: 3px; }
	.visually-hidden { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
	.edit-profile-button, .links-heading button, .editor-actions button { border: 1px solid #cfd6dd; border-radius: 3px; background: #fff; color: #394651; padding: 9px 13px; font: inherit; font-size: 12px; font-weight: 600; cursor: pointer; }
	.edit-profile-button:hover, .edit-profile-button:focus-visible { border-color: #17324d; color: #17324d; }

	.profile-editor { margin-top: 28px; border: 1px solid #d8dee5; border-top: 3px solid #17324d; background: #fafbfc; padding: 26px; }
	.editor-heading h2 { margin: 0; color: #26313b; font-size: 21px; }
	.editor-heading > span { color: #7f8993; font-size: 12px; }
	.editor-fields { display: grid; gap: 18px; margin-top: 24px; }
	.editor-fields label { position: relative; display: grid; gap: 8px; }
	.editor-fields label > span, .links-heading strong { color: #36424d; font-size: 13px; font-weight: 700; }
	.editor-fields input, .editor-fields textarea, .link-row input { width: 100%; border: 1px solid #d6dce2; border-radius: 3px; background: #fff; color: #27313a; padding: 11px 12px; font: inherit; font-size: 14px; outline: none; }
	.editor-fields input:focus, .editor-fields textarea:focus, .link-row input:focus { border-color: #17324d; box-shadow: 0 0 0 2px rgba(23, 50, 77, .1); }
	.editor-fields textarea { resize: vertical; line-height: 1.6; }
	.editor-fields label small { position: absolute; right: 10px; bottom: 9px; color: #9aa3ac; font-size: 11px; }

	.links-editor { margin-top: 24px; border-top: 1px solid #dfe4e9; padding-top: 22px; }
	.password-editor { margin-top: 24px; border-top: 1px solid #dfe4e9; padding-top: 22px; }
	.password-fields { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-top: 16px; }
	.password-fields label { display: grid; gap: 8px; }
	.password-fields label > span { color: #36424d; font-size: 13px; font-weight: 700; }
	.password-fields input { width: 100%; border: 1px solid #d6dce2; border-radius: 3px; background: #fff; color: #27313a; padding: 11px 12px; font: inherit; font-size: 14px; outline: none; }
	.password-fields input:focus { border-color: #17324d; box-shadow: 0 0 0 2px rgba(23, 50, 77, .1); }
	.password-actions { display: flex; justify-content: flex-end; margin-top: 16px; }
	.password-actions button { border: 1px solid #17324d; border-radius: 3px; background: #17324d; color: #fff; padding: 9px 13px; font: inherit; font-size: 12px; font-weight: 600; cursor: pointer; }
	.password-actions button:disabled { cursor: wait; opacity: .6; }
	.links-heading > div { display: grid; gap: 4px; }
	.links-heading span { color: #89939d; font-size: 12px; }
	.links-heading button:disabled { cursor: not-allowed; opacity: .5; }
	.link-rows { display: grid; gap: 8px; margin-top: 16px; }
	.link-row { display: grid; grid-template-columns: 34px minmax(100px, .55fr) minmax(220px, 1.45fr) auto; align-items: center; gap: 8px; }
	.link-index { color: #8c96a0; font-size: 11px; font-weight: 700; letter-spacing: .08em; }
	.link-actions { display: flex; }
	.link-actions button { width: 30px; height: 36px; border: 0; background: transparent; color: #6f7a85; cursor: pointer; }
	.link-actions button:disabled { cursor: default; opacity: .25; }
	.link-actions .remove-link { color: #9c4b4b; }
	.empty-links { width: 100%; margin-top: 16px; border: 1px dashed #c9d1d9; background: #fff; color: #7c8791; padding: 18px; font: inherit; font-size: 13px; cursor: pointer; }
	.save-error { margin: 18px 0 0; border-left: 3px solid #a14b4b; background: #f8eeee; color: #7b3434; padding: 10px 12px; font-size: 13px; }
	.editor-actions { justify-content: flex-end; margin-top: 22px; }
	.editor-actions .primary { border-color: #17324d; background: #17324d; color: #fff; min-width: 106px; }
	.editor-actions button:disabled { cursor: wait; opacity: .6; }
	.profile-empty .profile-login-link { margin-top: 18px; background: #17324d; color: #fff; }

	@media (max-width: 1280px) {
		.profile-page { padding: 34px 36px !important; }
		.link-row { grid-template-columns: 28px minmax(100px, .6fr) minmax(180px, 1.4fr); }
		.link-actions { grid-column: 2 / -1; justify-content: flex-end; }
		.password-fields { grid-template-columns: 1fr; }
	}
</style>
