<template>
	<div>
		<Comment/>
		<Pagination/>
	</div>
</template>

<script>
	import Comment from "./Comment";
	import Pagination from "./Pagination";
	import {SET_COMMENT_QUERY_BLOG_ID, SET_COMMENT_QUERY_INTERNAL, SET_COMMENT_QUERY_PAGE_NUM, SET_PARENT_COMMENT_ID} from "@/store/mutations-types";

	export default {
		name: "CommentList",
		components: {Comment, Pagination},
		props: {
			blogId: {
				type: Number,
				required: false
			},
			internal: {type: Boolean, default: false}
		},
		created() {
			this.init()
		},
		watch: {
			//文章路由切换时重新读取评论
			'$route.path'() {
				this.init()
			}
		},
		methods: {
			init() {
				//重置评论表单位置
				this.$store.commit(SET_PARENT_COMMENT_ID, -1)
				this.$store.commit(SET_COMMENT_QUERY_BLOG_ID, this.blogId)
				this.$store.commit(SET_COMMENT_QUERY_INTERNAL, this.internal)
				this.$store.commit(SET_COMMENT_QUERY_PAGE_NUM, 1)
				this.$store.dispatch('getCommentList')
			}
		}
	}
</script>

<style scoped>

</style>
