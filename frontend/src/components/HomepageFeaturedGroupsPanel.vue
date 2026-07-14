<template>
  <section class="featured-groups-admin" aria-label="首页精选编排">
    <header class="featured-groups-toolbar">
      <div>
        <h3>首页精选分组</h3>
        <p>每组固定三篇：一篇主文章和两篇次文章。最多三组，保存后立即按这里的顺序展示。</p>
      </div>
      <button
        v-if="drafts.length < MAX_GROUP_COUNT"
        class="featured-group-add"
        type="button"
        :disabled="busy || hasUnsavedGroup"
        @click="createDraft"
      >
        <Plus :size="17" />创建组
      </button>
    </header>

    <p v-if="message" class="operation-toast" role="status">{{ message }}</p>
    <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
    <p v-if="loading" class="featured-groups-loading" aria-live="polite">正在读取首页编排…</p>

    <div v-else class="featured-group-list">
      <article v-for="(draft, groupIndex) in drafts" :key="draft.key" class="featured-group-editor">
        <header class="featured-group-editor-header">
          <span class="featured-group-index">{{ String(groupIndex + 1).padStart(2, '0') }}</span>
          <label>
            <span>分组标题</span>
            <input v-model="draft.title" maxlength="100" placeholder="例如：训练方法与复盘" :disabled="busy">
          </label>
          <div class="featured-group-order-actions">
            <button
              type="button"
              :disabled="busy || groupIndex === 0 || hasDirtyDrafts"
              :aria-label="`将第 ${groupIndex + 1} 组上移`"
              @click="moveGroup(groupIndex, -1)"
            ><ArrowUp :size="16" />上移</button>
            <button
              type="button"
              :disabled="busy || groupIndex === drafts.length - 1 || hasDirtyDrafts"
              :aria-label="`将第 ${groupIndex + 1} 组下移`"
              @click="moveGroup(groupIndex, 1)"
            >下移<ArrowDown :size="16" /></button>
            <button
              class="danger-link"
              type="button"
              :disabled="busy"
              :aria-label="`删除第 ${groupIndex + 1} 组`"
              @click="requestDelete(groupIndex)"
            ><Trash2 :size="16" />删除</button>
          </div>
        </header>

        <div class="featured-article-slots">
          <button
            v-for="(article, slotIndex) in draft.articles"
            :key="`${draft.key}-${slotIndex}`"
            class="featured-article-slot"
            :class="{ 'is-empty': !article, 'is-unavailable': article && !article.available }"
            type="button"
            :disabled="busy"
            @click="openPicker(groupIndex, slotIndex)"
          >
            <span class="featured-slot-label">{{ SLOT_LABELS[slotIndex] }}</span>
            <span class="featured-slot-media">
              <img v-if="article?.firstPicture" :src="article.firstPicture" alt="">
              <ImageOff v-else :size="28" />
            </span>
            <span v-if="article" class="featured-slot-copy">
              <strong>{{ article.title }}</strong>
              <span>{{ article.authorNickname || article.authorUsername || '已注销用户' }} · {{ article.categoryName || '未分类' }}</span>
              <em v-if="!article.available">文章已不再公开，请替换后保存</em>
            </span>
            <span v-else class="featured-slot-empty-copy">
              <Plus :size="24" />选择{{ SLOT_LABELS[slotIndex] }}
            </span>
            <span class="featured-slot-edit"><Pencil :size="14" />{{ article ? '更换' : '选择' }}</span>
          </button>
        </div>

        <footer class="featured-group-editor-footer">
          <p :class="{ 'is-ready': draftReady(draft) }">
            <Check v-if="draftReady(draft)" :size="16" />
            <span>{{ draftReady(draft) ? '三篇文章已就绪' : '请补齐三篇互不重复的公开文章' }}</span>
          </p>
          <div>
            <button class="secondary-button" type="button" :disabled="busy" @click="resetDraft(groupIndex)">
              {{ draft.id === null ? '取消创建' : '撤销修改' }}
            </button>
            <button class="primary-button" type="button" :disabled="busy || !draftReady(draft)" @click="saveDraft(draft)">
              {{ busy ? '正在保存' : (draft.id === null ? '创建并发布' : '保存这一组') }}
            </button>
          </div>
        </footer>
      </article>

      <div v-if="!drafts.length" class="featured-groups-empty">
        <span><LayoutPanelTop :size="34" /></span>
        <h3>还没有精选分组</h3>
        <p>创建第一组并选满三篇公开文章后，首页会开始展示精选内容。</p>
        <button class="primary-button" type="button" :disabled="busy" @click="createDraft"><Plus :size="17" />创建第一组</button>
      </div>
    </div>

    <div
      v-if="picker"
      class="featured-picker-backdrop"
      role="presentation"
      @click.self="closePicker"
      @keydown.esc.stop.prevent="closePicker"
    >
      <section
        ref="pickerDialog"
        class="featured-picker-dialog"
        role="dialog"
        tabindex="-1"
        aria-modal="true"
        aria-labelledby="featured-picker-title"
        aria-describedby="featured-picker-hint"
        :aria-busy="candidateBusy"
        @keydown.tab="trapPickerFocus"
      >
        <header>
          <div>
            <span>{{ pickerSlotLabel }}</span>
            <h3 id="featured-picker-title">选择一篇公开文章</h3>
          </div>
          <button type="button" aria-label="关闭文章选择" @click="closePicker"><X :size="20" /></button>
        </header>
        <form class="featured-picker-search" @submit.prevent="searchCandidates">
          <Search :size="17" />
          <input
            ref="pickerSearchInput"
            v-model="candidateQuery"
            maxlength="80"
            placeholder="按文章标题搜索"
            aria-label="按文章标题搜索"
          >
          <button type="submit" :disabled="candidateBusy">{{ candidateBusy ? '搜索中' : '搜索' }}</button>
        </form>
        <p id="featured-picker-hint" class="featured-picker-hint">只显示已发布、公开且未进入回收站的文章；选择同组其他位置的文章会交换两个位置。</p>
        <div class="featured-candidate-list">
          <button
            v-for="candidate in candidates"
            :key="candidate.id"
            type="button"
            :disabled="candidateDisabled(candidate)"
            @click="selectCandidate(candidate)"
          >
            <span class="featured-candidate-cover">
              <img v-if="candidate.firstPicture" :src="candidate.firstPicture" alt="">
              <ImageOff v-else :size="20" />
            </span>
            <span class="featured-candidate-copy">
              <strong>{{ candidate.title }}</strong>
              <span>{{ candidate.authorNickname || candidate.authorUsername || '已注销用户' }} · {{ candidate.categoryName || '未分类' }}</span>
            </span>
            <em v-if="candidateDisabled(candidate)">{{ candidateUnavailableReason(candidate) }}</em>
            <Check v-else :size="17" />
          </button>
          <p v-if="!candidateBusy && !candidates.length">没有找到可选文章。</p>
        </div>
      </section>
    </div>

    <AdminConfirmDialog
      :open="pendingDeleteIndex !== null"
      dialog-id="featured-group-delete"
      title="删除这个精选分组？"
      :description="pendingDeleteDescription"
      confirm-label="确认删除"
      :busy="busy"
      icon="delete"
      tone="danger"
      @cancel="pendingDeleteIndex = null"
      @confirm="confirmDelete"
    />
  </section>
</template>

<script setup lang="ts">
// Author: huangbingrui.awa
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import {
  ArrowDown,
  ArrowUp,
  Check,
  ImageOff,
  LayoutPanelTop,
  Pencil,
  Plus,
  Search,
  Trash2,
  X,
} from '@lucide/vue';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard';
import type {
  HomepageFeaturedArticleCandidate,
  HomepageFeaturedGroup,
} from '../types';
import AdminConfirmDialog from './AdminConfirmDialog.vue';

interface GroupDraft {
  key: string;
  id: number | null;
  title: string;
  sortOrder: number;
  articles: Array<HomepageFeaturedArticleCandidate | null>;
}

const props = defineProps<{ dashboard: ReturnType<typeof usePlatformDashboard> }>();
const MAX_GROUP_COUNT = 3;
const SLOT_LABELS = ['主文章', '次文章一', '次文章二'] as const;
const drafts = ref<GroupDraft[]>([]);
const loading = ref(true);
const busy = ref(false);
const message = ref('');
const errorMessage = ref('');
const picker = ref<{ groupIndex: number; slotIndex: number } | null>(null);
const candidateQuery = ref('');
const candidateBusy = ref(false);
const candidates = ref<HomepageFeaturedArticleCandidate[]>([]);
const pendingDeleteIndex = ref<number | null>(null);
const pickerDialog = ref<globalThis.HTMLElement | null>(null);
const pickerSearchInput = ref<globalThis.HTMLInputElement | null>(null);
let pickerPreviouslyFocused: globalThis.HTMLElement | null = null;
let candidateRequestSequence = 0;

const hasUnsavedGroup = computed(() => drafts.value.some((draft) => draft.id === null));
const hasDirtyDrafts = computed(() => drafts.value.some(isDirty));
const pickerSlotLabel = computed(() => picker.value ? SLOT_LABELS[picker.value.slotIndex] : '文章');
const pendingDeleteDescription = computed(() => {
  const index = pendingDeleteIndex.value;
  if (index === null) return '';
  const draft = drafts.value[index];
  return `“${draft?.title || '未命名分组'}”会立即从首页移除，文章本身不会被删除。`;
});

onMounted(async () => {
  try {
    await props.dashboard.loadHomepageFeaturedGroups();
    syncDrafts();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '首页精选分组加载失败。';
  } finally {
    loading.value = false;
  }
});

function groupDraft(group: HomepageFeaturedGroup): GroupDraft {
  const articles: Array<HomepageFeaturedArticleCandidate | null> = [null, null, null];
  group.articles.forEach((article) => {
    if (article.sortOrder < 0 || article.sortOrder >= articles.length) return;
    articles[article.sortOrder] = { ...article, featuredGroupId: group.id };
  });
  return {
    key: `group-${group.id}`,
    id: group.id,
    title: group.title,
    sortOrder: group.sortOrder,
    articles,
  };
}

function copyDraft(draft: GroupDraft): GroupDraft {
  return {
    ...draft,
    articles: draft.articles.map((article) => article ? { ...article } : null),
  };
}

function dirtyDraftsExcept(key: string) {
  return drafts.value
    .filter((draft) => draft.key !== key && isDirty(draft))
    .map(copyDraft);
}

function syncDrafts(preservedDrafts: GroupDraft[] = []) {
  const preservedById = new Map(preservedDrafts
    .filter((draft): draft is GroupDraft & { id: number } => draft.id !== null)
    .map((draft) => [draft.id, draft]));
  const serverDrafts = props.dashboard.homepageFeaturedGroups.value
    .slice()
    .sort((left, right) => left.sortOrder - right.sortOrder)
    .map((group) => {
      const preserved = preservedById.get(group.id);
      return preserved ? { ...copyDraft(preserved), sortOrder: group.sortOrder } : groupDraft(group);
    });
  const unsavedDrafts = preservedDrafts.filter((draft) => draft.id === null).map(copyDraft);
  drafts.value = [...serverDrafts, ...unsavedDrafts];
}

function createDraft() {
  if (drafts.value.length >= MAX_GROUP_COUNT || hasUnsavedGroup.value) return;
  errorMessage.value = '';
  message.value = '';
  drafts.value.push({
    key: `new-${Date.now()}`,
    id: null,
    title: '精选文章',
    sortOrder: drafts.value.length,
    articles: [null, null, null],
  });
}

function draftReady(draft: GroupDraft) {
  const articles = draft.articles.filter((article): article is HomepageFeaturedArticleCandidate => article !== null);
  return draft.title.trim().length > 0
    && articles.length === 3
    && articles.every((article) => article.available)
    && new Set(articles.map((article) => article.id)).size === 3;
}

function articleIds(draft: GroupDraft) {
  return draft.articles
    .filter((article): article is HomepageFeaturedArticleCandidate => article !== null)
    .map((article) => article.id);
}

function isDirty(draft: GroupDraft) {
  if (draft.id === null) return true;
  const group = props.dashboard.homepageFeaturedGroups.value.find((item) => item.id === draft.id);
  if (!group || group.title !== draft.title) return true;
  const originalIds = group.articles.slice().sort((left, right) => left.sortOrder - right.sortOrder).map((item) => item.id);
  return originalIds.join(',') !== articleIds(draft).join(',');
}

function resetDraft(index: number) {
  const draft = drafts.value[index];
  if (draft.id === null) {
    drafts.value.splice(index, 1);
    return;
  }
  const original = props.dashboard.homepageFeaturedGroups.value.find((group) => group.id === draft.id);
  if (original) drafts.value[index] = groupDraft(original);
  errorMessage.value = '';
}

async function saveDraft(draft: GroupDraft) {
  if (!draftReady(draft)) return;
  const preservedDrafts = dirtyDraftsExcept(draft.key);
  busy.value = true;
  errorMessage.value = '';
  message.value = '';
  const request = { title: draft.title.trim(), articleIds: articleIds(draft) };
  try {
    if (draft.id === null) {
      await props.dashboard.createHomepageFeaturedGroup(request);
      message.value = '精选分组已创建并发布到首页。';
    } else {
      await props.dashboard.updateHomepageFeaturedGroup(draft.id, request);
      message.value = '这一组的标题和文章已更新。';
    }
    syncDrafts(preservedDrafts);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '精选分组保存失败。';
  } finally {
    busy.value = false;
  }
}

async function moveGroup(index: number, offset: number) {
  const target = index + offset;
  if (target < 0 || target >= drafts.value.length || hasDirtyDrafts.value) return;
  const reordered = [...drafts.value];
  [reordered[index], reordered[target]] = [reordered[target], reordered[index]];
  const ids = reordered.map((draft) => draft.id).filter((id): id is number => id !== null);
  busy.value = true;
  errorMessage.value = '';
  try {
    await props.dashboard.reorderHomepageFeaturedGroups(ids);
    syncDrafts();
    message.value = '精选分组顺序已更新。';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '精选分组排序失败。';
  } finally {
    busy.value = false;
  }
}

function requestDelete(index: number) {
  const draft = drafts.value[index];
  if (draft.id === null) {
    drafts.value.splice(index, 1);
    return;
  }
  pendingDeleteIndex.value = index;
}

async function confirmDelete() {
  const index = pendingDeleteIndex.value;
  if (index === null) return;
  const id = drafts.value[index]?.id;
  const key = drafts.value[index]?.key;
  pendingDeleteIndex.value = null;
  if (id === null || id === undefined || key === undefined) return;
  const preservedDrafts = dirtyDraftsExcept(key);
  busy.value = true;
  errorMessage.value = '';
  try {
    await props.dashboard.deleteHomepageFeaturedGroup(id);
    syncDrafts(preservedDrafts);
    message.value = '精选分组已从首页删除，文章内容未受影响。';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '精选分组删除失败。';
  } finally {
    busy.value = false;
  }
}

async function openPicker(groupIndex: number, slotIndex: number) {
  pickerPreviouslyFocused = document.activeElement instanceof globalThis.HTMLElement
    ? document.activeElement
    : null;
  picker.value = { groupIndex, slotIndex };
  candidateQuery.value = '';
  void searchCandidates();
  await nextTick();
  (pickerSearchInput.value ?? pickerDialog.value)?.focus();
}

async function closePicker() {
  candidateRequestSequence += 1;
  candidateBusy.value = false;
  picker.value = null;
  candidates.value = [];
  await nextTick();
  restorePickerFocus();
}

function pickerFocusableElements() {
  return Array.from(pickerDialog.value?.querySelectorAll<globalThis.HTMLElement>(
    'button:not(:disabled), [href], input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])',
  ) ?? []).filter((element) => !element.hasAttribute('hidden'));
}

function trapPickerFocus(event: globalThis.KeyboardEvent) {
  const focusable = pickerFocusableElements();
  if (focusable.length === 0) {
    event.preventDefault();
    pickerDialog.value?.focus();
    return;
  }
  const first = focusable[0];
  const last = focusable[focusable.length - 1];
  const active = document.activeElement;
  if (event.shiftKey && (active === first || !pickerDialog.value?.contains(active))) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && active === last) {
    event.preventDefault();
    first.focus();
  }
}

function restorePickerFocus() {
  const target = pickerPreviouslyFocused;
  pickerPreviouslyFocused = null;
  if (target?.isConnected) target.focus();
}

async function searchCandidates() {
  const sequence = ++candidateRequestSequence;
  candidateBusy.value = true;
  errorMessage.value = '';
  try {
    const result = await props.dashboard.searchHomepageFeaturedArticleCandidates(candidateQuery.value);
    if (sequence !== candidateRequestSequence || !picker.value) return;
    candidates.value = result;
  } catch (error) {
    if (sequence !== candidateRequestSequence || !picker.value) return;
    errorMessage.value = error instanceof Error ? error.message : '候选文章搜索失败。';
  } finally {
    if (sequence === candidateRequestSequence) candidateBusy.value = false;
  }
}

function selectedInOtherGroup(articleId: number) {
  if (!picker.value) return false;
  return drafts.value.some((draft, groupIndex) => (
    groupIndex !== picker.value?.groupIndex
      && draft.articles.some((article) => article?.id === articleId)
  ));
}

function candidateDisabled(candidate: HomepageFeaturedArticleCandidate) {
  if (!picker.value) return true;
  const currentDraft = drafts.value[picker.value.groupIndex];
  const assignedToOtherGroup = candidate.featuredGroupId !== null
    && candidate.featuredGroupId !== currentDraft.id;
  return assignedToOtherGroup || selectedInOtherGroup(candidate.id);
}

function candidateUnavailableReason(candidate: HomepageFeaturedArticleCandidate) {
  if (!picker.value) return '不可选择';
  const currentDraft = drafts.value[picker.value.groupIndex];
  if (candidate.featuredGroupId !== null && candidate.featuredGroupId !== currentDraft.id) {
    return '已在其他分组';
  }
  return selectedInOtherGroup(candidate.id) ? '已在其他分组' : '不可选择';
}

function selectCandidate(candidate: HomepageFeaturedArticleCandidate) {
  if (!picker.value || candidateDisabled(candidate)) return;
  const { groupIndex, slotIndex } = picker.value;
  const draft = drafts.value[groupIndex];
  const sourceSlotIndex = draft.articles.findIndex((article) => article?.id === candidate.id);
  const targetArticle = draft.articles[slotIndex];
  if (sourceSlotIndex >= 0 && sourceSlotIndex !== slotIndex) {
    draft.articles[sourceSlotIndex] = targetArticle
      ? { ...targetArticle, sortOrder: sourceSlotIndex }
      : null;
  }
  draft.articles[slotIndex] = { ...candidate, sortOrder: slotIndex };
  void closePicker();
}

onBeforeUnmount(() => {
  candidateRequestSequence += 1;
  restorePickerFocus();
});
</script>
