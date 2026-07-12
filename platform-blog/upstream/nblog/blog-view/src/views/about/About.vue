<template>
	<section class="profile-page ui segment">
		<template v-if="authUser">
			<header class="profile-heading">
				<div>
						<p class="profile-eyebrow">MY HOMEPAGE</p>
					<h1>{{ profileName }}</h1>
					<p class="profile-handle">@{{ authUser.username }}</p>
				</div>
				<button v-if="!editing" type="button" class="edit-profile-button" @click="startEditing">
					<i class="edit outline icon"></i>编辑资料
				</button>
			</header>

			<div class="oj-grid" aria-label="OJ 账号">
				<a class="oj-card codeforces" :href="ojProfileUrl('CODEFORCES')" :class="{'is-empty': !ojHandles.CODEFORCES}" target="_blank" rel="noopener">
					<span class="oj-abbr">CF</span>
					<span class="oj-copy"><strong>Codeforces</strong><small>{{ ojHandleText('CODEFORCES') }}</small></span>
				</a>
				<a class="oj-card atcoder" :href="ojProfileUrl('ATCODER')" :class="{'is-empty': !ojHandles.ATCODER}" target="_blank" rel="noopener">
					<span class="oj-abbr">AT</span>
					<span class="oj-copy"><strong>AtCoder</strong><small>{{ ojHandleText('ATCODER') }}</small></span>
				</a>
			</div>

			<div class="identity-grid">
				<div><span>用户名</span><strong>{{ authUser.username }}</strong></div>
				<div><span>昵称</span><strong>{{ profileName }}</strong></div>
			</div>

			<section class="signature-preview">
				<span>个性签名</span>
				<p :class="{'is-empty': !currentProfile.signature}">{{ currentProfile.signature || '还没有个性签名' }}</p>
			</section>

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
						<textarea v-model="draft.signature" maxlength="160" rows="4" placeholder="写一句想留在个人卡片上的话"></textarea>
						<small>{{ draft.signature.length }}/160</small>
					</label>
				</div>

				<div class="links-editor">
					<div class="links-heading">
						<div><strong>友情链接</strong><span>最多 8 条，仅支持 HTTP 或 HTTPS 地址</span></div>
						<button type="button" :disabled="draft.links.length >= 8" @click="addLink"><i class="plus icon"></i>添加链接</button>
					</div>
					<div v-if="draft.links.length" class="link-rows">
						<div v-for="(link, index) in draft.links" :key="link.key" class="link-row">
							<span class="link-index">{{ String(index + 1).padStart(2, '0') }}</span>
							<input v-model="link.label" maxlength="30" required :aria-label="`第 ${index + 1} 条链接名称`" placeholder="名称，例如 GitHub">
							<input v-model="link.url" maxlength="2048" required type="url" :aria-label="`第 ${index + 1} 条链接地址`" placeholder="https://example.com">
							<div class="link-actions">
								<button type="button" :disabled="index === 0" aria-label="上移" @click="moveLink(index, -1)"><i class="arrow up icon"></i></button>
								<button type="button" :disabled="index === draft.links.length - 1" aria-label="下移" @click="moveLink(index, 1)"><i class="arrow down icon"></i></button>
								<button type="button" class="remove-link" aria-label="删除" @click="removeLink(index)"><i class="trash alternate outline icon"></i></button>
							</div>
						</div>
					</div>
					<button v-else type="button" class="empty-links" @click="addLink"><i class="linkify icon"></i>添加第一条友情链接</button>
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
				<MyArticles/>
			</template>
			<div v-else class="profile-empty">
				<p class="profile-eyebrow">MY HOMEPAGE</p>
				<h1>登录后查看我的主页</h1>
				<p>你的个人资料、OJ 账号和已发布内容会集中在这里。</p>
			<router-link to="/training/login?returnTo=/about" class="ui button">登录训练中心</router-link>
		</div>
	</section>
</template>

<script>
		// Author: huangbingrui.awa
		import {changeCurrentPassword, getCurrentOjHandles, getCurrentProfile, replaceCurrentProfileLinks, updateCurrentProfile} from '@/api/profile'
		import {readToken, readUser, SESSION_CHANGE_EVENT, writeUser} from '@/auth/session'
		import MyArticles from '@/components/profile/MyArticles.vue'

	let nextLinkKey = 1

		export default {
			name: 'About',
			components: {MyArticles},
		data() {
			const authUser = readUser()
			return {
				authUser,
				profile: authUser,
				ojHandles: {},
				ojHandlesLoading: Boolean(authUser),
				editing: false,
				saving: false,
				saveError: '',
				draft: {nickname: '', signature: '', links: []},
				passwordDraft: {oldPassword: '', newPassword: '', confirmPassword: ''},
				passwordSaving: false,
				passwordError: '',
			}
		},
		computed: {
			currentProfile() { return this.profile || this.authUser || {} },
			profileName() { return this.currentProfile.nickname || this.authUser?.username },
		},
		mounted() {
			window.addEventListener('storage', this.refreshUser)
			window.addEventListener(SESSION_CHANGE_EVENT, this.refreshUser)
			this.loadProfile()
			this.loadOjHandles()
		},
		beforeUnmount() {
			window.removeEventListener('storage', this.refreshUser)
			window.removeEventListener(SESSION_CHANGE_EVENT, this.refreshUser)
		},
		methods: {
			async loadProfile() {
				const token = readToken()
				if (!token) return
				try {
					this.profile = await getCurrentProfile(token)
					writeUser(this.profile)
				} catch (error) { this.saveError = this.errorMessage(error, '个人资料读取失败') }
			},
			async loadOjHandles() {
				const token = readToken()
				if (!token) return
				try { this.ojHandles = await getCurrentOjHandles(token) } catch { this.ojHandles = {} }
				finally { this.ojHandlesLoading = false }
			},
			startEditing() {
				this.draft = {
					nickname: this.profileName || '',
					signature: this.currentProfile.signature || '',
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
				const token = readToken()
				if (!token) return this.saveError = '登录状态已失效，请重新登录。'
				this.saving = true
				this.saveError = ''
				try {
					await updateCurrentProfile(token, {nickname: this.draft.nickname, signature: this.draft.signature})
					this.profile = await replaceCurrentProfileLinks(token, this.draft.links.map(({label, url}) => ({label, url})))
					writeUser(this.profile)
					this.editing = false
					this.msgSuccess('个人资料已保存')
				} catch (error) { this.saveError = this.errorMessage(error, '保存失败，请检查填写内容。') }
				finally { this.saving = false }
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
			errorMessage(error, fallback) { return error?.response?.data?.msg || error?.message || fallback },
			ojHandleText(ojName) { return this.ojHandlesLoading ? '读取中…' : (this.ojHandles[ojName] || '未绑定') },
			ojProfileUrl(ojName) {
				const handle = this.ojHandles[ojName]
				if (!handle) return undefined
				return ojName === 'CODEFORCES' ? `https://codeforces.com/profile/${encodeURIComponent(handle)}` : `https://atcoder.jp/users/${encodeURIComponent(handle)}`
			},
			refreshUser() {
				this.authUser = readUser()
				if (!this.editing && this.authUser) this.profile = this.authUser
			},
		},
	}
</script>

<style scoped>
	.profile-page { min-height: 420px; border-top: 3px solid #17324d !important; padding: 42px 48px !important; font-family: inherit; }
	.profile-heading, .editor-heading, .links-heading, .editor-actions { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; }
	.profile-eyebrow { margin: 0 0 8px; color: #6d7a87; font-size: 11px; font-weight: 700; letter-spacing: .16em; }
	.profile-page h1 { margin: 0; color: #20252b; font-size: 34px; font-weight: 700; }
	.profile-handle { margin: 7px 0 30px; color: #929ca6; font-size: 14px; letter-spacing: .06em; }
	.edit-profile-button, .links-heading button, .editor-actions button { border: 1px solid #cfd6dd; border-radius: 3px; background: #fff; color: #394651; padding: 9px 13px; font: inherit; font-size: 12px; font-weight: 600; cursor: pointer; }
	.edit-profile-button:hover, .edit-profile-button:focus-visible { border-color: #17324d; color: #17324d; }

	.oj-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
	.oj-card { display: flex; min-height: 88px; align-items: center; gap: 14px; border: 1px solid #dce2e8; border-left: 3px solid #17324d; background: #f5f7f9; color: #26313b; padding: 16px 18px; text-decoration: none; transition: 150ms ease; }
	.oj-card:not(.is-empty):hover, .oj-card:not(.is-empty):focus-visible { border-color: #17324d; background: #eef2f5; transform: translateY(-1px); }
	.oj-card.is-empty { cursor: default; }
	.oj-abbr { display: grid; width: 42px; height: 42px; flex: 0 0 42px; place-items: center; border-radius: 3px; background: #17324d; color: #fff; font-size: 14px; font-weight: 800; letter-spacing: .05em; }
	.atcoder .oj-abbr { background: #30343a; }
	.oj-copy { display: grid; min-width: 0; gap: 5px; }
	.oj-copy strong, .oj-copy small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
	.oj-copy strong { font-size: 15px; }
	.oj-copy small { color: #7d8894; font-size: 12px; }

	.identity-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin-top: 28px; }
	.identity-grid > div { border: 1px solid #e0e5ea; padding: 16px; }
	.identity-grid span, .signature-preview > span { color: #9099a3; font-size: 12px; }
	.identity-grid strong { display: block; margin-top: 7px; color: #27313a; font-size: 15px; }
	.signature-preview { margin-top: 12px; border: 1px solid #e0e5ea; padding: 16px; }
	.signature-preview p { min-height: 42px; margin: 8px 0 0; color: #4c5862; font-size: 14px; line-height: 1.7; white-space: pre-wrap; }
	.signature-preview p.is-empty { color: #9aa3ac; }

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
	.profile-empty .ui.button { margin-top: 18px; background: #17324d; color: #fff; }

	@media (max-width: 1280px) {
		.profile-page { padding: 34px 36px !important; }
		.link-row { grid-template-columns: 28px minmax(100px, .6fr) minmax(180px, 1.4fr); }
		.link-actions { grid-column: 2 / -1; justify-content: flex-end; }
		.password-fields { grid-template-columns: 1fr; }
	}
</style>
