<template>
	<div class="ui segments m-box">
		<div class="ui secondary segment"><i class="bookmark icon"></i>精选文章</div>
		<div class="ui segment sidebar-accent">
			<div v-if="featuredBlogList.length" class="ui divided items">
				<div v-for="blog in featuredBlogList" :key="blog.id" class="m-item" @click.prevent="toBlog(blog)">
					<div class="img" :style="{'background-image':'url(' + blog.firstPicture + ')'}"></div>
					<div class="info"><div class="date">{{ $filters.dateFormat(blog.createTime, 'YYYY-MM-DD') }}</div><div class="title">{{ blog.title }}</div></div>
				</div>
			</div>
			<p v-else class="featured-empty">暂未设置精选文章</p>
		</div>
	</div>
</template>

<script>
	// Author: huangbingrui.awa
	export default {
		name: 'FeaturedBlog',
		props: { featuredBlogList: { type: Array, default: () => [] } },
		methods: { toBlog(blog) { this.$store.dispatch('goBlogPage', blog) } },
	}
</script>

<style scoped>
	.secondary.segment { padding: 10px; }
	.ui.divided.items .m-item:first-child { margin-top: 0; }
	.ui.divided.items .m-item { margin-top: 1rem; height: 7rem; position: relative; overflow: hidden; border-radius: 5px; cursor: pointer; user-select: none; }
	.ui.divided.items .m-item .img { position: absolute; inset: 0; background-position: center; background-size: cover; }
	.ui.divided.items .m-item .info { z-index: 1; position: absolute; right: 0; bottom: 0; left: 0; padding: .5rem !important; background: linear-gradient(to bottom, transparent, rgba(0, 0, 0, .8)); color: white; font-size: 12px; }
	.ui.divided.items .m-item .info .title { display: -webkit-box; overflow: hidden; -webkit-box-orient: vertical; -webkit-line-clamp: 1; text-overflow: ellipsis; word-break: break-word; }
	.featured-empty { margin: 4px 2px; color: #8a949d; font-size: 13px; text-align: center; }
</style>
