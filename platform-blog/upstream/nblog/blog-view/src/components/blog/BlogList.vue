<template>
	<div>
		<!--content-->
		<BlogItem :blogList="blogList" :layout="layout"/>
		<!--分页-->
		<Pagination :getBlogList="getBlogList" :totalPage="totalPage" :currentPage="currentPage"/>
	</div>
</template>

<script>
	import BlogItem from "./BlogItem";
	import Pagination from "./Pagination";

	export default {
		name: "BlogList",
		components: {Pagination, BlogItem},
		props: {
			currentPage: {type: Number, default: undefined},
			layout: {
				type: String,
				default: 'list',
				validator: value => ['list', 'grid'].includes(value)
			},
			getBlogList: {
				type: Function,
				required: true
			},
			blogList: {
				type: Array,
				required: true
			},
			totalPage: {
				type: Number,
				required: true
			}
		}
	}
</script>

<style scoped>

</style>
