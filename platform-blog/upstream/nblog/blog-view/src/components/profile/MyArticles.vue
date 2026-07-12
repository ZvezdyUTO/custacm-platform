<template>
	<section class="my-articles" aria-labelledby="my-articles-title">
		<header class="articles-heading">
			<div>
				<p class="section-eyebrow">WRITING DESK</p>
				<h2 id="my-articles-title">我的文章</h2>
				<p>在这里查看已发布内容，或继续完成草稿。</p>
			</div>
			<router-link to="/write" class="new-article-button"><i class="plus icon"></i>发布文章</router-link>
		</header>

		<form class="article-filters" @submit.prevent="search">
			<label><span>标题</span><input v-model="filters.title" maxlength="80" placeholder="搜索我的文章"></label>
			<label><span>分类</span><select v-model="filters.categoryId"><option value="">全部分类</option><option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option></select></label>
			<button type="submit" :disabled="loading">查询</button>
		</form>

		<p v-if="errorMessage" class="article-error" role="alert">{{ errorMessage }}</p>
		<div v-if="loading" class="article-empty">正在读取文章…</div>
		<div v-else-if="!blogs.length" class="article-empty">
			<strong>还没有文章</strong>
			<span>从一篇题解、训练笔记或复盘开始。</span>
		</div>
		<div v-else class="article-list">
			<article v-for="blog in blogs" :key="blog.id" class="article-row">
				<div class="article-state" :class="{'is-draft': !blog.published}">{{ blog.published ? '已发布' : '草稿' }}</div>
				<div class="article-copy">
					<strong>{{ blog.title }}</strong>
					<span>{{ blog.category?.name || '未分类' }} · {{ formatDate(blog.updateTime || blog.createTime) }} · {{ blog.commentEnabled ? '允许评论' : '评论关闭' }}</span>
				</div>
				<div class="article-actions">
					<router-link v-if="blog.published" :to="`/blog/${blog.id}`">查看文章</router-link>
					<router-link v-else :to="`/write/${blog.id}`">继续编辑</router-link>
					<button type="button" :disabled="deletingId === blog.id" @click="remove(blog)">{{ deletingId === blog.id ? '删除中…' : '删除' }}</button>
				</div>
			</article>
		</div>

		<el-pagination v-if="totalPages > 1" :current-page="pageNum" :page-count="totalPages" layout="prev, pager, next" background @current-change="changePage"/>
	</section>
</template>

<script>
	// Author: huangbingrui.awa
	import {ElMessageBox} from 'element-plus'
	import {deleteMyBlog, getMyBlogs} from '@/api/player-blog'
	import {clearSession, readToken} from '@/auth/session'

	export default {
		name: 'MyArticles',
		data() {
			return {
				blogs: [], categories: [], loading: true, deletingId: null, errorMessage: '',
				filters: {title: '', categoryId: ''}, pageNum: 1, totalPages: 0,
			}
		},
		mounted() { this.load() },
		methods: {
			async load() {
				const token = readToken()
				if (!token) return
				this.loading = true
				this.errorMessage = ''
				try {
					const data = await getMyBlogs(token, {
						title: this.filters.title.trim(),
						...(this.filters.categoryId ? {categoryId: Number(this.filters.categoryId)} : {}),
						pageNum: this.pageNum,
						pageSize: 6,
					})
					this.blogs = data.blogs?.list || []
					this.totalPages = data.blogs?.pages || 0
					this.categories = data.categories || []
				} catch (error) { this.handleError(error, '文章列表读取失败') }
				finally { this.loading = false }
			},
			search() { this.pageNum = 1; this.load() },
			changePage(page) { this.pageNum = page; this.load() },
			async remove(blog) {
				try {
					await ElMessageBox.confirm(`删除「${blog.title}」后无法恢复，文章下的评论也会删除。`, '删除文章', {confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'})
				} catch (_) { return }
				const token = readToken()
				if (!token) return this.goLogin()
				this.deletingId = blog.id
				try { await deleteMyBlog(token, blog.id); this.msgSuccess('文章已删除'); await this.load() }
				catch (error) { this.handleError(error, '文章删除失败') }
				finally { this.deletingId = null }
			},
			goLogin() { this.$router.push({path: '/training/login', query: {returnTo: '/about'}}) },
			handleError(error, fallback) {
				if (error?.response?.status === 401) { clearSession(); this.goLogin(); return }
				this.errorMessage = error?.response?.data?.msg || error?.message || fallback
			},
			formatDate(value) { return value ? this.$filters.dateFormat(value, 'YYYY-MM-DD HH:mm') : '时间未知' },
		},
	}
</script>

<style scoped>
	.my-articles { margin-top: 34px; border-top: 1px solid #dfe4e9; padding-top: 30px; }
	.articles-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 24px; }
	.section-eyebrow { margin: 0 0 7px; color: #6d7a87; font-size: 11px; font-weight: 700; letter-spacing: .16em; }
	.articles-heading h2 { margin: 0; color: #202b35; font-size: 24px; }
	.articles-heading p:last-child { margin: 8px 0 0; color: #7a8691; font-size: 13px; }
	.new-article-button { border: 1px solid #17324d; background: #17324d; color: #fff; padding: 10px 14px; font-size: 12px; font-weight: 700; }
	.article-filters { display: grid; grid-template-columns: minmax(0, 1fr) 180px auto; align-items: end; gap: 10px; margin-top: 22px; border: 1px solid #dfe4e9; background: #f7f9fa; padding: 14px; }
	.article-filters label { display: grid; gap: 6px; color: #596671; font-size: 11px; font-weight: 700; }
	.article-filters input, .article-filters select { height: 38px; border: 1px solid #d3dae1; background: #fff; padding: 0 10px; color: #26323c; font: inherit; }
	.article-filters button { height: 38px; border: 1px solid #17324d; background: #fff; color: #17324d; padding: 0 16px; font-weight: 700; cursor: pointer; }
	.article-list { margin-top: 14px; border: 1px solid #dfe4e9; border-bottom: 0; }
	.article-row { display: grid; grid-template-columns: 72px minmax(0, 1fr) auto; align-items: center; gap: 14px; min-height: 82px; border-bottom: 1px solid #dfe4e9; padding: 15px; }
	.article-state { border-left: 3px solid #2f6c57; color: #2f6c57; padding: 5px 0 5px 8px; font-size: 11px; font-weight: 800; }
	.article-state.is-draft { border-color: #9a6b2f; color: #8a5c21; }
	.article-copy { display: grid; min-width: 0; gap: 7px; }
	.article-copy strong { overflow: hidden; color: #26313a; font-size: 15px; text-overflow: ellipsis; white-space: nowrap; }
	.article-copy span { color: #89939d; font-size: 11px; }
	.article-actions { display: flex; align-items: center; gap: 6px; }
	.article-actions a, .article-actions button { border: 0; background: transparent; color: #17324d; padding: 7px; font: inherit; font-size: 12px; font-weight: 700; cursor: pointer; }
	.article-actions button { color: #9a4747; }
	.article-empty { display: grid; place-items: center; gap: 7px; min-height: 150px; margin-top: 14px; border: 1px dashed #cad2d9; color: #7e8994; font-size: 13px; }
	.article-empty strong { color: #384550; font-size: 16px; }
	.article-error { margin: 14px 0 0; border-left: 3px solid #a14b4b; background: #f8eeee; color: #7b3434; padding: 10px 12px; }
	.el-pagination { justify-content: center; margin-top: 18px; }
</style>
