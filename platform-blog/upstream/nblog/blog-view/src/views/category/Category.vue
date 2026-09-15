<template>
	<section class="article-catalog-page" aria-labelledby="article-catalog-title">
		<header class="catalog-hero">
			<span class="catalog-hero-mark" aria-hidden="true"><AppIcon name="book" :size="42" :stroke-width="1.35"/></span>
			<h1 id="article-catalog-title">{{ heroTitle }}</h1>
			<p>{{ heroDescription }}</p>
		</header>

		<div class="catalog-workspace">
			<aside class="catalog-sidebar" aria-label="文章筛选">
				<div class="catalog-sidebar-title">筛选</div>
				<nav class="catalog-filter-group" aria-label="按分类筛选">
					<div class="catalog-filter-heading">分类</div>
					<router-link to="/articles" class="catalog-filter-link" :class="{'is-active': routeMode === 'articles'}">
						<span>全部文章</span>
					</router-link>
					<router-link v-for="category in categoryList" :key="category.name" :to="`/category/${category.name}`"
					             class="catalog-filter-link" :class="{'is-active': routeMode === 'category' && category.name === taxonomyName}"
					             :style="taxonomyColorStyle(category)">
						<span>{{ category.name }}</span><span class="catalog-taxonomy-dot" aria-hidden="true"></span>
					</router-link>
				</nav>

				<section class="catalog-random-tags" aria-labelledby="random-tags-title">
					<div class="catalog-random-tags-heading">
						<h2 id="random-tags-title">随机标签</h2>
						<button type="button" aria-label="换一组随机标签" title="换一组" @click="refreshRandomTags">
							<AppIcon name="shuffle" :size="15"/>
						</button>
					</div>
					<div class="catalog-tag-cloud">
						<router-link v-for="tag in randomTags" :key="tag.name" :to="`/tag/${tag.name}`"
						             :class="{'is-active': routeMode === 'tag' && tag.name === taxonomyName}"
						             :style="taxonomyColorStyle(tag)">
							<span class="catalog-taxonomy-dot" aria-hidden="true"></span>{{ tag.name }}
						</router-link>
					</div>
				</section>
			</aside>

			<div ref="catalogResults" class="catalog-results" :class="`is-${viewMode}-view`" :style="{'--catalog-columns': gridColumns}" :aria-busy="catalogLoading">
				<div class="catalog-toolbar">
					<form class="catalog-search" role="search" @submit.prevent="submitSearch">
						<button type="submit" class="catalog-search-submit" aria-label="搜索文章" title="搜索文章"
						        :disabled="searchLoading">
							<AppIcon :name="searchLoading ? 'loader' : 'search'" :spin="searchLoading" :size="18"/>
						</button>
						<label for="article-catalog-search" class="catalog-visually-hidden">搜索全部文章</label>
						<input id="article-catalog-search" v-model="searchQuery" type="search"
						       placeholder="搜索全部文章" autocomplete="off">
					</form>
					<div class="catalog-view-switch" aria-label="文章视图">
						<button type="button" :class="{'is-active': viewMode === 'grid'}" :aria-pressed="viewMode === 'grid'" @click="viewMode = 'grid'">
							<AppIcon name="grid"/>网格
						</button>
						<button type="button" :class="{'is-active': viewMode === 'list'}" :aria-pressed="viewMode === 'list'" @click="viewMode = 'list'">
							<AppIcon name="list"/>列表
						</button>
					</div>
				</div>

				<p v-if="!searchSubmitted && totalArticles" class="catalog-result-count">共 {{ totalArticles }} 篇 · 每页 {{ pageSize }} 篇</p>
				<div v-if="!searchSubmitted && blogList.length && (catalogLoading || catalogError)" class="catalog-load-state" role="status">
					<span>{{ catalogLoading ? '正在加载文章' : catalogError }}</span>
					<button v-if="catalogError" type="button" @click="getBlogList(requestedPage)">重新加载</button>
				</div>
				<div v-if="searchSubmitted" class="catalog-search-state" aria-live="polite">
					<span v-if="searchLoading">正在搜索“{{ submittedQuery }}”</span>
					<span v-else-if="searchError">{{ searchError }}</span>
					<span v-else>“{{ submittedQuery }}”找到 {{ searchResults.length }} 篇文章</span>
				</div>

				<div v-if="searchSubmitted && !searchLoading && !searchError && searchResults.length" class="catalog-search-hits">
					<article v-for="item in searchResults" :key="item.id" class="catalog-search-hit">
						<router-link :to="`/blog/${item.id}`">
							<span>搜索结果</span>
							<h2>{{ item.title }}</h2>
							<p>{{ searchResultDescription(item.description) || '暂无文章简介' }}</p>
							<strong>阅读全文</strong>
						</router-link>
					</article>
				</div>
				<div v-else-if="!searchSubmitted && !blogList.length && (catalogLoading || catalogError)" class="catalog-empty" role="status">
					<AppIcon :name="catalogLoading ? 'loader' : 'search'" :spin="catalogLoading" :size="24"/>
					<strong>{{ catalogLoading ? '正在加载文章' : catalogError }}</strong>
					<button v-if="catalogError" type="button" @click="getBlogList(requestedPage)">重新加载</button>
				</div>
				<BlogList v-else-if="!searchSubmitted && blogList.length" :layout="viewMode" :getBlogList="getBlogList"
				          :blogList="blogList" :totalPage="totalPage" :currentPage="currentPage"/>
				<div v-else-if="!searchLoading && !searchError" class="catalog-empty" role="status">
					<AppIcon name="search" :size="24"/>
					<strong>{{ searchSubmitted ? '没有找到匹配的文章' : '当前没有文章' }}</strong>
					<span>{{ searchSubmitted ? '换个关键词后，按回车或点击搜索按钮重试。' : '请从左侧选择其他分类。' }}</span>
				</div>
			</div>
		</div>
	</section>
</template>

<script>
	import BlogList from "@/components/blog/BlogList";
	import {getSearchBlogList} from '@/api/blog'
	import {getBlogListByCategoryName} from "@/api/category";
	import {getBlogList} from '@/api/home'
	import {getBlogListByTagName} from '@/api/tag'
	import {SESSION_CHANGE_EVENT} from '@/auth/session'
	import {catalogColumns, catalogPageSize, createCatalogPager} from '@/util/catalogPagination'

	export default {
		name: "ArticleCatalog",
		components: {BlogList},
		props: {
			categoryList: {type: Array, default: () => []},
			tagList: {type: Array, default: () => []},
		},
		data() {
			return {
				blogList: [],
				totalPage: 0,
				totalArticles: 0,
				currentPage: 1,
				requestedPage: 1,
				catalogWidth: 0,
				catalogLoading: false,
				catalogError: '',
				catalogRequestId: 0,
				catalogPager: null,
				viewMode: 'grid',
				searchQuery: '',
				submittedQuery: '',
				searchResults: [],
				searchSubmitted: false,
				searchLoading: false,
				searchError: '',
				searchRequestId: 0,
				randomTags: [],
			}
		},
		watch: {
			//在当前组件被重用时，要重新获取博客列表
			'$route.fullPath'() {
				if (['articles', 'category', 'tag'].includes(this.$route.name)) {
					this.resetSearch()
					this.resetCatalog()
				}
			},
			pageSize(nextSize, previousSize) {
				if (!this.catalogPager) return
				const firstIndex = (this.currentPage - 1) * previousSize
				this.getBlogList(Math.floor(firstIndex / nextSize) + 1)
			},
			tagList: {
				immediate: true,
				handler() { this.refreshRandomTags() },
			},
		},
		computed: {
			gridColumns() { return catalogColumns(this.catalogWidth) },
			pageSize() { return catalogPageSize(this.gridColumns, this.viewMode) },
			routeMode() { return this.$route.name || 'articles' },
			taxonomyName() { return this.$route.params.name || '' },
			heroTitle() {
				if (this.routeMode === 'category' || this.routeMode === 'tag') return this.taxonomyName
				return '全部文章'
			},
			heroDescription() {
				if (this.routeMode === 'category') {
					const category = this.categoryList.find(item => item.name === this.taxonomyName)
					if (category?.description?.trim()) return category.description.trim()
				}
				return '记录思考、经验与发现，也分享简单的日常。'
			},
		},
		methods: {
			taxonomyColorStyle(item) {
				const color = /^#[0-9a-f]{6}$/i.test(item?.color || '') ? item.color : '#d97757'
				return {'--catalog-taxonomy-color': color}
			},
			refreshRandomTags() {
				const shuffled = [...this.tagList]
				for (let index = shuffled.length - 1; index > 0; index -= 1) {
					const target = Math.floor(Math.random() * (index + 1))
					;[shuffled[index], shuffled[target]] = [shuffled[target], shuffled[index]]
				}
				this.randomTags = shuffled.slice(0, 12)
			},
			refreshVisibleBlogs(event) {
				if (!event || event.type === SESSION_CHANGE_EVENT || event.key === null || event.key === 'custacm.accessToken' || event.key === 'custacm.user') {
					this.resetSearch()
					this.resetCatalog()
				}
			},
			resetSearch() {
				this.searchRequestId += 1
				this.searchQuery = ''
				this.submittedQuery = ''
				this.searchResults = []
				this.searchSubmitted = false
				this.searchLoading = false
				this.searchError = ''
			},
			isValidSearchQuery(query) {
				return query.length <= 20 && !['%', '_', '[', '#', '*'].some(character => query.includes(character))
			},
			submitSearch() {
				const query = this.searchQuery.trim()
				if (!query) {
					this.resetSearch()
					return Promise.resolve()
				}

				this.submittedQuery = query
				this.searchSubmitted = true
				this.searchResults = []
				this.searchError = ''
				const requestId = ++this.searchRequestId
				if (!this.isValidSearchQuery(query)) {
					this.searchLoading = false
					this.searchError = '关键词需为 1–20 个字符，且不能包含 %、_、[、#、*。'
					return Promise.resolve()
				}

				this.searchLoading = true
				return getSearchBlogList(query).then(res => {
					if (requestId !== this.searchRequestId) return
					if (res.code !== 200) throw new Error(res.msg || '搜索失败')
					this.searchResults = Array.isArray(res.data) ? res.data : []
				}).catch(() => {
					if (requestId !== this.searchRequestId) return
					this.searchResults = []
					this.searchError = '搜索失败，请稍后重试。'
				}).finally(() => {
					if (requestId === this.searchRequestId) this.searchLoading = false
				})
			},
			searchResultDescription(description) {
				return String(description || '').replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim()
			},
			measureCatalog() {
				const width = this.$refs.catalogResults?.getBoundingClientRect().width
				if (width > 0) this.catalogWidth = width
			},
			resetCatalog() {
				const mode = this.routeMode
				const name = this.taxonomyName
				this.catalogPager = createCatalogPager(page => {
					if (mode === 'category') return getBlogListByCategoryName(name, page)
					if (mode === 'tag') return getBlogListByTagName(name, page)
					return getBlogList(page)
				})
				this.blogList = []
				this.totalArticles = 0
				this.totalPage = 0
				this.currentPage = 1
				return this.getBlogList(1)
			},
			async getBlogList(pageNum = 1) {
				this.requestedPage = pageNum
				const requestId = ++this.catalogRequestId
				this.catalogLoading = true
				this.catalogError = ''
				try {
					const result = await this.catalogPager.readPage(pageNum, this.pageSize)
					if (requestId !== this.catalogRequestId) return
					this.blogList = result.list
					this.totalPage = result.totalPage
					this.totalArticles = result.total
					this.currentPage = result.page
					this.$nextTick(() => Prism.highlightAll())
				} catch {
					if (requestId === this.catalogRequestId) this.catalogError = '文章加载失败，请重试。'
				} finally {
					if (requestId === this.catalogRequestId) this.catalogLoading = false
				}
			}
		},
		mounted() {
			this.measureCatalog()
			this.resetCatalog()
			if (typeof ResizeObserver !== 'undefined') {
				this.catalogObserver = new ResizeObserver(() => this.measureCatalog())
				this.catalogObserver.observe(this.$refs.catalogResults)
			} else {
				window.addEventListener('resize', this.measureCatalog)
			}
			window.addEventListener('storage', this.refreshVisibleBlogs)
			window.addEventListener(SESSION_CHANGE_EVENT, this.refreshVisibleBlogs)
		},
		beforeUnmount() {
			this.catalogRequestId += 1
			this.catalogObserver?.disconnect()
			window.removeEventListener('resize', this.measureCatalog)
			window.removeEventListener('storage', this.refreshVisibleBlogs)
			window.removeEventListener(SESSION_CHANGE_EVENT, this.refreshVisibleBlogs)
		}
	}
</script>

<style scoped>
	.article-catalog-page {
		--catalog-canvas: var(--anthropic-ivory-light);
		--catalog-surface: #f7f7f5;
		--catalog-border: #d1cfc5;
		--catalog-border-strong: #9b998f;
		--catalog-text: var(--anthropic-slate-dark);
		--catalog-muted: var(--anthropic-slate-light);
		--catalog-accent: #9b4c36;
		--catalog-selection: var(--anthropic-ivory-dark);
		--catalog-media: var(--anthropic-ivory-dark);
		--catalog-media-text: var(--anthropic-slate-light);
		--catalog-action: var(--anthropic-slate-dark);
		--catalog-on-action: var(--anthropic-ivory-light);
		min-height: 100vh;
		margin: -2rem;
		padding: 0 4rem 6rem;
		background: var(--catalog-canvas);
		color: var(--catalog-text);
	}

	.catalog-hero {
		width: min(640px, calc(100% - 336px));
		margin-left: 336px;
		padding: 72px 0 64px;
	}

	.catalog-hero-mark {
		display: inline-flex;
		margin-bottom: 24px;
		color: var(--catalog-text);
	}

	.catalog-hero h1 {
		margin: 0;
		color: var(--catalog-text);
		font-family: Georgia, 'Times New Roman', 'Noto Serif SC', serif;
		font-size: clamp(48px, 4.45vw, 64px);
		font-weight: 500;
		letter-spacing: -0.035em;
		line-height: 1.1;
	}

	.catalog-hero p {
		max-width: 630px;
		overflow-wrap: anywhere;
		margin: 24px 0 0;
		color: var(--catalog-muted);
		font-size: 19px;
		line-height: 1.65;
	}

	.catalog-workspace {
		display: grid;
		grid-template-columns: 280px minmax(0, 1fr);
		align-items: start;
		gap: 56px;
	}

	.catalog-sidebar {
		position: sticky;
		top: 76px;
	}

	.catalog-sidebar-title {
		border-bottom: 1px solid var(--catalog-border);
		padding: 0 0 10px;
		font-size: 16px;
	}

	.catalog-filter-group {
		padding: 16px 0 30px;
	}

	.catalog-filter-heading {
		margin-bottom: 9px;
		color: var(--catalog-muted);
		font-size: 12px;
	}

	.catalog-filter-link {
		display: flex;
		min-height: 36px;
		align-items: center;
		justify-content: space-between;
		gap: 12px;
		border-radius: 7px;
		padding: 6px 8px;
		color: var(--catalog-muted);
		font-size: 14px;
		transition: background-color 150ms ease, color 150ms ease;
	}

	.catalog-filter-link:hover,
	.catalog-filter-link:focus-visible,
	.catalog-filter-link.is-active {
		background: var(--catalog-selection);
		color: var(--catalog-text);
		outline: none;
	}

	.catalog-taxonomy-dot {
		width: 7px;
		height: 7px;
		flex: 0 0 7px;
		border-radius: 50%;
		background: var(--catalog-taxonomy-color);
	}

	.catalog-filter-link em {
		color: var(--catalog-muted);
		font-size: 11px;
		font-style: normal;
	}

	.catalog-random-tags {
		border-top: 1px solid var(--catalog-border);
		padding-top: 20px;
	}

	.catalog-random-tags-heading {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 14px;
	}

	.catalog-random-tags-heading h2 {
		margin: 0;
		color: var(--catalog-muted);
		font-size: 12px;
		font-weight: 400;
	}

	.catalog-random-tags-heading button {
		display: grid;
		width: 30px;
		height: 30px;
		place-items: center;
		border: 1px solid var(--catalog-border);
		border-radius: 8px;
		background: transparent;
		color: var(--catalog-muted);
		cursor: pointer;
	}

	.catalog-random-tags-heading button:hover,
	.catalog-random-tags-heading button:focus-visible {
		border-color: var(--catalog-border-strong);
		color: var(--catalog-text);
		outline: none;
	}

	.catalog-tag-cloud {
		display: flex;
		flex-wrap: wrap;
		gap: 8px 7px;
	}

	.catalog-tag-cloud a {
		display: inline-flex;
		min-height: 28px;
		align-items: center;
		gap: 7px;
		border: 1px solid var(--catalog-border);
		border-radius: 999px;
		padding: 5px 10px;
		color: var(--catalog-muted);
		font-size: 12px;
		line-height: 1;
	}

	.catalog-tag-cloud a:hover,
	.catalog-tag-cloud a:focus-visible,
	.catalog-tag-cloud a.is-active {
		border-color: var(--catalog-border-strong);
		background: var(--catalog-selection);
		color: var(--catalog-text);
		outline: none;
	}

	.catalog-tag-cloud .catalog-taxonomy-dot {
		width: 6px;
		height: 6px;
		border-radius: 50%;
		background: var(--catalog-taxonomy-color);
	}

	.catalog-toolbar {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 24px;
		margin-bottom: 16px;
	}

	.catalog-result-count { margin: 0 0 24px; color: var(--catalog-muted); font-size: 13px; }

	.catalog-search {
		display: flex;
		width: 300px;
		min-height: 50px;
		align-items: center;
		gap: 10px;
		border: 1px solid var(--catalog-border-strong);
		border-radius: 12px;
		background: var(--catalog-surface);
		padding: 0 14px;
		color: var(--catalog-text);
	}

	.catalog-search:focus-within {
		border-color: var(--catalog-accent);
	}

	.catalog-search-submit {
		display: grid;
		width: 28px;
		height: 36px;
		flex: 0 0 28px;
		place-items: center;
		border: 0;
		background: transparent;
		padding: 0;
		color: inherit;
		cursor: pointer;
	}

	.catalog-search-submit:hover,
	.catalog-search-submit:focus-visible {
		color: var(--catalog-accent);
		outline: none;
	}

	.catalog-search-submit:disabled {
		cursor: wait;
	}

	.catalog-search input {
		width: 100%;
		min-width: 0;
		border: 0;
		outline: 0;
		background: transparent !important;
		color: var(--catalog-text);
		font-size: 15px;
	}

	.catalog-search input::placeholder { color: var(--catalog-muted); }

	.catalog-search-state {
		margin: -14px 0 24px;
		color: var(--catalog-muted);
		font-size: 13px;
	}

	.catalog-search-hits {
		display: grid;
		gap: 18px;
		margin-bottom: 46px;
	}

	.catalog-results.is-grid-view .catalog-search-hits {
		grid-template-columns: repeat(var(--catalog-columns, 1), minmax(0, 1fr));
	}

	.catalog-search-hit {
		min-width: 0;
		border: 1px solid var(--catalog-border);
		border-radius: 18px;
		background: var(--catalog-surface);
	}

	.catalog-search-hit:hover,
	.catalog-search-hit:focus-within {
		border-color: var(--catalog-border-strong);
	}

	.catalog-search-hit a {
		display: flex;
		min-height: 210px;
		flex-direction: column;
		padding: 24px;
		color: var(--catalog-text);
	}

	.catalog-search-hit a:focus-visible {
		border-radius: inherit;
		outline: 2px solid var(--catalog-accent);
		outline-offset: 3px;
	}

	.catalog-search-hit span {
		color: var(--catalog-muted);
		font-size: 11px;
		letter-spacing: .08em;
	}

	.catalog-search-hit h2 {
		margin: 16px 0 12px;
		font: 500 22px/1.2 Georgia, 'Times New Roman', 'Noto Serif SC', serif;
	}

	.catalog-search-hit p {
		display: -webkit-box;
		overflow: hidden;
		-webkit-box-orient: vertical;
		-webkit-line-clamp: 3;
		margin: 0 0 22px;
		color: var(--catalog-muted);
		font-size: 14px;
		line-height: 1.55;
	}

	.catalog-search-hit strong {
		margin-top: auto;
		color: var(--catalog-text);
		font-size: 13px;
		font-weight: 500;
	}

	.catalog-results.is-list-view .catalog-search-hit a {
		min-height: 0;
	}

	.catalog-view-switch {
		display: flex;
		border-radius: 12px;
		background: var(--catalog-surface);
		padding: 4px;
	}

	.catalog-view-switch button {
		display: inline-flex;
		min-height: 40px;
		align-items: center;
		gap: 7px;
		border: 1px solid transparent;
		border-radius: 9px;
		background: transparent;
		padding: 0 12px;
		color: var(--catalog-muted);
		font-size: 13px;
		cursor: pointer;
	}

	.catalog-view-switch button.is-active {
		border-color: var(--catalog-border-strong);
		background: var(--catalog-selection);
		color: var(--catalog-text);
	}

	.catalog-visually-hidden {
		position: absolute;
		width: 1px;
		height: 1px;
		overflow: hidden;
		clip: rect(0 0 0 0);
		white-space: nowrap;
	}

	.catalog-empty {
		display: grid;
		min-height: 320px;
		place-items: center;
		align-content: center;
		gap: 10px;
		border: 1px solid var(--catalog-border);
		border-radius: 18px;
		color: var(--catalog-muted);
		text-align: center;
	}

	.catalog-empty strong { color: var(--catalog-text); font: 500 22px/1.2 Georgia, serif; }
	.catalog-load-state { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; color: var(--catalog-muted); }
	.catalog-empty button,
	.catalog-load-state button { border: 1px solid var(--catalog-border); border-radius: 8px; background: var(--catalog-surface); padding: 8px 16px; color: var(--catalog-text); cursor: pointer; }
	.catalog-empty button:focus-visible,
	.catalog-load-state button:focus-visible { outline: 2px solid var(--catalog-accent); outline-offset: 3px; }

	.catalog-results :deep(.blog-item-collection.is-grid) {
		grid-template-columns: repeat(var(--catalog-columns, 1), minmax(0, 1fr));
		gap: 24px;
		margin-bottom: 46px;
	}

	.catalog-results :deep(.blog-list-card) {
		border: 1px solid var(--catalog-border) !important;
		border-radius: 20px !important;
		background: var(--catalog-canvas) !important;
		color: var(--catalog-text) !important;
	}

	.catalog-results :deep(.blog-item-collection.is-grid .blog-list-card) {
		border-top-width: 1px !important;
	}

	.catalog-results :deep(.blog-list-card:hover) {
		border-color: var(--catalog-border-strong) !important;
		transform: translateY(-2px);
	}

	.catalog-results :deep(.list-card-cover) {
		aspect-ratio: 16 / 9;
		background: var(--catalog-media) !important;
		border: 0 !important;
	}

	.catalog-results :deep(.list-card-cover img) {
		object-fit: contain;
		object-position: center;
	}

	.catalog-results :deep(.list-card-cover-empty) {
		background: var(--catalog-media) !important;
		color: var(--catalog-media-text) !important;
	}

	.catalog-results :deep(.blog-item-collection.is-grid .list-card-main) {
		padding: 24px 24px 16px;
	}

	.catalog-results :deep(.list-card-title),
	.catalog-results :deep(.list-card-title a) {
		color: var(--catalog-text) !important;
		font-family: Georgia, 'Times New Roman', 'Noto Serif SC', serif;
		font-weight: 500 !important;
	}

	.catalog-results :deep(.blog-item-collection.is-grid .list-card-title) {
		font-size: 22px !important;
		line-height: 1.25 !important;
	}

	.catalog-results :deep(.list-card-category) {
		border: 1px solid var(--catalog-border);
		background: transparent !important;
		color: var(--catalog-muted) !important;
	}

	.catalog-results :deep(.list-card-description),
	.catalog-results :deep(.list-card-description *) {
		color: var(--catalog-muted) !important;
	}

	.catalog-results :deep(.blog-item-collection.is-grid .list-card-description) {
		font-size: 15px;
		line-height: 1.58;
	}

	.catalog-results :deep(.blog-item-collection.is-grid .list-author-card) {
		margin: 0 24px 20px;
		border-top-color: var(--catalog-border);
		color: var(--catalog-muted);
	}

	.catalog-results :deep(.list-author-copy strong) { color: var(--catalog-text); }
	.catalog-results :deep(.list-author-copy span),
	.catalog-results :deep(.list-article-meta) { color: var(--catalog-muted); }
	.catalog-results :deep(.list-article-meta) { border-left-color: var(--catalog-border); }
	.catalog-results :deep(.list-article-meta .app-icon) { color: var(--catalog-muted) !important; }

	.catalog-results :deep(.list-author-tag) {
		border: 1px solid var(--catalog-border);
		background: transparent !important;
		color: var(--catalog-muted) !important;
	}

	.catalog-results.is-list-view :deep(.blog-list-card) {
		overflow: hidden;
		padding: 0 !important;
		margin-bottom: 24px !important;
	}

	.catalog-results.is-list-view :deep(.blog-list-grid) { margin: 0 !important; }
	.catalog-results.is-list-view :deep(.list-card-layout) {
		grid-template-columns: minmax(0, 1fr) 240px;
		grid-template-areas: "main cover" "author author";
		gap: 0;
		padding: 0;
	}

	.catalog-results.is-list-view :deep(.without-cover .list-card-layout) { grid-template-columns: minmax(0, 1fr); grid-template-areas: "main" "author"; }
	.catalog-results.is-list-view :deep(.list-card-main) { grid-area: main; min-height: 0; padding: 24px 24px 16px; }
	.catalog-results.is-list-view :deep(.list-card-title) { margin-bottom: 12px !important; }
	.catalog-results.is-list-view :deep(.list-card-category) { margin-bottom: 12px !important; }
	.catalog-results.is-list-view :deep(.list-card-description) { display: -webkit-box; max-height: 4.95em; overflow: hidden; padding: 0 !important; -webkit-box-orient: vertical; -webkit-line-clamp: 3; font-size: 15px; line-height: 1.65; }
	.catalog-results.is-list-view :deep(.list-card-description p) { margin: 0; font-size: inherit; line-height: inherit; }
	.catalog-results.is-list-view :deep(.list-card-aside) {
		display: contents;
	}

	.catalog-results.is-list-view :deep(.list-author-card) {
		grid-area: author;
		width: auto;
		margin: 0 24px 20px;
		padding: 12px 0 0;
		flex-direction: row;
		align-items: center;
		flex-wrap: wrap;
		border: 0;
		border-top: 1px solid var(--catalog-border);
		border-radius: 0;
		background: transparent;
		box-shadow: none;
		backdrop-filter: none;
	}

	.catalog-results.is-list-view :deep(.list-author-avatar) { width: 32px; height: 32px; flex-basis: 32px; }
	.catalog-results.is-list-view :deep(.list-author-copy strong) { font-size: 13px; }
	.catalog-results.is-list-view :deep(.list-author-copy span) { font-size: 11px; }
	.catalog-results.is-list-view :deep(.list-author-identity) { width: auto; }
	.catalog-results.is-list-view :deep(.list-article-meta) { display: flex; flex-wrap: wrap; gap: 8px 16px; width: auto; margin-left: auto; border: 0; padding: 0; }
	.catalog-results.is-list-view :deep(.list-card-cover) { grid-area: cover; align-self: center; width: calc(100% - 24px); margin: 24px 24px 16px 0; border-radius: 8px; }
	.catalog-results.is-list-view :deep(.list-card-action) { flex: 0 0 auto; margin: 0 0 0 12px; padding: 0; justify-content: flex-start; }
	.catalog-results.is-list-view :deep(.list-card-tags) { display: none; }
	.catalog-results.is-list-view :deep(.read-more-button) {
		min-width: 0;
		min-height: 36px;
		margin: 0;
		padding: 8px 12px;
		box-shadow: none;
		border-color: var(--catalog-action) !important;
		background: var(--catalog-action) !important;
		color: var(--catalog-on-action) !important;
	}

	.catalog-results :deep(.pagination-shell) { margin-top: 6px; }
	.catalog-results :deep(.el-pagination.is-background button),
	.catalog-results :deep(.el-pagination.is-background .el-pager li) {
		border: 1px solid var(--catalog-border);
		background: var(--catalog-surface) !important;
		color: var(--catalog-muted);
	}
	.catalog-results :deep(.el-pagination.is-background .el-pager li.is-active) {
		border-color: var(--catalog-action);
		background: var(--catalog-action) !important;
		color: var(--catalog-on-action) !important;
	}

	@media (max-width: 1180px) {
		.catalog-hero { width: calc(100% - 280px); margin-left: 280px; }
		.catalog-workspace { grid-template-columns: 224px minmax(0, 1fr); gap: 40px; }
	}

	@media (max-width: 900px) {
		.catalog-hero { width: 100%; margin-left: 0; padding-block: 48px; }
		.catalog-workspace { grid-template-columns: minmax(0, 1fr); }
		.catalog-sidebar { position: static; }
		.catalog-results.is-list-view :deep(.list-card-layout) { grid-template-columns: minmax(0, 1fr) 200px; }
	}

	@media (max-width: 640px) {
		.article-catalog-page { margin: -1rem; padding: 0 1rem 4rem; }
		.catalog-hero h1 { font-size: 40px; }
		.catalog-toolbar { align-items: stretch; flex-direction: column; }
		.catalog-search { width: 100%; }
		.catalog-view-switch { align-self: flex-end; }
		.catalog-results.is-list-view :deep(.list-card-layout) { grid-template-columns: minmax(0, 1fr); grid-template-areas: "cover" "main" "author"; }
		.catalog-results.is-list-view :deep(.without-cover .list-card-layout) { grid-template-areas: "main" "author"; }
		.catalog-results.is-list-view :deep(.list-card-cover) { width: calc(100% - 48px); margin: 24px 24px 0; }
		.catalog-results.is-list-view :deep(.list-author-identity) { flex: 1 1 calc(100% - 100px); }
		.catalog-results.is-list-view :deep(.list-article-meta) { order: 1; width: 100%; margin: 0; }
	}

	@media (prefers-reduced-motion: reduce) {
		.catalog-filter-link,
		.catalog-results :deep(.blog-list-card) { transition: none !important; }
	}
</style>
