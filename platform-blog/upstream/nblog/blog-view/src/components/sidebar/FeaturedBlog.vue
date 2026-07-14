<template>
	<section class="featured-release-section" aria-labelledby="featured-release-title">
		<h2 id="featured-release-title">精选文章</h2>
		<div v-if="displayedBlogs.length" class="featured-release-grid">
			<article
				v-for="blog in displayedBlogs"
				:key="blog.id"
				class="featured-release-card"
				:class="{'without-image': !blog.firstPicture}"
				role="link"
				tabindex="0"
				@click="toBlog(blog)"
				@keydown.enter.prevent="toBlog(blog)"
				@keydown.space.prevent="toBlog(blog)"
			>
				<div class="featured-release-content">
					<h3>{{ blog.title }}</h3>
					<div class="featured-release-description" v-html="summaryHtml(blog)"></div>
					<p class="featured-release-meta">
						<time :datetime="blog.createTime">{{ $filters.dateFormat(blog.createTime, 'YYYY-MM-DD') }}</time>
						<span>{{ blog.categoryName || '未分类' }}</span>
					</p>
				</div>
				<figure v-if="blog.firstPicture" class="featured-release-media">
					<img :src="blog.firstPicture" :alt="`${blog.title} 首图`" loading="lazy" decoding="async">
				</figure>
			</article>
		</div>
		<p v-else class="featured-empty">暂时还没有可以展示的文章。</p>
	</section>
</template>

<script>
	import {sanitizeHtml} from '@/util/sanitizeHtml'

	// Author: huangbingrui.awa
	export default {
		name: 'FeaturedBlog',
		props: { featuredBlogList: { type: Array, default: () => [] } },
		computed: {
			displayedBlogs() { return this.featuredBlogList.slice(0, 3) },
		},
		methods: {
			summaryHtml(blog) {
				const description = blog?.description?.trim()
				return description ? sanitizeHtml(description) : '<p>这篇文章暂时没有填写简介。</p>'
			},
			toBlog(blog) { this.$store.dispatch('goBlogPage', blog) },
		},
	}
</script>

<style scoped>
	.featured-release-section { margin: 1.5rem 0 2rem; }
	.featured-release-section > h2 { margin: 0 0 1.5rem; color: #1d1d1f; font-family: system-ui, -apple-system, BlinkMacSystemFont, sans-serif; font-size: clamp(2rem, 3vw, 2.5rem); font-weight: 600; line-height: 1.1; letter-spacing: -.01em; }
	.featured-release-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1.5rem; }
	.featured-release-card { display: flex; height: 22rem; overflow: hidden; flex-direction: column; cursor: pointer; background: #eadfce; border: 1px solid #eadfce; border-radius: 18px; color: #171715; box-shadow: none; transition: transform 160ms ease; }
	.featured-release-card:first-child { display: grid; height: 24rem; grid-column: 1 / -1; grid-template-columns: minmax(300px, .8fr) minmax(0, 1.2fr); background: #eadfce; border-color: #eadfce; }
	.featured-release-card:first-child.without-image { grid-template-columns: 1fr; }
	.featured-release-card:active { transform: scale(.99); }
	.featured-release-card:focus-visible { outline: 2px solid #8a5f24; outline-offset: 3px; }
	.featured-release-content { display: flex; min-width: 0; min-height: 0; padding: 2rem; flex: 1 1 56%; flex-direction: column; }
	.featured-release-card h3 { display: -webkit-box; margin: 0 0 1rem; overflow: hidden; -webkit-box-orient: vertical; -webkit-line-clamp: 2; line-clamp: 2; font-family: system-ui, -apple-system, BlinkMacSystemFont, sans-serif; font-size: clamp(1.8rem, 2.6vw, 2.5rem); font-weight: 600; line-height: 1.1; letter-spacing: -.01em; }
	.featured-release-card:first-child h3 { font-size: clamp(2.1rem, 3.2vw, 3rem); }
	.featured-release-description { display: -webkit-box; overflow: hidden; -webkit-box-orient: vertical; -webkit-line-clamp: 3; line-clamp: 3; color: #332d25; font-family: system-ui, -apple-system, BlinkMacSystemFont, sans-serif; font-size: 1.0625rem; font-weight: 400; line-height: 1.47; letter-spacing: -.022em; }
	.featured-release-description :deep(*) { margin: 0; color: inherit; font: inherit; }
	.featured-release-meta { display: flex; margin: auto 0 0; align-items: center; justify-content: flex-end; gap: 1rem; color: #6e5d46; font-family: system-ui, -apple-system, BlinkMacSystemFont, sans-serif; font-size: .875rem; font-weight: 400; line-height: 1.43; letter-spacing: -.016em; text-align: right; }
	.featured-release-meta span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
	.featured-release-media { min-height: 0; margin: 0; overflow: hidden; flex: 1 1 44%; background: #ded0bd; }
	.featured-release-media img { display: block; width: 100%; height: 100%; object-fit: cover; }
	.featured-release-card:first-child .featured-release-media { min-height: 100%; }
	.featured-empty { margin: 0; padding: 1.5rem; color: var(--color-text-muted); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 18px; text-align: center; }
	:global(html[data-theme="dark"] .featured-release-section > h2) { color: #ffffff; }
	:global(html[data-theme="dark"] .featured-release-card) { color: #f6efe3; background: #2c2723; border-color: #2c2723; }
	:global(html[data-theme="dark"] .featured-release-description) { color: #dbcdb8; }
	:global(html[data-theme="dark"] .featured-release-meta) { color: #c4b397; }
	:global(html[data-theme="dark"] .featured-release-media) { background: #39322c; }
	:global(html[data-theme="dark"] .featured-release-card:focus-visible) { outline-color: #d4a85f; }
	@media (max-width: 900px) { .featured-release-grid { grid-template-columns: 1fr; } .featured-release-card, .featured-release-card:first-child { display: flex; width: 100%; height: 22rem; grid-column: auto; } .featured-release-card:first-child .featured-release-media { min-height: 0; } }
	@media (max-width: 640px) { .featured-release-card, .featured-release-card:first-child { height: auto; min-height: 0; } .featured-release-content { min-height: 15rem; padding: 1.5rem; } .featured-release-media, .featured-release-card:first-child .featured-release-media { min-height: 12rem; } .featured-release-card h3, .featured-release-card:first-child h3 { font-size: 2rem; } }
	@media (prefers-reduced-motion: reduce) { .featured-release-card { transition: none; } .featured-release-card:active { transform: none; } }
</style>
