<template>
		<div class="site" :class="{'is-home': $route.name === 'home', 'is-article': $route.name === 'blog', 'is-editor': $route.name === 'write', 'is-catalog': catalogRoute, 'is-competition': competitionRoute}">
		<!--顶部导航-->
		<Nav/>
		<!--首页大图：桌面整屏显示，移动端使用紧凑高度-->
		<Header v-if="$route.name==='home'"/>
		<FeaturedImageMarquee v-if="$route.name==='home'"/>
		<router-view v-if="$route.name==='training'"/>

		<div v-else class="main">
			<div class="m-padded-tb-big main-stage">
				<div class="site-container">
					<div class="main-grid" :class="{'has-article-sidebar': $route.name === 'blog'}">
						<!--左侧-->
						<div v-if="$route.name === 'blog'" class="m-mobile-hide sidebar-column article-sidebar-column">
							<aside class="sticky-sidebar article-sidebar" aria-label="作者资料与文章导航">
								<Introduction :author-username="articleAuthor?.username || ''" :author-summary="articleAuthor" :class="{'m-display-none':focusMode}">
									<template #article-actions>
										<div v-if="canDownloadArticle || canEditArticle" class="article-author-actions" aria-label="文章操作">
											<button v-if="canDownloadArticle" type="button" :disabled="articleDownloading" @click="downloadArticle">
												<AppIcon :name="articleDownloading ? 'loader' : 'download'" :spin="articleDownloading" />
												<span>{{ articleDownloading ? '打包中' : '下载文章' }}</span>
											</button>
											<router-link v-if="canEditArticle" :to="`/write/${articleAuthor.articleId}`">
												<AppIcon name="edit" /><span>编辑文章</span>
											</router-link>
										</div>
									</template>
								</Introduction>
								<Tocbot/>
								<a class="article-comment-shortcut" href="#article-comments" @click.prevent="scrollToComments">
									<strong>评论</strong>
									<AppIcon name="chevron-down" />
									<em>{{ allComment }}</em>
								</a>
							</aside>
						</div>
						<!--右侧阅读区-->
						<div ref="mainContent" class="main-content-column" :class="{'article-reading-pane': $route.name === 'blog'}">
							<FeaturedBlog v-if="$route.name === 'home'" :featuredGroups="featuredGroups" :class="{'m-display-none':focusMode}"/>
							<router-view v-if="$route.name !== 'home'" v-slot="{ Component }">
								<keep-alive include="Home">
									<component :is="Component" v-bind="taxonomyPageProps" @article-author-change="articleAuthor = $event"/>
								</keep-alive>
							</router-view>
							<Footer v-if="$route.name === 'blog'"/>
						</div>
					</div>
				</div>
			</div>
		</div>

		<!--回到顶部-->
		<el-backtop v-if="$route.name!=='training'" :target="$route.name === 'blog' ? '.article-reading-pane' : undefined" style="box-shadow: none;background: none;z-index: 9999;">
			<img src="/img/paper-plane.png" style="width: 40px;height: 40px;">
		</el-backtop>
		<!--底部footer-->
		<Footer v-if="showFooter && $route.name !== 'blog'"/>
	</div>
</template>

<script>
	import {getSite} from '@/api/index'
	import {downloadBlog} from '@/api/blog'
	import Nav from "@/components/index/Nav";
	import Header from "@/components/index/Header";
	import FeaturedImageMarquee from "@/components/index/FeaturedImageMarquee";
	import Footer from "@/components/index/Footer";
	import Introduction from "@/components/sidebar/Introduction";
	import FeaturedBlog from "@/components/sidebar/FeaturedBlog";
	import Tocbot from "@/components/sidebar/Tocbot";
	import {mapState} from 'vuex'
	import getPageTitle from '@/util/get-page-title'
	import {clearSession, readToken, readUser, SESSION_CHANGE_EVENT} from '@/auth/session'
	import {articleDownloadFilename, retryAfterSeconds, saveArticleDownload} from '@/util/articleDownload'
	import {SAVE_CLIENT_SIZE, SAVE_INTRODUCTION, SAVE_SITE_INFO} from "@/store/mutations-types";

	export default {
		name: "Index",
		components: {Header, FeaturedImageMarquee, Tocbot, FeaturedBlog, Nav, Footer, Introduction},
		data() {
			return {
				siteInfo: {
					blogName: '',
					webTitleSuffix: ''
				},
				categoryList: [],
				tagList: [],
				featuredGroups: [],
				articleAuthor: null,
				authUser: readUser(),
				articleDownloading: false,
			}
		},
		computed: {
			...mapState(['focusMode', 'allComment']),
			catalogRoute() {
				return ['articles', 'category', 'tag'].includes(this.$route.name)
			},
			competitionRoute() {
				return ['competitions', 'competition-detail'].includes(this.$route.name)
			},
			taxonomyPageProps() {
				if (!this.catalogRoute) return {}
				return {categoryList: this.categoryList, tagList: this.tagList}
			},
			canDownloadArticle() {
				return Boolean(this.authUser && this.articleAuthor?.articleId)
			},
			canEditArticle() {
				return Boolean(
					this.authUser?.username
					&& this.articleAuthor?.username
					&& this.authUser.username === this.articleAuthor.username
				)
			},
			showFooter() {
				if (this.$route.name !== 'training') return true
				const value = this.$route.params.trainingPath
				const trainingPath = Array.isArray(value) ? value.join('/') : (value || '')
				return trainingPath !== 'login' && !trainingPath.startsWith('admin')
			}
		},
		watch: {
			//路由改变时，页面滚动至顶部
			'$route.path'() {
				if (this.$route.name === 'blog') {
					this.$nextTick(() => this.$refs.mainContent?.scrollTo({top: 0, behavior: 'auto'}))
				} else {
					this.scrollToTop()
				}
				if (this.$route.name !== 'blog') this.articleAuthor = null
			}
		},
		created() {
			this.getSite()
		},
	mounted() {
			window.addEventListener('storage', this.refreshVisibleContent)
			window.addEventListener(SESSION_CHANGE_EVENT, this.refreshVisibleContent)
			//保存可视窗口大小
			this.$store.commit(SAVE_CLIENT_SIZE, {clientHeight: document.body.clientHeight, clientWidth: document.body.clientWidth})
			window.onresize = () => {
				this.$store.commit(SAVE_CLIENT_SIZE, {clientHeight: document.body.clientHeight, clientWidth: document.body.clientWidth})
			}
		},
		beforeUnmount() {
			window.removeEventListener('storage', this.refreshVisibleContent)
			window.removeEventListener(SESSION_CHANGE_EVENT, this.refreshVisibleContent)
		},
		methods: {
			scrollToComments() {
				const target = document.getElementById('article-comments')
				const pane = this.$refs.mainContent
				if (!target || !pane) return
				const top = pane.scrollTop + target.getBoundingClientRect().top - pane.getBoundingClientRect().top - 24
				pane.scrollTo({top, behavior: 'smooth'})
			},
			refreshVisibleContent(event) {
				if (!event || event.type === SESSION_CHANGE_EVENT || event.key === null || event.key === 'custacm.accessToken' || event.key === 'custacm.user') {
					this.authUser = readUser()
					this.getSite()
				}
			},
			async downloadArticle() {
				if (this.articleDownloading) return
				const token = readToken()
				if (!token) {
					this.authUser = null
					this.$router.push({path: '/training/login', query: {returnTo: this.$route.fullPath}})
					return
				}
				const articleId = Number(this.articleAuthor?.articleId || this.$route.params.id)
				this.articleDownloading = true
				try {
					const blob = await downloadBlog(token, articleId)
					saveArticleDownload(blob, articleDownloadFilename(this.articleAuthor?.articleTitle, articleId))
					this.msgSuccess('文章下载已开始')
				} catch (error) {
					const status = error?.response?.status
					if (status === 401) {
						clearSession()
						this.authUser = null
						this.$router.push({path: '/training/login', query: {returnTo: this.$route.fullPath}})
						return
					}
					if (status === 429) return this.msgError(`下载过于频繁，请 ${retryAfterSeconds(error)} 秒后再试`)
					if (status === 503) return this.msgError('下载服务暂时不可用，请稍后重试')
					this.msgError('文章下载失败，请稍后重试')
				} finally {
					this.articleDownloading = false
				}
			},
			getSite() {
				getSite().then(res => {
					if (res.code === 200) {
						this.siteInfo = res.data.siteInfo
						this.categoryList = res.data.categoryList
						this.tagList = res.data.tagList
						this.featuredGroups = res.data.featuredGroups || []
						this.$store.commit(SAVE_SITE_INFO, this.siteInfo)
						this.$store.commit(SAVE_INTRODUCTION, res.data.introduction)
						document.title = getPageTitle(this.$route.meta.title)
					}
				})
			}
		}
	}
</script>

<style scoped>
	.site {
		display: flex;
		width: 100%;
		max-width: 100%;
		min-height: 100vh; /* 没有元素时，也把页面撑开至100% */
		flex-direction: column;
		overflow-x: clip;
	}

	.site.is-home {
		--home-canvas: var(--anthropic-ivory-light);
		--home-surface: #f7f7f5;
		--home-surface-hover: #f1f1ef;
		--home-media: #eeecea;
		--home-border: #e9e9e7;
		--home-border-strong: #d3d3d1;
		--home-text: #050505;
		--home-text-soft: #37352f;
		--home-muted: #686762;
		--home-action: #2383e2;
		background: var(--home-canvas);
		color: var(--home-text);
	}

	.site.is-catalog,
	.site.is-catalog .main {
		background: var(--anthropic-ivory-light);
		color: var(--anthropic-slate-dark);
	}

	.site.is-catalog .main .site-container {
		width: 100% !important;
		max-width: none !important;
	}

	.site.is-catalog :deep(.site-nav) {
		border-bottom-color: var(--glass-border) !important;
		background: var(--glass-background) !important;
		-webkit-backdrop-filter: var(--glass-filter);
		backdrop-filter: var(--glass-filter);
	}

	.site.is-catalog :deep(.site-nav .nav-item),
	.site.is-catalog :deep(.site-nav .nav-training-trigger),
	.site.is-catalog :deep(.site-nav .nav-auth-trigger) {
		color: var(--color-text) !important;
	}

	.site.is-catalog :deep(.site-nav .active.nav-item::after) {
		background: var(--color-action) !important;
	}

	.site.is-home .main {
		background: var(--home-canvas);
	}

	.site.is-competition,
	.site.is-competition .main {
		background: var(--anthropic-ivory-light);
	}

	.site.is-article {
		height: 100vh;
		min-height: 0;
		overflow: hidden;
		background: var(--anthropic-ivory-light);
	}

	.site.is-editor,
	.site.is-editor .main {
		background: var(--anthropic-ivory-light);
		color: var(--anthropic-slate-dark);
	}

	.main {
		--sidebar-sticky-top: 64px;
		width: 100%;
		max-width: 100%;
		margin-top: 40px;
		flex: 1;
	}

	.main .site-container {
		box-sizing: border-box;
		width: min(1400px, 100%) !important;
		max-width: 100% !important;
		margin-left: auto !important;
		margin-right: auto !important;
		padding-inline: 2rem;
	}

	.site.is-article .main {
		flex: 0 0 calc(100vh - 51px);
		height: calc(100vh - 51px);
		min-height: 0;
		margin-top: 51px;
		overflow: hidden;
		background: var(--anthropic-ivory-light);
	}

	.site.is-article .main-stage,
	.site.is-article .site-container,
	.site.is-article .main-grid {
		height: 100%;
		min-height: 0;
	}

	.site.is-article .main-grid {
		grid-template-rows: minmax(0, 1fr);
		overflow: hidden;
	}

	.site.is-article .main-stage {
		padding: 0 !important;
	}

	.site.is-article .site-container {
		width: 100% !important;
		max-width: none !important;
		padding: 0;
	}

	.site.is-article :deep(.site-nav .nav-container) {
		max-width: none !important;
		padding-inline: 16px;
	}

	.main .main-grid > :is(.sidebar-column, .main-content-column) {
		min-width: 0;
	}

	.main-grid {
		display: grid;
		grid-template-columns: minmax(0, 1fr);
		align-items: stretch;
		gap: 2rem;
	}

	.main-grid.has-article-sidebar {
		grid-template-columns: clamp(272px, 20vw, 320px) minmax(0, 1fr);
		gap: 0;
	}

	.sidebar-column {
		align-self: stretch !important;
	}

	.sticky-sidebar {
		position: sticky;
		top: var(--sidebar-sticky-top);
		box-sizing: border-box;
		max-height: calc(100vh - var(--sidebar-sticky-top) - 16px);
		overflow-x: hidden;
		overflow-y: auto;
		scrollbar-color: rgba(86, 96, 106, .56) transparent;
		scrollbar-width: thin;
	}

	.sticky-sidebar::-webkit-scrollbar {
		width: 5px;
	}

	.sticky-sidebar::-webkit-scrollbar-track {
		background: transparent;
	}

	.sticky-sidebar::-webkit-scrollbar-thumb {
		border-radius: 999px;
		background: rgba(86, 96, 106, .56);
	}

	.article-sidebar-column {
		height: 100%;
		min-height: 0;
		overflow: hidden;
		border-right: 1px solid var(--anthropic-cloud-light);
		background: color-mix(in srgb, var(--anthropic-ivory-medium) 38%, var(--anthropic-ivory-light));
	}

	.article-sidebar {
		position: static;
		display: flex;
		flex-direction: column;
		height: 100%;
		min-height: 0;
		max-height: 100%;
		overflow-x: hidden;
		overflow-y: auto;
		padding: 18px 16px 0;
		overscroll-behavior: contain;
		scrollbar-gutter: stable;
	}

	.article-sidebar > * {
		flex-shrink: 0;
	}

	.article-sidebar :deep(.profile-card),
	.article-sidebar :deep(.profile-summary-card),
	.article-sidebar :deep(.profile-summary-card > .content),
	.article-sidebar :deep(.profile-notes),
	.article-sidebar :deep(.m-toc),
	.article-sidebar :deep(.sidebar-panel-body),
	.article-sidebar :deep(.sidebar-panel-heading) {
		border-right: 0 !important;
		border-left: 0 !important;
		border-radius: 0 !important;
		background: transparent !important;
		box-shadow: none !important;
	}

	.article-sidebar :deep(.profile-card) {
		border-top: 0 !important;
		border-bottom: 0 !important;
	}

	.article-sidebar :deep(.m-toc) {
		border-top: 0 !important;
		border-bottom: 0 !important;
	}

	.article-sidebar :deep(.profile-summary-card) {
		display: grid;
		grid-template-columns: minmax(0, 1fr);
		align-items: start;
		gap: 12px;
		padding: 18px 4px 16px;
	}

	.article-sidebar :deep(.profile-avatar-shell) {
		width: 100%;
		height: auto;
		aspect-ratio: 1;
		border-radius: 20px;
	}

	.article-sidebar :deep(.profile-identity) {
		width: 100%;
		min-width: 0;
		padding: 0 !important;
		text-align: left !important;
	}

	.article-sidebar :deep(.profile-identity .header) {
		overflow: hidden;
		font-size: 16px !important;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.article-sidebar :deep(.profile-username) {
		font-size: 11px;
	}

	.article-sidebar :deep(.profile-email) {
		display: none;
	}

	.article-sidebar :deep(.profile-signature) {
		margin-top: 0;
		border: 0;
		padding: 0;
		font-size: 11px;
		line-height: 1.5;
	}

	.article-author-actions {
		display: grid;
		width: max-content;
		justify-items: start;
		gap: 0;
		padding-top: 1px;
	}

	.article-author-actions :is(button, a) {
		display: inline-flex;
		height: 18px;
		align-items: center;
		justify-content: flex-start;
		gap: 4px;
		border: 0;
		border-radius: 3px;
		background: transparent;
		color: var(--anthropic-slate-light) !important;
		padding: 0 2px;
		font: inherit;
		font-size: 10px;
		font-weight: 600;
		line-height: 18px;
		text-decoration: underline;
		text-decoration-color: transparent;
		text-underline-offset: 2px;
		white-space: nowrap;
		cursor: pointer;
		transition: color 120ms ease, text-decoration-color 120ms ease;
	}

	.article-author-actions :is(button, a):hover,
	.article-author-actions :is(button, a):focus-visible {
		background: transparent;
		color: var(--anthropic-slate-dark) !important;
		text-decoration-color: currentColor;
	}

	.article-author-actions button:disabled {
		cursor: wait;
		opacity: .55;
	}

	.article-author-actions .app-icon {
		width: 10px;
		height: 10px;
	}

	.article-sidebar :deep(.profile-notes) {
		padding: 4px !important;
	}

	.article-sidebar :deep(.sidebar-panel-heading) {
		border-top: 0 !important;
		border-bottom: 0 !important;
		padding: 16px 4px 8px;
		font-size: 12px;
	}

	.article-sidebar :deep(.sidebar-panel-body) {
		border-top: 0 !important;
		padding: 0 4px 16px;
	}

	.article-sidebar :deep(.m-box:hover) {
		transform: none !important;
	}

	.article-sidebar {
		color: var(--anthropic-slate-dark);
	}

	.article-sidebar :deep(.profile-notes),
	.article-sidebar :deep(.profile-signature),
	.article-sidebar :deep(.article-author-achievements + .profile-links-disclosure),
	.article-sidebar :deep(.profile-links a),
	.article-sidebar :deep(.empty-profile-links) {
		border-color: var(--anthropic-cloud-light) !important;
	}

	.article-sidebar :deep(.profile-identity .header),
	.article-sidebar :deep(.profile-links a) {
		color: var(--anthropic-slate-dark) !important;
	}

	.article-sidebar :deep(.profile-username),
	.article-sidebar :deep(.profile-signature),
	.article-sidebar :deep(.profile-links-heading),
	.article-sidebar :deep(.profile-links-heading small),
	.article-sidebar :deep(.profile-links-chevron),
	.article-sidebar :deep(.empty-profile-links) {
		color: var(--anthropic-slate-light) !important;
	}

	.article-sidebar :deep(.profile-links a) {
		border-left-color: var(--anthropic-clay) !important;
		background: var(--anthropic-ivory-light) !important;
	}

	.article-sidebar :deep(.profile-links a:hover),
	.article-sidebar :deep(.profile-links a:focus-visible) {
		border-color: var(--anthropic-clay) !important;
		background: var(--anthropic-ivory-dark) !important;
	}

	.article-sidebar :deep(.profile-link-icon) {
		background: var(--anthropic-ivory-dark) !important;
	}

	.article-reading-pane {
		height: 100%;
		min-height: 0;
		overflow-x: hidden;
		overflow-y: auto;
		overscroll-behavior: contain;
		scroll-padding-top: 28px;
		scrollbar-color: color-mix(in srgb, var(--anthropic-cloud-dark) 58%, transparent) transparent;
		scrollbar-width: thin;
		background: var(--anthropic-ivory-light);
	}

	.article-reading-pane::-webkit-scrollbar {
		width: 8px;
	}

	.article-reading-pane::-webkit-scrollbar-thumb {
		border: 2px solid transparent;
		border-radius: 999px;
		background: color-mix(in srgb, var(--anthropic-cloud-dark) 58%, transparent);
		background-clip: padding-box;
	}

	.article-comment-shortcut {
		display: flex;
		align-items: center;
		gap: 7px;
		box-sizing: border-box;
		min-height: 32px;
		margin-top: auto;
		border-top: 1px solid var(--anthropic-cloud-light);
		border-bottom: 0;
		padding: 3px 4px;
		color: var(--anthropic-slate-medium);
	}

	.article-comment-shortcut strong {
		font-size: 12px;
	}

	.article-comment-shortcut > .app-icon {
		width: 13px;
		height: 13px;
		color: var(--anthropic-slate-light);
	}

	.article-comment-shortcut em {
		margin-left: auto;
		color: var(--anthropic-slate-light);
		font-size: 10px;
		font-style: normal;
		font-variant-numeric: tabular-nums;
	}

	.article-comment-shortcut:hover,
	.article-comment-shortcut:focus-visible {
		color: var(--anthropic-clay);
	}

	@media (prefers-reduced-motion: reduce) {
		.article-author-actions :is(button, a) {
			transition: none;
		}
	}

	.main-grid .sidebar-column {
		padding: 0;
	}

	.main-grid .main-content-column {
		padding-top: 0;
	}

	.m-display-none {
		display: none !important;
	}

	@media screen and (max-width: 767px) {
		.site.is-article {
			height: auto;
			min-height: 100vh;
			overflow: visible;
		}

		.site.is-article .main {
			height: auto;
			min-height: calc(100vh - 51px);
			overflow: visible;
		}

		.site.is-article .main-stage,
		.site.is-article .site-container,
		.site.is-article .main-grid {
			height: auto;
		}

		.main .site-container {
			padding-inline: 1rem;
		}

		.site.is-article .site-container {
			padding-inline: 0;
		}

		.main-grid {
			grid-template-columns: minmax(0, 1fr);
			gap: 0;
		}

		.sticky-sidebar {
			position: static;
			max-height: none;
			overflow: visible;
		}

		.article-reading-pane {
			height: auto;
			overflow: visible;
		}
	}
</style>
