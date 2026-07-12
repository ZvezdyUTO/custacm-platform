<template>
  <section class="article-admin admin-reference-page" aria-label="管理文章">
    <header class="reference-page-header article-admin-header">
      <span class="reference-page-icon"><Newspaper :size="22" /></span>
      <div><h2>管理文章</h2><p>选择公开文章进入首页侧栏“精选文章”；精选按置顶状态和最近更新时间排序，最多展示 5 篇。</p></div>
    </header>

    <form class="article-admin-filters" @submit.prevent="search">
      <label>标题<input v-model="title" placeholder="输入文章标题"></label>
      <label>分类<select v-model="categoryId"><option value="">全部分类</option><option v-for="category in categories" :key="category.id" :value="String(category.id)">{{ category.name }}</option></select></label>
      <button class="primary-button" type="submit" :disabled="busy"><Search :size="16" />查询</button>
    </form>

    <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
    <div class="article-admin-list">
      <article v-for="article in articles" :key="article.id" class="article-admin-card">
        <div class="article-admin-cover" :class="{ 'is-empty': !article.firstPicture }"><img v-if="article.firstPicture" :src="article.firstPicture" alt=""><ImageOff v-else :size="22" /></div>
        <div class="article-admin-info"><div class="article-admin-title"><strong>{{ article.title }}</strong><span v-if="article.top">置顶</span></div><p>{{ article.category?.name || '未分类' }} · 更新于 {{ formatDate(article.updateTime) }}</p></div>
        <span :class="['article-publish-state', article.published ? 'is-published' : '']">{{ article.published ? '已发布' : '草稿' }}</span>
        <div class="article-admin-actions"><button class="featured-toggle" :class="{ 'is-featured': article.recommend }" type="button" :disabled="busy || !article.published" :aria-pressed="article.recommend" @click="toggleFeatured(article)"><Star :size="17" :fill="article.recommend ? 'currentColor' : 'none'" />{{ article.recommend ? '已精选' : '设为精选' }}</button><button class="article-delete-button" type="button" :disabled="busy" @click="pendingDelete = article"><Trash2 :size="17" />删除</button></div>
      </article>
      <p v-if="!busy && !articles.length" class="article-admin-empty">没有符合条件的文章。</p>
    </div>
    <footer v-if="page.pages > 1" class="article-admin-pagination"><button type="button" :disabled="busy || page.pageNum <= 1" @click="changePage(page.pageNum - 1)">上一页</button><span>{{ page.pageNum }} / {{ page.pages }} · 共 {{ page.total }} 篇</span><button type="button" :disabled="busy || page.pageNum >= page.pages" @click="changePage(page.pageNum + 1)">下一页</button></footer>

    <div v-if="pendingDelete" class="article-delete-backdrop" role="presentation" @click.self="pendingDelete = null">
      <section class="article-delete-dialog" role="alertdialog" aria-modal="true" aria-labelledby="article-delete-title" aria-describedby="article-delete-description">
        <span class="article-delete-warning-icon"><TriangleAlert :size="26" /></span>
        <div><h3 id="article-delete-title">确认永久删除这篇文章？</h3><p id="article-delete-description">你即将删除《{{ pendingDelete.title }}》。文章正文、关联标签及文章下的全部评论都会被删除，且无法恢复。</p></div>
        <div class="article-delete-dialog-actions"><button type="button" :disabled="busy" @click="pendingDelete = null">取消</button><button class="confirm-delete-button" type="button" :disabled="busy" @click="confirmDelete">确认永久删除</button></div>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
// Author: huangbingrui.awa
import { computed, onMounted, ref } from 'vue';
import { ImageOff, Newspaper, Search, Star, Trash2, TriangleAlert } from '@lucide/vue';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard';
import type { AdminArticle } from '../types';

const props = defineProps<{ dashboard: ReturnType<typeof usePlatformDashboard> }>();
const title = ref(''); const categoryId = ref(''); const busy = ref(false); const errorMessage = ref('');
const pendingDelete = ref<AdminArticle | null>(null);
const response = computed(() => props.dashboard.adminArticles.value);
const articles = computed(() => response.value?.blogs.list || []);
const categories = computed(() => response.value?.categories || []);
const page = computed(() => response.value?.blogs || { pageNum: 1, pages: 1, total: 0 });

onMounted(() => load(1));
async function load(pageNum: number) { busy.value = true; errorMessage.value = ''; try { await props.dashboard.loadAdminArticles({ title: title.value.trim(), categoryId: categoryId.value ? Number(categoryId.value) : null, pageNum, pageSize: 10 }); } catch (error) { errorMessage.value = error instanceof Error ? error.message : '文章列表加载失败。'; } finally { busy.value = false; } }
function search() { void load(1); }
function changePage(pageNum: number) { void load(pageNum); }
async function toggleFeatured(article: AdminArticle) { busy.value = true; errorMessage.value = ''; try { await props.dashboard.updateArticleFeatured(article.id, !article.recommend); } catch (error) { errorMessage.value = error instanceof Error ? error.message : '精选状态更新失败。'; } finally { busy.value = false; } }
async function confirmDelete() { if (!pendingDelete.value) return; busy.value = true; errorMessage.value = ''; try { await props.dashboard.deleteArticle(pendingDelete.value.id); pendingDelete.value = null; const nextPage = articles.value.length === 1 && page.value.pageNum > 1 ? page.value.pageNum - 1 : page.value.pageNum; await props.dashboard.loadAdminArticles({ title: title.value.trim(), categoryId: categoryId.value ? Number(categoryId.value) : null, pageNum: nextPage, pageSize: 10 }); } catch (error) { errorMessage.value = error instanceof Error ? error.message : '文章删除失败。'; } finally { busy.value = false; } }
function formatDate(value: string) { if (!value) return '未知时间'; return value.replace('T', ' ').slice(0, 16); }
</script>
