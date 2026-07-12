<template>
	<div class="site">
		<!--顶部导航-->
		<Nav :categoryList="categoryList"/>
		<!--首页大图：桌面整屏显示，移动端使用紧凑高度-->
		<Header v-if="$route.name==='home'"/>
		<router-view v-if="$route.name==='training'"/>

		<div v-else class="main">
			<div class="m-padded-tb-big">
				<div class="ui container">
					<div class="ui stackable grid">
						<!--左侧-->
						<div class="three wide column m-mobile-hide">
							<Introduction :author-username="articleAuthor?.username || ''" :author-summary="articleAuthor" :class="{'m-display-none':focusMode}"/>
						</div>
						<!--中间-->
						<div class="ten wide column">
							<router-view v-slot="{ Component }">
								<keep-alive include="Home">
									<component :is="Component" @article-author-change="articleAuthor = $event"/>
								</keep-alive>
							</router-view>
						</div>
						<!--右侧-->
						<div class="three wide column m-mobile-hide">
							<FeaturedBlog :featuredBlogList="featuredBlogList" :class="{'m-display-none':focusMode}"/>
							<Tags :tagList="tagList" :class="{'m-display-none':focusMode}"/>
							<!--只在文章页面显示目录-->
							<Tocbot v-if="$route.name==='blog'"/>
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
	import Tags from "@/components/sidebar/Tags";
	import FeaturedBlog from "@/components/sidebar/FeaturedBlog";
	import Tocbot from "@/components/sidebar/Tocbot";
	import {mapState} from 'vuex'
	import getPageTitle from '@/util/get-page-title'
	import {SESSION_CHANGE_EVENT} from '@/auth/session'
	import {SAVE_CLIENT_SIZE, SAVE_INTRODUCTION, SAVE_SITE_INFO, RESTORE_COMMENT_FORM} from "@/store/mutations-types";

	export default {
		name: "Index",
		components: {Header, Tocbot, FeaturedBlog, Tags, Nav, Footer, Introduction},
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
				return !trainingPath.startsWith('admin')
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
			//从localStorage恢复之前的评论信息
			this.$store.commit(RESTORE_COMMENT_FORM)
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
		overflow-x: hidden;
	}

	.main {
		width: 100%;
		max-width: 100%;
		margin-top: 40px;
		flex: 1;
	}

	.main .ui.container {
		box-sizing: border-box;
		width: min(1400px, 100%) !important;
		max-width: 100% !important;
		margin-left: auto !important;
		margin-right: auto !important;
	}

	.main .ui.grid > .column {
		min-width: 0;
	}

	.ui.grid .three.column {
		padding: 0;
	}

	.ui.grid .ten.column {
		padding-top: 0;
	}

	.m-display-none {
		display: none !important;
	}
</style>
