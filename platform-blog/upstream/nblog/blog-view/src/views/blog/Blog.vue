<template>
	<div>
		<div class="ui padded attached segment m-padded-tb-large">
			<div class="ui large red right corner label" v-if="blog.top">
				<i class="arrow alternate circle up icon"></i>
			</div>
			<div class="ui middle aligned mobile reversed stackable">
				<div class="ui grid m-margin-lr">
					<div class="row m-padded-tb-small">
						<h2 class="ui header m-center">{{ blog.title }}</h2>
					</div>
					<div class="row m-padded-tb-small">
						<div class="ui horizontal link list m-center">
							<div class="item m-common-black" v-if="blog.authorNickname"><i class="small user icon"></i><span>{{ blog.authorNickname }}</span></div>
							<div class="item m-datetime"><i class="small calendar icon"></i><span>{{ $filters.dateFormat(blog.createTime, 'YYYY-MM-DD') }}</span></div>
							<div class="item m-views"><i class="small eye icon"></i><span>{{ blog.views }}</span></div>
							<div class="item m-common-black"><i class="small pencil alternate icon"></i><span>字数≈{{ blog.words }}字</span></div>
							<div class="item m-common-black"><i class="small clock icon"></i><span>阅读时长≈{{ blog.readTime }}分</span></div>
							<a class="item m-common-black" aria-label="切换字体大小" @click.prevent="bigFontSize=!bigFontSize"><div data-inverted="" data-tooltip="点击切换字体大小" data-position="top center"><i class="font icon"></i></div></a>
							<a class="item m-common-black" aria-label="切换专注模式" @click.prevent="changeFocusMode"><div data-inverted="" data-tooltip="专注模式" data-position="top center"><i class="book icon"></i></div></a>
							<router-link v-if="isAuthor" :to="`/write/${blog.id}`" class="item article-edit-link"><i class="edit outline icon"></i><span>编辑文章</span></router-link>
						</div>
					</div>
					<!--分类-->
					<router-link :to="`/category/${blog.category.name}`" class="ui large ribbon label" :style="taxonomyStyle(blog.category.color)" v-if="blog.category">
						<i class="small folder open icon"></i><span class="m-text-500">{{ blog.category.name }}</span>
					</router-link>
					<!--文章Markdown正文-->
					<div class="typo js-toc-content m-padded-tb-small match-braces rainbow-braces" v-lazy-container="{selector: 'img'}" v-viewer :class="{'m-big-fontsize':bigFontSize}" @click.capture="openManagedImage" v-html="sanitizeHtml(blog.content)"></div>
					<!--赞赏-->
					<div style="margin: 2em auto">
						<el-popover placement="top" :width="220" trigger="click" v-if="blog.appreciation">
							<div class="ui orange basic label" style="width: 100%">
								<div class="image">
									<div style="font-size: 12px;text-align: center;margin-bottom: 5px;">一毛是鼓励</div>
									<img :src="$store.state.siteInfo.reward" alt="" class="ui rounded bordered image" style="width: 100%">
									<div style="font-size: 12px;text-align: center;margin-top: 5px;">一块是真爱</div>
								</div>
							</div>
							<template #reference>
								<el-button class="ui orange inverted circular button m-text-500">赞赏</el-button>
							</template>
						</el-popover>
					</div>
					<!--横线-->
					<el-divider></el-divider>
					<!--标签-->
					<div class="row m-padded-tb-no">
						<div class="column m-padding-left-no">
							<router-link :to="`/tag/${tag.name}`" class="ui tag label m-text-500 m-margin-small" :style="taxonomyStyle(tag.color)" v-for="(tag,index) in blog.tags" :key="index">{{ tag.name }}</router-link>
						</div>
					</div>
				</div>
			</div>
		</div>
		<!--博客信息-->
		<div class="ui attached positive message">
			<ul class="list">
				<li>作者：{{ blog.authorNickname || $store.state.introduction.name }}</li>
				<li>发表时间：{{ $filters.dateFormat(blog.createTime, 'YYYY-MM-DD HH:mm') }}</li>
				<li>最后修改：{{ $filters.dateFormat(blog.updateTime, 'YYYY-MM-DD HH:mm') }}</li>
				<li>本站点采用<a href="https://creativecommons.org/licenses/by/4.0/" target="_blank"> 署名 4.0 国际 (CC BY 4.0) </a>创作共享协议。可自由转载、引用，并且允许商业性使用。但需署名作者且注明文章出处。</li>
			</ul>
		</div>
		<!--评论-->
		<div class="ui bottom teal attached segment threaded comments">
			<CommentList :page="0" :blogId="blogId" :internal="Boolean(blog.internal)" v-if="blog.commentEnabled"/>
			<h3 class="ui header" v-else>评论已关闭</h3>
		</div>
		<ManagedImageViewer ref="managedImageViewer"/>
	</div>
</template>

<script>
	import {getBlogById} from "@/api/blog";
	import {getInternalBlog} from '@/api/player-blog'
	import CommentList from "@/components/comment/CommentList";
		import {mapState} from "vuex";
		import {SET_FOCUS_MODE, SET_IS_BLOG_RENDER_COMPLETE} from '@/store/mutations-types';
	import {readToken, readUser, SESSION_CHANGE_EVENT} from '@/auth/session'
	import getPageTitle from '@/util/get-page-title'
	import {isArticleAuthor} from '@/util/articleForm'
	import renderMathInElement from 'katex/contrib/auto-render'
	import 'katex/dist/katex.css'
	import ManagedImageViewer from '@/components/article/ManagedImageViewer.vue'
	import {originalUrlForManagedThumbnail} from '@/util/articleImages'
	import {sanitizeHtml} from '@/util/sanitizeHtml'

	export default {
		name: "Blog",
		components: {CommentList, ManagedImageViewer},
		emits: ['article-author-change'],
			data() {
				return {
					blog: {},
					bigFontSize: false,
					authUser: readUser(),
				}
			},
		computed: {
			blogId() {
				return parseInt(this.$route.params.id)
			},
				isAuthor() { return isArticleAuthor(this.blog, this.authUser) },
				...mapState(['siteInfo', 'focusMode'])
		},
		beforeRouteEnter(to, from, next) {
			//路由到博客文章页面之前，应将文章的渲染完成状态置为 false
			next(vm => {
				// 当 beforeRouteEnter 钩子执行前，组件实例尚未创建
				// vm 就是当前组件的实例，可以在 next 方法中把 vm 当做 this用
				vm.$store.commit(SET_IS_BLOG_RENDER_COMPLETE, false)
			})
		},
		beforeRouteLeave(to, from, next) {
			this.$store.commit(SET_FOCUS_MODE, false)
			// 从文章页面路由到其它页面时，销毁当前组件的同时，要销毁tocbot实例
			// 否则tocbot一直在监听页面滚动事件，而文章页面的锚点已经不存在了，会报"Uncaught TypeError: Cannot read property 'className' of null"
			tocbot.destroy()
			next()
		},
		beforeRouteUpdate(to, from, next) {
			// 一般有两种情况会触发这个钩子
			// ①当前文章页面跳转到其它文章页面
			// ②点击目录跳转锚点时，路由hash值会改变，导致当前页面会重新加载，这种情况是不希望出现的
			// 在路由 beforeRouteUpdate 中判断路径是否改变
			// 如果跳转到其它页面，to.path!==from.path 就放行 next()
			// 如果是跳转锚点，path不会改变，hash会改变，to.path===from.path, to.hash!==from.path 不放行路由跳转，就能让锚点正常跳转
			if (to.path !== from.path) {
				this.$store.commit(SET_FOCUS_MODE, false)
				//在当前组件内路由到其它博客文章时，要重新获取文章
				this.getBlog(to.params.id)
				//只要路由路径有改变，且停留在当前Blog组件内，就把文章的渲染完成状态置为 false
				this.$store.commit(SET_IS_BLOG_RENDER_COMPLETE, false)
				next()
			}
		},
			created() {
				this.getBlog()
			},
			mounted() {
				window.addEventListener('storage', this.refreshUser)
				window.addEventListener(SESSION_CHANGE_EVENT, this.refreshUser)
			},
			beforeUnmount() {
				window.removeEventListener('storage', this.refreshUser)
				window.removeEventListener(SESSION_CHANGE_EVENT, this.refreshUser)
			},
		methods: {
			sanitizeHtml,
			taxonomyStyle(color) { return {backgroundColor: color || '#8B1E3F', color: '#fff'} },
				refreshUser() { this.authUser = readUser() },
			openManagedImage(event) {
				const image = event.target instanceof HTMLImageElement ? event.target : null
				const link = image?.closest('a')
				if (!image) return
				const thumbnail = image.getAttribute('src') || ''
				const thumbnailMatch = thumbnail.match(/\/api\/image\/assets\/([0-9a-f-]{36})\/thumbnail\.(?:jpg|png)/i)
				if (!thumbnailMatch) return
				const linkedOriginal = link?.getAttribute('href') || ''
				const originalMatch = linkedOriginal.match(/\/api\/image\/assets\/([0-9a-f-]{36})\/original\.(?:jpg|png)/i)
				const original = originalMatch?.[1] === thumbnailMatch[1]
					? linkedOriginal : originalUrlForManagedThumbnail(thumbnail)
				if (!original) return
				event.preventDefault()
				event.stopImmediatePropagation()
				this.$refs.managedImageViewer.open(thumbnail, original, image.alt || '')
			},
			async getBlog(id = this.blogId) {
				let res
				try {
					res = await getBlogById(id)
					if (res.code !== 200) throw new Error(res.msg || '文章获取失败')
				} catch (error) {
					const token = readToken()
					if (!token) {
						this.msgError(error?.response?.data?.msg || error?.message || '文章不存在')
						return
					}
					try {
						res = {code: 200, data: await getInternalBlog(token, id)}
					} catch (internalError) {
						this.msgError(internalError?.response?.data?.msg || internalError?.message || '文章不存在')
						return
					}
				}
				Promise.resolve(res).then(res => {
					if (res.code === 200) {
						this.blog = res.data
						this.$emit('article-author-change', this.blog.authorUsername ? {
							username: this.blog.authorUsername,
							nickname: this.blog.authorNickname,
							avatar: this.blog.authorAvatar,
						} : null)
						document.title = getPageTitle(this.blog.title)
						//v-html渲染完毕后，渲染代码块样式
						this.$nextTick(() => {
							const article = this.$el.querySelector('.js-toc-content')
							article?.querySelectorAll('img').forEach(image => {
								image.loading = 'lazy'
								image.decoding = 'async'
							})
							if (article) renderMathInElement(article, {
								delimiters: [
									{left: '$$', right: '$$', display: true},
									{left: '$', right: '$', display: false},
								],
								throwOnError: false,
								trust: false,
							})
							Prism.highlightAll()
							//将文章渲染完成状态置为 true
							this.$store.commit(SET_IS_BLOG_RENDER_COMPLETE, true)
						})
					} else {
						this.msgError(res.msg)
					}
				}).catch(() => {
					this.msgError("请求失败")
				})
			},
			changeFocusMode() {
				this.$store.commit(SET_FOCUS_MODE, !this.focusMode)
			}
		}
	}
</script>

<style scoped>
	.el-divider {
		margin: 1rem 0 !important;
	}

	h1::before, h2::before, h3::before, h4::before, h5::before, h6::before {
		display: block;
		content: " ";
		height: 55px;
		margin-top: -55px;
		visibility: hidden;
	}

	.article-edit-link {
		color: #17324d !important;
		font-weight: 700;
	}
</style>
