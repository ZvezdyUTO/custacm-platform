<template>
	<div>
		<div class="ui padded attached segment m-padded-tb-large m-margin-bottom-big m-box blog-list-card" v-for="item in blogList" :key="item.id">
			<div class="ui large red right corner label" v-if="item.top">
				<i class="arrow alternate circle up icon"></i>
			</div>
			<div class="ui middle aligned mobile reversed stackable">
				<div class="ui grid m-margin-lr">
					<div class="list-card-layout">
						<div class="list-card-main">
							<h2 class="ui header list-card-title m-scaleup">
								<a href="javascript:;" :title="item.title" @click.prevent="toBlog(item)" class="m-black">{{ item.title }}</a>
							</h2>
							<router-link :to="`/category/${item.category.name}`" class="ui large ribbon label list-card-category" :style="taxonomyStyle(item.category.color)">
								<i class="small folder open icon"></i><span class="m-text-500">{{ item.category.name }}</span>
							</router-link>
							<div class="typo line-numbers match-braces rainbow-braces list-card-description" v-lazy-container="{selector: 'img'}" v-viewer v-html="sanitizeHtml(item.description)"></div>
							<div class="list-card-action">
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
								<div class="list-article-meta" aria-label="文章信息">
									<span><i class="calendar outline icon"></i>{{ $filters.dateFormat(item.createTime, 'YYYY-MM-DD')}}</span>
									<span><i class="file alternate outline icon"></i>{{ item.words }} 字</span>
								</div>
								</div>
							<figure v-if="item.firstPicture" class="list-card-cover">
								<img :src="item.firstPicture" :alt="`${item.title} 首图`" loading="lazy" decoding="async">
							</figure>
						</aside>
					</div>
					<!--横线-->
					<div class="ui section divider m-margin-lr-no"></div>
					<!--标签-->
					<div class="row m-padded-tb-no">
						<div class="column m-padding-left-no">
							<router-link :to="`/tag/${tag.name}`" class="ui tag label m-text-500 m-margin-small" :style="taxonomyStyle(tag.color)" v-for="(tag,index) in item.tags" :key="index">{{ tag.name }}</router-link>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</template>

<script>
	import {sanitizeHtml} from '@/util/sanitizeHtml'
	export default {
		name: "BlogItem",
		props: {
			blogList: {
				type: Array,
				required: true
			}
		},
		methods: {
			sanitizeHtml,
			taxonomyStyle(color) { return {backgroundColor: color || '#8B1E3F', color: '#fff'} },
			useDefaultAvatar(event) {
				if (!event.target.src.endsWith('/img/default-avatar.jpg')) event.target.src = '/img/default-avatar.jpg'
			},
			toBlog(blog) {
				this.$store.dispatch('goBlogPage', blog)
			}
		}
	}
</script>

<style scoped>
	.blog-list-card {
		container-type: inline-size;
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
		gap: 1rem;
		padding: 0.7rem 0.85rem;
		background: transparent;
		border: 2px solid #17324d;
		border-radius: 14px;
		box-shadow: none;
	}

	.list-author-identity {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		min-width: 135px;
	}

	.list-author-avatar {
		width: 48px;
		height: 48px;
		flex: 0 0 48px;
		object-fit: cover;
		border: 2px solid #fff;
		border-radius: 50%;
		box-shadow: 0 0 0 1px #d5e0e9, 0 5px 12px rgba(23, 50, 77, 0.13);
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
		display: flex;
		align-items: center;
		gap: 0.75rem;
		padding-left: 1rem;
		border-left: 1px solid rgba(23, 50, 77, 0.35);
		color: #526476;
		font-size: 0.84rem;
	}

	.list-card-category {
		align-self: flex-start;
		margin-bottom: 1.25rem !important;
	}

	.list-article-meta span {
		display: inline-flex;
		align-items: center;
		gap: 0.32rem;
		white-space: nowrap;
	}

	.list-article-meta i {
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
		width: 100%;
		aspect-ratio: 16 / 9;
		margin: 0;
		overflow: hidden;
		background: #edf1f4;
		border: 1px solid #d8e0e6;
		border-radius: 6px;
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

	@container (max-width: 900px) {
		.list-card-layout { gap: 1rem; }
		.list-author-card { align-items: flex-start; flex-direction: column; gap: 0.65rem; }
		.list-article-meta {
			width: 100%;
			flex-wrap: wrap;
			gap: 0.45rem 0.7rem;
			padding: 0.6rem 0 0;
			border-top: 1px solid rgba(23, 50, 77, 0.25);
			border-left: 0;
			font-size: 0.78rem;
		}
	}

	@container (max-width: 560px) {
		.list-card-layout { grid-template-columns: minmax(0, 1fr); }
		.list-card-aside { grid-row: 2; }
		.list-card-cover { max-width: 100%; }
	}

	@media (prefers-reduced-motion: reduce) {
		.read-more-button { transition: none; }
	}

</style>
