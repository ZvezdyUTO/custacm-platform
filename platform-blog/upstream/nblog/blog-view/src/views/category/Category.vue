<template>
	<div>
		<div class="ui top segment" style="text-align: center">
			<h2 class="m-text-500">分类 {{ categoryName }} 下的文章</h2>
		</div>
		<BlogList :getBlogList="getBlogList" :blogList="blogList" :totalPage="totalPage"/>
	</div>
</template>

<script>
	import BlogList from "@/components/blog/BlogList";
	import {getBlogListByCategoryName} from "@/api/category";
	import {SESSION_CHANGE_EVENT} from '@/auth/session'

	export default {
		name: "Category",
		components: {BlogList},
		data() {
			return {
				blogList: [],
				totalPage: 0
			}
		},
		watch: {
			//在当前组件被重用时，要重新获取博客列表
			'$route.fullPath'() {
				if (this.$route.name === 'category') {
					this.getBlogList()
				}
			}
		},
		created() {
			this.getBlogList()
		},
		computed: {
			categoryName() {
				return this.$route.params.name
			}
		},
		methods: {
			refreshVisibleBlogs(event) {
				if (!event || event.key === null || event.key === 'custacm.accessToken' || event.key === 'custacm.user') {
					this.getBlogList()
				}
			},
			getBlogList(pageNum) {
				getBlogListByCategoryName(this.categoryName, pageNum).then(res => {
					if (res.code === 200) {
						this.blogList = res.data.list
						this.totalPage = res.data.totalPage
						this.$nextTick(() => {
							Prism.highlightAll()
						})
					} else {
						this.msgError(res.msg)
					}
				}).catch(() => {
					this.msgError("请求失败")
				})
			}
		},
		mounted() {
			window.addEventListener('storage', this.refreshVisibleBlogs)
			window.addEventListener(SESSION_CHANGE_EVENT, this.refreshVisibleBlogs)
		},
		beforeUnmount() {
			window.removeEventListener('storage', this.refreshVisibleBlogs)
			window.removeEventListener(SESSION_CHANGE_EVENT, this.refreshVisibleBlogs)
		}
	}
</script>

<style scoped>

</style>
