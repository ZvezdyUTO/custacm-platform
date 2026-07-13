<template>
  <section class="category-admin admin-reference-page" aria-label="管理分类与标签">
    <header class="reference-page-header"><span class="reference-page-icon"><Tags :size="22" /></span><div><h2>管理分类与标签</h2><p>分类名称和颜色可以自定义；标签只允许新增或删除，新增时自动生成深色随机颜色。</p></div></header>
    <p v-if="notice" class="category-admin-notice" role="status">{{ notice }}</p><p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
    <section class="taxonomy-admin-section" aria-labelledby="category-section-title"><h3 id="category-section-title">分类</h3>
      <form class="category-create-form category-color-form" @submit.prevent="createCategory"><label>新分类名称<input v-model="newCategoryName" maxlength="255" placeholder="输入分类名称" :disabled="busy"></label><label>展示颜色<input v-model="newCategoryColor" type="color" aria-label="新分类展示颜色" :disabled="busy"></label><span class="taxonomy-color-preview" :style="{backgroundColor:newCategoryColor}">分类预览</span><button class="primary-button" type="submit" :disabled="busy||!newCategoryName.trim()">新增分类</button></form>
      <div class="category-admin-list"><div class="taxonomy-list-header category-grid"><span>分类名称</span><span>颜色</span><span>预览</span><span>操作</span></div><form v-for="category in categoryDrafts" :key="category.id" class="category-admin-row category-grid" @submit.prevent="saveCategory(category)"><input v-model="category.name" maxlength="255" :aria-label="`分类 ${category.id} 名称`" :disabled="busy"><input v-model="category.color" type="color" :aria-label="`分类 ${category.name} 展示颜色`" :disabled="busy"><span class="taxonomy-color-preview" :style="{backgroundColor:category.color}">{{ category.name }}</span><div class="category-admin-actions"><button type="submit" :disabled="busy||!category.name.trim()">保存</button><button class="danger-button" type="button" :disabled="busy" @click="removeCategory(category.id,category.name)">删除</button></div></form><p v-if="!busy&&!categoryDrafts.length" class="category-admin-empty">暂无分类。</p></div>
      <PaginationBar class="category-pagination" :page="categoryPage.pageNum" :limit="categoryPage.pageSize" :total-pages="categoryPage.pages" :disabled="busy" @change="changeCategoryPage" />
    </section>
    <section class="taxonomy-admin-section" aria-labelledby="tag-section-title"><h3 id="tag-section-title">标签</h3>
      <form class="category-create-form tag-create-form" @submit.prevent="createTag"><label>新标签名称<input v-model="newTagName" maxlength="255" placeholder="输入标签名称" :disabled="busy"></label><p>颜色会从连续数值空间随机生成深色，并以白字展示。</p><button class="primary-button" type="submit" :disabled="busy||!newTagName.trim()">新增标签</button></form>
      <div class="category-admin-list"><div class="taxonomy-list-header tag-grid"><span>标签</span><span>操作</span></div><div v-for="tag in tags" :key="tag.id" class="category-admin-row tag-grid"><span class="taxonomy-color-preview tag-preview" :style="{backgroundColor:tag.color||stableTagColor(tag.name)}">{{ tag.name }}</span><div class="category-admin-actions"><button class="danger-button" type="button" :disabled="busy" @click="removeTag(tag.id,tag.name)">删除</button></div></div><p v-if="!busy&&!tags.length" class="category-admin-empty">暂无标签。</p></div>
      <PaginationBar class="tag-pagination" :page="tagPage.pageNum" :limit="tagPage.pageSize" :total-pages="tagPage.pages" :disabled="busy" @change="changeTagPage" />
    </section>
    <AdminConfirmDialog
      :open="pendingDelete !== null"
      dialog-id="taxonomy-delete-confirm"
      :title="pendingDelete?.kind === 'category' ? '删除这个分类？' : '删除这个标签？'"
      :description="pendingDelete ? `“${pendingDelete.name}”将被删除。请确认当前内容已经不再需要这个${pendingDelete.kind === 'category' ? '分类' : '标签'}。` : ''"
      confirm-label="确认删除"
      :busy="busy"
      icon="delete"
      tone="danger"
      @cancel="pendingDelete = null"
      @confirm="confirmDelete"
    />
  </section>
</template>
<script setup lang="ts">
// Author: huangbingrui.awa
import { Tags } from '@lucide/vue';
import { computed, onMounted, ref, watch } from 'vue';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard';
import type { AdminCategory } from '../types';
import AdminConfirmDialog from './AdminConfirmDialog.vue';
import PaginationBar from './PaginationBar.vue';

type PendingDelete = { kind: 'category' | 'tag'; id: number; name: string };

const props = defineProps<{ dashboard: ReturnType<typeof usePlatformDashboard> }>();
const busy = ref(false);
const notice = ref('');
const errorMessage = ref('');
const newCategoryName = ref('');
const newCategoryColor = ref('#8B1E3F');
const newTagName = ref('');
const categoryDrafts = ref<AdminCategory[]>([]);
const pendingDelete = ref<PendingDelete | null>(null);
const categories = computed(() => props.dashboard.adminCategories.value?.list || []);
const tags = computed(() => props.dashboard.adminTags.value?.list || []);
const categoryPage = computed(() => props.dashboard.adminCategories.value || { list: [], pageNum: 1, pageSize: 15, pages: 1, total: 0 });
const tagPage = computed(() => props.dashboard.adminTags.value || { list: [], pageNum: 1, pageSize: 15, pages: 1, total: 0 });

watch(categories, (items) => {
  categoryDrafts.value = items.map((item) => ({ ...item, color: item.color || '#8B1E3F' }));
}, { immediate: true });

onMounted(() => run(async () => {
  await Promise.all([props.dashboard.loadAdminCategories(1, 15), props.dashboard.loadAdminTags(1, 15)]);
}));

async function run(operation: () => Promise<void>) {
  busy.value = true;
  notice.value = '';
  errorMessage.value = '';
  try {
    await operation();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '操作失败。';
  } finally {
    busy.value = false;
  }
}

function createCategory() {
  const name = newCategoryName.value.trim();
  if (!name) return;
  void run(async () => {
    await props.dashboard.createCategory(name, newCategoryColor.value);
    newCategoryName.value = '';
    notice.value = '分类已新增。';
  });
}

function saveCategory(category: AdminCategory) {
  const name = category.name.trim();
  if (!name) return;
  void run(async () => {
    await props.dashboard.updateCategory(category.id, name, category.color || '#8B1E3F');
    notice.value = '分类已更新。';
  });
}

function createTag() {
  const name = newTagName.value.trim();
  if (!name) return;
  void run(async () => {
    await props.dashboard.createTag(name);
    newTagName.value = '';
    notice.value = '标签已新增。';
  });
}

function removeCategory(id: number, name: string) {
  pendingDelete.value = { kind: 'category', id, name };
}

function removeTag(id: number, name: string) {
  pendingDelete.value = { kind: 'tag', id, name };
}

async function confirmDelete() {
  const target = pendingDelete.value;
  if (!target) return;
  pendingDelete.value = null;
  await run(async () => {
    if (target.kind === 'category') {
      await props.dashboard.deleteCategory(target.id);
      notice.value = '分类已删除。';
    } else {
      await props.dashboard.deleteTag(target.id);
      notice.value = '标签已删除。';
    }
  });
}

function changeCategoryPage(page: number, limit: number) {
  void run(async () => { await props.dashboard.loadAdminCategories(page, limit); });
}

function changeTagPage(page: number, limit: number) {
  void run(() => props.dashboard.loadAdminTags(page, limit));
}

function stableTagColor(name: string) {
  let hash = 2166136261;
  for (const char of name) {
    hash ^= char.codePointAt(0) || 0;
    hash = Math.imul(hash, 16777619);
  }
  const value = Math.abs(hash);
  return `hsl(${value % 360} ${65 + (Math.abs(hash >>> 8) % 26)}% ${22 + (Math.abs(hash >>> 16) % 6)}%)`;
}
</script>
