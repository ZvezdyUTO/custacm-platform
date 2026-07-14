<template>
	<div v-if="displayedGroups.length" class="featured-release-groups">
		<section
			v-for="(group, groupIndex) in displayedGroups"
			:key="group.id ?? groupIndex"
			class="featured-release-section"
			:aria-labelledby="groupHeadingId(group, groupIndex)"
		>
			<h2 :id="groupHeadingId(group, groupIndex)">{{ group.title || '精选文章' }}</h2>
			<div class="featured-release-grid">
				<article
					v-for="(blog, blogIndex) in group.articles"
					:key="blog.id"
					class="featured-release-card"
					:class="{'without-image': !blog.firstPicture}"
					:style="{'--featured-order': groupIndex * 3 + blogIndex}"
					role="link"
					tabindex="0"
					@click="toBlog(blog)"
					@keydown.enter.prevent="toBlog(blog)"
					@keydown.space.prevent="toBlog(blog)"
				>
					<div class="featured-release-content">
						<p class="featured-release-category">{{ categoryName(blog) }}</p>
						<h3>{{ blog.title }}</h3>
						<div class="featured-release-description" v-html="summaryHtml(blog)"></div>
						<footer class="featured-release-footer">
							<div class="featured-release-identity">
								<div class="featured-release-author">
									<img
										class="featured-release-avatar"
										:src="blog.authorAvatar || '/img/default-avatar.jpg'"
										:alt="`${authorName(blog)}的头像`"
										loading="lazy"
										decoding="async"
										@error="useDefaultAvatar"
									>
									<span class="featured-release-author-copy">
										<strong>{{ authorName(blog) }}</strong>
										<small v-if="blog.authorUsername">@{{ blog.authorUsername }}</small>
									</span>
								</div>
								<div v-if="blog.tags?.length" class="featured-release-tags" aria-label="文章标签">
									<span
										v-for="tag in visibleTags(blog, blogIndex)"
										:key="tag.id ?? tag.name"
										class="featured-release-tag"
										:style="tagStyle(tag.color)"
									>{{ tag.name }}</span>
									<span v-if="hasHiddenTags(blog, blogIndex)" class="featured-release-tags-more" aria-label="还有更多标签">…</span>
								</div>
							</div>
							<div class="featured-release-date">
								<time :datetime="blog.createTime">{{ $filters.dateFormat(blog.createTime, 'YYYY-MM-DD') }}</time>
								<span class="featured-release-arrow" aria-hidden="true">→</span>
							</div>
						</footer>
					</div>
					<figure v-if="blog.firstPicture" class="featured-release-media">
						<img :src="blog.firstPicture" :alt="`${blog.title} 首图`" loading="lazy" decoding="async">
					</figure>
				</article>
			</div>
		</section>
	</div>
	<p v-else class="featured-empty">暂时还没有可以展示的文章。</p>
</template>

<script>
	import {sanitizeHtml} from '@/util/sanitizeHtml'

	// Author: huangbingrui.awa
	export default {
		name: 'FeaturedBlog',
		props: { featuredGroups: { type: Array, default: () => [] } },
		computed: {
			displayedGroups() {
				return this.featuredGroups.slice(0, 3).map(group => ({
					...group,
					articles: Array.isArray(group?.articles) ? group.articles.slice(0, 3) : [],
				})).filter(group => group.articles.length)
			},
		},
		methods: {
			groupHeadingId(group, index) {
				const key = String(group?.id ?? index + 1).replace(/[^a-zA-Z0-9_-]/g, '-')
				return `featured-release-title-${key}`
			},
			categoryName(blog) { return blog?.categoryName || blog?.category?.name || '未分类' },
			authorName(blog) { return blog?.authorNickname || blog?.authorUsername || '已注销用户' },
			visibleTags(blog, blogIndex) { return (blog?.tags || []).slice(0, blogIndex === 0 ? 5 : 3) },
			hasHiddenTags(blog, blogIndex) { return (blog?.tags?.length || 0) > (blogIndex === 0 ? 5 : 3) },
			tagStyle(color) { return {backgroundColor: color || '#8B1E3F', color: '#fff'} },
			summaryHtml(blog) {
				const description = blog?.description?.trim()
				return description ? sanitizeHtml(description) : '<p>这篇文章暂时没有填写简介。</p>'
			},
			toBlog(blog) { this.$store.dispatch('goBlogPage', blog) },
			useDefaultAvatar(event) {
				const image = event.currentTarget
				if (image && !image.src.endsWith('/img/default-avatar.jpg')) image.src = '/img/default-avatar.jpg'
			},
		},
	}
</script>

<style scoped>
	.featured-release-groups { display: grid; gap: clamp(2.5rem, 5vw, 5rem); }
	.featured-release-section { margin: 1.5rem 0 2rem; }
	.featured-release-section > h2 { margin: 0 0 1.5rem; color: var(--home-text, #050505); font-family: system-ui, -apple-system, BlinkMacSystemFont, sans-serif; font-size: clamp(2rem, 3vw, 2.5rem); font-weight: 600; line-height: 1.1; letter-spacing: -.01em; }
	.featured-release-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1.5rem; }
	.featured-release-card { display: flex; min-width: 0; overflow: hidden; flex-direction: column; cursor: pointer; background: var(--home-surface, #f7f7f5); border: 1px solid var(--home-border, #e9e9e7); border-radius: 18px; color: var(--home-text, #050505); box-shadow: none; animation: featured-release-enter 520ms cubic-bezier(.2, .75, .25, 1) both; animation-delay: calc(var(--featured-order) * 65ms); transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease, background-color 180ms ease; }
	.featured-release-card:first-child { display: grid; grid-column: 1 / -1; grid-template-columns: minmax(300px, .8fr) minmax(0, 1.2fr); align-items: center; background: var(--home-surface, #f7f7f5); border-color: var(--home-border, #e9e9e7); }
	.featured-release-card:first-child.without-image { grid-template-columns: 1fr; }
	.featured-release-card:hover { border-color: var(--home-border-strong, #d3d3d1); background: var(--home-surface-hover, #f1f1ef); transform: translateY(-4px); box-shadow: 0 16px 34px rgb(15 15 15 / 8%); }
	.featured-release-card:active { transform: translateY(-1px) scale(.995); }
	.featured-release-card:focus-visible { outline: 2px solid var(--home-action, #2383e2); outline-offset: 3px; }
	.featured-release-content { display: flex; min-width: 0; padding: 2rem; flex: 1 1 auto; flex-direction: column; }
	.featured-release-category { margin: 0 0 .75rem; color: var(--home-muted, #787774); font-family: system-ui, -apple-system, BlinkMacSystemFont, sans-serif; font-size: .8125rem; font-weight: 650; letter-spacing: .01em; }
	.featured-release-card h3 { display: -webkit-box; margin: 0 0 1rem; overflow: hidden; -webkit-box-orient: vertical; -webkit-line-clamp: 2; line-clamp: 2; font-family: system-ui, -apple-system, BlinkMacSystemFont, sans-serif; font-size: clamp(1.8rem, 2.6vw, 2.5rem); font-weight: 600; line-height: 1.1; letter-spacing: -.01em; overflow-wrap: anywhere; text-overflow: ellipsis; }
	.featured-release-card:first-child h3 { -webkit-line-clamp: 3; line-clamp: 3; font-size: clamp(2.1rem, 3.2vw, 3rem); }
	.featured-release-description { display: -webkit-box; overflow: hidden; -webkit-box-orient: vertical; -webkit-line-clamp: 3; line-clamp: 3; color: var(--home-text-soft, #37352f); font-family: system-ui, -apple-system, BlinkMacSystemFont, sans-serif; font-size: 1.0625rem; font-weight: 400; line-height: 1.47; letter-spacing: -.022em; overflow-wrap: anywhere; text-overflow: ellipsis; }
	.featured-release-description :deep(*) { display: inline; margin: 0; color: inherit; font: inherit; }
	.featured-release-footer { display: flex; min-width: 0; margin: auto 0 0; padding-top: 2rem; align-items: flex-end; justify-content: space-between; gap: 1rem; color: var(--home-muted, #787774); font-family: system-ui, -apple-system, BlinkMacSystemFont, sans-serif; }
	.featured-release-identity { display: grid; min-width: 0; gap: .8rem; }
	.featured-release-author { display: flex; min-width: 0; align-items: center; gap: .7rem; }
	.featured-release-avatar { display: block; width: 2rem; height: 2rem; flex: 0 0 2rem; border-radius: 50%; object-fit: cover; }
	.featured-release-author-copy { display: grid; min-width: 0; line-height: 1.15; }
	.featured-release-author-copy strong, .featured-release-author-copy small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
	.featured-release-author-copy strong { color: var(--home-text-soft, #37352f); font-size: .8125rem; font-weight: 650; }
	.featured-release-author-copy small { margin-top: .2rem; color: var(--home-muted, #787774); font-size: .6875rem; }
	.featured-release-tags { display: flex; min-width: 0; overflow: hidden; align-items: center; gap: .4rem; }
	.featured-release-tag { display: block; max-width: 7rem; overflow: hidden; padding: .28rem .55rem; border: 1px solid rgb(255 255 255 / 24%); border-radius: 7px; color: #fff; font-size: .6875rem; font-weight: 650; line-height: 1.2; text-overflow: ellipsis; white-space: nowrap; }
	.featured-release-tags-more { flex: 0 0 auto; color: var(--home-muted, #787774); font-size: 1rem; font-weight: 700; line-height: 1; }
	.featured-release-date { display: flex; flex: 0 0 auto; align-items: center; gap: .8rem; font-size: .8125rem; font-variant-numeric: tabular-nums; }
	.featured-release-arrow { display: grid; width: 2rem; height: 2rem; place-items: center; border-radius: 50%; background: var(--home-text, #050505); color: #fff; font-size: 1.1rem; line-height: 1; transition: transform 180ms ease, background-color 180ms ease; }
	.featured-release-card:hover .featured-release-arrow, .featured-release-card:focus-visible .featured-release-arrow { transform: translateX(3px); background: var(--home-action, #2383e2); }
	.featured-release-media { width: 100%; aspect-ratio: 16 / 9; margin: 0; overflow: hidden; align-self: center; background: var(--home-media, #eeecea); }
	.featured-release-media img { display: block; width: 100%; height: 100%; object-fit: contain; }
	.featured-empty { margin: 1.5rem 0 2rem; padding: 1.5rem; color: var(--color-text-muted); background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 18px; text-align: center; }
	@keyframes featured-release-enter { from { opacity: 0; transform: translateY(14px); } to { opacity: 1; transform: translateY(0); } }
	@media (max-width: 900px) { .featured-release-grid { grid-template-columns: 1fr; } .featured-release-card:first-child { display: flex; width: 100%; grid-column: auto; } }
	@media (max-width: 640px) { .featured-release-content { padding: 1.5rem; } .featured-release-footer { align-items: flex-end; } .featured-release-card h3, .featured-release-card:first-child h3 { font-size: 2rem; } }
	@media (prefers-reduced-motion: reduce) { .featured-release-card { animation: none; transition: none; } .featured-release-card:hover, .featured-release-card:active { transform: none; box-shadow: none; } .featured-release-arrow { transition: none; } .featured-release-card:hover .featured-release-arrow, .featured-release-card:focus-visible .featured-release-arrow { transform: none; } }
</style>
