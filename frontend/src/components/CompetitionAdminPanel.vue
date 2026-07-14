<template>
  <section class="competition-admin admin-reference-page" aria-label="管理比赛与奖项">
    <header class="reference-page-header competition-admin-header">
      <span class="reference-page-icon"><Trophy :size="22" /></span>
      <div>
        <h2>比赛与奖项</h2>
        <p>比赛、参赛者和奖项只允许添加或删除；比赛删除后进入固定七天回收站。</p>
      </div>
      <button
        v-if="viewMode === 'active'"
        class="competition-create-trigger"
        type="button"
        :aria-expanded="showCreateForm"
        @click="showCreateForm = !showCreateForm"
      >
        <Plus :size="16" />{{ showCreateForm ? '收起创建表单' : '创建比赛' }}
      </button>
    </header>

    <nav class="competition-scope-tabs" aria-label="比赛状态">
      <button type="button" :class="{ 'is-active': viewMode === 'active' }" :disabled="operationBusy" @click="switchMode('active')">
        当前比赛
      </button>
      <button type="button" :class="{ 'is-active': viewMode === 'recycle' }" :disabled="operationBusy" @click="switchMode('recycle')">
        七天回收站
      </button>
    </nav>

    <form class="competition-filters" data-test="competition-filters" @submit.prevent="search">
      <label>起始年份<input v-model="filters.startYear" inputmode="numeric" placeholder="1900–9999"></label>
      <label>结束年份<input v-model="filters.endYear" inputmode="numeric" placeholder="1900–9999"></label>
      <label>规范分类
        <select v-model="filters.category" aria-label="筛选规范分类">
          <option value="">全部分类</option>
          <optgroup v-for="group in COMPETITION_CATEGORY_GROUPS" :key="group.label" :label="group.label">
            <option v-for="option in group.options" :key="option.code" :value="option.code">
              {{ option.label }}
            </option>
          </optgroup>
        </select>
      </label>
      <button class="primary-button" type="submit" :disabled="busy"><Search :size="16" />查询</button>
      <button class="competition-filter-reset" type="button" :disabled="busy" @click="resetFilters">清空</button>
    </form>

    <p v-if="notice" class="competition-notice" role="status">{{ notice }}</p>
    <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>

    <form
      v-if="viewMode === 'active' && showCreateForm"
      class="competition-create-form"
      data-test="competition-create-form"
      @submit.prevent="submitCompetition"
    >
      <div class="competition-form-heading">
        <div><span>新增记录</span><h3>创建比赛</h3></div>
        <p>保存后不提供普通编辑；信息有误时需移入回收站后重新创建。</p>
      </div>
      <div class="competition-create-fields">
        <label class="competition-name-field">比赛全称
          <input v-model="createDraft.fullName" maxlength="255" placeholder="例如：2026 ICPC 亚洲区域赛（合肥）">
        </label>
        <label>年份<input v-model.number="createDraft.year" type="number" min="1900" max="9999"></label>
        <label>规范分类
          <select v-model="createDraft.category" aria-label="比赛规范分类">
            <option value="" disabled>请选择分类</option>
            <optgroup v-for="group in COMPETITION_CATEGORY_GROUPS" :key="group.label" :label="group.label">
              <option v-for="option in group.options" :key="option.code" :value="option.code">
                {{ option.label }}
              </option>
            </optgroup>
          </select>
        </label>
      </div>
      <footer class="competition-form-footer">
        <p :class="{ 'is-error': createValidation }">{{ createValidation || '创建后可继续添加参赛者和奖项。' }}</p>
        <button class="primary-button" type="submit" :disabled="busy || Boolean(createValidation)">
          {{ busy ? '正在创建…' : '确认创建' }}
        </button>
      </footer>
    </form>

    <div class="competition-list" :aria-busy="busy">
      <div class="competition-list-header" aria-hidden="true">
        <span>比赛</span><span>分类</span><span>参赛 / 奖项</span><span>操作</span>
      </div>
      <article v-for="competition in competitions" :key="competition.id" class="competition-row">
        <div class="competition-row-summary">
          <div class="competition-identity">
            <strong>{{ competition.fullName }}</strong>
            <span>{{ competition.year }} · {{ competition.participationModeLabel }}</span>
          </div>
          <div class="competition-category-tag" :class="{ 'is-unclassified': !competition.category }">
            <span>{{ competition.categoryLabel || '待归类历史记录' }}</span>
          </div>
          <div class="competition-counts">
            <span><UsersRound :size="14" />{{ competition.participants.length }} 人</span>
            <span><Medal :size="14" />{{ competition.awards.length }} 项</span>
          </div>
          <div class="competition-row-actions">
            <template v-if="viewMode === 'active'">
              <button
                class="competition-detail-trigger"
                type="button"
                :aria-expanded="expandedId === competition.id"
                @click="toggleExpanded(competition)"
              >
                {{ expandedId === competition.id ? '收起' : '管理' }}
                <ChevronDown :size="15" :class="{ 'is-open': expandedId === competition.id }" />
              </button>
              <button class="competition-danger-action" type="button" @click="askMoveToRecycleBin(competition)">
                <Trash2 :size="15" />移入回收站
              </button>
            </template>
            <template v-else>
              <span class="competition-retention">{{ retentionLabel(competition.deletedAt) }}</span>
              <button class="competition-restore-action" type="button" @click="askRestore(competition)">
                <RotateCcw :size="15" />恢复
              </button>
            </template>
          </div>
        </div>

        <div v-if="viewMode === 'active' && expandedId === competition.id" class="competition-detail-workspace">
          <section class="competition-participants" aria-labelledby="competition-participant-heading">
            <header>
              <div><span>参赛名单</span><h3 id="competition-participant-heading">批量添加参赛者</h3></div>
              <strong>{{ competition.participants.length }}</strong>
            </header>
            <form class="competition-participant-form" @submit.prevent="submitParticipants(competition)">
              <label>用户名（换行、空格或逗号分隔）
                <textarea
                  v-model="participantInput"
                  rows="3"
                  maxlength="13000"
                  placeholder="player_a&#10;player_b"
                />
              </label>
              <button type="submit" :disabled="busy || !participantInput.trim()">
                <UsersRound :size="15" />批量添加
              </button>
            </form>
            <div class="competition-participant-list">
              <div v-for="participant in competition.participants" :key="participant.id">
                <span>
                  <strong>{{ participant.displayName }}</strong>
                  <small>{{ participant.username ? `@${participant.username}` : '账号已注销' }} · {{ participant.articles.length }} 篇绑定文章</small>
                </span>
                <button type="button" :aria-label="`删除参赛者 ${participant.displayName}`" @click="askDeleteParticipant(competition, participant)">
                  <Trash2 :size="14" />
                </button>
              </div>
              <p v-if="competition.participants.length === 0">暂无参赛者。</p>
            </div>
          </section>

          <section class="competition-awards" aria-labelledby="competition-award-heading">
            <header>
              <div><span>获奖记录</span><h3 id="competition-award-heading">添加奖项</h3></div>
              <strong>{{ competition.awards.length }}</strong>
            </header>
            <form class="competition-award-form" @submit.prevent="submitAward(competition)">
              <div class="competition-award-fields">
                <label>归属形态
                  <select v-model="awardDraft.awardMode" aria-label="奖项归属形态" :disabled="awardModeLocked(competition)">
                    <option value="INDIVIDUAL">个人</option>
                    <option value="TEAM">团队</option>
                  </select>
                </label>
                <label>奖项档位
                  <select v-model="awardDraft.awardTier" aria-label="奖项档位" :disabled="awardTierOptions(competition).length === 0">
                    <option v-if="awardTierOptions(competition).length === 0" value="">暂无可用档位</option>
                    <option v-for="option in awardTierOptions(competition)" :key="option.code" :value="option.code">
                      {{ option.label }}
                    </option>
                  </select>
                </label>
                <label v-if="awardDraft.awardMode === 'TEAM'">队伍名称<input v-model="awardDraft.teamName" maxlength="255" placeholder="可选"></label>
                <template v-if="requiresAwardRank(competition)">
                  <label>排名<input v-model.number="awardDraft.rankPosition" type="number" min="1" aria-label="排名名次"></label>
                  <label>总排名数<input v-model.number="awardDraft.rankTotal" type="number" min="1" aria-label="排名总数"></label>
                </template>
              </div>
              <fieldset class="competition-recipient-picker">
                <legend>获奖人</legend>
                <label v-for="participant in competition.participants" :key="participant.id" :class="{ 'is-disabled': !participant.username }">
                  <input
                    v-model="awardDraft.recipientUsernames"
                    type="checkbox"
                    :value="participant.username || ''"
                    :disabled="!participant.username"
                  >
                  <span>{{ participant.displayName }}<small>{{ participant.username ? `@${participant.username}` : '已注销' }}</small></span>
                </label>
                <p v-if="competition.participants.length === 0">请先添加参赛者。</p>
              </fieldset>
              <div class="competition-award-submit">
                <p :class="{ 'is-error': awardValidation(competition) }">
                  {{ awardValidation(competition) || awardHint(competition) }}
                </p>
                <button type="submit" :disabled="busy || Boolean(awardValidation(competition))">
                  <Plus :size="15" />添加奖项
                </button>
              </div>
            </form>
            <div class="competition-award-list">
              <article v-for="award in competition.awards" :key="award.id">
                <div>
                  <span>{{ award.awardModeLabel }}<template v-if="award.rank"> · {{ award.rank }}</template></span>
                  <strong>{{ award.awardTierLabel || '待归类历史奖项' }}</strong>
                  <p v-if="award.teamName">队伍：{{ award.teamName }}</p>
                  <p>获奖人：{{ award.recipients.map((item) => item.displayName).join('、') }}</p>
                </div>
                <button type="button" :aria-label="`删除奖项 ${award.awardTierLabel || '待归类历史奖项'}`" @click="askDeleteAward(competition, award)">
                  <Trash2 :size="14" />
                </button>
              </article>
              <p v-if="competition.awards.length === 0">暂无奖项。</p>
            </div>
          </section>
        </div>
      </article>
      <p v-if="!busy && competitions.length === 0" class="competition-empty">
        {{ viewMode === 'active' ? '没有符合筛选条件的比赛。' : '七天回收站中没有符合条件的比赛。' }}
      </p>
    </div>

    <footer class="competition-pagination">
      <span>第 {{ currentPage.pageNum }}/{{ Math.max(currentPage.totalPages, 1) }} 页 · 共 {{ currentPage.total }} 场</span>
      <div>
        <button type="button" :disabled="busy || currentPage.pageNum <= 1" @click="changePage(currentPage.pageNum - 1)">上一页</button>
        <button type="button" :disabled="busy || currentPage.pageNum >= currentPage.totalPages" @click="changePage(currentPage.pageNum + 1)">下一页</button>
      </div>
    </footer>

    <AdminConfirmDialog
      :open="pendingAction !== null"
      dialog-id="competition-admin-confirm"
      :title="confirmTitle"
      :description="confirmDescription"
      :confirm-label="confirmLabel"
      :busy="busy"
      :icon="confirmIcon"
      :tone="confirmTone"
      @cancel="pendingAction = null"
      @confirm="confirmPendingAction"
    />
  </section>
</template>

<script setup lang="ts">
// Author: huangbingrui.awa
import { ChevronDown, Medal, Plus, RotateCcw, Search, Trash2, Trophy, UsersRound } from '@lucide/vue';
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard';
import {
  BAIDU_AWARD_TIER_OPTIONS,
  COMPETITION_CATEGORY_GROUPS,
  MEDAL_AWARD_TIER_OPTIONS,
  PRIZE_AWARD_TIER_OPTIONS,
  type Competition,
  type CompetitionAward,
  type CompetitionAwardMode,
  type CompetitionAwardTier,
  type CompetitionCategory,
  type CompetitionListQuery,
  type CompetitionParticipant,
  type CompetitionParticipationMode,
} from '../types';
import AdminConfirmDialog from './AdminConfirmDialog.vue';

type ViewMode = 'active' | 'recycle';
type PendingAction =
  | { kind: 'competition'; competition: Competition }
  | { kind: 'participant'; competition: Competition; participant: CompetitionParticipant }
  | { kind: 'award'; competition: Competition; award: CompetitionAward }
  | { kind: 'restore'; competition: Competition };

const MEDAL_CATEGORIES = new Set<CompetitionCategory>([
  'PROVINCIAL',
  'ICPC_NATIONAL_INVITATIONAL',
  'CCPC_NATIONAL_INVITATIONAL',
  'ICPC_ASIA_REGIONAL',
  'CCPC_REGIONAL',
  'EC_FINAL',
  'CCPC_FINAL',
]);
const PARTICIPATION_MODE_BY_CATEGORY: Record<CompetitionCategory, CompetitionParticipationMode> = {
  PROVINCIAL: 'TEAM',
  ICPC_NATIONAL_INVITATIONAL: 'TEAM',
  CCPC_NATIONAL_INVITATIONAL: 'TEAM',
  ICPC_ASIA_REGIONAL: 'TEAM',
  CCPC_REGIONAL: 'TEAM',
  EC_FINAL: 'TEAM',
  CCPC_FINAL: 'TEAM',
  BAIDU_STAR: 'INDIVIDUAL',
  GPLT_NATIONAL: 'MIXED',
  LANQIAO_CUP_NATIONAL: 'INDIVIDUAL',
};

const props = defineProps<{ dashboard: ReturnType<typeof usePlatformDashboard> }>();
const EMPTY_PAGE = { pageNum: 1, pageSize: 10, total: 0, totalPages: 0, list: [] as Competition[] };
const viewMode = ref<ViewMode>('active');
const listBusy = ref(false);
const operationBusy = ref(false);
const busy = computed(() => listBusy.value || operationBusy.value);
const notice = ref('');
const errorMessage = ref('');
const showCreateForm = ref(false);
const expandedId = ref<number | null>(null);
const pendingAction = ref<PendingAction | null>(null);
const participantInput = ref('');
const filters = reactive<{ startYear: string; endYear: string; category: '' | CompetitionCategory }>({
  startYear: '',
  endYear: '',
  category: '',
});
const createDraft = reactive<{
  fullName: string;
  year: number;
  category: '' | CompetitionCategory;
}>({ fullName: '', year: new Date().getFullYear(), category: '' });
const awardDraft = reactive<{
  awardMode: CompetitionAwardMode;
  teamName: string;
  awardTier: '' | CompetitionAwardTier;
  rankPosition: number | null;
  rankTotal: number | null;
  recipientUsernames: string[];
}>({
  awardMode: 'TEAM', teamName: '', awardTier: '',
  rankPosition: null, rankTotal: null, recipientUsernames: [],
});
let listRequestSequence = 0;

const currentPage = computed(() => (
  viewMode.value === 'active'
    ? props.dashboard.adminCompetitions.value ?? EMPTY_PAGE
    : props.dashboard.adminCompetitionRecycleBin.value ?? EMPTY_PAGE
));
const competitions = computed(() => currentPage.value.list);
const createValidation = computed(() => {
  if (!createDraft.fullName.trim()) return '请输入比赛全称。';
  if (!Number.isInteger(createDraft.year) || createDraft.year < 1900 || createDraft.year > 9999) {
    return '比赛年份必须在 1900 到 9999 之间。';
  }
  if (!createDraft.category) return '请选择唯一的规范分类。';
  return '';
});
const confirmTitle = computed(() => {
  if (pendingAction.value?.kind === 'competition') return '将整场比赛移入回收站？';
  if (pendingAction.value?.kind === 'participant') return '删除这名参赛者？';
  if (pendingAction.value?.kind === 'award') return '删除这项奖项？';
  return '恢复这场比赛？';
});
const confirmDescription = computed(() => {
  const action = pendingAction.value;
  if (!action) return '';
  if (action.kind === 'competition') {
    return `“${action.competition.fullName}”会从公开页面和个人获奖记录中隐藏，参赛者、奖项和文章绑定保留 7 天，之后才会物理清理。`;
  }
  if (action.kind === 'participant') {
    return `将删除 ${action.participant.displayName} 的参赛关系及文章绑定；若该成员仍被奖项引用，服务端会拒绝删除。`;
  }
  if (action.kind === 'award') {
    return `将删除“${action.award.awardTierLabel || '待归类历史奖项'}”及全部获奖人关联和个人展示选择。`;
  }
  return `将“${action.competition.fullName}”恢复到公开比赛列表；若已有同名正常比赛，恢复会失败。`;
});
const confirmLabel = computed(() => pendingAction.value?.kind === 'restore' ? '确认恢复' : '确认删除');
const confirmIcon = computed<'delete' | 'warning'>(() => pendingAction.value?.kind === 'restore' ? 'warning' : 'delete');
const confirmTone = computed<'danger' | 'info'>(() => pendingAction.value?.kind === 'restore' ? 'info' : 'danger');

watch(() => awardDraft.awardMode, (mode) => {
  if (mode === 'INDIVIDUAL') awardDraft.teamName = '';
});
onMounted(() => { void loadList('active', 1, 10); });
onBeforeUnmount(() => { listRequestSequence += 1; });

function filterValidation() {
  const startYear = optionalYear(filters.startYear);
  const endYear = optionalYear(filters.endYear);
  if (startYear === undefined || endYear === undefined) return '年份必须是 1900 到 9999 之间的整数。';
  if (startYear !== null && endYear !== null && startYear > endYear) return '起始年份不能大于结束年份。';
  return '';
}

function optionalYear(value: string): number | null | undefined {
  if (!value.trim()) return null;
  const year = Number(value);
  return Number.isInteger(year) && year >= 1900 && year <= 9999 ? year : undefined;
}

function queryFor(pageNum: number, pageSize: number): CompetitionListQuery {
  return {
    startYear: optionalYear(filters.startYear) ?? null,
    endYear: optionalYear(filters.endYear) ?? null,
    category: filters.category || null,
    pageNum,
    pageSize,
  };
}

async function run(operation: () => Promise<void>, fallback: string) {
  operationBusy.value = true;
  notice.value = '';
  errorMessage.value = '';
  try {
    await operation();
    return true;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : fallback;
    return false;
  } finally {
    operationBusy.value = false;
  }
}

async function fetchList(target: ViewMode, pageNum: number, pageSize: number) {
  const query = queryFor(pageNum, pageSize);
  if (target === 'active') await props.dashboard.loadAdminCompetitions(query);
  else await props.dashboard.loadAdminCompetitionRecycleBin(query);
}

async function loadList(target: ViewMode, pageNum: number, pageSize: number) {
  const validation = filterValidation();
  if (validation) {
    errorMessage.value = validation;
    return;
  }
  const requestId = ++listRequestSequence;
  listBusy.value = true;
  notice.value = '';
  errorMessage.value = '';
  try {
    await fetchList(target, pageNum, pageSize);
    if (requestId !== listRequestSequence) return;
    if (expandedId.value !== null && !competitions.value.some((item) => item.id === expandedId.value)) {
      expandedId.value = null;
    }
  } catch (error) {
    if (requestId !== listRequestSequence) return;
    errorMessage.value = error instanceof Error ? error.message : '比赛列表加载失败。';
  } finally {
    if (requestId === listRequestSequence) listBusy.value = false;
  }
}

function search() {
  void loadList(viewMode.value, 1, currentPage.value.pageSize || 10);
}

function resetFilters() {
  filters.startYear = '';
  filters.endYear = '';
  filters.category = '';
  void loadList(viewMode.value, 1, currentPage.value.pageSize || 10);
}

function switchMode(mode: ViewMode) {
  if (operationBusy.value) return;
  if (viewMode.value === mode) return;
  viewMode.value = mode;
  expandedId.value = null;
  participantInput.value = '';
  pendingAction.value = null;
  void loadList(mode, 1, 10);
}

function changePage(pageNum: number) {
  void loadList(viewMode.value, pageNum, currentPage.value.pageSize || 10);
}

function resetCreateDraft() {
  createDraft.fullName = '';
  createDraft.year = new Date().getFullYear();
  createDraft.category = '';
}

function submitCompetition() {
  if (createValidation.value) return;
  void run(async () => {
    const created = await props.dashboard.createCompetition({
      fullName: createDraft.fullName.trim(),
      year: createDraft.year,
      category: createDraft.category as CompetitionCategory,
      participationMode: PARTICIPATION_MODE_BY_CATEGORY[createDraft.category as CompetitionCategory],
    });
    resetCreateDraft();
    showCreateForm.value = false;
    await fetchList('active', 1, currentPage.value.pageSize || 10);
    if (competitions.value.some((item) => item.id === created.id)) expandedId.value = created.id;
    notice.value = '比赛已创建，可继续添加参赛者和奖项。';
  }, '比赛创建失败。');
}

function toggleExpanded(competition: Competition) {
  if (expandedId.value === competition.id) {
    expandedId.value = null;
    return;
  }
  expandedId.value = competition.id;
  participantInput.value = '';
  resetAwardDraft(competition);
}

function parseUsernames(raw: string) {
  const usernames = raw.split(/[\s,，]+/u).map((item) => item.trim()).filter(Boolean);
  if (usernames.length === 0) return { usernames, error: '请输入至少一个用户名。' };
  if (usernames.length > 100) return { usernames, error: '单次最多添加 100 名用户。' };
  if (new Set(usernames).size !== usernames.length) return { usernames, error: '用户名列表不能重复。' };
  if (usernames.some((username) => !/^[\p{L}\p{N}._-]{1,128}$/u.test(username))) {
    return { usernames, error: '用户名只能包含字母、数字、点、下划线和连字符。' };
  }
  return { usernames, error: '' };
}

function submitParticipants(competition: Competition) {
  const parsed = parseUsernames(participantInput.value);
  if (parsed.error) {
    errorMessage.value = parsed.error;
    return;
  }
  const existing = new Set(competition.participants.map((item) => item.username).filter(Boolean));
  const duplicate = parsed.usernames.find((username) => existing.has(username));
  if (duplicate) {
    errorMessage.value = `${duplicate} 已在本场比赛的参赛名单中。`;
    return;
  }
  void run(async () => {
    await props.dashboard.addCompetitionParticipants(competition.id, { usernames: parsed.usernames });
    participantInput.value = '';
    notice.value = `已添加 ${parsed.usernames.length} 名参赛者。`;
  }, '参赛者添加失败。');
}

function awardTierOptions(
  competition: Competition,
): ReadonlyArray<{ code: CompetitionAwardTier; label: string }> {
  if (competition.category === 'BAIDU_STAR') return BAIDU_AWARD_TIER_OPTIONS;
  if (competition.category === 'GPLT_NATIONAL' || competition.category === 'LANQIAO_CUP_NATIONAL') {
    return PRIZE_AWARD_TIER_OPTIONS;
  }
  if (competition.category && MEDAL_CATEGORIES.has(competition.category)) {
    return MEDAL_AWARD_TIER_OPTIONS;
  }
  return [];
}

function requiresAwardRank(competition: Competition) {
  return Boolean(competition.category && MEDAL_CATEGORIES.has(competition.category));
}

function fixedAwardMode(competition: Competition): CompetitionAwardMode | null {
  if (competition.participationMode === 'INDIVIDUAL') return 'INDIVIDUAL';
  if (competition.participationMode === 'TEAM') return 'TEAM';
  return null;
}

function awardModeLocked(competition: Competition) {
  return fixedAwardMode(competition) !== null;
}

function awardHint(competition: Competition) {
  if (!competition.category) return '这条历史比赛缺少规范分类，暂不能添加新奖项。';
  if (requiresAwardRank(competition)) return '奖牌类奖项必须填写合法排名；团队奖可只绑定一名当前队员。';
  return '普通奖项不记录排名；个人奖绑定一人，团队奖至少绑定一人。';
}

function resetAwardDraft(competition: Competition) {
  awardDraft.awardMode = fixedAwardMode(competition) ?? 'TEAM';
  awardDraft.teamName = '';
  awardDraft.awardTier = awardTierOptions(competition)[0]?.code ?? '';
  awardDraft.rankPosition = requiresAwardRank(competition) ? 1 : null;
  awardDraft.rankTotal = requiresAwardRank(competition) ? 1 : null;
  awardDraft.recipientUsernames = [];
}

function awardValidation(competition: Competition) {
  if (!competition.category) return '这条历史比赛缺少规范分类，暂不能添加新奖项。';
  const fixedMode = fixedAwardMode(competition);
  if (fixedMode && awardDraft.awardMode !== fixedMode) return '奖项归属形态必须与比赛参赛形态一致。';
  if (!awardDraft.awardTier
    || !awardTierOptions(competition).some((option) => option.code === awardDraft.awardTier)) {
    return '请选择当前分类支持的奖项档位。';
  }
  const recipientCount = awardDraft.recipientUsernames.length;
  if (awardDraft.awardMode === 'INDIVIDUAL' && recipientCount !== 1) return '个人奖项必须且只能选择一名获奖人。';
  if (awardDraft.awardMode === 'TEAM' && recipientCount < 1) return '团队奖项至少需要选择一名获奖人。';
  const position = awardDraft.rankPosition;
  const total = awardDraft.rankTotal;
  if (requiresAwardRank(competition)
    && (typeof position !== 'number' || typeof total !== 'number'
      || !Number.isInteger(position) || !Number.isInteger(total)
      || position < 1 || total < 1 || position > total)) {
    return '排名必须满足 1 ≤ 名次 ≤ 总排名数。';
  }
  return '';
}

function submitAward(competition: Competition) {
  if (awardValidation(competition)) return;
  void run(async () => {
    await props.dashboard.addCompetitionAward(competition.id, {
      awardMode: awardDraft.awardMode,
      teamName: awardDraft.awardMode === 'TEAM' ? awardDraft.teamName.trim() || null : null,
      awardTier: awardDraft.awardTier as CompetitionAwardTier,
      rankPosition: requiresAwardRank(competition) ? awardDraft.rankPosition : null,
      rankTotal: requiresAwardRank(competition) ? awardDraft.rankTotal : null,
      recipientUsernames: [...awardDraft.recipientUsernames],
    });
    resetAwardDraft(competition);
    notice.value = '奖项已添加；新获奖人的个人名片展示默认关闭。';
  }, '奖项添加失败。');
}

function askMoveToRecycleBin(competition: Competition) {
  pendingAction.value = { kind: 'competition', competition };
}
function askDeleteParticipant(competition: Competition, participant: CompetitionParticipant) {
  pendingAction.value = { kind: 'participant', competition, participant };
}
function askDeleteAward(competition: Competition, award: CompetitionAward) {
  pendingAction.value = { kind: 'award', competition, award };
}
function askRestore(competition: Competition) {
  pendingAction.value = { kind: 'restore', competition };
}

function confirmPendingAction() {
  const action = pendingAction.value;
  if (!action) return;
  void run(async () => {
    if (action.kind === 'participant') {
      await props.dashboard.deleteCompetitionParticipant(action.competition.id, action.participant.id);
      if (action.participant.username) {
        awardDraft.recipientUsernames = awardDraft.recipientUsernames.filter(
          (username) => username !== action.participant.username,
        );
      }
      notice.value = '参赛者已删除。';
    } else if (action.kind === 'award') {
      await props.dashboard.deleteCompetitionAward(action.competition.id, action.award.id);
      notice.value = '奖项及其获奖人关联已删除。';
    } else if (action.kind === 'competition') {
      await props.dashboard.moveCompetitionToRecycleBin(action.competition.id);
      const pageNum = competitions.value.length === 1 && currentPage.value.pageNum > 1
        ? currentPage.value.pageNum - 1 : currentPage.value.pageNum;
      await fetchList('active', pageNum, currentPage.value.pageSize || 10);
      expandedId.value = null;
      notice.value = '比赛已移入七天回收站。';
    } else {
      await props.dashboard.restoreCompetition(action.competition.id);
      const recyclePage = competitions.value.length === 1 && currentPage.value.pageNum > 1
        ? currentPage.value.pageNum - 1 : currentPage.value.pageNum;
      await Promise.all([
        fetchList('recycle', recyclePage, currentPage.value.pageSize || 10),
        fetchList('active', 1, props.dashboard.adminCompetitions.value?.pageSize || 10),
      ]);
      notice.value = '比赛已恢复到当前比赛。';
    }
    pendingAction.value = null;
  }, '比赛管理操作失败。');
}

function retentionLabel(value: string | null) {
  if (!value) return '保留期未知';
  const remaining = new Date(value).getTime() + 7 * 24 * 60 * 60 * 1000 - Date.now();
  if (!Number.isFinite(remaining) || remaining <= 0) return '等待到期清理';
  const hours = Math.ceil(remaining / (60 * 60 * 1000));
  return hours > 24 ? `剩余 ${Math.ceil(hours / 24)} 天` : `剩余 ${hours} 小时`;
}
</script>
