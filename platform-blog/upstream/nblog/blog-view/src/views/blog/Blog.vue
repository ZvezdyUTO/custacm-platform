<template>
	<div class="article-view">
		<article class="content-panel article-panel">
			<div class="featured-corner-mark" v-if="blog.top" aria-label="置顶文章">
				<AppIcon name="arrow-up-circle" />
			</div>
			<header class="article-hero article-reading-width">
				<router-link
					v-if="blog.category"
					:to="`/category/${blog.category.name}`"
					class="article-category"
					:style="{'--category-color': blog.category.color || '#60758a'}"
				>
					{{ blog.category.name }}
				</router-link>
				<h1 class="article-title">{{ blog.title }}</h1>
				<time class="article-date" :datetime="blog.createTime">{{ $filters.dateFormat(blog.createTime, 'YYYY年M月D日') }}</time>
			</header>
			<figure v-if="blog.firstPicture" class="article-cover article-reading-width" :class="{'has-summary': blog.description}">
				<img :src="blog.firstPicture" :alt="`${blog.title} 首图`" decoding="async">
			</figure>
			<p v-if="blog.description" class="article-summary article-copy-width" :class="{'without-cover': !blog.firstPicture}">
				{{ blog.description }}
			</p>
			<div class="article-copy">
					<!--文章Markdown正文-->
					<div class="typo js-toc-content match-braces rainbow-braces" v-lazy-container="{selector: 'img'}" v-viewer
					     @click.capture="openManagedImage" @keydown.capture="openManagedImage" v-html="sanitizeHtml(blog.content)"></div>
					<!--赞赏-->
					<div style="margin: 2em auto">
						<el-popover placement="top" :width="220" trigger="click" v-if="blog.appreciation">
							<div class="reward-card">
								<div class="image">
									<div style="font-size: 12px;text-align: center;margin-bottom: 5px;">一毛是鼓励</div>
									<img :src="$store.state.siteInfo.reward" alt="" class="reward-image">
									<div style="font-size: 12px;text-align: center;margin-top: 5px;">一块是真爱</div>
								</div>
							</div>
							<template #reference>
								<el-button class="reward-button m-text-500">赞赏</el-button>
							</template>
						</el-popover>
					</div>
					<!--横线-->
					<el-divider></el-divider>
					<!--标签-->
					<div class="row m-padded-tb-no">
						<div class="column m-padding-left-no">
							<router-link :to="`/tag/${tag.name}`" class="taxonomy-chip m-text-500 m-margin-small" :style="taxonomyStyle(tag.color)" v-for="(tag,index) in blog.tags" :key="index">{{ tag.name }}</router-link>
						</div>
					</div>
			</div>
		</article>
		<!--博客信息-->
		<div class="article-license-message article-secondary-width">
			<ul class="list">
				<li>作者：{{ blog.authorNickname || $store.state.introduction.name }}</li>
				<li>发表时间：{{ $filters.dateFormat(blog.createTime, 'YYYY-MM-DD HH:mm') }}</li>
				<li>最后修改：{{ $filters.dateFormat(blog.updateTime, 'YYYY-MM-DD HH:mm') }}</li>
				<li>本站点采用<a href="https://creativecommons.org/licenses/by/4.0/" target="_blank"> 署名 4.0 国际 (CC BY 4.0) </a>创作共享协议。可自由转载、引用，并且允许商业性使用。但需署名作者且注明文章出处。</li>
			</ul>
		</div>
		<!--评论-->
		<div id="article-comments" class="content-panel article-comments article-secondary-width">
			<CommentList :blogId="blogId" :internal="Boolean(blog.internal)" v-if="blog.commentEnabled"/>
			<h3 class="section-heading" v-else>评论已关闭</h3>
		</div>
		<ManagedImageViewer ref="managedImageViewer"/>
	</div>
</template>

<script>
	import {getBlogById} from "@/api/blog";
	import {getInternalBlog} from '@/api/player-blog'
	import CommentList from "@/components/comment/CommentList";
	import {SET_FOCUS_MODE, SET_IS_BLOG_RENDER_COMPLETE} from '@/store/mutations-types';
	import {readToken} from '@/auth/session'
	import getPageTitle from '@/util/get-page-title'
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
				}
			},
		computed: {
			blogId() {
				return parseInt(this.$route.params.id)
			},
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
		methods: {
			sanitizeHtml,
				taxonomyStyle(color) { return {backgroundColor: color || '#8B1E3F', color: '#fff'} },
			openManagedImage(event) {
				if (event.type === 'keydown' && !['Enter', ' '].includes(event.key)) return
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
			prepareManagedImages(article) {
				article?.querySelectorAll('img').forEach(image => {
					image.loading = 'lazy'
					image.decoding = 'async'
					if (!/\/api\/image\/assets\/[0-9a-f-]{36}\/thumbnail\.(?:jpg|png)/i.test(image.getAttribute('src') || '')) return
					image.tabIndex = 0
					image.setAttribute('role', 'button')
					image.setAttribute('data-managed-preview', '')
					image.setAttribute('aria-label', `查看大图：${image.alt || '文章图片'}`)
				})
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
							articleId: this.blog.id,
							articleTitle: this.blog.title,
							username: this.blog.authorUsername,
							nickname: this.blog.authorNickname,
							avatar: this.blog.authorAvatar,
						} : null)
						document.title = getPageTitle(this.blog.title)
						//v-html渲染完毕后，渲染代码块样式
						this.$nextTick(() => {
							const article = this.$el.querySelector('.js-toc-content')
							this.prepareManagedImages(article)
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
		}
	}
</script>

<style scoped>
	.article-view {
		--article-media-radius: 20px;
		--article-reading-width: 820px;
		--article-copy-width: 760px;
		--color-surface: var(--anthropic-ivory-medium);
		--color-surface-subtle: var(--anthropic-ivory-dark);
		--color-border: var(--anthropic-cloud-light);
		--color-border-strong: var(--anthropic-cloud-medium);
		--color-text: var(--anthropic-slate-dark);
		--color-text-muted: var(--anthropic-slate-light);
		--color-text-faint: var(--anthropic-cloud-dark);
		--color-action: var(--anthropic-clay);
		background: var(--anthropic-ivory-light);
		color: var(--anthropic-slate-dark);
		min-height: 100%;
		padding: 0 clamp(28px, 5vw, 80px);
	}

	.article-view > .article-panel {
		border: 0 !important;
		border-radius: 0 !important;
		background: transparent !important;
		padding: 0 !important;
		box-shadow: none !important;
	}

	.article-comments {
		position: relative;
	}

	.article-reading-width,
	.article-copy-width {
		margin-right: auto;
		margin-left: auto;
	}

	.article-reading-width {
		width: min(100%, var(--article-reading-width));
	}

	.article-copy-width {
		width: min(100%, var(--article-copy-width));
	}

	.article-hero {
		position: relative;
		padding: clamp(68px, 8vh, 104px) 0 46px;
		text-align: center;
	}

	.article-category {
		display: inline-flex;
		align-items: center;
		gap: 8px;
		margin-bottom: 20px;
		color: var(--anthropic-slate-light);
		font-size: 12px;
		font-weight: 700;
		letter-spacing: .12em;
		text-transform: uppercase;
	}

	.article-category::before {
		width: 8px;
		height: 8px;
		border-radius: 50%;
		background: var(--category-color);
		content: '';
	}

	.taxonomy-chip {
		border: 1px solid transparent !important;
		box-shadow: inset 0 0 0 1px rgba(255, 255, 255, .16);
		color: #fff !important;
		transition: filter 140ms ease, box-shadow 140ms ease;
	}

	.taxonomy-chip:hover,
	.taxonomy-chip:focus-visible {
		box-shadow: inset 0 0 0 1px rgba(255, 255, 255, .28);
		color: #fff !important;
		filter: brightness(1.08) saturate(1.06);
		outline: none;
	}

	.article-title {
		max-width: 900px;
		margin: 0 auto;
		color: var(--anthropic-slate-dark);
		font-size: clamp(2.75rem, 4vw, 4.35rem);
		font-weight: 760;
		letter-spacing: -.045em;
		line-height: 1.05;
		overflow-wrap: anywhere;
		text-wrap: balance;
		word-break: break-word;
	}

	.article-date {
		display: block;
		margin-top: 24px;
		color: var(--anthropic-slate-light);
		font-size: 13px;
		font-variant-numeric: tabular-nums;
		letter-spacing: .02em;
	}

	.reward-card {
		width: 100%;
		border: 1px solid var(--anthropic-clay);
		border-radius: 8px;
		background: var(--anthropic-ivory-medium);
		padding: 10px;
	}

	.reward-image {
		display: block;
		width: 100%;
		border: 1px solid var(--anthropic-cloud-light);
		border-radius: 8px;
	}

	.reward-button {
		border-color: var(--anthropic-clay) !important;
		border-radius: 999px !important;
		background: transparent !important;
		color: var(--anthropic-slate-dark) !important;
	}

	.article-license-message {
		border: 1px solid var(--anthropic-cloud-light);
		border-radius: 14px;
		background: var(--anthropic-ivory-medium);
		padding: 1rem 1.25rem;
		color: var(--anthropic-slate-medium);
	}

	.article-license-message .list {
		margin: 0;
		padding-left: 1.25rem;
	}

	.article-license-message a {
		color: var(--anthropic-slate-dark);
		text-decoration-color: var(--anthropic-clay);
	}

	.article-copy,
	.article-secondary-width {
		width: min(100%, var(--article-copy-width));
		margin-right: auto;
		margin-left: auto;
	}

	.article-copy {
		padding-bottom: 16px;
	}

	.article-secondary-width {
		width: min(100%, 860px);
	}

	.article-comments {
		margin-top: 20px;
		margin-bottom: 96px;
		border-color: var(--anthropic-cloud-light) !important;
		background: var(--anthropic-ivory-medium) !important;
		padding: clamp(22px, 3vw, 34px);
	}

	.el-divider {
		margin: 1rem 0 !important;
	}


	.article-cover {
		aspect-ratio: 16 / 9;
		margin-bottom: clamp(52px, 7vw, 82px);
		overflow: hidden;
		background: var(--anthropic-ivory-medium);
		border: 0;
		border-radius: var(--article-media-radius) !important;
	}

	.article-cover.has-summary {
		margin-bottom: 0;
	}

	.article-summary {
		margin-top: 18px;
		margin-bottom: clamp(48px, 6vw, 72px);
		color: var(--anthropic-slate-light);
		font-family: "Songti SC", STSong, "Noto Serif CJK SC", "Source Han Serif SC", serif;
		font-size: 14px;
		font-weight: 400;
		letter-spacing: .015em;
		line-height: 1.75;
		overflow-wrap: anywhere;
		text-align: left;
		word-break: break-word;
	}

	.article-summary.without-cover {
		margin-top: 0;
	}

	.article-cover img {
		display: block;
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.js-toc-content {
		color: var(--anthropic-slate-dark);
		font-size: 17px;
		text-align: left;
	}

	.js-toc-content :deep(h1),
	.js-toc-content :deep(h2),
	.js-toc-content :deep(h3),
	.js-toc-content :deep(h4),
	.js-toc-content :deep(h5),
	.js-toc-content :deep(h6),
	.js-toc-content :deep(p),
	.js-toc-content :deep(li),
	.js-toc-content :deep(strong) {
		color: var(--anthropic-slate-dark);
	}

	.js-toc-content :deep(h1),
	.js-toc-content :deep(h2),
	.js-toc-content :deep(h3),
	.js-toc-content :deep(hr) {
		border-color: var(--anthropic-cloud-light);
	}

	.js-toc-content :deep(a) {
		color: var(--anthropic-slate-dark);
		text-decoration-color: var(--anthropic-slate-dark);
	}

	.js-toc-content :deep(a:hover) {
		color: var(--anthropic-clay);
	}

	.js-toc-content :deep(a::before) {
		background-color: var(--anthropic-clay);
	}

	.js-toc-content :deep(blockquote) {
		border-left-color: var(--anthropic-clay);
		color: var(--anthropic-slate-light);
	}

	.js-toc-content :deep(mark) {
		border-bottom-color: var(--anthropic-clay);
		background: #e3dacc;
	}

	.js-toc-content :deep(:not(pre) > code) {
		border-color: var(--anthropic-cloud-light);
		background: var(--anthropic-ivory-medium);
		color: var(--anthropic-slate-medium);
	}

	.js-toc-content :deep(pre[class*="language-"]) {
		border-color: var(--anthropic-cloud-light);
		background: var(--anthropic-ivory-medium);
		box-shadow: 0 10px 28px color-mix(in srgb, var(--anthropic-slate-dark) 8%, transparent);
	}

	.js-toc-content :deep(pre[class*="language-"] code[class*="language-"]) {
		color: var(--anthropic-slate-dark);
	}

	.js-toc-content :deep(.token.comment),
	.js-toc-content :deep(.token.block-comment),
	.js-toc-content :deep(.token.prolog),
	.js-toc-content :deep(.token.doctype),
	.js-toc-content :deep(.token.cdata) { color: var(--anthropic-slate-light); }
	.js-toc-content :deep(.token.keyword),
	.js-toc-content :deep(.token.atrule),
	.js-toc-content :deep(.token.important),
	.js-toc-content :deep(.token.selector),
	.js-toc-content :deep(.token.builtin) { color: var(--anthropic-error); }
	.js-toc-content :deep(.token.string),
	.js-toc-content :deep(.token.char),
	.js-toc-content :deep(.token.attr-value),
	.js-toc-content :deep(.token.regex),
	.js-toc-content :deep(.token.inserted) { color: #788c5d; }
	.js-toc-content :deep(.token.number),
	.js-toc-content :deep(.token.boolean),
	.js-toc-content :deep(.token.constant),
	.js-toc-content :deep(.token.symbol) { color: #6a9bcc; }
	.js-toc-content :deep(.token.function),
	.js-toc-content :deep(.token.function-name),
	.js-toc-content :deep(.token.class-name),
	.js-toc-content :deep(.token.property) { color: #c46686; }

	.js-toc-content :deep(table th),
	.js-toc-content :deep(table td),
	.js-toc-content :deep(table caption) {
		border-color: var(--anthropic-cloud-light);
		color: var(--anthropic-slate-medium);
	}

	.js-toc-content :deep(table th) { background: var(--anthropic-ivory-medium); }
	.js-toc-content :deep(table thead th) { background: var(--anthropic-ivory-dark); }

	.js-toc-content :deep(p) {
		font-size: 17px;
		line-height: 1.88;
		text-align: left;
	}

	.js-toc-content :deep(img) {
		border-radius: var(--article-media-radius);
		clip-path: inset(0 round var(--article-media-radius));
	}

	.js-toc-content :deep(img[data-managed-preview]) {
		cursor: zoom-in;
	}

	@media (max-width: 767px) {
		.article-view {
			--article-media-radius: 14px;
			padding: 0 16px;
		}

		.article-hero {
			padding: 48px 0 34px;
		}

		.article-title {
			font-size: clamp(2.2rem, 11vw, 3.25rem);
		}

		.article-comments {
			margin-bottom: 48px;
		}
	}
</style>
