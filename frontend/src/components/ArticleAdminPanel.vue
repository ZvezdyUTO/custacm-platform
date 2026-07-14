<template>
	  <section class="article-admin admin-reference-page" aria-label="管理文章">
	    <header class="reference-page-header article-admin-header">
	      <span class="reference-page-icon"><Newspaper :size="22" /></span>
			<div><h2>管理文章</h2><p>{{ headerDescription }}</p></div>
			<button v-if="viewMode !== 'featured'" class="article-backup-button" type="button" :disabled="backupBusy" @click="pendingBackup = true"><Download :size="17" />{{ backupBusy ? '正在打包…' : '下载全部文章' }}</button>
	    </header>

		<nav class="article-admin-tabs" aria-label="文章范围">
			<button type="button" :class="{ 'is-active': viewMode === 'featured' }" @click="switchMode('featured')">首页编排</button>
			<button type="button" :class="{ 'is-active': viewMode === 'active' }" @click="switchMode('active')">当前文章</button>
			<button type="button" :class="{ 'is-active': viewMode === 'recycle' }" @click="switchMode('recycle')">回收站</button>
		</nav>

		<HomepageFeaturedGroupsPanel v-show="viewMode === 'featured'" :dashboard="dashboard" />
		<div v-show="viewMode !== 'featured'" class="article-admin-list-view">
	    <form class="article-admin-filters" @submit.prevent="search">
      <label>标题<input v-model="title" placeholder="输入文章标题"></label>
      <label>分类<select v-model="categoryId"><option value="">全部分类</option><option v-for="category in categories" :key="category.id" :value="String(category.id)">{{ category.name }}</option></select></label>
      <button class="primary-button" type="submit" :disabled="busy"><Search :size="16" />查询</button>
    </form>

	<p v-if="backupMessage" class="admin-notice article-backup-notice" role="status">{{ backupMessage }}</p>
    <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
    <div class="article-admin-list">
      <article v-for="article in articles" :key="article.id" class="article-admin-card">
        <div class="article-admin-cover" :class="{ 'is-empty': !article.firstPicture }"><img v-if="article.firstPicture" :src="article.firstPicture" alt=""><ImageOff v-else :size="22" /></div>
		<div class="article-admin-info"><div class="article-admin-title"><strong>{{ article.title }}</strong><span v-if="viewMode === 'active' && article.top">置顶</span></div><p v-if="viewMode === 'active'">{{ article.category?.name || '未分类' }} · 更新于 {{ formatDate(article.updateTime) }}</p><p v-else>{{ article.category?.name || '未分类' }} · 作者 {{ article.user?.nickname || article.user?.username || '已注销用户' }} · 删除于 {{ formatDate(article.deletedAt) }} · {{ remainingRetention(article.deletedAt) }}</p></div>
		<span :class="['article-publish-state', viewMode === 'recycle' ? 'is-recycle' : (article.published ? 'is-published' : '')]">{{ viewMode === 'recycle' ? '回收站' : (article.published ? '已发布' : '草稿') }}</span>
			<div class="article-admin-actions"><button v-if="viewMode === 'active'" class="article-delete-button" type="button" :disabled="busy" @click="pendingDelete = article"><Trash2 :size="17" />删除</button><button v-else class="article-restore-button" type="button" :disabled="busy" @click="restore(article)"><RotateCcw :size="17" />恢复文章</button></div>
      </article>
		<p v-if="!busy && !articles.length" class="article-admin-empty">{{ viewMode === 'recycle' ? '回收站中没有符合条件的文章。' : '没有符合条件的文章。' }}</p>
    </div>
    <footer v-if="page.pages > 1" class="article-admin-pagination"><button type="button" :disabled="busy || page.pageNum <= 1" @click="changePage(page.pageNum - 1)">上一页</button><span>{{ page.pageNum }} / {{ page.pages }} · 共 {{ page.total }} 篇</span><button type="button" :disabled="busy || page.pageNum >= page.pages" @click="changePage(page.pageNum + 1)">下一页</button></footer>

	    <div v-if="pendingDelete" class="article-delete-backdrop" role="presentation" @click.self="pendingDelete = null">
      <section class="article-delete-dialog" role="alertdialog" aria-modal="true" aria-labelledby="article-delete-title" aria-describedby="article-delete-description">
        <span class="article-delete-warning-icon"><TriangleAlert :size="26" /></span>
		<div><h3 id="article-delete-title">将这篇文章移入回收站？</h3><p id="article-delete-description">《{{ pendingDelete.title }}》会固定保留 7 天，期间作者本人或管理员可以恢复；正文、标签、评论和图片都会一并保留。</p></div>
		<div class="article-delete-dialog-actions"><button type="button" :disabled="busy" @click="pendingDelete = null">取消</button><button class="confirm-delete-button" type="button" :disabled="busy" @click="confirmDelete">移入回收站</button></div>
	      </section>
	    </div>

		<AdminConfirmDialog
			:open="pendingBackup"
			dialog-id="article-backup-confirm"
			title="下载全部文章备份？"
			description="将打包当前文章、草稿、内部文章、回收站内容、去敏评论、作者资料和托管图片。数据较多时生成下载需要一些时间。"
			confirm-label="开始下载"
			busy-label="正在打包…"
			:busy="backupBusy"
			icon="backup"
			tone="warning"
			@cancel="pendingBackup = false"
				@confirm="backupAll"
			/>
		</div>
		  </section>
</template>

<script setup lang="ts">
// Author: huangbingrui.awa
import { computed, ref } from 'vue';
import { Download, ImageOff, Newspaper, RotateCcw, Search, Trash2, TriangleAlert } from '@lucide/vue';
import AdminConfirmDialog from './AdminConfirmDialog.vue';
import HomepageFeaturedGroupsPanel from './HomepageFeaturedGroupsPanel.vue';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard';
import type { AdminArticle } from '../types';

const props = defineProps<{ dashboard: ReturnType<typeof usePlatformDashboard> }>();
const title = ref(''); const categoryId = ref(''); const busy = ref(false); const errorMessage = ref('');
const backupBusy = ref(false); const backupMessage = ref('');
const pendingBackup = ref(false);
const viewMode = ref<'featured' | 'active' | 'recycle'>('featured');
const pendingDelete = ref<AdminArticle | null>(null);
const response = computed(() => props.dashboard.adminArticles.value);
const articles = computed(() => response.value?.blogs.list || []);
const categories = computed(() => response.value?.categories || []);
const page = computed(() => response.value?.blogs || { pageNum: 1, pages: 1, total: 0 });
const headerDescription = computed(() => {
	if (viewMode.value === 'featured') return '按组编排首页精选内容；每组固定三篇文章，最多展示三组。';
	if (viewMode.value === 'active') return '查看、筛选和维护当前文章；首页展示请前往“首页编排”。';
	return '删除的文章固定保留 7 天，期间作者本人或管理员可以恢复。';
});

async function load(pageNum: number) { busy.value = true; errorMessage.value = ''; try { await props.dashboard.loadAdminArticles({ title: title.value.trim(), categoryId: categoryId.value ? Number(categoryId.value) : null, pageNum, pageSize: 10 }, viewMode.value === 'recycle'); } catch (error) { errorMessage.value = error instanceof Error ? error.message : '文章列表加载失败。'; } finally { busy.value = false; } }
function search() { void load(1); }
function switchMode(mode: 'featured' | 'active' | 'recycle') { if (viewMode.value === mode) return; viewMode.value = mode; pendingDelete.value = null; if (mode !== 'featured') void load(1); }
function changePage(pageNum: number) { void load(pageNum); }
async function confirmDelete() { if (!pendingDelete.value) return; busy.value = true; errorMessage.value = ''; try { await props.dashboard.deleteArticle(pendingDelete.value.id); pendingDelete.value = null; const nextPage = articles.value.length === 1 && page.value.pageNum > 1 ? page.value.pageNum - 1 : page.value.pageNum; await props.dashboard.loadAdminArticles({ title: title.value.trim(), categoryId: categoryId.value ? Number(categoryId.value) : null, pageNum: nextPage, pageSize: 10 }, false); } catch (error) { errorMessage.value = error instanceof Error ? error.message : '文章移入回收站失败。'; } finally { busy.value = false; } }
async function restore(article: AdminArticle) { busy.value = true; errorMessage.value = ''; try { await props.dashboard.restoreArticle(article.id); const nextPage = articles.value.length === 1 && page.value.pageNum > 1 ? page.value.pageNum - 1 : page.value.pageNum; await props.dashboard.loadAdminArticles({ title: title.value.trim(), categoryId: categoryId.value ? Number(categoryId.value) : null, pageNum: nextPage, pageSize: 10 }, true); } catch (error) { errorMessage.value = error instanceof Error ? error.message : '文章恢复失败。'; } finally { busy.value = false; } }
async function backupAll() { if (!pendingBackup.value) return; pendingBackup.value = false; backupBusy.value = true; backupMessage.value = ''; errorMessage.value = ''; try { await props.dashboard.backupAllArticles(); backupMessage.value = '文章、评论、作者资料和托管图片备份已开始下载。'; } catch (error) { errorMessage.value = error instanceof Error ? error.message : '文章备份下载失败。'; } finally { backupBusy.value = false; } }
function formatDate(value?: string) { if (!value) return '未知时间'; return value.replace('T', ' ').slice(0, 16); }
function remainingRetention(value?: string) { if (!value) return '保留期未知'; const remaining = new Date(value).getTime() + 7 * 24 * 60 * 60 * 1000 - Date.now(); if (remaining <= 0) return '等待清理'; const hours = Math.ceil(remaining / (60 * 60 * 1000)); return hours > 24 ? `剩余 ${Math.ceil(hours / 24)} 天` : `剩余 ${hours} 小时`; }
</script>
