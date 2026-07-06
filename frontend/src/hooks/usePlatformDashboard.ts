import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ApiError,
  batchCreateUsers,
  checkAuthHealth,
  checkTrainingDataHealth,
  collectCodeforcesSubmissions,
  createCodeforcesHandle,
  deleteAdminUser,
  getAcceptedSummary,
  getAutoCollectAcceptedSummaries,
  getCodeforcesHandle,
  getCodeforcesSubmissionCollectionJob,
  getCurrentUser,
  getFirstAcceptedProblems,
  getStudentSubmissions,
  listCodeforcesSubmissionCollectionJobs,
  listUsers,
  login,
  purgeCodeforcesStudentData,
  refreshCodeforcesWarehouse,
  startCodeforcesSubmissionCollectionJob,
  upsertCodeforcesSubmissions,
  updateAdminUser,
  updateCodeforcesHandleAccount,
} from '../api/platform';
import { defaultLookbackHours, seededStudentIdentities } from '../data/dashboard';
import type {
  AuthUser,
  BatchCollectOptions,
  BatchCollectSummary,
  BatchCollectStudentResult,
  BatchStudentImportRow,
  BatchStudentImportSummary,
  CodeforcesAcceptedSummary,
  FullUserDataDeleteSummary,
  CodeforcesFirstAcceptedReport,
  CodeforcesHandleOperationResult,
  CodeforcesOdsBatchUpsertResponse,
  CodeforcesSubmissionCollectionJobItem,
  CodeforcesSubmissionCollectionJobResponse,
  CodeforcesStudentSubmissionReport,
  CurrentUser,
  DashboardOperation,
  ServiceHealth,
  SubmissionPageQuery,
  StudentIdentity,
  StudentTrainingRecord,
  TrainingQueryMode,
  TrainingQueryRange,
  UserInfoUpdateInput,
  UserInfoUpdateSummary,
  WarehouseRefreshResult,
} from '../types';

const TOKEN_STORAGE_KEY = 'custacm.platform.accessToken';
const USER_STORAGE_KEY = 'custacm.platform.currentUser';
const COLLECTION_JOB_STORAGE_KEY = 'custacm.platform.codeforcesCollectionJobId';
const COLLECTION_JOB_POLL_INTERVAL_MS = 1500;
const COLLECTION_JOBS_LIST_POLL_INTERVAL_MS = 3000;
const UTC_PLUS_8_OFFSET_MS = 8 * 60 * 60 * 1000;
const RECENT_WEEK_DAYS = 7;

type DashboardStatus = 'signed-out' | 'loading' | 'ready' | 'error';

interface LoginCredentials {
  studentIdentity: StudentIdentity;
  password: string;
}

interface RefreshDashboardOptions {
  loadStudentDetails?: boolean;
}

const emptyTrainingQuery: TrainingQueryRange = {
  acceptedFromDateUtcPlus8: '',
  acceptedToDateUtcPlus8: '',
  minProblemRating: '',
  maxProblemRating: '',
};

const defaultSubmissionPage: SubmissionPageQuery = {
  page: 1,
  limit: 100,
};

function readStoredToken() {
  return window.localStorage.getItem(TOKEN_STORAGE_KEY);
}

function formatDateUtcPlus8(date: Date) {
  return new Date(date.getTime() + UTC_PLUS_8_OFFSET_MS).toISOString().slice(0, 10);
}

function recentWeekTrainingQuery(now = new Date()): TrainingQueryRange {
  const start = new Date(now.getTime() - (RECENT_WEEK_DAYS - 1) * 24 * 60 * 60 * 1000);
  return {
    ...emptyTrainingQuery,
    acceptedFromDateUtcPlus8: formatDateUtcPlus8(start),
    acceptedToDateUtcPlus8: formatDateUtcPlus8(now),
  };
}

function readStoredUser() {
  const raw = window.localStorage.getItem(USER_STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as CurrentUser;
  } catch {
    return null;
  }
}

function writeSession(token: string, user: CurrentUser) {
  window.localStorage.setItem(TOKEN_STORAGE_KEY, token);
  window.localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
}

function clearSession() {
  window.localStorage.removeItem(TOKEN_STORAGE_KEY);
  window.localStorage.removeItem(USER_STORAGE_KEY);
  window.localStorage.removeItem(COLLECTION_JOB_STORAGE_KEY);
}

function operationStatusFromError(error: unknown) {
  return error instanceof ApiError && error.status >= 500 ? 'failed' : 'pending';
}

function operationStatusFromCollection(status: string): DashboardOperation['status'] {
  if (status === 'RUNNING') {
    return 'syncing';
  }
  if (status === 'FAILED') {
    return 'failed';
  }
  if (status === 'SKIPPED' || status === 'PARTIAL_SUCCESS') {
    return 'pending';
  }
  return 'completed';
}

function batchSummaryFromCollectionJob(job: CodeforcesSubmissionCollectionJobResponse): BatchCollectSummary {
  return {
    requestedCount: job.requestedCount,
    collectedCount: job.collectedCount,
    failedCount: job.failedCount,
    refreshedCount: job.refreshedCount,
    writtenRows: job.writtenRows,
    batchIds: job.batchIds,
    results: job.items.map(collectionJobItemToBatchResult),
  };
}

function collectionJobItemToBatchResult(item: CodeforcesSubmissionCollectionJobItem): BatchCollectStudentResult {
  return {
    studentIdentity: item.studentIdentity,
    status: batchCollectStatusFromJobItem(item),
    handle: item.handle,
    batchId: item.batchId,
    writtenRows: item.writtenRows,
    fetchedSubmissionCount: item.fetchedSubmissionCount,
    matchedSubmissionCount: item.matchedSubmissionCount,
    message: item.message,
    refreshStatus: item.refreshStatus,
    refreshMessage: item.refreshMessage,
  };
}

function batchCollectStatusFromJobItem(item: CodeforcesSubmissionCollectionJobItem) {
  if (item.itemStatus === 'PENDING' || item.itemStatus === 'RUNNING') {
    return item.itemStatus;
  }
  return item.collectionStatus ?? (item.itemStatus === 'SUCCESS' ? 'SUCCESS' : 'FAILED');
}

function sleep(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

function upsertCollectionJob(
  jobs: CodeforcesSubmissionCollectionJobResponse[],
  job: CodeforcesSubmissionCollectionJobResponse,
) {
  return [job, ...jobs.filter((item) => item.jobId !== job.jobId)]
    .sort((left, right) => new Date(right.startedAt).getTime() - new Date(left.startedAt).getTime())
    .slice(0, 50);
}

function formatError(error: unknown) {
  if (error instanceof ApiError) {
    return error.code ? `${error.code}: ${error.message}` : error.message;
  }
  return error instanceof Error ? error.message : 'unknown error';
}

function nowTime() {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date());
}

function shouldLoadTrainingData(studentIdentity: StudentIdentity) {
  return seededStudentIdentities.includes(studentIdentity) || /^\d{6,}.+/.test(studentIdentity);
}

function operation(title: string, detail: string, status: DashboardOperation['status']): DashboardOperation {
  return {
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    title,
    detail,
    status,
    time: nowTime(),
  };
}

export function usePlatformDashboard() {
  const [token, setToken] = useState<string | null>(() => readStoredToken());
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(() => readStoredUser());
  const [status, setStatus] = useState<DashboardStatus>(token ? 'loading' : 'signed-out');
  const [health, setHealth] = useState<ServiceHealth[]>([]);
  const [users, setUsers] = useState<AuthUser[]>([]);
  const [records, setRecords] = useState<StudentTrainingRecord[]>([]);
  const [autoCollectAcceptedSummaries, setAutoCollectAcceptedSummaries] = useState<CodeforcesAcceptedSummary[]>([]);
  const [selectedIdentity, setSelectedIdentity] = useState<StudentIdentity | null>(null);
  const [submissions, setSubmissions] = useState<CodeforcesStudentSubmissionReport | null>(null);
  const [firstAccepted, setFirstAccepted] = useState<CodeforcesFirstAcceptedReport | null>(null);
  const [lastBatch, setLastBatch] = useState<CodeforcesOdsBatchUpsertResponse | null>(null);
  const [lastRefresh, setLastRefresh] = useState<WarehouseRefreshResult | null>(null);
  const [collectionJob, setCollectionJob] = useState<CodeforcesSubmissionCollectionJobResponse | null>(null);
  const [collectionJobSummary, setCollectionJobSummary] = useState<BatchCollectSummary | null>(null);
  const [collectionJobs, setCollectionJobs] = useState<CodeforcesSubmissionCollectionJobResponse[]>([]);
  const [operations, setOperations] = useState<DashboardOperation[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [trainingQuery, setTrainingQuery] = useState<TrainingQueryRange>(() => recentWeekTrainingQuery());
  const [submissionPage, setSubmissionPage] = useState(defaultSubmissionPage.page);
  const [submissionLimit, setSubmissionLimit] = useState(defaultSubmissionPage.limit);
  const [studentDetailsRequested, setStudentDetailsRequested] = useState(false);
  const detailsRequestSeq = useRef(0);
  const collectionJobPollingRef = useRef<{
    jobId: string;
    promise: Promise<BatchCollectSummary>;
  } | null>(null);

  const addOperation = useCallback((title: string, detail: string, operationStatus: DashboardOperation['status']) => {
    setOperations((current) => [operation(title, detail, operationStatus), ...current].slice(0, 12));
  }, []);

  const refreshHealth = useCallback(async () => {
    const nextHealth = await Promise.all([checkAuthHealth(), checkTrainingDataHealth()]);
    setHealth(nextHealth);
    return nextHealth;
  }, []);

  const clearStudentDetails = useCallback(() => {
    detailsRequestSeq.current += 1;
    setSubmissions(null);
    setFirstAccepted(null);
    setStudentDetailsRequested(false);
  }, []);

  const loadStudentDetails = useCallback(async (
    identity: StudentIdentity,
    query: TrainingQueryRange = trainingQuery,
    pagination: SubmissionPageQuery = { page: submissionPage, limit: submissionLimit },
  ) => {
    const requestSeq = detailsRequestSeq.current + 1;
    detailsRequestSeq.current = requestSeq;
    setSubmissions(null);
    setFirstAccepted(null);
    setStudentDetailsRequested(true);

    const [summaryResult, submissionsResult, firstAcceptedResult] = await Promise.allSettled([
      getAcceptedSummary(identity, query),
      getStudentSubmissions(identity, query, pagination),
      getFirstAcceptedProblems(identity, query),
    ]);

    if (requestSeq !== detailsRequestSeq.current) {
      return false;
    }

    if (summaryResult.status === 'fulfilled') {
      setRecords((current) => current.map((record) => (
        record.studentIdentity === identity
          ? {
            ...record,
            acceptedSummary: summaryResult.value,
            summaryStatus: 'loaded',
            errorMessage: null,
          }
          : record
      )));
    } else {
      setRecords((current) => current.map((record) => (
        record.studentIdentity === identity
          ? {
            ...record,
            acceptedSummary: null,
            summaryStatus: summaryResult.reason instanceof ApiError && summaryResult.reason.status === 404
              ? 'missing'
              : 'error',
            errorMessage: formatError(summaryResult.reason),
          }
          : record
      )));
    }

    if (submissionsResult.status === 'fulfilled') {
      setSubmissions(submissionsResult.value);
    } else {
      setSubmissions(null);
    }

    if (firstAcceptedResult.status === 'fulfilled') {
      setFirstAccepted(firstAcceptedResult.value);
    } else {
      setFirstAccepted(null);
    }
    return true;
  }, [submissionLimit, submissionPage, trainingQuery]);

  const refreshDashboard = useCallback(async (
    queryOverride?: TrainingQueryRange,
    submissionPaginationOverride?: SubmissionPageQuery,
    options: RefreshDashboardOptions = {},
  ) => {
    const effectiveQuery = queryOverride ?? trainingQuery;
    const effectiveSubmissionPagination = submissionPaginationOverride ?? {
      page: submissionPage,
      limit: submissionLimit,
    };
    const shouldLoadStudentDetails = options.loadStudentDetails ?? studentDetailsRequested;
    setStatus(token ? 'loading' : 'signed-out');
    setErrorMessage(null);
    await refreshHealth();

    try {
      let userList: AuthUser[] = [];
      let authenticatedUser: CurrentUser | null = null;
      if (token) {
        authenticatedUser = await getCurrentUser(token);
        setCurrentUser(authenticatedUser);
        writeSession(token, authenticatedUser);
      } else {
        setCurrentUser(null);
      }
      try {
        userList = await listUsers(token ?? undefined);
      } catch (error) {
        if (authenticatedUser) {
          userList = [{
            studentIdentity: authenticatedUser.studentIdentity,
            role: authenticatedUser.role,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          }];
        }
        addOperation('用户列表加载失败', formatError(error), operationStatusFromError(error));
      }
      setUsers(userList);
      const identities = userList.length > 0
        ? Array.from(new Set(userList.map((user) => user.studentIdentity)))
        : seededStudentIdentities;
      try {
        setAutoCollectAcceptedSummaries(await getAutoCollectAcceptedSummaries(effectiveQuery));
      } catch (error) {
        setAutoCollectAcceptedSummaries([]);
        addOperation('自动更新用户汇总失败', formatError(error), operationStatusFromError(error));
      }
      const nextRecords = await Promise.all(
        identities.map(async (studentIdentity) => {
          const user = userList.find((item) => item.studentIdentity === studentIdentity);
          const role = user?.role ?? 'player';
          const updatedAt = user?.updatedAt ?? new Date().toISOString();
          if (!shouldLoadTrainingData(studentIdentity)) {
            return {
              studentIdentity,
              role,
              handle: null,
              needCollect: null,
              handleStatus: 'missing',
              acceptedSummary: null,
              summaryStatus: 'not-requested',
              errorMessage: null,
              updatedAt,
            } satisfies StudentTrainingRecord;
          }
          try {
            const handle = await getCodeforcesHandle(studentIdentity);
            return {
              studentIdentity,
              role,
              handle: handle.handle,
              needCollect: handle.needCollect ?? true,
              handleStatus: 'bound',
              acceptedSummary: null,
              summaryStatus: 'not-requested',
              errorMessage: null,
              updatedAt,
            } satisfies StudentTrainingRecord;
          } catch (error) {
            return {
              studentIdentity,
              role,
              handle: null,
              needCollect: null,
              handleStatus: error instanceof ApiError && error.status === 404 ? 'missing' : 'error',
              acceptedSummary: null,
              summaryStatus: 'not-requested',
              errorMessage: error instanceof ApiError && error.status === 404 ? null : formatError(error),
              updatedAt,
            } satisfies StudentTrainingRecord;
          }
        }),
      );

      setRecords(nextRecords);
      const nextSelected =
        selectedIdentity && nextRecords.some((record) => record.studentIdentity === selectedIdentity)
          ? selectedIdentity
          : null;
      setSelectedIdentity(nextSelected);
      if (!shouldLoadStudentDetails) {
        clearStudentDetails();
      }
      const detailsApplied = nextSelected && shouldLoadStudentDetails
        ? await loadStudentDetails(nextSelected, effectiveQuery, effectiveSubmissionPagination)
        : true;
      if (detailsApplied) {
        setStatus('ready');
      }
    } catch (error) {
      const message = formatError(error);
      if (error instanceof ApiError && (error.status === 401 || error.status === 403)) {
        clearSession();
        setToken(null);
        setCurrentUser(null);
        setStatus('signed-out');
      } else {
        setStatus('error');
      }
      setErrorMessage(message);
      addOperation('刷新工作台失败', message, operationStatusFromError(error));
    }
  }, [
    addOperation,
    clearStudentDetails,
    loadStudentDetails,
    refreshHealth,
    selectedIdentity,
    studentDetailsRequested,
    submissionLimit,
    submissionPage,
    token,
    trainingQuery,
  ]);

  const signIn = useCallback(
    async ({ studentIdentity, password }: LoginCredentials) => {
      setStatus('loading');
      setErrorMessage(null);
      try {
        const result = await login(studentIdentity, password);
        writeSession(result.accessToken, result.user);
        setToken(result.accessToken);
        setCurrentUser(result.user);
        addOperation('登录成功', `${result.user.studentIdentity} / ${result.user.role}`, 'completed');
      } catch (error) {
        const message = formatError(error);
        setStatus('signed-out');
        setErrorMessage(message);
        addOperation('登录失败', message, 'failed');
        throw error;
      }
    },
    [addOperation],
  );

  const signOut = useCallback(() => {
    clearSession();
    setToken(null);
    setCurrentUser(null);
    setStatus('signed-out');
    setUsers([]);
    setRecords([]);
    setAutoCollectAcceptedSummaries([]);
    setSubmissions(null);
    setFirstAccepted(null);
    setStudentDetailsRequested(false);
    setTrainingQuery(recentWeekTrainingQuery());
    setCollectionJob(null);
    setCollectionJobSummary(null);
    setCollectionJobs([]);
    setSubmissionPage(defaultSubmissionPage.page);
    setSubmissionLimit(defaultSubmissionPage.limit);
    addOperation('退出登录', '已清除本地 access token', 'completed');
  }, [addOperation]);

  const chooseIdentity = useCallback(
    async (identity: StudentIdentity) => {
      setSubmissionPage(defaultSubmissionPage.page);
      setSelectedIdentity(identity);
      clearStudentDetails();
      addOperation('选择选手', identity, 'completed');
    },
    [addOperation, clearStudentDetails],
  );

  const refreshWarehouseBatch = useCallback(async (batchId: string) => {
    if (!token) {
      throw new Error('需要先登录 admin 账号才能刷新仓库。');
    }
    const result = await refreshCodeforcesWarehouse(token, batchId);
    setLastRefresh(result);
    addOperation(
      'Codeforces 仓库刷新',
      `${result.status}: ${result.tasks.length} 个 SQL task`,
      result.status === 'SUCCESS' ? 'completed' : 'failed',
    );
    return result;
  }, [addOperation, token]);

  const applyTrainingQuery = useCallback(async (
    nextQuery: TrainingQueryRange,
    mode: TrainingQueryMode = 'multiple',
  ) => {
    const shouldLoadStudentDetails = mode === 'single';
    setTrainingQuery(nextQuery);
    setSubmissionPage(defaultSubmissionPage.page);
    if (!shouldLoadStudentDetails) {
      clearStudentDetails();
    }
    addOperation(
      '训练数据查询范围更新',
      querySummary(nextQuery),
      'completed',
    );
    await refreshDashboard(nextQuery, {
      page: defaultSubmissionPage.page,
      limit: submissionLimit,
    }, {
      loadStudentDetails: shouldLoadStudentDetails,
    });
  }, [addOperation, clearStudentDetails, refreshDashboard, submissionLimit]);

  const changeSubmissionPage = useCallback(async (nextPage: number, nextLimit: number) => {
    if (!selectedIdentity || !submissions) {
      return;
    }
    const normalizedPage = Math.max(1, nextPage);
    setStatus('loading');
    setSubmissionPage(normalizedPage);
    setSubmissionLimit(nextLimit);
    const detailsApplied = await loadStudentDetails(selectedIdentity, trainingQuery, {
      page: normalizedPage,
      limit: nextLimit,
    });
    if (detailsApplied) {
      setStatus('ready');
    }
  }, [loadStudentDetails, selectedIdentity, submissions, trainingQuery]);

  const collectSelectedIdentity = useCallback(async () => {
    if (!token || !selectedIdentity) {
      return;
    }
    try {
      const result = await collectCodeforcesSubmissions(token, selectedIdentity, defaultLookbackHours);
      let warehouseStatus = '';
      if (result.batchId) {
        setLastBatch({
          batchId: result.batchId,
          tableName: result.tableName ?? 'ods_codeforces__submission',
          writtenRows: result.writtenRows,
          fetchedAt: result.fetchedAt ?? new Date().toISOString(),
        });
        try {
          const refreshResult = await refreshWarehouseBatch(result.batchId);
          warehouseStatus = refreshResult.status === 'SUCCESS' ? '，已刷新仓库' : '，仓库刷新失败';
        } catch (error) {
          const message = formatError(error);
          warehouseStatus = '，仓库刷新失败';
          addOperation('仓库刷新失败', message, 'failed');
          setErrorMessage(message);
        }
      }
      addOperation(
        'Codeforces 最近提交采集',
        `${selectedIdentity}: ${result.status}, 写入 ${result.writtenRows} 行${warehouseStatus}`,
        operationStatusFromCollection(result.status),
      );
      await refreshDashboard();
    } catch (error) {
      const message = formatError(error);
      addOperation('Codeforces 采集失败', message, 'failed');
      setErrorMessage(message);
    }
  }, [addOperation, refreshDashboard, refreshWarehouseBatch, selectedIdentity, token]);

  const batchImportStudents = useCallback(async (
    rows: BatchStudentImportRow[],
  ): Promise<BatchStudentImportSummary> => {
    if (!token) {
      throw new Error('需要先登录 admin 账号才能批量录入学生信息。');
    }
    if (rows.length === 0) {
      throw new Error('请先输入至少一条学生信息。');
    }

    setStatus('loading');
    setErrorMessage(null);
    try {
      const userResults = await batchCreateUsers(
        token,
        rows.map(({ studentIdentity, role, password }) => ({ studentIdentity, role, password })),
      );
      const handleRows = rows.filter((row) => row.handle);
      const handleResults: CodeforcesHandleOperationResult[] = [];

      for (const row of handleRows) {
        try {
          const result = await createCodeforcesHandle(token, row.studentIdentity, row.handle ?? '');
          handleResults.push({
            success: true,
            studentIdentity: result.studentIdentity,
            handle: result.handle,
            needCollect: result.needCollect ?? true,
            errorCode: null,
            message: 'handle created',
          });
        } catch (error) {
          handleResults.push({
            success: false,
            studentIdentity: row.studentIdentity,
            handle: row.handle ?? null,
            needCollect: null,
            errorCode: error instanceof ApiError ? error.code : null,
            message: formatError(error),
          });
        }
      }

      const successfulUsers = userResults.filter((item) => item.success).length;
      const successfulHandles = handleResults.filter((item) => item.success).length;
      const failedItems = (userResults.length - successfulUsers) + (handleResults.length - successfulHandles);
      addOperation(
        '批量录入学生信息',
        `账号 ${successfulUsers}/${userResults.length}，CF 绑定 ${successfulHandles}/${handleResults.length}`,
        failedItems === 0 ? 'completed' : successfulUsers + successfulHandles === 0 ? 'failed' : 'pending',
      );
      await refreshDashboard();
      return { userResults, handleResults };
    } catch (error) {
      const message = formatError(error);
      addOperation('批量录入学生信息失败', message, 'failed');
      setErrorMessage(message);
      setStatus('error');
      throw error;
    }
  }, [addOperation, refreshDashboard, token]);

  const updateStudentInfo = useCallback(async (
    input: UserInfoUpdateInput,
  ): Promise<UserInfoUpdateSummary> => {
    if (!token) {
      throw new Error('需要先登录 admin 账号才能修改用户信息。');
    }
    if (!input.studentIdentity) {
      throw new Error('请先选择一个用户。');
    }

    setStatus('loading');
    setErrorMessage(null);
    try {
      const userResult = await updateAdminUser(token, input.studentIdentity, {
        role: input.role,
        newPassword: input.newPassword,
      });
      let handleResult: CodeforcesHandleOperationResult | null = null;

      if (input.handle) {
        try {
          const result = await createCodeforcesHandle(token, input.studentIdentity, input.handle);
          const effectiveAccount = input.needCollect === undefined || result.needCollect === input.needCollect
            ? result
            : await updateCodeforcesHandleAccount(token, input.studentIdentity, input.needCollect);
          handleResult = {
            success: true,
            studentIdentity: effectiveAccount.studentIdentity,
            handle: effectiveAccount.handle,
            needCollect: effectiveAccount.needCollect,
            errorCode: null,
            message: input.needCollect === false ? 'handle created, collection disabled' : 'handle created',
          };
        } catch (error) {
          handleResult = {
            success: false,
            studentIdentity: input.studentIdentity,
            handle: input.handle,
            needCollect: input.needCollect ?? null,
            errorCode: error instanceof ApiError ? error.code : null,
            message: formatError(error),
          };
        }
      } else if (input.needCollect !== undefined) {
        try {
          const result = await updateCodeforcesHandleAccount(token, input.studentIdentity, input.needCollect);
          handleResult = {
            success: true,
            studentIdentity: result.studentIdentity,
            handle: result.handle,
            needCollect: result.needCollect,
            errorCode: null,
            message: 'collection flag updated',
          };
        } catch (error) {
          handleResult = {
            success: false,
            studentIdentity: input.studentIdentity,
            handle: null,
            needCollect: input.needCollect,
            errorCode: error instanceof ApiError ? error.code : null,
            message: formatError(error),
          };
        }
      }

      addOperation(
        '修改用户信息',
        `${input.studentIdentity}: ${userResult.user?.role ?? input.role}${
          handleResult ? `, CF ${handleResult.success ? '绑定成功' : '绑定失败'}` : ''
        }`,
        userResult.success && (!handleResult || handleResult.success) ? 'completed' : 'pending',
      );
      await refreshDashboard();
      return { userResult, handleResult };
    } catch (error) {
      const message = formatError(error);
      addOperation('修改用户信息失败', message, 'failed');
      setErrorMessage(message);
      setStatus('error');
      throw error;
    }
  }, [addOperation, refreshDashboard, token]);

  const applyCollectionJobSnapshot = useCallback((job: CodeforcesSubmissionCollectionJobResponse) => {
    const summary = batchSummaryFromCollectionJob(job);
    setCollectionJobs((current) => upsertCollectionJob(current, job));
    setCollectionJob(job.status === 'RUNNING' ? job : null);
    setCollectionJobSummary(summary);

    const latestBatchItem = [...job.items].reverse().find((item) => item.batchId);
    if (latestBatchItem?.batchId) {
      setLastBatch({
        batchId: latestBatchItem.batchId,
        tableName: latestBatchItem.tableName ?? 'ods_codeforces__submission',
        writtenRows: latestBatchItem.writtenRows,
        fetchedAt: latestBatchItem.fetchedAt ?? new Date().toISOString(),
      });
    }
    return summary;
  }, []);

  const waitForCollectionJob = useCallback((
    jobId: string,
    options: { resumed?: boolean } = {},
  ): Promise<BatchCollectSummary> => {
    if (!token) {
      return Promise.reject(new Error('需要先登录 admin 账号才能批量采集。'));
    }
    if (collectionJobPollingRef.current?.jobId === jobId) {
      return collectionJobPollingRef.current.promise;
    }

    const promise = (async () => {
      try {
        while (true) {
          const job = await getCodeforcesSubmissionCollectionJob(token, jobId);
          const summary = applyCollectionJobSnapshot(job);
          if (job.status !== 'RUNNING') {
            window.localStorage.removeItem(COLLECTION_JOB_STORAGE_KEY);
            setCollectionJob(null);
            addOperation(
              'Codeforces 批量采集完成',
              `采集 ${job.collectedCount}/${job.requestedCount}，刷新 ${job.refreshedCount}，写入 ${job.writtenRows} 行`,
              operationStatusFromCollection(job.status),
            );
            await refreshDashboard();
            return summary;
          }

          window.localStorage.setItem(COLLECTION_JOB_STORAGE_KEY, job.jobId);
          await sleep(COLLECTION_JOB_POLL_INTERVAL_MS);
        }
      } catch (error) {
        if (options.resumed && error instanceof ApiError && error.status === 404) {
          window.localStorage.removeItem(COLLECTION_JOB_STORAGE_KEY);
          setCollectionJob(null);
          setCollectionJobSummary(null);
          addOperation('Codeforces 采集任务状态失效', '后端没有找到上次保存的采集任务', 'pending');
        } else {
          const message = formatError(error);
          addOperation('Codeforces 采集任务查询失败', message, 'failed');
          setErrorMessage(message);
        }
        throw error;
      } finally {
        if (collectionJobPollingRef.current?.jobId === jobId) {
          collectionJobPollingRef.current = null;
        }
      }
    })();

    collectionJobPollingRef.current = { jobId, promise };
    return promise;
  }, [addOperation, applyCollectionJobSnapshot, refreshDashboard, token]);

  const batchCollectSubmissions = useCallback(async (
    options: BatchCollectOptions,
  ): Promise<BatchCollectSummary> => {
    if (!token) {
      throw new Error('需要先登录 admin 账号才能批量采集。');
    }
    const identities = Array.from(new Set(options.studentIdentities.map((item) => item.trim()).filter(Boolean)));
    if (identities.length === 0) {
      throw new Error('没有可采集的 Codeforces 绑定选手。');
    }

    const lookbackHours = Math.max(1, Math.floor(options.lookbackHours));
    setErrorMessage(null);
    setCollectionJobSummary(null);

    const job = await startCodeforcesSubmissionCollectionJob(token, {
      studentIdentities: identities,
      lookbackHours,
      refreshWarehouse: options.refreshWarehouse,
    });
    window.localStorage.setItem(COLLECTION_JOB_STORAGE_KEY, job.jobId);
    const summary = applyCollectionJobSnapshot(job);
    addOperation(
      'Codeforces 批量采集已启动',
      `任务 ${job.jobId}，选手 ${job.requestedCount} 个，窗口 ${lookbackHours} 小时`,
      'syncing',
    );
    if (job.status !== 'RUNNING') {
      window.localStorage.removeItem(COLLECTION_JOB_STORAGE_KEY);
      setCollectionJob(null);
      return summary;
    }
    return waitForCollectionJob(job.jobId);
  }, [addOperation, applyCollectionJobSnapshot, token, waitForCollectionJob]);

  const deleteFullUserData = useCallback(async (
    studentIdentity: StudentIdentity,
  ): Promise<FullUserDataDeleteSummary> => {
    if (!token) {
      throw new Error('需要先登录 admin 账号才能删除用户数据。');
    }
    const normalizedIdentity = studentIdentity.trim();
    if (!normalizedIdentity) {
      throw new Error('请先选择一个用户。');
    }
    if (currentUser?.studentIdentity === normalizedIdentity) {
      throw new Error('不能删除当前登录用户。');
    }

    setStatus('loading');
    setErrorMessage(null);
    try {
      const trainingDataResult = await purgeCodeforcesStudentData(token, normalizedIdentity);
      const authUserResult = await deleteAdminUser(token, normalizedIdentity);
      if (!authUserResult.success) {
        throw new Error(authUserResult.message ?? 'auth 账号删除失败。');
      }
      addOperation(
        '彻底删除用户数据',
        `${normalizedIdentity}: 训练数据 ${trainingDataResult.totalDeletedRows} 行，auth ${
          authUserResult.success ? '已删除' : '删除失败'
        }`,
        authUserResult.success ? 'completed' : 'failed',
      );
      await refreshDashboard();
      return { trainingDataResult, authUserResult };
    } catch (error) {
      const message = formatError(error);
      addOperation('彻底删除用户数据失败', `${normalizedIdentity}: ${message}`, 'failed');
      setErrorMessage(message);
      setStatus('error');
      throw error;
    }
  }, [addOperation, currentUser?.studentIdentity, refreshDashboard, token]);

  const importOdsFile = useCallback(
    async (file: File) => {
      if (!token) {
        setErrorMessage('需要先登录 admin 账号才能导入 ODS。');
        return;
      }
      try {
        const text = await file.text();
        const payload = JSON.parse(text) as unknown;
        const result = await upsertCodeforcesSubmissions(token, payload);
        setLastBatch(result);
        addOperation(
          'ODS Codeforces 批量 upsert',
          `写入 ${result.writtenRows} 行，batchId=${result.batchId}`,
          'completed',
        );
        try {
          await refreshWarehouseBatch(result.batchId);
        } catch (error) {
          const message = formatError(error);
          addOperation('ODS 导入后仓库刷新失败', message, 'failed');
          setErrorMessage(message);
        }
        await refreshDashboard();
      } catch (error) {
        const message = formatError(error);
        addOperation('ODS 导入失败', message, 'failed');
        setErrorMessage(message);
      }
    },
    [addOperation, refreshDashboard, refreshWarehouseBatch, token],
  );

  const refreshWarehouse = useCallback(async () => {
    if (!token || !lastBatch) {
      return;
    }
    try {
      await refreshWarehouseBatch(lastBatch.batchId);
      await refreshDashboard();
    } catch (error) {
      const message = formatError(error);
      addOperation('仓库刷新失败', message, 'failed');
      setErrorMessage(message);
    }
  }, [addOperation, lastBatch, refreshDashboard, refreshWarehouseBatch, token]);

  const refreshDashboardRef = useRef(refreshDashboard);

  useEffect(() => {
    refreshDashboardRef.current = refreshDashboard;
  }, [refreshDashboard]);

  useEffect(() => {
    void refreshDashboardRef.current();
  }, [token]);

  useEffect(() => {
    if (!token) {
      return;
    }
    const storedJobId = window.localStorage.getItem(COLLECTION_JOB_STORAGE_KEY);
    if (!storedJobId) {
      return;
    }
    void waitForCollectionJob(storedJobId, { resumed: true }).catch(() => {
      // The poller already records the visible error or stale-job notice.
    });
  }, [token, waitForCollectionJob]);

  useEffect(() => {
    if (!token || currentUser?.role !== 'admin') {
      setCollectionJobs([]);
      return;
    }

    const adminToken = token;
    let cancelled = false;
    async function pollCollectionJobs() {
      try {
        const jobs = await listCodeforcesSubmissionCollectionJobs(adminToken);
        if (cancelled) {
          return;
        }
        setCollectionJobs(jobs);
        const activeJob = jobs.find((job) => job.status === 'RUNNING') ?? null;
        if (activeJob) {
          applyCollectionJobSnapshot(activeJob);
          window.localStorage.setItem(COLLECTION_JOB_STORAGE_KEY, activeJob.jobId);
          return;
        }
        setCollectionJob(null);
        const storedJobId = window.localStorage.getItem(COLLECTION_JOB_STORAGE_KEY);
        if (storedJobId && jobs.every((job) => job.jobId !== storedJobId || job.status !== 'RUNNING')) {
          window.localStorage.removeItem(COLLECTION_JOB_STORAGE_KEY);
        }
      } catch (error) {
        if (!cancelled && error instanceof ApiError && error.status !== 401 && error.status !== 403) {
          setErrorMessage(formatError(error));
        }
      }
    }

    void pollCollectionJobs();
    const intervalId = window.setInterval(() => {
      void pollCollectionJobs();
    }, COLLECTION_JOBS_LIST_POLL_INTERVAL_MS);
    return () => {
      cancelled = true;
      window.clearInterval(intervalId);
    };
  }, [applyCollectionJobSnapshot, currentUser?.role, token]);

  const boundRecords = useMemo(
    () => records.filter((record) => record.handleStatus === 'bound'),
    [records],
  );

  return {
    token,
    currentUser,
    status,
    health,
    trainingQuery,
    submissionPage,
    submissionLimit,
    users,
    records,
    autoCollectAcceptedSummaries,
    boundRecords,
    selectedIdentity,
    submissions,
    firstAccepted,
    lastBatch,
    lastRefresh,
    collectionJob,
    collectionJobSummary,
    collectionJobs,
    operations,
    errorMessage,
    signIn,
    signOut,
    refreshDashboard,
    applyTrainingQuery,
    changeSubmissionPage,
    chooseIdentity,
    batchCollectSubmissions,
    batchImportStudents,
    deleteFullUserData,
    updateStudentInfo,
    collectSelectedIdentity,
    importOdsFile,
    refreshWarehouse,
  };
}

function querySummary(query: TrainingQueryRange) {
  const datePart = query.acceptedFromDateUtcPlus8 || query.acceptedToDateUtcPlus8
    ? `${query.acceptedFromDateUtcPlus8 || '不限'} ~ ${query.acceptedToDateUtcPlus8 || '不限'}`
    : '全量日期';
  const ratingPart = query.minProblemRating || query.maxProblemRating
    ? `${query.minProblemRating || '不限'} ~ ${query.maxProblemRating || '不限'} rating`
    : '全 rating';
  return `${datePart} / ${ratingPart}`;
}
