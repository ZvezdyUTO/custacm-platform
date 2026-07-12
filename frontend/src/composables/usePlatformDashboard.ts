// Author: huangbingrui.awa
import { computed, onBeforeUnmount, ref, watch, type Ref } from 'vue';
import {
  batchCreateUsers as batchCreateUsersApi,
  deleteUser as deleteUserApi,
  getCollectionJob,
  deleteHomepageBanner as deleteHomepageBannerApi,
  listAdminUsers,
  listAdminArticles,
  listCollectionJobs,
  listHomepageBanners as listHomepageBannersApi,
  patchUser as patchUserApi,
  refreshWarehouse,
  reorderHomepageBanners as reorderHomepageBannersApi,
  startCollectionJob,
  uploadHomepageBanner as uploadHomepageBannerApi,
  updateOjHandles as updateOjHandlesApi,
  replaceOjHandle as replaceOjHandleApi,
  updateArticleFeatured as updateArticleFeaturedApi,
  deleteArticle as deleteArticleApi,
  listAdminCategories as listAdminCategoriesApi,
  createCategory as createCategoryApi,
  updateCategory as updateCategoryApi,
  deleteCategory as deleteCategoryApi,
  listAdminTags as listAdminTagsApi, createTag as createTagApi, deleteTag as deleteTagApi,
} from '../api/admin';
import { ApiError } from '../api/client';
import {
  getAcceptedSummary,
  getProblemFirstAccepted,
  getProblemSubmissions,
  getUserFirstAccepted,
  getUserSubmissions,
  listTrainingUsers,
} from '../api/training';
import { OJ_NAMES } from '../types';
import type {
  AcceptedSummary,
  AdminArticleListResponse,
  AdminCategoryPage,
  AdminTagPage,
  AdminUserCreateRequest,
  AdminUserMutationResponse,
  AdminUserPatchRequest,
  CollectionJob,
  CollectionJobStartRequest,
  CurrentUser,
  HomepageBannerImage,
  OjHandlesUpdateRequest,
  OjName,
  ProblemFirstAcceptedReport,
  ProblemSubmissionReport,
  TrainingQueryMode,
  TrainingQueryRange,
  TrainingUser,
  UserFirstAcceptedReport,
  UserSubmissionReport,
  Username,
  WarehouseRefreshRequest,
  WarehouseRefreshResult,
} from '../types';
import { runLimited } from '../utils/runLimited';

export interface MultiUserSummaryRow {
  user: TrainingUser;
  status: 'ready' | 'error';
  summary: AcceptedSummary | null;
  message: string | null;
}

export interface MultiUserLoadProgress {
  completed: number;
  total: number;
  active: boolean;
  failed: number;
}

type DashboardStatus = 'signed-out' | 'loading' | 'ready' | 'error';

function dateUtcPlus8(date: Date) {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit',
  }).format(date);
}

function recentWeek(): TrainingQueryRange {
  const end = new Date();
  const start = new Date(end);
  start.setDate(start.getDate() - 6);
  return {
    acceptedFromDateUtcPlus8: dateUtcPlus8(start),
    acceptedToDateUtcPlus8: dateUtcPlus8(end),
    minProblemRating: '',
    maxProblemRating: '',
  };
}

function errorMessageOf(error: unknown) {
  return error instanceof Error ? error.message : '请求失败。';
}

function isUnauthorized(error: unknown) {
  return error instanceof ApiError && error.status === 401;
}

function withoutGeneratedPassword(user: AdminUserMutationResponse): AdminUserMutationResponse {
  return { ...user, generatedPassword: null };
}

// Author: huangbingrui.awa
export function usePlatformDashboard(options: {
  token: Readonly<Ref<string | null>>;
  user: Readonly<Ref<CurrentUser | null>>;
  mode: Ref<TrainingQueryMode>;
  adminTrainingActive: Ref<boolean>;
  onUnauthorized(): void;
}) {
  const status = ref<DashboardStatus>(options.token.value ? 'loading' : 'signed-out');
  const errorMessage = ref<string | null>(null);
  const adminUsers = ref<AdminUserMutationResponse[]>([]);
  const trainingUsers = ref<TrainingUser[]>([]);
  const selectedUsername = ref<Username | null>(null);
  const selectedOjName = ref<OjName>(OJ_NAMES.CODEFORCES);
  const trainingQuery = ref<TrainingQueryRange>(recentWeek());
  const multiUserRows = ref<MultiUserSummaryRow[]>([]);
  const multiUserProgress = ref<MultiUserLoadProgress>({ completed: 0, total: 0, active: false, failed: 0 });
  const acceptedSummary = ref<AcceptedSummary | null>(null);
  const submissions = ref<UserSubmissionReport | null>(null);
  const firstAccepted = ref<UserFirstAcceptedReport | null>(null);
  const problemKey = ref('');
  const problemSubmissions = ref<ProblemSubmissionReport | null>(null);
  const problemFirstAccepted = ref<ProblemFirstAcceptedReport | null>(null);
  const submissionPage = ref(1);
  const submissionLimit = ref(15);
  const firstAcceptedPage = ref(1);
  const firstAcceptedLimit = ref(15);
  const problemSubmissionPage = ref(1);
  const problemSubmissionLimit = ref(15);
  const problemFirstAcceptedPage = ref(1);
  const problemFirstAcceptedLimit = ref(15);
  const collectionJob = ref<CollectionJob | null>(null);
  const collectionJobs = ref<CollectionJob[]>([]);
  const homepageBanners = ref<HomepageBannerImage[]>([]);
  const adminArticles = ref<AdminArticleListResponse | null>(null);
  const adminCategories = ref<AdminCategoryPage | null>(null);
  const adminTags = ref<AdminTagPage | null>(null);
  let requestSequence = 0;
  let pollTimer: number | null = null;

  const selectedTrainingUser = computed(() => (
    trainingUsers.value.find((item) => item.username === selectedUsername.value) ?? null
  ));

  function activeToken() {
    if (!options.token.value) throw new Error('请先登录。');
    return options.token.value;
  }

  function handleError(error: unknown) {
    if (isUnauthorized(error)) {
      options.onUnauthorized();
      return;
    }
    errorMessage.value = errorMessageOf(error);
  }

  async function loadMultiUserSummaries(sequence = ++requestSequence) {
    const token = activeToken();
    const users = trainingUsers.value.filter((item) => item.ojNames.includes(selectedOjName.value));
    multiUserProgress.value = { completed: 0, total: users.length, active: true, failed: 0 };
    multiUserRows.value = [];
    const results = await runLimited(users, 6, async (user) => {
      try {
        const summary = await getAcceptedSummary(token, user.username, trainingQuery.value, selectedOjName.value);
        return { user, status: 'ready' as const, summary, message: null };
      } catch (error) {
        if (isUnauthorized(error)) throw error;
        return { user, status: 'error' as const, summary: null, message: errorMessageOf(error) };
      } finally {
        if (sequence === requestSequence) {
          multiUserProgress.value = {
            ...multiUserProgress.value,
            completed: multiUserProgress.value.completed + 1,
          };
        }
      }
    });
    if (sequence !== requestSequence) return;
    const rows = results.flatMap((result) => result.status === 'fulfilled' ? [result.value] : []);
    multiUserRows.value = rows.sort((left, right) => left.user.username.localeCompare(right.user.username));
    multiUserProgress.value = {
      completed: rows.length,
      total: users.length,
      active: false,
      failed: rows.filter((row) => row.status === 'error').length,
    };
  }

  async function loadSingleDetails(sequence = ++requestSequence) {
    if (!selectedUsername.value) return;
    const token = activeToken();
    const username = selectedUsername.value;
    const [summary, submissionReport, acceptedReport] = await Promise.all([
      getAcceptedSummary(token, username, trainingQuery.value, selectedOjName.value),
      getUserSubmissions(token, username, trainingQuery.value, {
        page: submissionPage.value, limit: submissionLimit.value,
      }, selectedOjName.value),
      getUserFirstAccepted(token, username, trainingQuery.value, {
        page: firstAcceptedPage.value, limit: firstAcceptedLimit.value,
      }, selectedOjName.value),
    ]);
    if (sequence !== requestSequence) return;
    acceptedSummary.value = summary;
    submissions.value = submissionReport;
    firstAccepted.value = acceptedReport;
  }

  async function loadProblemDetails(sequence = ++requestSequence) {
    const key = problemKey.value.trim();
    if (!key) return;
    const token = activeToken();
    const [submissionReport, acceptedReport] = await Promise.all([
      getProblemSubmissions(token, key, trainingQuery.value, {
        page: problemSubmissionPage.value, limit: problemSubmissionLimit.value,
      }, selectedOjName.value),
      getProblemFirstAccepted(token, key, trainingQuery.value, {
        page: problemFirstAcceptedPage.value, limit: problemFirstAcceptedLimit.value,
      }, selectedOjName.value),
    ]);
    if (sequence !== requestSequence) return;
    problemSubmissions.value = submissionReport;
    problemFirstAccepted.value = acceptedReport;
  }

  async function refreshDashboard(mode = options.mode.value) {
    if (!options.token.value) {
      status.value = 'signed-out';
      return;
    }
    const sequence = ++requestSequence;
    status.value = 'loading';
    errorMessage.value = null;
    try {
      const token = activeToken();
      const users = await listTrainingUsers(token);
      if (sequence !== requestSequence) return;
      trainingUsers.value = users;
      if (selectedUsername.value && !users.some((item) => item.username === selectedUsername.value)) {
        selectedUsername.value = null;
        acceptedSummary.value = null;
        submissions.value = null;
        firstAccepted.value = null;
      }
      if (options.user.value?.role === 'ROLE_admin') {
        const [allUsers, jobs] = await Promise.all([
          listAdminUsers(token),
          options.adminTrainingActive.value ? listCollectionJobs(token) : Promise.resolve(collectionJobs.value),
        ]);
        if (sequence !== requestSequence) return;
        adminUsers.value = allUsers.map(withoutGeneratedPassword);
        collectionJobs.value = jobs;
      }
      if (mode === 'multiple') await loadMultiUserSummaries(sequence);
      if (mode === 'single') await loadSingleDetails(sequence);
      if (mode === 'problem') await loadProblemDetails(sequence);
      if (sequence === requestSequence) status.value = 'ready';
    } catch (error) {
      if (sequence !== requestSequence) return;
      status.value = 'error';
      handleError(error);
    }
  }

  async function applyTrainingQuery(query: TrainingQueryRange, mode = options.mode.value) {
    trainingQuery.value = { ...query };
    submissionPage.value = 1;
    firstAcceptedPage.value = 1;
    problemSubmissionPage.value = 1;
    problemFirstAcceptedPage.value = 1;
    await refreshDashboard(mode);
  }

  async function chooseUsername(username: Username) {
    selectedUsername.value = username;
    if (options.mode.value === 'single') await loadWithStatus(loadSingleDetails);
  }

  async function chooseOjName(ojName: OjName) {
    selectedOjName.value = ojName;
    await refreshDashboard(options.mode.value);
  }

  async function loadWithStatus(loader: () => Promise<void>) {
    status.value = 'loading';
    errorMessage.value = null;
    try {
      await loader();
      status.value = 'ready';
    } catch (error) {
      status.value = 'error';
      handleError(error);
    }
  }

  async function retryMultiUserSummary(username: Username) {
    const user = trainingUsers.value.find((item) => item.username === username);
    if (!user) return;
    try {
      const summary = await getAcceptedSummary(activeToken(), username, trainingQuery.value, selectedOjName.value);
      const row: MultiUserSummaryRow = { user, status: 'ready', summary, message: null };
      multiUserRows.value = [...multiUserRows.value.filter((item) => item.user.username !== username), row]
        .sort((left, right) => left.user.username.localeCompare(right.user.username));
    } catch (error) {
      handleError(error);
    }
  }

  async function changeSubmissionPage(page: number, limit: number) {
    submissionPage.value = page; submissionLimit.value = limit;
    await loadWithStatus(loadSingleDetails);
  }
  async function changeFirstAcceptedPage(page: number, limit: number) {
    firstAcceptedPage.value = page; firstAcceptedLimit.value = limit;
    await loadWithStatus(loadSingleDetails);
  }
  async function changeProblemSubmissionPage(page: number, limit: number) {
    problemSubmissionPage.value = page; problemSubmissionLimit.value = limit;
    await loadWithStatus(loadProblemDetails);
  }
  async function changeProblemFirstAcceptedPage(page: number, limit: number) {
    problemFirstAcceptedPage.value = page; problemFirstAcceptedLimit.value = limit;
    await loadWithStatus(loadProblemDetails);
  }

  async function batchCreateUsers(requests: AdminUserCreateRequest[]) {
    const results = await batchCreateUsersApi(activeToken(), requests);
    adminUsers.value = [...adminUsers.value, ...results.map(withoutGeneratedPassword)];
    return results;
  }
  async function patchUser(username: Username, patch: AdminUserPatchRequest) {
    const result = await patchUserApi(activeToken(), username, patch);
    adminUsers.value = adminUsers.value.map((item) => item.user.username === username
      ? withoutGeneratedPassword(result) : item);
    return result;
  }
  async function updateOjHandles(username: Username, request: OjHandlesUpdateRequest) {
    const result = await updateOjHandlesApi(activeToken(), username, request);
    adminUsers.value = adminUsers.value.map((item) => item.user.username === username
      ? withoutGeneratedPassword(result) : item);
    return result;
  }
  async function replaceOjHandle(username: Username, ojName: OjName, newHandle: string) {
    const result = await replaceOjHandleApi(activeToken(), username, { ojName, newHandle });
    adminUsers.value = adminUsers.value.map((item) => item.user.username === username
      ? withoutGeneratedPassword(result) : item);
    return result;
  }
  async function deleteUser(username: Username) {
    await deleteUserApi(activeToken(), username);
    adminUsers.value = adminUsers.value.filter((item) => item.user.username !== username);
  }

  function upsertJob(job: CollectionJob) {
    collectionJobs.value = [job, ...collectionJobs.value.filter((item) => item.jobId !== job.jobId)];
    collectionJob.value = job;
  }

  async function waitForJob(jobId: string): Promise<CollectionJob> {
    while (options.token.value) {
      const job = await getCollectionJob(activeToken(), jobId);
      upsertJob(job);
      if (job.status !== 'PENDING' && job.status !== 'RUNNING') return job;
      await new Promise((resolve) => { pollTimer = window.setTimeout(resolve, 1200); });
    }
    throw new Error('登录状态已失效。');
  }

  async function batchCollectSubmissions(request: CollectionJobStartRequest) {
    const started = await startCollectionJob(activeToken(), request);
    upsertJob(started);
    return started.status === 'PENDING' || started.status === 'RUNNING'
      ? waitForJob(started.jobId)
      : started;
  }

  async function refreshTrainingWarehouse(ojName: OjName, request: WarehouseRefreshRequest): Promise<WarehouseRefreshResult> {
    return refreshWarehouse(activeToken(), ojName, request);
  }

  async function loadHomepageBanners() {
    homepageBanners.value = await listHomepageBannersApi(activeToken());
  }

  async function uploadHomepageBanner(image: Blob) {
    const created = await uploadHomepageBannerApi(activeToken(), image);
    homepageBanners.value = [...homepageBanners.value, created]
      .sort((left, right) => left.sortOrder - right.sortOrder);
    return created;
  }

  async function reorderHomepageBanners(ids: number[]) {
    homepageBanners.value = await reorderHomepageBannersApi(activeToken(), ids);
  }

  async function deleteHomepageBanner(id: number) {
    homepageBanners.value = await deleteHomepageBannerApi(activeToken(), id);
  }

  async function loadAdminArticles(query: { title?: string; categoryId?: number | null; pageNum?: number; pageSize?: number } = {}) {
    adminArticles.value = await listAdminArticles(activeToken(), query);
    return adminArticles.value;
  }

  async function updateArticleFeatured(id: number, featured: boolean) {
    await updateArticleFeaturedApi(activeToken(), id, featured);
    if (adminArticles.value) {
      adminArticles.value = {
        ...adminArticles.value,
        blogs: {
          ...adminArticles.value.blogs,
          list: adminArticles.value.blogs.list.map((article) => article.id === id
            ? { ...article, recommend: featured }
            : article),
        },
      };
    }
  }

  async function deleteArticle(id: number) {
    await deleteArticleApi(activeToken(), id);
  }

  async function loadAdminCategories(pageNum = 1, pageSize = 10) {
    adminCategories.value = await listAdminCategoriesApi(activeToken(), pageNum, pageSize);
    return adminCategories.value;
  }

  async function createCategory(name: string, color: string) {
    await createCategoryApi(activeToken(), name, color);
    await loadAdminCategories(1, adminCategories.value?.pageSize || 10);
  }

  async function updateCategory(id: number, name: string, color: string) {
    await updateCategoryApi(activeToken(), { id, name, color });
    await loadAdminCategories(adminCategories.value?.pageNum || 1, adminCategories.value?.pageSize || 10);
  }

  async function loadAdminTags(pageNum = 1, pageSize = 10) { adminTags.value = await listAdminTagsApi(activeToken(), pageNum, pageSize); }
  async function createTag(name: string) { await createTagApi(activeToken(), name); await loadAdminTags(); }
  async function deleteTag(id: number) { await deleteTagApi(activeToken(), id); await loadAdminTags(adminTags.value?.pageNum || 1); }

  async function deleteCategory(id: number) {
    await deleteCategoryApi(activeToken(), id);
    const currentPage = adminCategories.value?.pageNum || 1;
    const nextPage = adminCategories.value?.list.length === 1 && currentPage > 1
      ? currentPage - 1
      : currentPage;
    await loadAdminCategories(nextPage, adminCategories.value?.pageSize || 10);
  }

  watch(() => options.token.value, (token) => {
    if (!token) {
      requestSequence += 1;
      status.value = 'signed-out';
      return;
    }
    void refreshDashboard(options.mode.value);
  }, { immediate: true });

  watch(options.mode, (mode) => {
    if (options.token.value) void refreshDashboard(mode);
  });

  onBeforeUnmount(() => {
    requestSequence += 1;
    if (pollTimer !== null) window.clearTimeout(pollTimer);
  });

  return {
    status, errorMessage, adminUsers, trainingUsers, selectedTrainingUser,
    selectedUsername, selectedOjName, trainingQuery, multiUserRows, multiUserProgress,
    acceptedSummary, submissions, firstAccepted, problemKey, problemSubmissions,
    problemFirstAccepted, submissionPage, submissionLimit, firstAcceptedPage,
    firstAcceptedLimit, problemSubmissionPage, problemSubmissionLimit,
    problemFirstAcceptedPage, problemFirstAcceptedLimit, collectionJob, collectionJobs,
    homepageBanners, adminArticles, adminCategories, adminTags,
    refreshDashboard, applyTrainingQuery, chooseUsername, chooseOjName,
    retryMultiUserSummary, changeSubmissionPage, changeFirstAcceptedPage,
    changeProblemSubmissionPage, changeProblemFirstAcceptedPage,
    batchCreateUsers, patchUser, updateOjHandles, replaceOjHandle, deleteUser,
    batchCollectSubmissions, refreshTrainingWarehouse,
    loadHomepageBanners, uploadHomepageBanner, reorderHomepageBanners, deleteHomepageBanner,
    loadAdminArticles, updateArticleFeatured, deleteArticle,
    loadAdminCategories, createCategory, updateCategory, deleteCategory,
    loadAdminTags, createTag, deleteTag,
  };
}
