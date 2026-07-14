<template>
	<div class="blog-item-collection" :class="{'is-grid': layout === 'grid'}">
		<article class="content-panel m-padded-tb-large m-margin-bottom-big m-box blog-list-card" v-for="item in blogList" :key="item.id" :style="cardStyle(item)">
			<a v-if="layout === 'grid'" class="list-card-hit-area" :href="`/blog/${item.id}`" :aria-label="`阅读文章：${item.title}`" @click.prevent="toBlog(item)"></a>
			<div class="featured-corner-mark" v-if="item.top" aria-label="置顶文章">
				<AppIcon name="arrow-up-circle" />
			</div>
			<div class="blog-list-content">
				<div class="blog-list-grid m-margin-lr">
					<div class="list-card-layout">
						<div class="list-card-main">
							<h2 class="list-card-title m-scaleup">
								<a href="javascript:;" :title="item.title" @click.prevent="toBlog(item)" class="m-black">{{ item.title }}</a>
							</h2>
							<router-link :to="`/category/${item.category.name}`" class="category-ribbon list-card-category" :style="taxonomyStyle(item.category.color)">
								<AppIcon name="folder" /><span class="m-text-500">{{ item.category.name }}</span>
							</router-link>
							<div class="typo line-numbers match-braces rainbow-braces list-card-description" v-lazy-container="{selector: 'img'}" v-viewer v-html="sanitizeHtml(item.description)"></div>
							<div v-if="layout !== 'grid'" class="list-card-action">
								<a href="javascript:;" @click.prevent="toBlog(item)" class="read-more-button">阅读全文</a>
							</div>
						</div>
						<aside class="list-card-aside" aria-label="文章作者与首图">
							<div class="list-author-card">
								<div class="list-author-identity">
									<img class="list-author-avatar" :src="item.authorAvatar || '/img/default-avatar.jpg'" :alt="`${item.authorNickname || '文章作者'}的头像`" @error="useDefaultAvatar">
									<div class="list-author-copy">
										<strong>{{ item.authorNickname || '已注销用户' }}</strong>
										<span v-if="item.authorUsername">@{{ item.authorUsername }}</span>
									</div>
								</div>
								<div v-if="layout === 'grid' && item.tags.length" class="list-author-tags" aria-label="文章标签">
									<router-link :to="`/tag/${tag.name}`" class="taxonomy-chip list-author-tag" :style="taxonomyStyle(tag.color)" v-for="(tag,index) in item.tags" :key="index">{{ tag.name }}</router-link>
									<span class="list-author-tags-more" aria-label="还有更多标签" title="还有更多标签" hidden>…</span>
								</div>
								<div class="list-article-meta" aria-label="文章信息">
									<span class="list-article-date"><AppIcon name="calendar" />{{ $filters.dateFormat(item.createTime, 'YYYY-MM-DD')}}</span>
									<span class="list-article-stats">
										<span><AppIcon name="file" />{{ item.words }} 字</span>
									</span>
								</div>
							</div>
							<figure v-if="item.firstPicture" class="list-card-cover">
								<img :src="item.firstPicture" :alt="`${item.title} 首图`" loading="lazy" decoding="async">
							</figure>
							<figure v-else-if="layout === 'grid'" class="list-card-cover list-card-cover-empty" :aria-label="`${item.title} 暂无首图`">
								<div><AppIcon name="file" :size="28" /><span>暂无首图</span></div>
							</figure>
						</aside>
					</div>
					<template v-if="layout !== 'grid'">
						<!--横线-->
						<div class="section-divider m-margin-lr-no"></div>
						<!--标签-->
						<div class="row m-padded-tb-no list-card-tags">
							<div class="column m-padding-left-no">
								<router-link :to="`/tag/${tag.name}`" class="taxonomy-chip m-text-500 m-margin-small" :style="taxonomyStyle(tag.color)" v-for="(tag,index) in item.tags" :key="index">{{ tag.name }}</router-link>
							</div>
						</div>
					</template>
				</div>
			</div>
		</article>
	</div>
</template>

<script>
	import {sanitizeHtml} from '@/util/sanitizeHtml'

	export function countVisibleTags(widths, availableWidth, ellipsisWidth, gap) {
		const totalWidth = widths.reduce((total, width) => total + width, 0) + Math.max(0, widths.length - 1) * gap
		if (totalWidth <= availableWidth) return {count: widths.length, overflow: false}

		const tagBudget = Math.max(0, availableWidth - ellipsisWidth - gap)
		let usedWidth = 0
		let count = 0
		for (const width of widths) {
			const nextWidth = usedWidth + (count > 0 ? gap : 0) + width
			if (nextWidth > tagBudget) break
			usedWidth = nextWidth
			count += 1
		}
		return {count, overflow: true}
	}

	export default {
		name: "BlogItem",
		props: {
			blogList: {
				type: Array,
				required: true
			},
			layout: {
				type: String,
				default: 'list',
				validator: value => ['list', 'grid'].includes(value)
			}
		},
		methods: {
			sanitizeHtml,
			measureTagRow(row) {
				const tags = [...row.querySelectorAll('.list-author-tag')]
				const more = row.querySelector('.list-author-tags-more')
				if (!tags.length || !more) return

				tags.forEach(tag => { tag.hidden = false })
				more.hidden = false
				const availableWidth = row.clientWidth
				if (!availableWidth) {
					more.hidden = true
					return
				}

				const rowStyle = window.getComputedStyle(row)
				const gap = Number.parseFloat(rowStyle.columnGap || rowStyle.gap) || 0
				const widths = tags.map(tag => tag.getBoundingClientRect().width)
				const ellipsisWidth = more.getBoundingClientRect().width
				const result = countVisibleTags(widths, availableWidth, ellipsisWidth, gap)
				tags.forEach((tag, index) => { tag.hidden = index >= result.count })
				more.hidden = !result.overflow
			},
			observeTagRows() {
				const rows = [...this.$el.querySelectorAll('.list-author-tags')]
				if (this.tagResizeObserver) this.tagResizeObserver.disconnect()
				rows.forEach(row => {
					this.measureTagRow(row)
					if (this.tagResizeObserver) this.tagResizeObserver.observe(row)
				})
			},
			taxonomyStyle(color) { return {backgroundColor: color || '#8B1E3F', color: '#fff'} },
			cardStyle(item) { return {'--category-color': item.category?.color || '#17324d'} },
			useDefaultAvatar(event) {
				if (!event.target.src.endsWith('/img/default-avatar.jpg')) event.target.src = '/img/default-avatar.jpg'
			},
			toBlog(blog) {
				this.$store.dispatch('goBlogPage', blog)
			}
		},
		mounted() {
			if (typeof ResizeObserver !== 'undefined') {
				this.tagResizeObserver = new ResizeObserver(entries => {
					entries.forEach(entry => this.measureTagRow(entry.target))
				})
			}
			this.$nextTick(this.observeTagRows)
		},
		updated() {
			this.$nextTick(this.observeTagRows)
		},
		beforeUnmount() {
			if (this.tagResizeObserver) this.tagResizeObserver.disconnect()
		}
	}
</script>

<style scoped>
	.blog-item-collection.is-grid {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		align-items: stretch;
		gap: 1.5rem;
		margin-bottom: 3rem;
	}

	.blog-list-card {
		container-type: inline-size;
	}

	.blog-item-collection.is-grid .blog-list-card {
		display: flex;
		min-width: 0;
		height: 100%;
		margin-bottom: 0 !important;
		overflow: hidden;
		padding: 0 !important;
		border-top: 4px solid var(--category-color);
	}

	.list-card-hit-area {
		position: absolute;
		inset: 0;
		z-index: 1;
		border-radius: inherit;
		cursor: pointer;
	}

	.list-card-hit-area:focus-visible {
		outline: 3px solid var(--color-focus-ring, #0071e3);
		outline-offset: -4px;
	}

	.blog-item-collection.is-grid .list-card-title a,
	.blog-item-collection.is-grid .list-card-category,
	.blog-item-collection.is-grid .list-author-tag {
		position: relative;
		z-index: 2;
	}

	.blog-item-collection.is-grid .featured-corner-mark {
		pointer-events: none;
	}

	.blog-item-collection.is-grid .blog-list-content,
	.blog-item-collection.is-grid .blog-list-grid {
		width: 100%;
		min-width: 0;
		height: 100%;
	}

	.blog-item-collection.is-grid .blog-list-grid {
		display: flex;
		margin: 0 !important;
		flex-direction: column;
	}

	.list-card-layout {
		display: grid;
		width: 100%;
		grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
		align-items: start;
		gap: 1.75rem;
		padding: 0.5rem 0 1.25rem;
	}

	.list-card-main,
	.list-card-aside {
		min-width: 0;
	}

	.list-card-main {
		display: flex;
		min-height: 100%;
		flex-direction: column;
		align-items: stretch;
	}

	.list-card-aside {
		display: grid;
		gap: 0.75rem;
	}

	.list-card-title {
		min-width: 0;
		margin: 0 0 1.25rem !important;
		font-size: clamp(1.35rem, 1.5vw, 1.75rem) !important;
		line-height: 1.25 !important;
		letter-spacing: -0.015em;
		text-align: left;
	}

	.list-card-title a {
		display: -webkit-box;
		overflow: hidden;
		-webkit-box-orient: vertical;
		-webkit-line-clamp: 3;
		line-clamp: 3;
		max-height: 3.6em;
		text-overflow: ellipsis;
		word-break: break-word;
	}

	.list-author-card {
		display: flex;
		align-items: center;
		width: 100%;
		min-width: 0;
		gap: 0.75rem;
		padding: 0.9rem 0.75rem 0.9rem 1rem;
		background: rgba(255, 255, 255, 0.68);
		border: 1px solid rgba(23, 50, 77, 0.16);
		border-radius: 14px;
		box-shadow: 0 10px 28px rgba(23, 50, 77, 0.06);
		-webkit-backdrop-filter: blur(10px);
		backdrop-filter: blur(10px);
	}

	.list-author-identity {
		display: flex;
		align-items: center;
		min-width: 0;
		flex: 1 1 auto;
		gap: 0.9rem;
	}

	.list-author-avatar {
		width: 72px;
		height: 72px;
		flex: 0 0 72px;
		object-fit: cover;
		border: 2px solid rgba(255, 255, 255, 0.86);
		border-radius: 50%;
		box-shadow: 0 0 0 1px rgba(23, 50, 77, 0.1), 0 6px 18px rgba(23, 50, 77, 0.1);
	}

	.list-author-copy {
		display: flex;
		min-width: 0;
		flex-direction: column;
		gap: 0.12rem;
	}

	.list-author-copy strong {
		overflow: hidden;
		color: #171d24;
		font-size: 1rem;
		font-weight: 800;
		line-height: 1.25;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.list-author-copy span {
		overflow: hidden;
		color: #718096;
		font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
		font-size: 0.78rem;
		line-height: 1.25;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.list-article-meta {
		display: grid;
		min-width: 0;
		flex: 0 0 auto;
		justify-items: start;
		margin-left: auto;
		gap: 0.3rem;
		padding-left: 0.7rem;
		border-left: 1px solid rgba(23, 50, 77, 0.35);
		color: #526476;
		font-size: 0.78rem;
		text-align: left;
	}

	.list-article-date {
		font-weight: 700;
	}

	.list-article-stats {
		display: flex;
		align-items: center;
		gap: 0.85rem;
	}

	.list-card-category {
		align-self: flex-start;
		margin-bottom: 1.25rem !important;
	}

	.list-article-date,
	.list-article-stats > span {
		display: inline-flex;
		align-items: center;
		gap: 0.32rem;
		white-space: nowrap;
	}

	.list-article-meta .app-icon {
		display: inline-flex !important;
		width: 1em;
		height: 1em;
		align-items: center;
		justify-content: center;
		margin: 0 !important;
		color: #17324d;
		line-height: 1 !important;
		vertical-align: middle !important;
		transform: translateY(-1px);
	}

	.read-more-button {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		min-width: 7.5rem;
		min-height: 2.75rem;
		margin: 0 auto;
		padding: 0.65rem 1.25rem;
		color: #fff !important;
		font-size: 0.9rem;
		font-weight: 700;
		letter-spacing: 0.02em;
		background: #17324d;
		border: 1px solid #17324d;
		border-radius: 999px;
		box-shadow: 0 6px 14px rgba(23, 50, 77, 0.18);
		transition: background-color 160ms ease, border-color 160ms ease, transform 160ms ease;
	}

	.read-more-button:hover,
	.read-more-button:focus-visible {
		color: #fff !important;
		background: #244d72;
		border-color: #244d72;
		outline: none;
		transform: translateY(-1px);
	}

	.list-card-description {
		min-width: 0;
		padding: 0.25rem 0 0.75rem !important;
	}

	.list-card-cover {
		box-sizing: border-box;
		width: 100%;
		aspect-ratio: 16 / 9;
		margin: 0;
		overflow: hidden;
		background: #edf1f4;
		border: 2px solid #17324d;
		border-radius: 14px;
	}

	.list-card-cover img {
		display: block;
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.list-card-action {
		display: flex;
		margin-top: auto;
		justify-content: center;
		padding: 0.75rem 0 0.2rem;
	}

	.blog-item-collection.is-grid .list-card-layout {
		display: grid;
		min-height: 0;
		flex: 1 1 auto;
		grid-template-areas:
			"cover"
			"main"
			"author";
		grid-template-columns: minmax(0, 1fr);
		grid-template-rows: auto minmax(0, 1fr) auto;
		gap: 0;
		padding: 0;
	}

	.blog-item-collection.is-grid .list-card-main {
		grid-area: main;
		padding: 1.35rem 1.5rem 1.15rem;
	}

	.blog-item-collection.is-grid .list-card-aside {
		display: contents;
	}

	.blog-item-collection.is-grid .list-card-cover {
		grid-area: cover;
		border: 0;
		border-radius: 0;
	}

	.blog-item-collection.is-grid .list-card-cover-empty {
		display: grid;
		place-items: center;
		background:
			linear-gradient(135deg, color-mix(in srgb, var(--category-color) 10%, transparent), transparent 58%),
			var(--color-surface-subtle, #fafafc);
		color: var(--color-text-faint, #7a7a80);
	}

	.blog-item-collection.is-grid .list-card-cover-empty > div {
		display: inline-flex;
		align-items: center;
		flex-direction: column;
		gap: 0.55rem;
		font-size: 0.78rem;
		font-weight: 700;
		letter-spacing: 0.08em;
	}

	.blog-item-collection.is-grid .list-card-category {
		order: 1;
		margin: 0 0 0.85rem !important;
		padding: 0.45rem 0.72rem;
		border-radius: 999px;
		font-size: 0.78rem;
	}

	.blog-item-collection.is-grid .list-card-title {
		order: 2;
		margin: 0 0 0.85rem !important;
		font-size: clamp(1.3rem, 1.45vw, 1.65rem) !important;
	}

	.blog-item-collection.is-grid .list-card-title a {
		-webkit-line-clamp: 2;
		line-clamp: 2;
		max-height: 2.5em;
	}

	.blog-item-collection.is-grid .list-card-description {
		order: 3;
		max-height: 4.8em;
		overflow: hidden;
		padding: 0 !important;
		color: var(--color-text-muted, #606066);
		font-size: 0.92rem;
		line-height: 1.6;
	}

	.blog-item-collection.is-grid .list-card-description :deep(> :first-child) {
		margin-top: 0;
	}

	.blog-item-collection.is-grid .list-card-description :deep(> :last-child) {
		margin-bottom: 0;
	}

	.blog-item-collection.is-grid .list-author-card {
		grid-area: author;
		width: auto;
		min-width: 0;
		margin: 0 1.5rem 1.25rem;
		padding: 0.75rem 0;
		background: transparent;
		border: 0;
		border-top: 1px solid var(--color-border, #d9d9de);
		border-radius: 0;
		box-shadow: none;
		-webkit-backdrop-filter: none;
		backdrop-filter: none;
	}

	.blog-item-collection.is-grid .list-author-identity {
		flex: 0 1 42%;
	}

	.blog-item-collection.is-grid .list-author-tags {
		display: flex;
		min-width: 0;
		flex: 1 1 auto;
		align-items: center;
		gap: 0.35rem;
		overflow: hidden;
	}

	.blog-item-collection.is-grid .list-author-tag {
		min-width: max-content;
		max-width: 7rem;
		flex: 0 0 auto;
		overflow: hidden;
		padding: 0.42em 0.65em;
		font-size: 0.7rem;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.blog-item-collection.is-grid .list-author-tags-more {
		display: inline-flex;
		min-width: 1.5rem;
		min-height: 1.75rem;
		flex: 0 0 1.5rem;
		align-items: center;
		justify-content: center;
		color: var(--catalog-muted, #87867f);
		font-size: 1rem;
		font-weight: 700;
		line-height: 1;
	}

	.blog-item-collection.is-grid .list-author-tag[hidden],
	.blog-item-collection.is-grid .list-author-tags-more[hidden] {
		display: none;
	}

	.blog-item-collection.is-grid .list-author-avatar {
		width: 44px;
		height: 44px;
		flex-basis: 44px;
	}

	.blog-item-collection.is-grid .list-author-copy strong {
		font-size: 0.9rem;
	}

	.blog-item-collection.is-grid .list-author-copy span,
	.blog-item-collection.is-grid .list-article-meta {
		font-size: 0.72rem;
	}

	@media (max-width: 900px) {
		.blog-item-collection.is-grid {
			grid-template-columns: minmax(0, 1fr);
		}
	}

	@container (max-width: 900px) {
		.list-card-layout { gap: 1rem; }
		.list-author-card { gap: 0.65rem; }
		.list-author-avatar { width: 64px; height: 64px; flex-basis: 64px; }
		.list-article-meta { padding-left: 0.55rem; font-size: 0.74rem; }
	}

	@container (max-width: 560px) {
		.list-card-layout { grid-template-columns: minmax(0, 1fr); }
		.list-card-aside { grid-row: 2; }
		.list-card-cover { max-width: 100%; }
		.list-author-card { align-items: flex-start; flex-direction: column; gap: 0.75rem; }
		.list-author-identity { width: 100%; }
		.blog-item-collection.is-grid .list-author-tags { width: 100%; }
		.list-article-meta {
			width: 100%;
			padding: 0.75rem 0 0;
			border-top: 1px solid rgba(23, 50, 77, 0.25);
			border-left: 0;
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.read-more-button { transition: none; }
	}

</style>
