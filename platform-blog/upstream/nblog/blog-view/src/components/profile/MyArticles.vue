<template>
	<section class="my-articles" aria-labelledby="my-articles-title">
		<header class="articles-heading">
			<div>
				<p class="section-eyebrow">WRITING DESK</p>
				<h2 id="my-articles-title">我的文章</h2>
				<p>在这里查看已发布内容，或继续完成草稿。</p>
			</div>
		</header>

		<nav class="article-view-tabs" aria-label="文章范围">
			<button type="button" :class="{'is-active': viewMode === 'active'}" :aria-pressed="viewMode === 'active'" @click="switchMode('active')">当前文章</button>
			<button type="button" :class="{'is-active': viewMode === 'recycle'}" :aria-pressed="viewMode === 'recycle'" @click="switchMode('recycle')">回收站</button>
			<span v-if="viewMode === 'recycle'">文章固定保留 7 天，期间本人或管理员可以恢复</span>
		</nav>

		<form class="article-filters" @submit.prevent="search">
			<label><span>标题</span><input v-model="filters.title" maxlength="80" placeholder="搜索我的文章"></label>
			<label><span>分类</span><select v-model="filters.categoryId"><option value="">全部分类</option><option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option></select></label>
			<button type="submit" :disabled="loading">查询</button>
		</form>

		<p v-if="errorMessage" class="article-error" role="alert">{{ errorMessage }}</p>
		<div v-if="loading" class="article-empty">正在读取文章…</div>
		<div v-else-if="!blogs.length" class="article-empty">
			<strong>{{ viewMode === 'recycle' ? '回收站是空的' : '还没有文章' }}</strong>
			<span>{{ viewMode === 'recycle' ? '删除的文章会在这里保留 7 天。' : '从一篇题解、训练笔记或复盘开始。' }}</span>
		</div>
		<div v-else class="article-list">
			<article v-for="blog in blogs" :key="blog.id" class="article-row" :aria-labelledby="articleTitleId(blog.id)">
				<div class="article-state" :class="{'is-draft': viewMode === 'active' && !blog.published, 'is-recycle': viewMode === 'recycle'}">{{ viewMode === 'recycle' ? '回收站' : (blog.published ? '已发布' : '草稿') }}</div>
				<div class="article-copy">
					<h3 :id="articleTitleId(blog.id)">{{ blog.title }}</h3>
					<span v-if="viewMode === 'recycle'">{{ blog.category?.name || '未分类' }} · 删除于 {{ formatDate(blog.deletedAt) }} · {{ remainingRetention(blog.deletedAt) }}</span>
					<span v-else>{{ blog.category?.name || '未分类' }} · {{ formatDate(blog.updateTime || blog.createTime) }} · {{ blog.commentEnabled ? '允许评论' : '评论关闭' }}</span>
				</div>
				<div class="article-actions">
					<template v-if="viewMode === 'active'">
						<router-link v-if="blog.published" :to="`/blog/${blog.id}`" :aria-label="`查看文章“${blog.title}”`">查看文章</router-link>
						<router-link v-else :to="`/write/${blog.id}`" :aria-label="`继续编辑文章“${blog.title}”`">继续编辑</router-link>
						<button type="button" :aria-label="`删除文章“${blog.title}”`" :disabled="deletingId === blog.id" @click="remove(blog)">{{ deletingId === blog.id ? '移入中…' : '删除' }}</button>
					</template>
					<button v-else class="restore-button" type="button" :aria-label="`恢复文章“${blog.title}”`" :disabled="restoringId === blog.id" @click="restore(blog)">{{ restoringId === blog.id ? '恢复中…' : '恢复文章' }}</button>
					<button
						v-if="canManageCompetition(blog)"
						class="competition-panel-toggle"
						type="button"
						:aria-label="`${isCompetitionPanelExpanded(blog.id) ? '收起' : '管理'}文章“${blog.title}”的参赛比赛`"
						:aria-expanded="isCompetitionPanelExpanded(blog.id)"
						:aria-controls="competitionPanelId(blog.id)"
						@click="toggleCompetitionPanel(blog)"
					>
						<AppIcon name="link" :size="13" />
						<span>关联赛事</span>
						<small v-if="competitionLoaded && boundCompetitionCount(blog.id)">{{ boundCompetitionCount(blog.id) }}</small>
						<AppIcon name="chevron-down" :size="13" class="competition-toggle-chevron" />
					</button>
				</div>
				<div v-if="isCompetitionPanelExpanded(blog.id)" :id="competitionPanelId(blog.id)" class="article-competition-bindings">
					<header>
						<div>
							<strong>选择参赛赛事</strong>
							<span v-if="competitionLoaded">已关联 {{ boundCompetitionCount(blog.id) }} 项</span>
						</div>
						<button type="button" :aria-label="`刷新文章“${blog.title}”的参赛比赛`" :disabled="competitionLoading" @click="loadCompetitionBindings({force: true})">
							{{ competitionLoading ? '同步中…' : (competitionLoaded ? '刷新' : '重试') }}
						</button>
					</header>
					<p v-if="competitionError" class="competition-panel-message is-error" role="alert">{{ competitionError }}</p>
					<p v-else-if="competitionLoading" class="competition-panel-message">正在读取全部参赛比赛…</p>
					<p v-else-if="competitionLoaded && !competitions.length" class="competition-panel-message">当前账号尚未加入任何正常比赛。</p>
					<div v-if="competitionLoaded && competitions.length" class="competition-binding-list">
						<button
							v-for="competition in competitions"
							:key="competition.id"
							type="button"
							:class="{'is-bound': isArticleBound(competition, blog.id)}"
							:aria-label="`${isArticleBound(competition, blog.id) ? '解绑' : '关联'}文章“${blog.title}”与比赛“${competition.fullName}”`"
							:aria-pressed="isArticleBound(competition, blog.id) ? 'true' : 'false'"
							:disabled="Boolean(competitionBusy[competitionBindingKey(competition.id, blog.id)])"
							@click="toggleCompetitionBinding(competition, blog)"
						>
							<span class="competition-binding-mark" aria-hidden="true">{{ isArticleBound(competition, blog.id) ? '✓' : '+' }}</span>
							<strong>{{ competition.fullName }}</strong>
							<small>
								<time v-if="competitionDate(competition).isKnown" :datetime="competitionDate(competition).datetime">{{ competitionDate(competition).label }}</time>
								<span v-else class="competition-date-missing">日期待补充</span>
								· {{ isArticleBound(competition, blog.id) ? '已关联' : '未关联' }}
							</small>
						</button>
					</div>
				</div>
			</article>
		</div>

		<el-pagination v-if="totalPages > 1" :current-page="pageNum" :page-count="totalPages" layout="prev, pager, next" background @current-change="changePage"/>
	</section>
</template>

<script>
	// Author: huangbingrui.awa
	import {ElMessageBox} from 'element-plus'
	import {getCompetitions} from '@/api/competition'
	import {deleteMyBlog, getMyBlogs, getMyDeletedBlogs, restoreMyBlog} from '@/api/player-blog'
	import {bindCompetitionArticle, unbindCompetitionArticle} from '@/api/player-competition'
	import {clearSession, readToken, readUser, SESSION_CHANGE_EVENT} from '@/auth/session'
	import {competitionDatePresentation} from '@/utils/competitionDatePresentation'

	export default {
		name: 'MyArticles',
		data() {
			return {
				blogs: [], categories: [], loading: false, deletingId: null, restoringId: null, errorMessage: '',
				viewMode: 'active',
				filters: {title: '', categoryId: ''}, pageNum: 1, totalPages: 0,
				currentUsername: '', sessionToken: '', sessionInitialized: false, sessionVersion: 0,
				articleRequestId: 0,
				competitions: [], competitionLoading: false, competitionLoaded: false,
				competitionError: '', competitionBusy: {}, competitionRequestId: 0,
				competitionWriteGeneration: 0, expandedCompetitionBlogId: null,
			}
		},
		computed: {
			hasBindableArticles() {
				return this.viewMode === 'active' && this.blogs.some(blog => this.canManageCompetition(blog))
			},
		},
		mounted() {
			window.addEventListener('storage', this.refreshSession)
			window.addEventListener(SESSION_CHANGE_EVENT, this.refreshSession)
			this.refreshSession()
		},
		beforeUnmount() {
			window.removeEventListener('storage', this.refreshSession)
			window.removeEventListener(SESSION_CHANGE_EVENT, this.refreshSession)
			this.articleRequestId += 1
			this.competitionRequestId += 1
		},
		methods: {
			competitionDate(competition) {
				return competitionDatePresentation(competition)
			},
			refreshSession() {
				const token = readToken() || ''
				const username = readUser()?.username || ''
				if (this.sessionInitialized && token === this.sessionToken && username === this.currentUsername) return

				this.sessionInitialized = true
				this.sessionVersion += 1
				this.articleRequestId += 1
				this.competitionRequestId += 1
				this.competitionWriteGeneration += 1
				this.sessionToken = token
				this.currentUsername = username
				this.blogs = []
				this.categories = []
				this.pageNum = 1
				this.totalPages = 0
				this.errorMessage = ''
				this.loading = false
				this.deletingId = null
				this.restoringId = null
				this.competitions = []
				this.competitionLoading = false
				this.competitionLoaded = false
				this.competitionError = ''
				this.competitionBusy = {}
				this.expandedCompetitionBlogId = null
				if (token && username) void this.load()
			},
			isCurrentArticleRequest(requestId, sessionVersion) {
				return requestId === this.articleRequestId && sessionVersion === this.sessionVersion
			},
			isCurrentCompetitionRequest(requestId, sessionVersion) {
				return requestId === this.competitionRequestId && sessionVersion === this.sessionVersion
			},
			async loadCompetitionBindings({force = false} = {}) {
				if (!this.hasBindableArticles) return
				if (this.competitionLoaded && !force) return
				const username = this.currentUsername
				if (!this.sessionToken || !username) return this.goLogin()
				const requestId = ++this.competitionRequestId
				const sessionVersion = this.sessionVersion
				const writeGeneration = this.competitionWriteGeneration
				this.competitionLoading = true
				this.competitionError = ''
				try {
					const all = []
					let pageNum = 1
					let totalPages = 1
					do {
						const page = await getCompetitions({pageNum, pageSize: 100})
						if (!this.isCurrentCompetitionRequest(requestId, sessionVersion)) return
						all.push(...(page?.list || []))
						totalPages = Math.max(0, Number(page?.totalPages) || 0)
						pageNum += 1
					} while (pageNum <= totalPages)
					if (writeGeneration !== this.competitionWriteGeneration) return
					const byId = new Map(all.map(competition => [competition.id, competition]))
					this.competitions = [...byId.values()].filter(competition => competition?.participants?.some(participant => participant.username === username))
					this.competitionLoaded = true
				} catch (error) {
					if (this.isCurrentCompetitionRequest(requestId, sessionVersion)) {
						this.competitionError = error?.response?.data?.msg || error?.message || '参赛比赛读取失败，请重试。'
					}
				} finally {
					if (this.isCurrentCompetitionRequest(requestId, sessionVersion)) this.competitionLoading = false
				}
			},
			async load() {
				const token = this.sessionToken
				if (!token) {
					this.articleRequestId += 1
					this.loading = false
					return
				}
				const requestId = ++this.articleRequestId
				const sessionVersion = this.sessionVersion
				const mode = this.viewMode
				const query = {
					title: this.filters.title.trim(),
					...(this.filters.categoryId ? {categoryId: Number(this.filters.categoryId)} : {}),
					pageNum: this.pageNum,
					pageSize: 6,
				}
				this.loading = true
				this.errorMessage = ''
				try {
					const list = mode === 'recycle' ? getMyDeletedBlogs : getMyBlogs
					const data = await list(token, query)
					if (!this.isCurrentArticleRequest(requestId, sessionVersion)) return
					this.blogs = data.blogs?.list || []
					this.totalPages = data.blogs?.pages || 0
					this.categories = data.categories || []
				} catch (error) {
					if (this.isCurrentArticleRequest(requestId, sessionVersion)) this.handleError(error, '文章列表读取失败')
				} finally {
					if (this.isCurrentArticleRequest(requestId, sessionVersion)) this.loading = false
				}
			},
			search() { this.pageNum = 1; this.load() },
			switchMode(mode) { if (this.viewMode === mode) return; this.viewMode = mode; this.pageNum = 1; this.expandedCompetitionBlogId = null; this.load() },
			changePage(page) { this.pageNum = page; this.load() },
			async remove(blog) {
				try {
					await ElMessageBox.confirm(`「${blog.title}」将移入回收站并保留 7 天，期间你或管理员可以恢复，关联评论、标签和图片也会保留。`, '移入回收站', {confirmButtonText: '移入回收站', cancelButtonText: '取消', type: 'warning'})
				} catch (_) { return }
				const token = readToken()
				if (!token) return this.goLogin()
				this.deletingId = blog.id
				try { await deleteMyBlog(token, blog.id); this.msgSuccess('文章已移入回收站'); await this.load() }
				catch (error) { this.handleError(error, '文章移入回收站失败') }
				finally { this.deletingId = null }
			},
			async restore(blog) {
				const token = readToken()
				if (!token) return this.goLogin()
				this.restoringId = blog.id
				try { await restoreMyBlog(token, blog.id); this.msgSuccess('文章已恢复'); await this.load() }
				catch (error) { this.handleError(error, '文章恢复失败') }
				finally { this.restoringId = null }
			},
			canManageCompetition(blog) {
				return this.viewMode === 'active' && blog?.published === true && blog?.internal !== true
			},
			articleTitleId(blogId) { return `my-article-title-${blogId}` },
			competitionPanelId(blogId) { return `my-article-competitions-${blogId}` },
			isCompetitionPanelExpanded(blogId) { return this.expandedCompetitionBlogId === blogId },
			async toggleCompetitionPanel(blog) {
				if (!this.canManageCompetition(blog)) return
				if (this.isCompetitionPanelExpanded(blog.id)) {
					this.expandedCompetitionBlogId = null
					return
				}
				this.expandedCompetitionBlogId = blog.id
				await this.loadCompetitionBindings()
			},
			currentParticipant(competition) {
				return competition?.participants?.find(participant => participant.username === this.currentUsername) || null
			},
			isArticleBound(competition, blogId) {
				return Boolean(this.currentParticipant(competition)?.articles?.some(article => article.id === blogId))
			},
			boundCompetitionCount(blogId) {
				return this.competitions.filter(competition => this.isArticleBound(competition, blogId)).length
			},
			competitionBindingKey(competitionId, blogId) { return `${competitionId}:${blogId}` },
			async toggleCompetitionBinding(competition, blog) {
				if (!this.canManageCompetition(blog)) return
				const key = this.competitionBindingKey(competition.id, blog.id)
				if (this.competitionBusy[key]) return
				const token = this.sessionToken || readToken()
				if (!token) return this.goLogin()
				const sessionVersion = this.sessionVersion
				const username = this.currentUsername
				const bound = this.isArticleBound(competition, blog.id)
				this.competitionError = ''
				this.competitionBusy = {...this.competitionBusy, [key]: true}
				try {
					if (bound) await unbindCompetitionArticle(token, competition.id, blog.id)
					else await bindCompetitionArticle(token, competition.id, blog.id)
					if (sessionVersion !== this.sessionVersion || username !== this.currentUsername) return
					this.competitionWriteGeneration += 1
					this.updateLocalCompetitionArticle(competition.id, blog, !bound)
					this.msgSuccess(bound ? '参赛文章已解绑' : '参赛文章已关联')
				} catch (error) {
					if (sessionVersion !== this.sessionVersion || username !== this.currentUsername) return
					if (this.isUnauthorized(error)) {
						clearSession()
						this.goLogin()
					} else {
						this.competitionError = error?.response?.data?.msg || error?.message || (bound ? '参赛文章解绑失败。' : '参赛文章关联失败。')
					}
				} finally {
					if (sessionVersion !== this.sessionVersion || username !== this.currentUsername) return
					const next = {...this.competitionBusy}
					delete next[key]
					this.competitionBusy = next
				}
			},
			updateLocalCompetitionArticle(competitionId, blog, bound) {
				this.competitions = this.competitions.map(competition => {
					if (competition.id !== competitionId) return competition
					return {
						...competition,
						participants: (competition.participants || []).map(participant => {
							if (participant.username !== this.currentUsername) return participant
							const articles = (participant.articles || []).filter(article => article.id !== blog.id)
							return {...participant, articles: bound ? [...articles, {id: blog.id, title: blog.title}] : articles}
						}),
					}
				})
			},
			goLogin() { this.$router.push({path: '/training/login', query: {returnTo: '/profile'}}) },
			isUnauthorized(error) { return error?.response?.status === 401 || Number(error?.code) === 401 },
			handleError(error, fallback) {
				if (this.isUnauthorized(error)) { clearSession(); this.goLogin(); return }
				this.errorMessage = error?.response?.data?.msg || error?.message || fallback
			},
			formatDate(value) { return value ? this.$filters.dateFormat(value, 'YYYY-MM-DD HH:mm') : '时间未知' },
			remainingRetention(value) {
				if (!value) return '保留期未知'
				const remaining = new Date(value).getTime() + 7 * 24 * 60 * 60 * 1000 - Date.now()
				if (remaining <= 0) return '等待清理'
				const hours = Math.ceil(remaining / (60 * 60 * 1000))
				return hours > 24 ? `剩余 ${Math.ceil(hours / 24)} 天` : `剩余 ${hours} 小时`
			},
		},
	}
</script>

<style scoped>
	.my-articles { margin-top: 34px; border-top: 1px solid #dfe4e9; padding-top: 30px; }
	.articles-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 24px; }
	.section-eyebrow { margin: 0 0 7px; color: #6d7a87; font-size: 11px; font-weight: 700; letter-spacing: .16em; }
	.articles-heading h2 { margin: 0; color: #202b35; font-size: 24px; }
	.articles-heading p:last-child { margin: 8px 0 0; color: #7a8691; font-size: 13px; }
	.article-view-tabs { display: flex; align-items: center; gap: 4px; margin-top: 22px; border-bottom: 1px solid #dfe4e9; }
	.article-view-tabs button { border: 0; border-bottom: 2px solid transparent; background: transparent; color: #73808c; padding: 10px 13px; font: inherit; font-size: 12px; font-weight: 800; cursor: pointer; }
	.article-view-tabs button.is-active { border-bottom-color: #17324d; color: #17324d; }
	.article-view-tabs span { margin-left: auto; color: #8a6d45; font-size: 11px; }
	.article-filters { display: grid; grid-template-columns: minmax(0, 1fr) 180px auto; align-items: end; gap: 10px; margin-top: 22px; border: 1px solid #dfe4e9; background: #f7f9fa; padding: 14px; }
	.article-filters label { display: grid; gap: 6px; color: #596671; font-size: 11px; font-weight: 700; }
	.article-filters input, .article-filters select { height: 38px; border: 1px solid #d3dae1; background: #fff; padding: 0 10px; color: #26323c; font: inherit; }
	.article-filters button { height: 38px; border: 1px solid #17324d; background: #fff; color: #17324d; padding: 0 16px; font-weight: 700; cursor: pointer; }
	.article-list { margin-top: 14px; border: 1px solid #dfe4e9; border-bottom: 0; }
	.article-row { display: grid; grid-template-columns: 72px minmax(0, 1fr) auto; align-items: center; gap: 14px; min-height: 82px; border-bottom: 1px solid #dfe4e9; padding: 15px; }
	.article-state { border-left: 3px solid #2f6c57; color: #2f6c57; padding: 5px 0 5px 8px; font-size: 11px; font-weight: 800; }
	.article-state.is-draft { border-color: #9a6b2f; color: #8a5c21; }
	.article-state.is-recycle { border-color: #7c5960; color: #7c5960; }
	.article-copy { display: grid; min-width: 0; gap: 7px; }
	.article-copy h3 { margin: 0; overflow: hidden; color: #26313a; font-size: 15px; text-overflow: ellipsis; white-space: nowrap; }
	.article-copy span { color: #89939d; font-size: 11px; }
	.article-actions { display: flex; align-items: center; gap: 6px; }
	.article-actions a, .article-actions button { border: 0; background: transparent; color: #17324d; padding: 7px; font: inherit; font-size: 12px; font-weight: 700; cursor: pointer; }
	.article-actions button { color: #9a4747; }
	.article-actions .restore-button { color: #2f6c57; }
	.article-empty { display: grid; place-items: center; gap: 7px; min-height: 150px; margin-top: 14px; border: 1px dashed #cad2d9; color: #7e8994; font-size: 13px; }
	.article-empty strong { color: #384550; font-size: 16px; }
	.article-error { margin: 14px 0 0; border-left: 3px solid #a14b4b; background: #f8eeee; color: #7b3434; padding: 10px 12px; }
	.article-actions .competition-panel-toggle { display: inline-flex; align-items: center; gap: 5px; margin-left: 2px; border-left: 1px solid var(--color-border); color: var(--color-action); padding-left: 11px; }
	.competition-panel-toggle small { display: grid; min-width: 17px; height: 17px; place-items: center; border-radius: 9px; background: var(--color-action); color: var(--color-surface); font-size: 9px; }
	.competition-toggle-chevron { transition: transform var(--duration-fast); }
	.competition-panel-toggle[aria-expanded="true"] .competition-toggle-chevron { transform: rotate(180deg); }
	.article-competition-bindings { display: grid; grid-column: 2 / -1; gap: 9px; border-top: 1px solid var(--color-border); padding-top: 12px; }
	.article-competition-bindings > header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
	.article-competition-bindings > header > div { display: flex; align-items: baseline; gap: 9px; }
	.article-competition-bindings > header strong { color: var(--color-text); font-size: 11px; }
	.article-competition-bindings > header span { color: var(--color-text-faint); font-size: 10px; }
	.article-competition-bindings > header button { border: 0; background: transparent; padding: 4px 6px; color: var(--color-action); font: inherit; font-size: 10px; font-weight: 700; cursor: pointer; }
	.article-competition-bindings > header button:disabled { cursor: wait; opacity: .58; }
	.competition-panel-message { margin: 0; color: var(--color-text-muted); font-size: 11px; }
	.competition-panel-message.is-error { color: var(--color-danger); }
	.competition-binding-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); border-top: 1px solid var(--color-border); }
	.competition-binding-list > button { display: grid; grid-template-columns: 20px minmax(0, 1fr); align-items: center; column-gap: 8px; row-gap: 3px; min-width: 0; border: 0; border-bottom: 1px solid var(--color-border); background: transparent; padding: 9px 7px; color: var(--color-text-muted); font: inherit; text-align: left; cursor: pointer; }
	.competition-binding-list > button:nth-child(odd) { border-right: 1px solid var(--color-border); }
	.competition-binding-list > button.is-bound { background: #eaf4ef; color: #126244; }
	.competition-binding-list > button:focus-visible { position: relative; outline: 2px solid var(--color-focus-ring); outline-offset: -2px; }
	.competition-binding-list > button:disabled { cursor: wait; opacity: .58; }
	.competition-binding-list strong, .competition-binding-list small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
	.competition-binding-list strong { font-size: 11px; }
	.competition-binding-list small { font-size: 10px; }
	.competition-binding-mark { display: grid; grid-row: 1 / span 2; width: 18px; height: 18px; place-items: center; border: 1px solid currentColor; border-radius: 50%; font-size: 11px; font-weight: 800; }
	.el-pagination { justify-content: center; margin-top: 18px; }
</style>
