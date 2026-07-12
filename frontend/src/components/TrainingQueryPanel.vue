<template>
  <section class="training-query" aria-label="训练数据查询">
    <component :is="mode === 'problem' ? 'form' : 'div'" :class="['query-form', { 'multi-query-form': mode === 'multiple' }]" @submit.prevent="apply(filterSignature(), true)">
      <label class="query-field query-oj-field compact"><span class="query-field-label">OJ</span>
        <select v-model="selectedOjName" aria-label="选择 OJ" :disabled="isRefreshing" @change="changeOj">
          <option :value="OJ_NAMES.CODEFORCES">{{ OJ_LABELS.CODEFORCES }}</option>
          <option :value="OJ_NAMES.ATCODER">{{ OJ_LABELS.ATCODER }}</option>
        </select>
      </label>
      <label v-if="mode === 'single'" class="query-field wide"><span class="query-field-label">队员</span>
        <select v-model="selectedUsername" aria-label="队员" :disabled="trainingUsers.length === 0 || isRefreshing" @change="chooseUser">
          <option disabled :value="null">{{ trainingUsers.length ? '请选择队员' : '等待训练数据' }}</option>
          <option v-for="item in trainingUsers" :key="item.username" :value="item.username">{{ item.nickname || item.username }}</option>
        </select>
      </label>
      <label v-else-if="mode === 'problem'" class="query-field wide"><span class="query-field-label">题目编号</span>
        <input v-model="problemKey" :placeholder="selectedOjName === OJ_NAMES.ATCODER ? '例如 abc443_c' : '例如 2242:C'" />
      </label>
      <div v-else class="query-field wide query-mode-field">
        <span class="query-field-label">队员统计</span><strong>{{ multiUserRows.length }} 人 / {{ multiAcceptedCount }} 题</strong>
      </div>
      <label class="query-field"><span class="query-field-label">通过起始日期</span><input v-model="draft.acceptedFromDateUtcPlus8" type="date" /></label>
      <label class="query-field"><span class="query-field-label">通过结束日期</span><input v-model="draft.acceptedToDateUtcPlus8" type="date" /></label>
      <template v-if="mode !== 'problem'">
        <label class="query-field compact"><span class="query-field-label">最低 rating</span><input v-model="draft.minProblemRating" min="0" placeholder="不限" type="number" /></label>
        <label class="query-field compact"><span class="query-field-label">最高 rating</span><input v-model="draft.maxProblemRating" min="0" placeholder="不限" type="number" /></label>
      </template>
      <button v-if="mode === 'problem'" class="primary-button query-problem-apply-button" :disabled="isRefreshing || Boolean(queryError)" type="submit">查询</button>
      <span v-else class="query-auto-refresh-hint" aria-live="polite">{{ isRefreshing ? '正在刷新…' : '筛选后自动刷新' }}</span>
    </component>
    <div class="query-meta-row">
      <span>更新于 {{ updatedAt }}</span>
      <button class="query-refresh-button" :disabled="isRefreshing" aria-label="刷新训练数据" type="button" @click="refresh">
        <RefreshCw :class="{ spin: isRefreshing }" :size="14" />
      </button>
    </div>
    <div v-if="queryError" class="query-summary"><small class="query-error" role="alert">{{ queryError }}</small></div>

    <article v-if="mode === 'multiple'" class="multi-summary-panel">
      <header><div><h2>多人通过统计</h2><p>按当前筛选条件汇总队员通过题目。</p></div>
        <span>{{ multiUserProgress.active ? `加载 ${multiUserProgress.completed}/${multiUserProgress.total}` : `${multiUserRows.length} 人` }}</span>
      </header>
      <div class="auto-summary-table-scroll">
        <table class="auto-summary-table" aria-label="多人通过统计" :style="{ minWidth: `${multiTableMinWidth}px` }">
          <thead><tr><th class="auto-summary-player-col">队员</th><th class="auto-summary-total-col">总计</th><th v-for="bucket in ratingBuckets" :key="bucket" :class="['auto-summary-rating-col', ratingToneClass(bucket)]">{{ ratingLabel(bucket) }}</th></tr></thead>
          <tbody>
            <tr v-for="row in displayRows" :key="row.row.user.username" :class="{ 'is-error': row.row.status === 'error' }">
              <th class="auto-summary-player-cell"><span class="auto-summary-player"><strong>{{ row.row.user.nickname || row.row.user.username }}</strong><small>{{ row.row.user.username }} · {{ row.row.summary?.authorHandle || '查询失败' }}</small></span></th>
              <td class="auto-summary-total-cell"><template v-if="row.row.status === 'error'"><span class="multi-summary-error"><strong>查询失败</strong><button class="multi-summary-retry" type="button" @click="retry(row.row.user.username)">重试</button></span></template><strong v-else>{{ row.row.summary?.totalAcceptedProblemCount || 0 }}</strong></td>
              <td v-for="bucket in ratingBuckets" :key="bucket" :class="['auto-summary-rating-cell', ratingToneClass(bucket)]"><span v-if="row.counts.has(bucket)" class="auto-rating-count">{{ row.counts.get(bucket) }}</span><span v-else class="auto-rating-empty">-</span></td>
            </tr>
            <tr v-if="multiUserRows.length === 0"><td class="submission-empty" :colspan="2 + ratingBuckets.length">{{ multiUserProgress.active ? '正在加载队员通过统计…' : '暂无队员通过统计。' }}</td></tr>
          </tbody>
        </table>
      </div>
    </article>

    <template v-else-if="mode === 'single' && (submissions || firstAccepted)">
      <div class="training-stat-grid" aria-label="训练数据统计">
        <article class="training-stat-card identity-card"><span>个人信息</span><strong>{{ selectedTrainingUser?.nickname || selectedUsername || '未选择队员' }}</strong><small>{{ acceptedSummary?.authorHandle || `未绑定 ${OJ_LABELS[selectedOjName]}` }}</small></article>
        <article class="training-stat-card primary"><span>通过题目数</span><strong>{{ acceptedSummary?.totalAcceptedProblemCount || 0 }}</strong><small>按筛选条件统计</small></article>
        <article class="training-stat-card"><span>提交明细</span><strong>{{ submissions?.total || 0 }}</strong><small>本页 {{ submissions?.submissions.length || 0 }} 条</small></article>
        <article class="training-stat-card"><span>首次通过明细</span><strong>{{ firstAccepted?.total || 0 }}</strong><small>本页 {{ firstAccepted?.problems.length || 0 }} 题</small></article>
      </div>
      <div class="training-result-grid">
        <article class="rating-panel"><header><div class="rating-panel-title"><h2>难度分布</h2></div></header>
          <div class="rating-bars"><div v-for="item in acceptedSummary?.ratingCounts || []" :key="item.problemRating" class="rating-bar-row"><span>{{ ratingLabel(item.problemRating) }}</span><div><i :style="{ background: ratingColor(item.problemRating), width: ratingWidth(item.acceptedProblemCount) }" /></div><strong>{{ item.acceptedProblemCount }}</strong></div><p v-if="!acceptedSummary?.ratingCounts.length">暂无通过汇总。</p></div>
        </article>
        <article class="recent-submission-panel"><header><div class="activity-switch" role="tablist"><button :class="{ 'is-active': singleTab === 'submissions' }" type="button" @click="singleTab = 'submissions'">最近提交</button><button :class="{ 'is-active': singleTab === 'accepted' }" type="button" @click="singleTab = 'accepted'">最近通过</button></div></header>
          <div class="submission-table-scroll">
            <table v-if="singleTab === 'submissions'" class="submission-table"><thead><tr><th>题目</th><th>判题</th><th>提交时间</th></tr></thead><tbody><tr v-for="item in submissions?.submissions || []" :key="item.submissionId"><td><div class="submission-problem"><strong>{{ item.problemName || item.problemKey || '题目名称缺失' }}</strong><span>{{ [item.difficulty, item.language].filter(Boolean).join(' / ') }}</span></div></td><td><span :class="['submission-verdict', { accepted: item.accepted }]">{{ verdict(item) }}</span></td><td class="submission-time">{{ item.submittedAtUtcPlus8 || '-' }}</td></tr><tr v-if="!submissions?.submissions.length"><td class="submission-empty" colspan="3">暂无提交明细。</td></tr></tbody></table>
            <table v-else class="submission-table accepted-table"><thead><tr><th>题目</th><th>难度</th><th>首次通过时间</th></tr></thead><tbody><tr v-for="item in firstAccepted?.problems || []" :key="item.problemKey"><td><strong>{{ item.problemName || item.problemKey }}</strong></td><td>{{ item.difficulty || '-' }}</td><td class="submission-time">{{ item.firstAcceptedAtUtcPlus8 }}</td></tr><tr v-if="!firstAccepted?.problems.length"><td class="submission-empty" colspan="3">暂无首次通过明细。</td></tr></tbody></table>
          </div>
          <PaginationBar v-if="singleTab === 'submissions'" :page="submissionPage" :limit="submissionLimit" :total-pages="submissions?.totalPages || 1" :disabled="isRefreshing" @change="dashboard.changeSubmissionPage" />
          <PaginationBar v-else :page="firstAcceptedPage" :limit="firstAcceptedLimit" :total-pages="firstAccepted?.totalPages || 1" :disabled="isRefreshing" @change="dashboard.changeFirstAcceptedPage" />
        </article>
      </div>
    </template>

    <template v-else-if="mode === 'problem' && (problemSubmissions || problemFirstAccepted)">
      <div class="training-stat-grid problem-stat-grid">
        <article class="training-stat-card identity-card"><span>题目</span><strong>{{ problemSubmissions?.problemKey || problemFirstAccepted?.problemKey || problemKey }}</strong><small>{{ OJ_LABELS[selectedOjName] }}</small></article>
        <article class="training-stat-card primary"><span>提交总数</span><strong>{{ problemSubmissions?.total || 0 }}</strong><small>本页 {{ problemSubmissions?.submissions.length || 0 }} 条</small></article>
        <article class="training-stat-card"><span>首 AC 人数</span><strong>{{ problemFirstAccepted?.total || 0 }}</strong><small>按筛选条件统计</small></article>
      </div>
      <article class="recent-submission-panel problem-query-panel"><header><div class="activity-switch"><button :class="{ 'is-active': problemTab === 'submissions' }" type="button" @click="problemTab = 'submissions'">提交明细</button><button :class="{ 'is-active': problemTab === 'accepted' }" type="button" @click="problemTab = 'accepted'">首 AC handle</button></div></header>
        <div class="submission-table-scroll"><table class="submission-table"><thead><tr v-if="problemTab === 'submissions'"><th>队员</th><th>判题</th><th>提交时间</th></tr><tr v-else><th>队员</th><th>handle</th><th>首次通过时间</th></tr></thead><tbody v-if="problemTab === 'submissions'"><tr v-for="item in problemSubmissions?.submissions || []" :key="item.submissionId"><td><strong>{{ item.username }}</strong><small>{{ item.handle }} / {{ item.language }}</small></td><td><span :class="['submission-verdict', { accepted: item.accepted }]">{{ verdict(item) }}</span></td><td>{{ item.submittedAtUtcPlus8 || '-' }}</td></tr></tbody><tbody v-else><tr v-for="item in problemFirstAccepted?.acceptedHandles || []" :key="`${item.username}-${item.handle}`"><td><strong>{{ item.username }}</strong></td><td>{{ item.handle }}</td><td>{{ item.firstAcceptedAtUtcPlus8 }}</td></tr></tbody></table></div>
        <PaginationBar v-if="problemTab === 'submissions'" :page="problemSubmissionPage" :limit="problemSubmissionLimit" :total-pages="problemSubmissions?.totalPages || 1" :disabled="isRefreshing" @change="dashboard.changeProblemSubmissionPage" />
        <PaginationBar v-else :page="problemFirstAcceptedPage" :limit="problemFirstAcceptedLimit" :total-pages="problemFirstAccepted?.totalPages || 1" :disabled="isRefreshing" @change="dashboard.changeProblemFirstAcceptedPage" />
      </article>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue';
import { RefreshCw } from '@lucide/vue';
import PaginationBar from './PaginationBar.vue';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard';
import { OJ_LABELS, OJ_NAMES, type SubmissionItem, type TrainingQueryMode, type TrainingQueryRange } from '../types';

// Author: huangbingrui.awa
const props = defineProps<{ dashboard: ReturnType<typeof usePlatformDashboard>; mode: TrainingQueryMode }>();
const dashboard = props.dashboard;
const { status, trainingUsers, selectedTrainingUser, selectedUsername, selectedOjName, trainingQuery,
  multiUserRows, multiUserProgress, acceptedSummary, submissions, firstAccepted, problemKey,
  problemSubmissions, problemFirstAccepted, submissionPage, submissionLimit, firstAcceptedPage,
  firstAcceptedLimit, problemSubmissionPage, problemSubmissionLimit, problemFirstAcceptedPage,
  problemFirstAcceptedLimit } = dashboard;
const draft = reactive<TrainingQueryRange>({ ...trainingQuery.value });
const singleTab = ref<'submissions' | 'accepted'>('submissions');
const problemTab = ref<'submissions' | 'accepted'>('submissions');
const isRefreshing = computed(() => status.value === 'loading');
const updatedAt = computed(() => new Intl.DateTimeFormat('zh-CN', { dateStyle: 'short', timeStyle: 'medium', hour12: false }).format(new Date()));
const queryError = computed(() => {
  if (draft.acceptedFromDateUtcPlus8 && draft.acceptedToDateUtcPlus8 && draft.acceptedFromDateUtcPlus8 > draft.acceptedToDateUtcPlus8) return '通过起始日期不能晚于结束日期。';
  if (draft.minProblemRating && draft.maxProblemRating && Number(draft.minProblemRating) > Number(draft.maxProblemRating)) return '最低 rating 不能大于最高 rating。';
  return '';
});
let autoApplyTimer: number | undefined;
let lastAppliedSignature = filterSignature();
const multiAcceptedCount = computed(() => multiUserRows.value.reduce((sum, row) => sum + (row.summary?.totalAcceptedProblemCount || 0), 0));
const ratingBuckets = computed(() => [...new Set(multiUserRows.value.flatMap((row) => row.summary?.ratingCounts.map((item) => item.problemRating) || []))].sort(compareRating));
const multiTableMinWidth = computed(() => 276 + ratingBuckets.value.length * 56);
const displayRows = computed(() => multiUserRows.value.map((row) => ({ row, counts: new Map(row.summary?.ratingCounts.map((item) => [item.problemRating, item.acceptedProblemCount]) || []) })));
const maxRatingCount = computed(() => Math.max(1, ...(acceptedSummary.value?.ratingCounts.map((item) => item.acceptedProblemCount) || [])));
watch(trainingQuery, (query) => Object.assign(draft, query), { deep: true });
watch(draft, scheduleAutoApply, { deep: true });
onBeforeUnmount(() => window.clearTimeout(autoApplyTimer));

function filterSignature() { return JSON.stringify({ ...draft, problemKey: props.mode === 'problem' ? problemKey.value.trim() : '' }); }
function scheduleAutoApply() {
  window.clearTimeout(autoApplyTimer);
  if (props.mode === 'problem' || queryError.value) return;
  const signature = filterSignature();
  if (signature === lastAppliedSignature) return;
  autoApplyTimer = window.setTimeout(() => { void apply(signature); }, 250);
}
async function apply(signature = filterSignature(), force = false) {
  if (queryError.value || (!force && signature === lastAppliedSignature)) return;
  lastAppliedSignature = signature;
  await dashboard.applyTrainingQuery({
    ...draft,
    minProblemRating: String(draft.minProblemRating ?? ''),
    maxProblemRating: String(draft.maxProblemRating ?? ''),
  }, props.mode);
}
async function refresh() { await dashboard.refreshDashboard(props.mode); }
async function changeOj() { if (props.mode !== 'problem') await dashboard.chooseOjName(selectedOjName.value); }
async function chooseUser() { if (selectedUsername.value) await dashboard.chooseUsername(selectedUsername.value); }
async function retry(username: string) { await dashboard.retryMultiUserSummary(username); }
function ratingStart(value: string) { return Number(value.match(/^\d+/)?.[0] ?? Number.NaN); }
function compareRating(a: string, b: string) { return (Number.isFinite(ratingStart(a)) ? ratingStart(a) : Infinity) - (Number.isFinite(ratingStart(b)) ? ratingStart(b) : Infinity); }
function ratingLabel(value: string) { return value === 'UNRATED' ? 'UNR' : value; }
function ratingTone(value: string) {
  const rating = ratingStart(value);
  if (!Number.isFinite(rating)) return 'gray';
  if (selectedOjName.value === OJ_NAMES.ATCODER) return rating < 400 ? 'gray' : rating < 800 ? 'green' : rating < 1200 ? 'blue' : rating < 1600 ? 'yellow' : 'red';
  return rating < 1200 ? 'gray' : rating < 1400 ? 'green' : rating < 1600 ? 'cyan' : rating < 1900 ? 'blue' : rating < 2100 ? 'violet' : rating < 2400 ? 'orange' : 'red';
}
function ratingToneClass(value: string) { return `rating-tone-${ratingTone(value)}`; }
function ratingColor(value: string) { return ({ gray: '#808080', green: '#008000', cyan: '#03a89e', blue: '#0000ff', yellow: '#d49b00', violet: '#aa00aa', orange: '#ff8c00', red: '#ff0000' } as const)[ratingTone(value)]; }
function ratingWidth(value: number) { return `${Math.max(6, value / maxRatingCount.value * 100)}%`; }
function verdict(item: SubmissionItem) { return item.accepted || item.verdict === 'OK' ? 'Accept' : item.verdict || 'UNKNOWN'; }
</script>
