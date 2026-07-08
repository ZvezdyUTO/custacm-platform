import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ApiError,
  batchCreateUsers,
  changeCurrentPassword,
  changeOjHandleIdentity,
  checkAuthHealth,
  checkTrainingDataHealth,
  collectCodeforcesSubmissions,
  createOjHandleAccount,
  deleteAdminUser,
  getAcceptedSummary,
  getAuthModuleInfo,
  getCurrentUser,
  getFirstAcceptedProblems,
  getProblemFirstAcceptedHandles,
  getProblemSubmissions,
  getStudentSubmissions,
  getTrainingDataModuleInfo,
  listCodeforcesSubmissionCollectionJobs,
  listOjHandleAccounts,
  listUsers,
  login,
  purgeOjStudentData,
  startCodeforcesSubmissionCollectionJob,
  updateAdminUser,
  updateOjHandleAccount,
} from '../api/platform';
import { defaultLookbackHours, seededStudentIdentities } from '../data/dashboard';
import {
  OJ_NAMES,
  OJ_LABELS,
  type AuthUser,
  type BatchCollectOptions,
  type BatchCollectSummary,
  type BatchCollectStudentResult,
  type BatchStudentImportRow,
  type BatchStudentImportSummary,
  type ChangeCurrentPasswordRequest,
  type CodeforcesAcceptedSummary,
  type OjHandleAccount,
  type OjHandleAccountMap,
  type OjStudentDataPurgeResult,
  type FullUserDataDeleteSummary,
  type CodeforcesFirstAcceptedReport,
  type CodeforcesHandleOperationResult,
  type CodeforcesProblemFirstAcceptedReport,
  type CodeforcesProblemSubmissionReport,
  type CodeforcesSubmissionCollectionJobItem,
  type CodeforcesSubmissionCollectionJobResponse,
  type CodeforcesStudentSubmissionReport,
  type CurrentUser,
  type DashboardOperation,
  type OjName,
  type PlatformModuleInfo,
  type ServiceHealth,
  type SubmissionPageQuery,
  type StudentIdentity,
  type StudentTrainingRecord,
  type TrainingQueryMode,
  type TrainingQueryRange,
  type TrainingDataBatchSummary,
  type UserInfoUpdateInput,
  type UserInfoUpdateSummary,
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
  rememberMe: boolean;
}

interface RefreshDashboardOptions {
  firstAcceptedPaginationOverride?: SubmissionPageQuery;
  loadMultiUserSummaries?: boolean;
  loadProblemDetails?: boolean;
  loadStudentDetails?: boolean;
  ojNameOverride?: OjName;
  problemFirstAcceptedPaginationOverride?: SubmissionPageQuery;
  problemKeyOverride?: string;
  problemSubmissionPaginationOverride?: SubmissionPageQuery;
  selectedIdentityOverride?: StudentIdentity | null;
}

const emptyTrainingQuery: TrainingQueryRange = {
  acceptedFromDateUtcPlus8: '',
  acceptedToDateUtcPlus8: '',
  minProblemRating: '',
  maxProblemRating: '',
};

const defaultSubmissionPage: SubmissionPageQuery = {
  page: 1,
  limit: 15,
};

function readStoredToken() {
  return window.localStorage.getItem(TOKEN_STORAGE_KEY);
}

function formatDateUtcPlus8(date: Date) {
  return new Date(date.getTime() + UTC_PLUS_8_OFFSET_MS).toISOString().slice(0, 10);
}

function recentWeekTrainingQuery(now = new Date()): TrainingQueryRange {
  const start = new Date(now.getTime() - RECENT_WEEK_DAYS * 24 * 60 * 60 * 1000);
  return {
    ...emptyTrainingQuery,
    acceptedFromDateUtcPlus8: formatDateUtcPlus8(start),
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
    ojName: item.ojName,
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

function handleFrom(account: OjHandleAccount | undefined, ojName: OjName) {
  return account?.handles[ojName] ?? null;
}

function handleFromRecord(record: StudentTrainingRecord, ojName: OjName) {
  return record.handles[ojName] ?? null;
}

function hasAnyHandle(handles: Partial<Record<OjName, string>>) {
  return Object.values(handles).some((handle) => typeof handle === 'string' && handle.trim().length > 0);
}

function normalizeHandles(handles: Partial<Record<OjName, string>> | undefined) {
  const normalized: Partial<Record<OjName, string>> = {};
  Object.entries(handles ?? {}).forEach(([ojName, handle]) => {
    const normalizedHandle = handle?.trim();
    if (normalizedHandle) {
      normalized[ojName as OjName] = normalizedHandle;
    }
  });
  return normalized;
}

function handlesFromImportRow(row: BatchStudentImportRow) {
  const handles: Partial<Record<OjName, string>> = {
    ...(row.handles ?? {}),
  };
  const codeforcesHandle = row.codeforcesHandle ?? row.handle;
  if (codeforcesHandle !== undefined) {
    handles[OJ_NAMES.CODEFORCES] = codeforcesHandle;
  }
  if (row.atcoderHandle !== undefined) {
    handles[OJ_NAMES.ATCODER] = row.atcoderHandle;
  }
  return normalizeHandles(handles);
}

function primaryHandleFrom(handles: Partial<Record<OjName, string>>) {
  return handles[OJ_NAMES.CODEFORCES] ?? handles[OJ_NAMES.ATCODER] ?? null;
}

function needCollectFrom(account: OjHandleAccount | undefined) {
  return account?.needCollect ?? true;
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

function ojLabel(ojName: OjName | null | undefined) {
  return ojName ? OJ_LABELS[ojName] : '全部 OJ';
}

function defaultOdsTable(ojName: OjName | null | undefined) {
  return ojName === OJ_NAMES.ATCODER ? 'ods_atcoder__submission' : 'ods_codeforces__submission';
}

function aggregatePurgeResults(
  studentIdentity: StudentIdentity,
  handles: Partial<Record<OjName, string>>,
  results: OjStudentDataPurgeResult[],
): OjStudentDataPurgeResult {
  return {
    studentIdentity,
    ojName: results.length === 1 ? results[0].ojName : null,
    handle: results.length === 1 ? results[0].handle : primaryHandleFrom(handles),
    handles,
    ojResults: results.flatMap((result) => result.ojResults),
    handleAccountRows: results.reduce((sum, result) => sum + result.handleAccountRows, 0),
    odsSubmissionRows: results.reduce((sum, result) => sum + result.odsSubmissionRows, 0),
    dwdSubmissionRows: results.reduce((sum, result) => sum + result.dwdSubmissionRows, 0),
    dwmFirstAcceptedRows: results.reduce((sum, result) => sum + result.dwmFirstAcceptedRows, 0),
    dwsAcceptedSummaryRows: results.reduce((sum, result) => sum + result.dwsAcceptedSummaryRows, 0),
    totalDeletedRows: results.reduce((sum, result) => sum + result.totalDeletedRows, 0),
  };
}

async function saveOjHandleAccount(
  token: string,
  studentIdentity: StudentIdentity,
  handles: Partial<Record<OjName, string>>,
  needCollect?: boolean,
) {
  try {
    const created = await createOjHandleAccount(token, studentIdentity, handles);
    if (needCollect === undefined || created.needCollect === needCollect) {
      return created;
    }
    return updateOjHandleAccount(token, studentIdentity, needCollect, created.handles);
  } catch (error) {
    if (error instanceof ApiError && error.code === 'OJ_HANDLE_ACCOUNT_IDENTITY_EXISTS') {
      return updateOjHandleAccount(token, studentIdentity, needCollect ?? true, handles);
    }
    throw error;
  }
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

function compareAcceptedSummaries(left: CodeforcesAcceptedSummary, right: CodeforcesAcceptedSummary) {
  const acceptedOrder = right.totalAcceptedProblemCount - left.totalAcceptedProblemCount;
  return acceptedOrder === 0
    ? left.studentIdentity.localeCompare(right.studentIdentity, 'zh-CN')
    : acceptedOrder;
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
  const [moduleInfo, setModuleInfo] = useState<PlatformModuleInfo[]>([]);
  const [users, setUsers] = useState<AuthUser[]>([]);
  const [records, setRecords] = useState<StudentTrainingRecord[]>([]);
  const [multiUserAcceptedSummaries, setMultiUserAcceptedSummaries] = useState<CodeforcesAcceptedSummary[]>([]);
  const [selectedIdentity, setSelectedIdentity] = useState<StudentIdentity | null>(null);
  const [submissions, setSubmissions] = useState<CodeforcesStudentSubmissionReport | null>(null);
  const [firstAccepted, setFirstAccepted] = useState<CodeforcesFirstAcceptedReport | null>(null);
  const [problemKey, setProblemKey] = useState('');
  const [problemSubmissions, setProblemSubmissions] = useState<CodeforcesProblemSubmissionReport | null>(null);
  const [problemFirstAccepted, setProblemFirstAccepted] =
    useState<CodeforcesProblemFirstAcceptedReport | null>(null);
  const [lastBatch, setLastBatch] = useState<TrainingDataBatchSummary | null>(null);
  const [collectionJob, setCollectionJob] = useState<CodeforcesSubmissionCollectionJobResponse | null>(null);
  const [collectionJobSummary, setCollectionJobSummary] = useState<BatchCollectSummary | null>(null);
  const [collectionJobs, setCollectionJobs] = useState<CodeforcesSubmissionCollectionJobResponse[]>([]);
  const [operations, setOperations] = useState<DashboardOperation[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [selectedOjName, setSelectedOjName] = useState<OjName>(OJ_NAMES.CODEFORCES);
  const [trainingQuery, setTrainingQuery] = useState<TrainingQueryRange>(() => recentWeekTrainingQuery());
  const [submissionPage, setSubmissionPage] = useState(defaultSubmissionPage.page);
  const [submissionLimit, setSubmissionLimit] = useState(defaultSubmissionPage.limit);
  const [firstAcceptedPage, setFirstAcceptedPage] = useState(defaultSubmissionPage.page);
  const [firstAcceptedLimit, setFirstAcceptedLimit] = useState(defaultSubmissionPage.limit);
  const [problemSubmissionPage, setProblemSubmissionPage] = useState(defaultSubmissionPage.page);
  const [problemSubmissionLimit, setProblemSubmissionLimit] = useState(defaultSubmissionPage.limit);
  const [problemFirstAcceptedPage, setProblemFirstAcceptedPage] = useState(defaultSubmissionPage.page);
  const [problemFirstAcceptedLimit, setProblemFirstAcceptedLimit] = useState(defaultSubmissionPage.limit);
  const [studentDetailsRequested, setStudentDetailsRequested] = useState(false);
  const [problemDetailsRequested, setProblemDetailsRequested] = useState(false);
  const detailsRequestSeq = useRef(0);
  const problemRequestSeq = useRef(0);
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

  const refreshModuleInfo = useCallback(async () => {
    const results = await Promise.allSettled([getAuthModuleInfo(), getTrainingDataModuleInfo()]);
    const nextInfo = results
      .filter((result): result is PromiseFulfilledResult<PlatformModuleInfo> => result.status === 'fulfilled')
      .map((result) => result.value);
    setModuleInfo(nextInfo);
    results.forEach((result, index) => {
      if (result.status === 'rejected') {
        addOperation(
          index === 0 ? 'auth module-info 加载失败' : 'training-data module-info 加载失败',
          formatError(result.reason),
          operationStatusFromError(result.reason),
        );
      }
    });
    return nextInfo;
  }, [addOperation]);

  const clearStudentDetails = useCallback(() => {
    detailsRequestSeq.current += 1;
    setSubmissions(null);
    setFirstAccepted(null);
    setStudentDetailsRequested(false);
  }, []);

  const clearProblemDetails = useCallback(() => {
    problemRequestSeq.current += 1;
    setProblemSubmissions(null);
    setProblemFirstAccepted(null);
    setProblemDetailsRequested(false);
  }, []);

  const loadStudentDetails = useCallback(async (
    identity: StudentIdentity,
    query: TrainingQueryRange = trainingQuery,
    submissionPagination: SubmissionPageQuery = { page: submissionPage, limit: submissionLimit },
    firstAcceptedPagination: SubmissionPageQuery = { page: firstAcceptedPage, limit: firstAcceptedLimit },
    ojName: OjName = selectedOjName,
  ) => {
    const requestSeq = detailsRequestSeq.current + 1;
    detailsRequestSeq.current = requestSeq;
    setSubmissions(null);
    setFirstAccepted(null);
    setStudentDetailsRequested(true);

    const [summaryResult, submissionsResult, firstAcceptedResult] = await Promise.allSettled([
      getAcceptedSummary(identity, query, ojName),
      getStudentSubmissions(identity, query, submissionPagination, ojName),
      getFirstAcceptedProblems(identity, query, firstAcceptedPagination, ojName),
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
  }, [
    firstAcceptedLimit,
    firstAcceptedPage,
    selectedOjName,
    submissionLimit,
    submissionPage,
    trainingQuery,
  ]);

  const loadProblemDetails = useCallback(async (
    key: string = problemKey,
    query: TrainingQueryRange = trainingQuery,
    submissionPagination: SubmissionPageQuery = { page: problemSubmissionPage, limit: problemSubmissionLimit },
    firstAcceptedPagination: SubmissionPageQuery = {
      page: problemFirstAcceptedPage,
      limit: problemFirstAcceptedLimit,
    },
    ojName: OjName = selectedOjName,
  ) => {
    const normalizedKey = key.trim();
    if (!normalizedKey) {
      clearProblemDetails();
      return false;
    }
    const requestSeq = problemRequestSeq.current + 1;
    problemRequestSeq.current = requestSeq;
    setProblemSubmissions(null);
    setProblemFirstAccepted(null);
    setProblemDetailsRequested(true);

    const [submissionsResult, firstAcceptedResult] = await Promise.allSettled([
      getProblemSubmissions(normalizedKey, query, submissionPagination, ojName),
      getProblemFirstAcceptedHandles(normalizedKey, query, firstAcceptedPagination, ojName),
    ]);

    if (requestSeq !== problemRequestSeq.current) {
      return false;
    }

    if (submissionsResult.status === 'fulfilled') {
      setProblemSubmissions(submissionsResult.value);
    } else {
      setProblemSubmissions(null);
      addOperation('题目提交查询失败', formatError(submissionsResult.reason), operationStatusFromError(submissionsResult.reason));
    }

    if (firstAcceptedResult.status === 'fulfilled') {
      setProblemFirstAccepted(firstAcceptedResult.value);
    } else {
      setProblemFirstAccepted(null);
      addOperation('题目首 AC 查询失败', formatError(firstAcceptedResult.reason), operationStatusFromError(firstAcceptedResult.reason));
    }
    return submissionsResult.status === 'fulfilled' || firstAcceptedResult.status === 'fulfilled';
  }, [
    addOperation,
    clearProblemDetails,
    problemKey,
    problemFirstAcceptedLimit,
    problemFirstAcceptedPage,
    problemSubmissionLimit,
    problemSubmissionPage,
    selectedOjName,
    trainingQuery,
  ]);

  const loadMultiUserSummaries = useCallback(async (
    summaryRecords: StudentTrainingRecord[],
    query: TrainingQueryRange,
    ojName: OjName,
  ) => {
    const candidates = summaryRecords.filter((record) =>
      record.needCollect === true
      && shouldLoadTrainingData(record.studentIdentity)
      && Boolean(handleFromRecord(record, ojName))
    );
    if (candidates.length === 0) {
      setMultiUserAcceptedSummaries([]);
      return true;
    }

    const results = await Promise.allSettled(
      candidates.map((record) => getAcceptedSummary(record.studentIdentity, query, ojName)),
    );
    const summaries = results
      .filter((result): result is PromiseFulfilledResult<CodeforcesAcceptedSummary> => result.status === 'fulfilled')
      .map((result) => result.value)
      .sort(compareAcceptedSummaries);
    setMultiUserAcceptedSummaries(summaries);

    results.forEach((result, index) => {
      if (result.status === 'rejected') {
        addOperation(
          '队员统计加载失败',
          `${candidates[index]?.studentIdentity ?? '未知队员'}: ${formatError(result.reason)}`,
          operationStatusFromError(result.reason),
        );
      }
    });
    return true;
  }, [addOperation]);

  const refreshDashboard = useCallback(async (
    queryOverride?: TrainingQueryRange,
    submissionPaginationOverride?: SubmissionPageQuery,
    options: RefreshDashboardOptions = {},
  ) => {
    const effectiveQuery = queryOverride ?? trainingQuery;
    const effectiveOjName = options.ojNameOverride ?? selectedOjName;
    const requestedProblemKey = options.problemKeyOverride ?? problemKey;
    const requestedSelectedIdentity = options.selectedIdentityOverride ?? selectedIdentity;
    const effectiveSubmissionPagination = submissionPaginationOverride ?? {
      page: submissionPage,
      limit: submissionLimit,
    };
    const effectiveFirstAcceptedPagination = options.firstAcceptedPaginationOverride ?? {
      page: firstAcceptedPage,
      limit: firstAcceptedLimit,
    };
    const effectiveProblemSubmissionPagination = options.problemSubmissionPaginationOverride ?? {
      page: problemSubmissionPage,
      limit: problemSubmissionLimit,
    };
    const effectiveProblemFirstAcceptedPagination = options.problemFirstAcceptedPaginationOverride ?? {
      page: problemFirstAcceptedPage,
      limit: problemFirstAcceptedLimit,
    };
    const shouldLoadStudentDetails = options.loadStudentDetails ?? studentDetailsRequested;
    const shouldLoadProblemDetails = options.loadProblemDetails ?? problemDetailsRequested;
    const shouldLoadMultiUserSummaries = options.loadMultiUserSummaries ?? true;
    setStatus(token ? 'loading' : 'signed-out');
    setErrorMessage(null);
    await refreshHealth();
    await refreshModuleInfo();

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
      let handleAccountsByIdentity: OjHandleAccountMap = {};
      let handleListError: unknown = null;
      try {
        handleAccountsByIdentity = await listOjHandleAccounts();
      } catch (error) {
        handleListError = error;
        addOperation('OJ handle 列表加载失败', formatError(error), operationStatusFromError(error));
      }
      setMultiUserAcceptedSummaries([]);
      const nextRecords = identities.map((studentIdentity) => {
        const user = userList.find((item) => item.studentIdentity === studentIdentity);
        const role = user?.role ?? 'player';
        const updatedAt = user?.updatedAt ?? new Date().toISOString();
        if (!shouldLoadTrainingData(studentIdentity)) {
          return {
            studentIdentity,
            role,
            handle: null,
            handles: {},
            needCollect: null,
            collectionStates: {},
            handleStatus: 'missing',
            acceptedSummary: null,
            summaryStatus: 'not-requested',
            errorMessage: null,
            updatedAt,
          } satisfies StudentTrainingRecord;
        }
        const handleAccount = handleAccountsByIdentity[studentIdentity];
        const handles = handleAccount?.handles ?? {};
        const selectedHandle = handleFrom(handleAccount, effectiveOjName);
        return {
          studentIdentity,
          role,
          handle: selectedHandle,
          handles,
          needCollect: handleAccount ? needCollectFrom(handleAccount) : null,
          collectionStates: handleAccount?.collectionStates ?? {},
          handleStatus: handleListError ? 'error' : selectedHandle ? 'bound' : 'missing',
          acceptedSummary: null,
          summaryStatus: 'not-requested',
          errorMessage: handleListError ? formatError(handleListError) : null,
          updatedAt,
        } satisfies StudentTrainingRecord;
      });

      setRecords(nextRecords);
      const nextSelected =
        requestedSelectedIdentity && nextRecords.some((record) => record.studentIdentity === requestedSelectedIdentity)
          ? requestedSelectedIdentity
          : null;
      setSelectedIdentity(nextSelected);
      if (!shouldLoadStudentDetails) {
        clearStudentDetails();
      }
      if (!shouldLoadProblemDetails) {
        clearProblemDetails();
      }
      const detailsApplied = nextSelected && shouldLoadStudentDetails
        ? await loadStudentDetails(
          nextSelected,
          effectiveQuery,
          effectiveSubmissionPagination,
          effectiveFirstAcceptedPagination,
          effectiveOjName
        )
        : true;
      const problemDetailsApplied = shouldLoadProblemDetails
        ? await loadProblemDetails(
          requestedProblemKey,
          effectiveQuery,
          effectiveProblemSubmissionPagination,
          effectiveProblemFirstAcceptedPagination,
          effectiveOjName
        )
        : true;
      const multiUserSummariesApplied = shouldLoadMultiUserSummaries
        ? await loadMultiUserSummaries(nextRecords, effectiveQuery, effectiveOjName)
        : true;
      if (detailsApplied && problemDetailsApplied && multiUserSummariesApplied) {
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
    clearProblemDetails,
    clearStudentDetails,
    firstAcceptedLimit,
    firstAcceptedPage,
    loadStudentDetails,
    loadProblemDetails,
    loadMultiUserSummaries,
    problemDetailsRequested,
    problemFirstAcceptedLimit,
    problemFirstAcceptedPage,
    problemKey,
    problemSubmissionLimit,
    problemSubmissionPage,
    refreshHealth,
    refreshModuleInfo,
    selectedIdentity,
    selectedOjName,
    studentDetailsRequested,
    submissionLimit,
    submissionPage,
    token,
    trainingQuery,
  ]);

  const signIn = useCallback(
    async ({ studentIdentity, password, rememberMe }: LoginCredentials) => {
      setStatus('loading');
      setErrorMessage(null);
      try {
        const result = await login(studentIdentity, password, rememberMe);
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
    setMultiUserAcceptedSummaries([]);
    setSubmissions(null);
    setFirstAccepted(null);
    setProblemKey('');
    setProblemSubmissions(null);
    setProblemFirstAccepted(null);
    setStudentDetailsRequested(false);
    setProblemDetailsRequested(false);
    setSelectedOjName(OJ_NAMES.CODEFORCES);
    setTrainingQuery(recentWeekTrainingQuery());
    setCollectionJob(null);
    setCollectionJobSummary(null);
    setCollectionJobs([]);
    setSubmissionPage(defaultSubmissionPage.page);
    setSubmissionLimit(defaultSubmissionPage.limit);
    setFirstAcceptedPage(defaultSubmissionPage.page);
    setFirstAcceptedLimit(defaultSubmissionPage.limit);
    setProblemSubmissionPage(defaultSubmissionPage.page);
    setProblemSubmissionLimit(defaultSubmissionPage.limit);
    setProblemFirstAcceptedPage(defaultSubmissionPage.page);
    setProblemFirstAcceptedLimit(defaultSubmissionPage.limit);
    addOperation('退出登录', '已清除本地 access token', 'completed');
  }, [addOperation]);

  const changePassword = useCallback(async (request: ChangeCurrentPasswordRequest) => {
    if (!token) {
      throw new Error('需要先登录后才能修改密码。');
    }
    setErrorMessage(null);
    try {
      await changeCurrentPassword(token, request);
      addOperation('修改当前账号密码', currentUser?.studentIdentity ?? '当前账号', 'completed');
    } catch (error) {
      const message = formatError(error);
      addOperation('修改当前账号密码失败', message, 'failed');
      setErrorMessage(message);
      throw error;
    }
  }, [addOperation, currentUser?.studentIdentity, token]);

  const chooseIdentity = useCallback(
    async (identity: StudentIdentity) => {
      setSubmissionPage(defaultSubmissionPage.page);
      setFirstAcceptedPage(defaultSubmissionPage.page);
      setSelectedIdentity(identity);
      clearStudentDetails();
      addOperation('选择队员', identity, 'completed');
    },
    [addOperation, clearStudentDetails],
  );

  const chooseOjName = useCallback((ojName: OjName) => {
    setSelectedOjName(ojName);
    clearStudentDetails();
    clearProblemDetails();
    setSubmissionPage(defaultSubmissionPage.page);
    setFirstAcceptedPage(defaultSubmissionPage.page);
    setProblemSubmissionPage(defaultSubmissionPage.page);
    setProblemFirstAcceptedPage(defaultSubmissionPage.page);
    setMultiUserAcceptedSummaries([]);
    setRecords((current) => current.map((record) => {
      const handle = handleFromRecord(record, ojName);
      return {
        ...record,
        handle,
        handleStatus: record.errorMessage ? 'error' : handle ? 'bound' : 'missing',
        acceptedSummary: null,
        summaryStatus: 'not-requested',
      };
    }));
    addOperation('切换 OJ', ojLabel(ojName), 'completed');
  }, [addOperation, clearProblemDetails, clearStudentDetails]);

  const applyTrainingQuery = useCallback(async (
    nextQuery: TrainingQueryRange,
    mode: TrainingQueryMode = 'multiple',
  ) => {
    const shouldLoadStudentDetails = mode === 'single';
    const shouldLoadProblemDetails = mode === 'problem';
    setTrainingQuery(nextQuery);
    setSubmissionPage(defaultSubmissionPage.page);
    setFirstAcceptedPage(defaultSubmissionPage.page);
    setProblemSubmissionPage(defaultSubmissionPage.page);
    setProblemFirstAcceptedPage(defaultSubmissionPage.page);
    if (!shouldLoadStudentDetails) {
      clearStudentDetails();
    }
    if (!shouldLoadProblemDetails) {
      clearProblemDetails();
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
      firstAcceptedPaginationOverride: {
        page: defaultSubmissionPage.page,
        limit: firstAcceptedLimit,
      },
      loadMultiUserSummaries: mode === 'multiple',
      problemFirstAcceptedPaginationOverride: {
        page: defaultSubmissionPage.page,
        limit: problemFirstAcceptedLimit,
      },
      loadProblemDetails: shouldLoadProblemDetails,
      problemSubmissionPaginationOverride: {
        page: defaultSubmissionPage.page,
        limit: problemSubmissionLimit,
      },
      loadStudentDetails: shouldLoadStudentDetails,
    });
  }, [
    addOperation,
    clearProblemDetails,
    clearStudentDetails,
    firstAcceptedLimit,
    problemFirstAcceptedLimit,
    problemSubmissionLimit,
    refreshDashboard,
    submissionLimit,
  ]);

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
    }, {
      page: firstAcceptedPage,
      limit: firstAcceptedLimit,
    });
    if (detailsApplied) {
      setStatus('ready');
    }
  }, [
    firstAcceptedLimit,
    firstAcceptedPage,
    loadStudentDetails,
    selectedIdentity,
    submissions,
    trainingQuery,
  ]);

  const changeFirstAcceptedPage = useCallback(async (nextPage: number, nextLimit: number) => {
    if (!selectedIdentity || !firstAccepted) {
      return;
    }
    const normalizedPage = Math.max(1, nextPage);
    setStatus('loading');
    setFirstAcceptedPage(normalizedPage);
    setFirstAcceptedLimit(nextLimit);
    const detailsApplied = await loadStudentDetails(selectedIdentity, trainingQuery, {
      page: submissionPage,
      limit: submissionLimit,
    }, {
      page: normalizedPage,
      limit: nextLimit,
    });
    if (detailsApplied) {
      setStatus('ready');
    }
  }, [
    firstAccepted,
    loadStudentDetails,
    selectedIdentity,
    submissionLimit,
    submissionPage,
    trainingQuery,
  ]);

  const changeProblemSubmissionPage = useCallback(async (nextPage: number, nextLimit: number) => {
    if (!problemKey.trim() || !problemSubmissions) {
      return;
    }
    const normalizedPage = Math.max(1, nextPage);
    setStatus('loading');
    setProblemSubmissionPage(normalizedPage);
    setProblemSubmissionLimit(nextLimit);
    const detailsApplied = await loadProblemDetails(problemKey, trainingQuery, {
      page: normalizedPage,
      limit: nextLimit,
    }, {
      page: problemFirstAcceptedPage,
      limit: problemFirstAcceptedLimit,
    });
    if (detailsApplied) {
      setStatus('ready');
    }
  }, [
    loadProblemDetails,
    problemFirstAcceptedLimit,
    problemFirstAcceptedPage,
    problemKey,
    problemSubmissions,
    trainingQuery,
  ]);

  const changeProblemFirstAcceptedPage = useCallback(async (nextPage: number, nextLimit: number) => {
    if (!problemKey.trim() || !problemFirstAccepted) {
      return;
    }
    const normalizedPage = Math.max(1, nextPage);
    setStatus('loading');
    setProblemFirstAcceptedPage(normalizedPage);
    setProblemFirstAcceptedLimit(nextLimit);
    const detailsApplied = await loadProblemDetails(problemKey, trainingQuery, {
      page: problemSubmissionPage,
      limit: problemSubmissionLimit,
    }, {
      page: normalizedPage,
      limit: nextLimit,
    });
    if (detailsApplied) {
      setStatus('ready');
    }
  }, [
    loadProblemDetails,
    problemFirstAccepted,
    problemKey,
    problemSubmissionLimit,
    problemSubmissionPage,
    trainingQuery,
  ]);

  const changeProblemKey = useCallback((nextProblemKey: string) => {
    setProblemKey(nextProblemKey);
    clearProblemDetails();
    setProblemSubmissionPage(defaultSubmissionPage.page);
    setProblemFirstAcceptedPage(defaultSubmissionPage.page);
  }, [clearProblemDetails]);

  const collectSelectedIdentity = useCallback(async () => {
    if (!token || !selectedIdentity) {
      return;
    }
    try {
      const result = await collectCodeforcesSubmissions(token, selectedIdentity, defaultLookbackHours, selectedOjName);
      if (result.batchId) {
        setLastBatch({
          batchId: result.batchId,
          tableName: result.tableName ?? defaultOdsTable(result.ojName),
          writtenRows: result.writtenRows,
          fetchedAt: result.fetchedAt ?? new Date().toISOString(),
        });
      }
      addOperation(
        `${ojLabel(result.ojName)} 最近提交采集`,
        `${selectedIdentity}: ${result.ojName} ${result.status}, 写入 ${result.writtenRows} 行`,
        operationStatusFromCollection(result.status),
      );
      await refreshDashboard();
    } catch (error) {
      const message = formatError(error);
      addOperation(`${ojLabel(selectedOjName)} 采集失败`, message, 'failed');
      setErrorMessage(message);
    }
  }, [addOperation, refreshDashboard, selectedIdentity, selectedOjName, token]);

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
      const userCommands = rows.map(({ studentIdentity, role, password }) => ({ studentIdentity, role, password }));
      const userResults = await batchCreateUsers(token, userCommands);
      const handleRows = rows
        .map((row) => ({ row, handles: handlesFromImportRow(row) }))
        .filter(({ handles }) => hasAnyHandle(handles));
      const handleResults: CodeforcesHandleOperationResult[] = [];

      for (const { row, handles } of handleRows) {
        try {
          const result = await saveOjHandleAccount(token, row.studentIdentity, handles);
          handleResults.push({
            success: true,
            studentIdentity: result.studentIdentity,
            handle: primaryHandleFrom(result.handles),
            handles: result.handles,
            needCollect: needCollectFrom(result),
            errorCode: null,
            message: 'handle created',
          });
        } catch (error) {
          handleResults.push({
            success: false,
            studentIdentity: row.studentIdentity,
            handle: primaryHandleFrom(handles),
            handles,
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
        `账号 ${successfulUsers}/${userResults.length}，OJ 绑定 ${successfulHandles}/${handleResults.length}`,
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
      throw new Error('需要先登录 admin 账号才能管理用户信息。');
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

      const requestedHandles = normalizeHandles(input.handles ?? (
        input.handle ? { [OJ_NAMES.CODEFORCES]: input.handle } : undefined
      ));
      const newStudentIdentity = input.newStudentIdentity?.trim() || input.studentIdentity;
      const migratesHandleIdentity = newStudentIdentity !== input.studentIdentity;
      if (migratesHandleIdentity) {
        try {
          const result = await changeOjHandleIdentity(
            token,
            input.studentIdentity,
            newStudentIdentity,
            input.needCollect,
            hasAnyHandle(requestedHandles) ? requestedHandles : undefined,
          );
          handleResult = {
            success: true,
            studentIdentity: result.studentIdentity,
            handle: primaryHandleFrom(result.handles),
            handles: result.handles,
            needCollect: needCollectFrom(result),
            errorCode: null,
            message: 'handle identity changed',
          };
        } catch (error) {
          handleResult = {
            success: false,
            studentIdentity: newStudentIdentity,
            handle: primaryHandleFrom(requestedHandles),
            handles: requestedHandles,
            needCollect: input.needCollect ?? null,
            errorCode: error instanceof ApiError ? error.code : null,
            message: formatError(error),
          };
        }
      } else if (hasAnyHandle(requestedHandles)) {
        try {
          const result = await saveOjHandleAccount(
            token,
            input.studentIdentity,
            requestedHandles,
            input.needCollect,
          );
          const effectiveAccount = input.needCollect === undefined || result.needCollect === input.needCollect
            ? result
            : await updateOjHandleAccount(token, input.studentIdentity, input.needCollect);
          handleResult = {
            success: true,
            studentIdentity: effectiveAccount.studentIdentity,
            handle: primaryHandleFrom(effectiveAccount.handles),
            handles: effectiveAccount.handles,
            needCollect: needCollectFrom(effectiveAccount),
            errorCode: null,
            message: input.needCollect === false ? 'handle created, collection disabled' : 'handle created',
          };
        } catch (error) {
          handleResult = {
            success: false,
            studentIdentity: input.studentIdentity,
            handle: primaryHandleFrom(requestedHandles),
            handles: requestedHandles,
            needCollect: input.needCollect ?? null,
            errorCode: error instanceof ApiError ? error.code : null,
            message: formatError(error),
          };
        }
      } else if (input.needCollect !== undefined) {
        try {
          const result = await updateOjHandleAccount(token, input.studentIdentity, input.needCollect);
          handleResult = {
            success: true,
            studentIdentity: result.studentIdentity,
            handle: primaryHandleFrom(result.handles),
            handles: result.handles,
            needCollect: needCollectFrom(result),
            errorCode: null,
            message: 'collection flag updated',
          };
        } catch (error) {
          handleResult = {
            success: false,
            studentIdentity: input.studentIdentity,
            handle: null,
            handles: {},
            needCollect: input.needCollect,
            errorCode: error instanceof ApiError ? error.code : null,
            message: formatError(error),
          };
        }
      }

      addOperation(
        '管理用户信息',
        `${input.studentIdentity}: ${userResult.user?.role ?? input.role}${
          handleResult ? `, OJ ${handleResult.success ? '绑定成功' : '绑定失败'}` : ''
        }`,
        userResult.success && (!handleResult || handleResult.success) ? 'completed' : 'pending',
      );
      await refreshDashboard();
      return { userResult, handleResult };
    } catch (error) {
      const message = formatError(error);
      addOperation('管理用户信息失败', message, 'failed');
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
        tableName: latestBatchItem.tableName ?? defaultOdsTable(latestBatchItem.ojName),
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
          const jobs = await listCodeforcesSubmissionCollectionJobs(token);
          setCollectionJobs(jobs);
          const job = jobs.find((item) => item.jobId === jobId);
          if (!job) {
            window.localStorage.removeItem(COLLECTION_JOB_STORAGE_KEY);
            setCollectionJob(null);
            setCollectionJobSummary(null);
            addOperation('OJ 采集任务状态失效', '后端没有找到上次保存的采集任务', 'pending');
            throw new Error(`Codeforces collection job not found: ${jobId}`);
          }
          const summary = applyCollectionJobSnapshot(job);
          if (job.status !== 'RUNNING') {
            window.localStorage.removeItem(COLLECTION_JOB_STORAGE_KEY);
            setCollectionJob(null);
            addOperation(
              `${ojLabel(job.ojName)} 批量采集完成`,
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
        if (!(options.resumed && error instanceof Error && error.message.includes('collection job not found'))) {
          const message = formatError(error);
          addOperation('OJ 采集任务查询失败', message, 'failed');
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
      throw new Error('没有现役且已绑定 OJ handle 的队员。');
    }

    const lookbackHours = Math.max(1, Math.floor(options.lookbackHours));
    setErrorMessage(null);
    setCollectionJobSummary(null);

    const job = await startCodeforcesSubmissionCollectionJob(token, {
      studentIdentities: identities,
      lookbackHours,
      refreshWarehouse: options.refreshWarehouse,
      ojName: options.ojName ?? null,
    });
    window.localStorage.setItem(COLLECTION_JOB_STORAGE_KEY, job.jobId);
    const summary = applyCollectionJobSnapshot(job);
    addOperation(
      `${ojLabel(job.ojName)} 批量采集已启动`,
      `任务 ${job.jobId}，OJ ${ojLabel(job.ojName)}，队员 ${job.requestedCount} 个，窗口 ${lookbackHours} 小时`,
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
      const handleAccountsByIdentity = await listOjHandleAccounts();
      const handles = normalizeHandles(handleAccountsByIdentity[normalizedIdentity]?.handles);
      const purgeResults: OjStudentDataPurgeResult[] = [];
      for (const ojName of Object.keys(handles) as OjName[]) {
        purgeResults.push(await purgeOjStudentData(token, normalizedIdentity, ojName));
      }
      const trainingDataResult = aggregatePurgeResults(normalizedIdentity, handles, purgeResults);
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
    () => records.filter((record) => Boolean(handleFromRecord(record, selectedOjName))),
    [records, selectedOjName],
  );

  return {
    token,
    currentUser,
    status,
    health,
    moduleInfo,
    trainingQuery,
    selectedOjName,
    submissionPage,
    submissionLimit,
    firstAcceptedPage,
    firstAcceptedLimit,
    problemKey,
    problemSubmissionPage,
    problemSubmissionLimit,
    problemFirstAcceptedPage,
    problemFirstAcceptedLimit,
    users,
    records,
    multiUserAcceptedSummaries,
    boundRecords,
    selectedIdentity,
    submissions,
    firstAccepted,
    problemSubmissions,
    problemFirstAccepted,
    lastBatch,
    collectionJob,
    collectionJobSummary,
    collectionJobs,
    operations,
    errorMessage,
    signIn,
    signOut,
    changePassword,
    refreshDashboard,
    applyTrainingQuery,
    changeSubmissionPage,
    changeFirstAcceptedPage,
    changeProblemSubmissionPage,
    changeProblemFirstAcceptedPage,
    changeProblemKey,
    chooseIdentity,
    chooseOjName,
    batchCollectSubmissions,
    batchImportStudents,
    deleteFullUserData,
    updateStudentInfo,
    collectSelectedIdentity,
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
