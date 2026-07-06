import type {
  AdminUserCreateRequest,
  AdminUserOperationResult,
  AdminUserUpdateRequest,
  AuthUser,
  CodeforcesAcceptedSummary,
  CodeforcesFirstAcceptedReport,
  CodeforcesHandleAccount,
  CodeforcesOdsBatchUpsertResponse,
  CodeforcesStudentDataPurgeResult,
  CodeforcesSubmissionCollectionJobResponse,
  CodeforcesStudentSubmissionReport,
  CodeforcesSubmissionCollectionResponse,
  CurrentUser,
  LoginResponse,
  ServiceHealth,
  BatchCollectOptions,
  SubmissionPageQuery,
  StudentIdentity,
  TrainingQueryRange,
  WarehouseRefreshResult,
} from '../types';

const AUTH_API_BASE = import.meta.env.VITE_AUTH_API_BASE ?? '/api/auth';
const TRAINING_DATA_API_BASE = import.meta.env.VITE_TRAINING_DATA_API_BASE ?? '/api/training-data';
const AUTH_HEALTH_PATH = import.meta.env.VITE_AUTH_HEALTH_PATH ?? '/health/auth';
const TRAINING_DATA_HEALTH_PATH =
  import.meta.env.VITE_TRAINING_DATA_HEALTH_PATH ?? '/health/training-data';

export class ApiError extends Error {
  readonly status: number;
  readonly code: string | null;
  readonly body: unknown;

  constructor(status: number, message: string, body: unknown, code: string | null = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.body = body;
  }
}

interface ApiErrorBody {
  code?: string;
  message?: string;
  error?: string;
  detail?: string;
}

function authHeaders(token?: string): HeadersInit {
  return token ? { Authorization: `Bearer ${token}` } : {};
}

function jsonHeaders(token?: string): HeadersInit {
  return {
    ...authHeaders(token),
    'Content-Type': 'application/json',
  };
}

async function parseResponseBody(response: Response) {
  const text = await response.text();
  if (text.length === 0) {
    return null;
  }
  try {
    return JSON.parse(text) as unknown;
  } catch {
    return text;
  }
}

async function requestJson<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, init);
  const body = await parseResponseBody(response);
  if (!response.ok) {
    const errorBody = typeof body === 'object' && body !== null ? (body as ApiErrorBody) : {};
    throw new ApiError(
      response.status,
      buildErrorMessage(response, body, errorBody),
      body,
      errorBody.code ?? null,
    );
  }
  return body as T;
}

function buildErrorMessage(response: Response, body: unknown, errorBody: ApiErrorBody) {
  const statusLabel = httpStatusLabel(response.status, response.statusText);
  const detail = errorBody.message
    ?? errorBody.detail
    ?? errorBody.error
    ?? textBodySummary(body);
  const timeoutHint = response.status === 504
    ? '请求超过网关等待时间；长时间采集会转入后台任务后轮询状态。'
    : '';
  return [statusLabel, detail, timeoutHint]
    .filter((part) => part && part.trim().length > 0)
    .join('：');
}

function httpStatusLabel(status: number, statusText: string) {
  const knownStatus: Record<number, string> = {
    400: '请求参数错误',
    401: '未登录或登录已过期',
    403: '权限不足',
    404: '接口或资源不存在',
    408: '请求超时',
    429: '请求过于频繁',
    500: '服务内部错误',
    502: '网关错误',
    503: '服务暂不可用',
    504: '网关超时',
  };
  const label = knownStatus[status] ?? statusText.trim();
  return label ? `HTTP ${status} ${label}` : `HTTP ${status}`;
}

function textBodySummary(body: unknown) {
  if (typeof body !== 'string') {
    return '';
  }
  const text = body
    .replace(/<style[\s\S]*?<\/style>/gi, ' ')
    .replace(/<script[\s\S]*?<\/script>/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
  if (!text) {
    return '';
  }
  return text.length > 240 ? `${text.slice(0, 240)}...` : text;
}

function buildUrl(base: string, path: string, query?: Record<string, string | number | null | undefined>) {
  const normalizedBase = base.endsWith('/') ? base.slice(0, -1) : base;
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const url = new URL(`${normalizedBase}${normalizedPath}`, window.location.origin);
  Object.entries(query ?? {}).forEach(([key, value]) => {
    if (value !== null && value !== undefined && String(value).trim().length > 0) {
      url.searchParams.set(key, String(value));
    }
  });
  return url.toString();
}

export async function checkAuthHealth(): Promise<ServiceHealth> {
  try {
    const result = await requestJson<{ service: string; status: string }>(
      new URL(AUTH_HEALTH_PATH, window.location.origin).toString(),
    );
    return {
      service: 'auth-web',
      status: result.status === 'UP' ? 'UP' : 'UNKNOWN',
      detail: result.service || 'auth-web',
    };
  } catch (error) {
    return {
      service: 'auth-web',
      status: 'DOWN',
      detail: error instanceof Error ? error.message : 'health check failed',
    };
  }
}

export async function checkTrainingDataHealth(): Promise<ServiceHealth> {
  try {
    const result = await requestJson<{ service: string; status: string }>(
      new URL(TRAINING_DATA_HEALTH_PATH, window.location.origin).toString(),
    );
    return {
      service: 'training-data-web',
      status: result.status === 'UP' ? 'UP' : 'UNKNOWN',
      detail: result.service || 'training-data-web',
    };
  } catch (error) {
    return {
      service: 'training-data-web',
      status: 'DOWN',
      detail: error instanceof Error ? error.message : 'health check failed',
    };
  }
}

export function login(studentIdentity: StudentIdentity, password: string) {
  return requestJson<LoginResponse>(buildUrl(AUTH_API_BASE, '/login'), {
    method: 'POST',
    headers: jsonHeaders(),
    body: JSON.stringify({ studentIdentity, password }),
  });
}

export function getCurrentUser(token: string) {
  return requestJson<CurrentUser>(buildUrl(AUTH_API_BASE, '/player/me'), {
    headers: authHeaders(token),
  });
}

export function listUsers(token?: string) {
  return requestJson<AuthUser[]>(buildUrl(AUTH_API_BASE, '/users'), {
    headers: authHeaders(token),
  });
}

export function batchCreateUsers(token: string, users: AdminUserCreateRequest[]) {
  return requestJson<AdminUserOperationResult[]>(buildUrl(AUTH_API_BASE, '/admin/users:batch-create'), {
    method: 'POST',
    headers: jsonHeaders(token),
    body: JSON.stringify({ users }),
  });
}

export function updateAdminUser(token: string, studentIdentity: StudentIdentity, update: AdminUserUpdateRequest) {
  return requestJson<AdminUserOperationResult>(
    buildUrl(AUTH_API_BASE, `/admin/users/${encodeURIComponent(studentIdentity)}`),
    {
      method: 'PATCH',
      headers: jsonHeaders(token),
      body: JSON.stringify(update),
    },
  );
}

export function deleteAdminUser(token: string, studentIdentity: StudentIdentity) {
  return requestJson<AdminUserOperationResult>(
    buildUrl(AUTH_API_BASE, `/admin/users/${encodeURIComponent(studentIdentity)}`),
    {
      method: 'DELETE',
      headers: authHeaders(token),
    },
  );
}

export function getCodeforcesHandle(studentIdentity: StudentIdentity) {
  return requestJson<CodeforcesHandleAccount>(
    buildUrl(TRAINING_DATA_API_BASE, '/codeforces/handles', { studentIdentity }),
  );
}

export function createCodeforcesHandle(token: string, studentIdentity: StudentIdentity, handle: string) {
  return requestJson<CodeforcesHandleAccount>(
    buildUrl(TRAINING_DATA_API_BASE, '/admin/codeforces/handles'),
    {
      method: 'POST',
      headers: jsonHeaders(token),
      body: JSON.stringify({ studentIdentity, handle }),
    },
  );
}

export function updateCodeforcesHandleAccount(
  token: string,
  studentIdentity: StudentIdentity,
  needCollect: boolean,
) {
  return requestJson<CodeforcesHandleAccount>(
    buildUrl(TRAINING_DATA_API_BASE, '/admin/codeforces/handles:change-identity'),
    {
      method: 'PATCH',
      headers: jsonHeaders(token),
      body: JSON.stringify({
        oldStudentIdentity: studentIdentity,
        newStudentIdentity: studentIdentity,
        needCollect,
      }),
    },
  );
}

export function purgeCodeforcesStudentData(token: string, studentIdentity: StudentIdentity) {
  return requestJson<CodeforcesStudentDataPurgeResult>(
    buildUrl(TRAINING_DATA_API_BASE, `/admin/codeforces/users/${encodeURIComponent(studentIdentity)}/data`),
    {
      method: 'DELETE',
      headers: authHeaders(token),
    },
  );
}

function dateStart(value: string) {
  return value ? `${value}T00:00:00` : '';
}

function dateEnd(value: string) {
  return value ? `${value}T23:59:59` : '';
}

export function getAcceptedSummary(studentIdentity: StudentIdentity, range?: TrainingQueryRange) {
  return requestJson<CodeforcesAcceptedSummary>(
    buildUrl(TRAINING_DATA_API_BASE, '/codeforces/accepted-summary', {
      studentIdentity,
      acceptedFromDateUtcPlus8: range?.acceptedFromDateUtcPlus8,
      acceptedToDateUtcPlus8: range?.acceptedToDateUtcPlus8,
      minProblemRating: range?.minProblemRating,
      maxProblemRating: range?.maxProblemRating,
    }),
  );
}

export function getAutoCollectAcceptedSummaries(range?: TrainingQueryRange) {
  return requestJson<CodeforcesAcceptedSummary[]>(
    buildUrl(TRAINING_DATA_API_BASE, '/codeforces/accepted-summary/auto-collect-users', {
      acceptedFromDateUtcPlus8: range?.acceptedFromDateUtcPlus8,
      acceptedToDateUtcPlus8: range?.acceptedToDateUtcPlus8,
      minProblemRating: range?.minProblemRating,
      maxProblemRating: range?.maxProblemRating,
    }),
  );
}

export function getStudentSubmissions(
  studentIdentity: StudentIdentity,
  range?: TrainingQueryRange,
  pagination?: SubmissionPageQuery,
) {
  return requestJson<CodeforcesStudentSubmissionReport>(
    buildUrl(TRAINING_DATA_API_BASE, '/codeforces/submissions/by-student', {
      studentIdentity,
      submittedFromUtcPlus8: dateStart(range?.acceptedFromDateUtcPlus8 ?? ''),
      submittedToUtcPlus8: dateEnd(range?.acceptedToDateUtcPlus8 ?? ''),
      minProblemRating: range?.minProblemRating,
      maxProblemRating: range?.maxProblemRating,
      page: pagination?.page,
      limit: pagination?.limit,
    }),
  );
}

export function getFirstAcceptedProblems(studentIdentity: StudentIdentity, range?: TrainingQueryRange) {
  return requestJson<CodeforcesFirstAcceptedReport>(
    buildUrl(TRAINING_DATA_API_BASE, '/codeforces/first-accepted/by-student', {
      studentIdentity,
      firstAcceptedFromUtcPlus8: dateStart(range?.acceptedFromDateUtcPlus8 ?? ''),
      firstAcceptedToUtcPlus8: dateEnd(range?.acceptedToDateUtcPlus8 ?? ''),
      minProblemRating: range?.minProblemRating,
      maxProblemRating: range?.maxProblemRating,
    }),
  );
}

export function collectCodeforcesSubmissions(
  token: string,
  studentIdentity: StudentIdentity,
  lookbackHours: number,
) {
  return requestJson<CodeforcesSubmissionCollectionResponse>(
    buildUrl(TRAINING_DATA_API_BASE, '/admin/codeforces/submissions:collect'),
    {
      method: 'POST',
      headers: jsonHeaders(token),
      body: JSON.stringify({ studentIdentity, lookbackHours }),
    },
  );
}

export function startCodeforcesSubmissionCollectionJob(token: string, options: BatchCollectOptions) {
  return requestJson<CodeforcesSubmissionCollectionJobResponse>(
    buildUrl(TRAINING_DATA_API_BASE, '/admin/codeforces/submissions:collect-batch-jobs'),
    {
      method: 'POST',
      headers: jsonHeaders(token),
      body: JSON.stringify(options),
    },
  );
}

export function getCodeforcesSubmissionCollectionJob(token: string, jobId: string) {
  return requestJson<CodeforcesSubmissionCollectionJobResponse>(
    buildUrl(TRAINING_DATA_API_BASE, `/admin/codeforces/submissions/collect-batch-jobs/${encodeURIComponent(jobId)}`),
    {
      headers: authHeaders(token),
    },
  );
}

export function listCodeforcesSubmissionCollectionJobs(token: string) {
  return requestJson<CodeforcesSubmissionCollectionJobResponse[]>(
    buildUrl(TRAINING_DATA_API_BASE, '/admin/codeforces/submissions/collect-batch-jobs'),
    {
      headers: authHeaders(token),
    },
  );
}

export function upsertCodeforcesSubmissions(token: string, submissions: unknown) {
  return requestJson<CodeforcesOdsBatchUpsertResponse>(
    buildUrl(TRAINING_DATA_API_BASE, '/admin/ods/codeforces/submissions:batch-upsert'),
    {
      method: 'POST',
      headers: jsonHeaders(token),
      body: JSON.stringify(submissions),
    },
  );
}

export function refreshCodeforcesWarehouse(token: string, batchId: string) {
  return requestJson<WarehouseRefreshResult>(
    buildUrl(TRAINING_DATA_API_BASE, '/admin/codeforces/warehouse:refresh'),
    {
      method: 'POST',
      headers: jsonHeaders(token),
      body: JSON.stringify({ batchId }),
    },
  );
}
