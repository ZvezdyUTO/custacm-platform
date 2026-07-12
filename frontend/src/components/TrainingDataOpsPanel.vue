<template>
  <section class="training-data-collection-panel admin-reference-page" aria-label="训练数据采集">
    <header class="reference-page-header collection-reference-header">
      <span class="reference-page-icon"><Database :size="22" /></span>
      <div><h2>训练数据采集</h2><p>按 OJ 列出现役队员中已绑定 handle 的成员，每次采集完成后都会自动刷新数仓。</p></div>
      <div class="collection-reference-controls">
        <label>OJ<select v-model="collectionOj"><option :value="OJ_NAMES.CODEFORCES">Codeforces</option><option :value="OJ_NAMES.ATCODER">AtCoder</option></select></label>
        <label>统一回看小时数<input v-model="globalLookback" min="1" type="number" placeholder="不限" /></label>
        <button class="primary-button collect-all-button" :disabled="allBusy || !collectableUsers.length" type="button" @click="collectAll"><RefreshCw :class="{ spin: allBusy }" :size="18" />{{ allBusy ? '正在采集' : '全部采集' }}</button>
      </div>
    </header>
    <p v-if="collectError" class="form-error" role="alert">{{ collectError }}</p>

    <div class="collection-member-list">
      <article v-for="item in collectableUsers" :key="item.user.username" class="collection-member-row">
        <div class="collection-member-identity"><strong><span>{{ item.user.username }}</span><template v-if="item.user.nickname"><i aria-hidden="true">·</i><span>{{ item.user.nickname }}</span></template></strong><span>{{ OJ_LABELS[collectionOj] }}：{{ item.handles[collectionOj] }}</span></div>
        <label>回看小时数<input v-model="lookbackByUsername[item.user.username]" min="1" type="number" placeholder="不限" /></label>
        <button class="primary-button" :disabled="busyUsers.has(item.user.username)" type="button" @click="collectOne(item.user.username)"><RefreshCw :class="{ spin: busyUsers.has(item.user.username) }" :size="18" />{{ busyUsers.has(item.user.username) ? '正在采集' : '执行采集' }}</button>
      </article>
      <p v-if="!collectableUsers.length" class="batch-target-empty">当前 OJ 暂无可采集的现役队员。</p>
    </div>

    <section class="collection-history-section">
      <header><div><h2>采集任务历史</h2><p>运行中任务会自动轮询，终态后停止。</p></div></header>
      <ul v-if="jobs.length" class="collection-job-list"><li v-for="job in jobs" :key="job.jobId"><button class="collection-job-row" type="button" @click="toggleJob(job.jobId)"><span><strong>{{ shortJobId(job.jobId) }}</strong><small>{{ formatTime(job.startedAt) }}</small></span><strong :class="['result-status', `result-status-${statusClass(job.status)}`]">{{ jobStatus(job.status) }}</strong><small>采集 {{ job.collectedCount }}/{{ job.requestedCount }}，失败 {{ job.failedCount }}，写入 {{ job.writtenRows }} 行</small><ChevronDown :class="{ 'is-expanded': expandedJobs.has(job.jobId) }" :size="16" /></button>
        <div v-if="expandedJobs.has(job.jobId)" class="collection-job-detail"><dl><div><dt>任务 ID</dt><dd>{{ job.jobId }}</dd></div><div><dt>完成时间</dt><dd>{{ job.finishedAt ? formatTime(job.finishedAt) : '运行中' }}</dd></div></dl><div class="collection-result-table-scroll"><table class="collection-result-table"><thead><tr><th>队员 / handle</th><th>状态</th><th>写入</th><th>匹配</th><th>仓库</th><th>批次</th></tr></thead><tbody><tr v-for="row in job.items" :key="`${row.username}-${row.ojName}`"><td><strong>{{ row.username }}</strong><small>{{ row.handle || row.message || '等待解析' }}</small></td><td>{{ row.itemStatus }}</td><td>{{ row.writtenRows }} 行</td><td>{{ row.matchedSubmissionCount }}/{{ row.fetchedSubmissionCount }}</td><td>{{ row.refreshStatus }}</td><td>{{ row.batchId || '无批次' }}</td></tr></tbody></table></div></div>
      </li></ul><p v-else class="batch-target-empty">暂无采集任务。</p>
    </section>

    <div v-if="pendingCollection" class="collection-confirm-backdrop" role="presentation" @click.self="pendingCollection = null">
      <section class="collection-confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="collection-confirm-title" aria-describedby="collection-confirm-description">
        <span class="collection-confirm-icon"><TriangleAlert :size="26" /></span>
        <div><h3 id="collection-confirm-title">确认执行数据采集？</h3><p id="collection-confirm-description">即将采集 <strong>{{ pendingCollection.targetLabel }}</strong> 的 {{ OJ_LABELS[pendingCollection.ojName] }} 提交，回看范围为 <strong>{{ lookbackLabel(pendingCollection.lookbackHours) }}</strong>。采集完成后会自动刷新数仓，请确认目标与范围无误。</p></div>
        <div class="collection-confirm-actions"><button type="button" :disabled="confirmBusy" @click="pendingCollection = null">取消</button><button class="confirm-collection-button" type="button" :disabled="confirmBusy" @click="confirmCollection"><RefreshCw :class="{ spin: confirmBusy }" :size="16" />{{ confirmBusy ? '正在启动' : '确认执行采集' }}</button></div>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { ChevronDown, Database, RefreshCw, TriangleAlert } from '@lucide/vue';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard';
import { OJ_LABELS, OJ_NAMES, UNLIMITED_LOOKBACK_HOURS, type CollectionJob, type OjName } from '../types';
import { collectionRequest } from '../utils/adminTraining';

// Author: huangbingrui.awa
const props = defineProps<{ dashboard: ReturnType<typeof usePlatformDashboard> }>();
interface PendingCollection { usernames: string[]; targetLabel: string; lookbackHours: number; ojName: OjName; all: boolean }
const collectionOj = ref<OjName>(OJ_NAMES.CODEFORCES); const globalLookback = ref<string | number>('1440'); const lookbackByUsername = ref<Record<string, string | number>>({}); const collectError = ref(''); const allBusy = ref(false); const busyUsers = ref(new Set<string>()); const expandedJobs = ref(new Set<string>()); const pendingCollection = ref<PendingCollection | null>(null); const confirmBusy = ref(false);
const collectableUsers = computed(() => props.dashboard.adminUsers.value.filter((item) => item.needCollect === true && Boolean(item.handles[collectionOj.value])));
const jobs = computed(() => props.dashboard.collectionJobs.value);
function parseLookback(value: string | number) { const normalized = String(value).trim(); if (!normalized) return UNLIMITED_LOOKBACK_HOURS; const hours = Math.floor(Number(normalized)); if (!Number.isFinite(hours) || hours <= 0) throw new Error('回看小时数必须是大于 0 的整数。'); return hours; }
async function runCollection(usernames: string[], lookbackHours: number, ojName: OjName) { await props.dashboard.batchCollectSubmissions(collectionRequest(usernames, lookbackHours, ojName)); }
function collectAll() { const usernames = collectableUsers.value.map((item) => item.user.username); if (!usernames.length) return; let lookback; try { lookback = parseLookback(globalLookback.value); } catch (error) { collectError.value = error instanceof Error ? error.message : '回看小时数无效。'; return; } pendingCollection.value = { usernames, targetLabel: `全部 ${usernames.length} 名队员`, lookbackHours: lookback, ojName: collectionOj.value, all: true }; }
function collectOne(username: string) { let lookback; try { lookback = parseLookback(lookbackByUsername.value[username] || ''); } catch (error) { collectError.value = error instanceof Error ? error.message : '回看小时数无效。'; return; } const user = collectableUsers.value.find((item) => item.user.username === username); pendingCollection.value = { usernames: [username], targetLabel: user?.user.nickname ? `${username} · ${user.user.nickname}` : username, lookbackHours: lookback, ojName: collectionOj.value, all: false }; }
async function confirmCollection() { const request = pendingCollection.value; if (!request) return; confirmBusy.value = true; collectError.value = ''; if (request.all) allBusy.value = true; else { const next = new Set(busyUsers.value); next.add(request.usernames[0]!); busyUsers.value = next; } try { await runCollection(request.usernames, request.lookbackHours, request.ojName); pendingCollection.value = null; } catch (error) { collectError.value = error instanceof Error ? error.message : '采集失败。'; } finally { confirmBusy.value = false; allBusy.value = false; const done = new Set(busyUsers.value); request.usernames.forEach((username) => done.delete(username)); busyUsers.value = done; } }
function lookbackLabel(hours: number) { return hours === UNLIMITED_LOOKBACK_HOURS ? '不限时间' : `最近 ${hours} 小时`; }
function toggleJob(id: string) { const next = new Set(expandedJobs.value); if (next.has(id)) next.delete(id); else next.add(id); expandedJobs.value = next; }
function shortJobId(id: string) { return id.length > 18 ? `${id.slice(0, 18)}...` : id; } function formatTime(value: string) { return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'short', timeStyle: 'short', hour12: false }).format(new Date(value)); } function statusClass(status: string) { return status.toLowerCase().replaceAll('_', '-'); } function jobStatus(status: CollectionJob['status']) { return status === 'PENDING' ? '等待中' : status === 'RUNNING' ? '正在执行' : status === 'SUCCESS' ? '执行成功' : status === 'PARTIAL_SUCCESS' ? '部分成功' : '执行失败'; }
</script>
