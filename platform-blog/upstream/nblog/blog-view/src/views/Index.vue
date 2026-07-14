<template>
		<div class="site" :class="{'is-home': $route.name === 'home'}">
		<!--顶部导航-->
		<Nav :categoryList="categoryList"/>
		<!--首页大图：桌面整屏显示，移动端使用紧凑高度-->
		<Header v-if="$route.name==='home'"/>
		<router-view v-if="$route.name==='training'"/>

		<div v-else class="main">
			<div class="m-padded-tb-big">
				<div class="site-container">
					<div class="main-grid" :class="{'has-author-sidebar': $route.name === 'blog'}">
						<!--左侧-->
						<div v-if="$route.name === 'blog'" class="m-mobile-hide sidebar-column">
							<aside class="sticky-sidebar sticky-sidebar-left" aria-label="个人资料">
								<Introduction :author-username="articleAuthor?.username || ''" :author-summary="articleAuthor" :class="{'m-display-none':focusMode}"/>
							</aside>
						</div>
						<!--中间-->
						<div class="main-content-column">
							<FeaturedBlog v-if="$route.name === 'home'" :featuredBlogList="featuredBlogList" :class="{'m-display-none':focusMode}"/>
							<router-view v-if="$route.name !== 'home'" v-slot="{ Component }">
								<keep-alive include="Home">
									<component :is="Component" @article-author-change="articleAuthor = $event"/>
								</keep-alive>
							</router-view>
						</div>
						<!--右侧-->
						<div v-if="$route.name === 'blog'" class="m-mobile-hide sidebar-column">
							<aside class="sticky-sidebar sticky-sidebar-right" aria-label="文章目录">
								<Tocbot/>
							</aside>
						</div>
					</div>
				</div>
			</div>
		</div>

		<!--回到顶部-->
		<el-backtop v-if="$route.name!=='training'" style="box-shadow: none;background: none;z-index: 9999;">
			<img src="/img/paper-plane.png" style="width: 40px;height: 40px;">
		</el-backtop>
		<!--底部footer-->
		<Footer v-if="showFooter"/>
	</div>
</template>

<script>
	import {getSite} from '@/api/index'
	import Nav from "@/components/index/Nav";
	import Header from "@/components/index/Header";
	import Footer from "@/components/index/Footer";
	import Introduction from "@/components/sidebar/Introduction";
	import FeaturedBlog from "@/components/sidebar/FeaturedBlog";
	import Tocbot from "@/components/sidebar/Tocbot";
	import {mapState} from 'vuex'
	import getPageTitle from '@/util/get-page-title'
	import {SESSION_CHANGE_EVENT} from '@/auth/session'
	import {SAVE_CLIENT_SIZE, SAVE_INTRODUCTION, SAVE_SITE_INFO} from "@/store/mutations-types";

	export default {
		name: "Index",
		components: {Header, Tocbot, FeaturedBlog, Nav, Footer, Introduction},
		data() {
			return {
				siteInfo: {
					blogName: '',
					webTitleSuffix: ''
				},
				categoryList: [],
				tagList: [],
				featuredBlogList: [],
				articleAuthor: null,
			}
		},
		computed: {
			...mapState(['focusMode']),
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
				this.scrollToTop()
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
			refreshVisibleContent(event) {
				if (!event || event.key === null || event.key === 'custacm.accessToken' || event.key === 'custacm.user') {
					this.getSite()
				}
			},
			getSite() {
				getSite().then(res => {
					if (res.code === 200) {
						this.siteInfo = res.data.siteInfo
						this.categoryList = res.data.categoryList
						this.tagList = res.data.tagList
						this.featuredBlogList = res.data.featuredBlogList || []
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
		--home-canvas: #f2ede3;
		background: var(--home-canvas);
	}

	.site.is-home .main {
		background: var(--home-canvas);
	}

	:global(html[data-theme="dark"] .site.is-home) {
		--home-canvas: #171513;
		background: var(--home-canvas);
	}

	:global(html[data-theme="dark"] .site.is-home .main) {
		background: var(--home-canvas);
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

	.main .main-grid > :is(.sidebar-column, .main-content-column) {
		min-width: 0;
	}

	.main-grid {
		display: grid;
		grid-template-columns: minmax(0, 1fr);
		align-items: stretch;
		gap: 2rem;
	}

	.main-grid.has-author-sidebar {
		grid-template-columns: minmax(0, 3fr) minmax(0, 10fr) minmax(0, 3fr);
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
		.main .site-container {
			padding-inline: 1rem;
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
	}
</style>
