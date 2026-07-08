import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  ApiError,
  batchCreateUsers,
  changeCurrentPassword,
  changeOjHandleIdentity,
  collectCodeforcesSubmissions,
  createOjHandleAccount,
  deleteAdminUser,
  getAuthModuleInfo,
  getFirstAcceptedProblems,
  getProblemFirstAcceptedHandles,
  getProblemSubmissions,
  getStudentSubmissions,
  getTrainingDataModuleInfo,
  login,
  listCodeforcesSubmissionCollectionJobs,
  listOjHandleAccounts,
  listUsers,
  purgeOjStudentData,
  startCodeforcesSubmissionCollectionJob,
  updateAdminUser,
  updateOjHandleAccount,
} from '../api/platform';
import { OJ_NAMES, type TrainingQueryRange } from '../types';

const range: TrainingQueryRange = {
  acceptedFromDateUtcPlus8: '2024-01-01',
  acceptedToDateUtcPlus8: '2024-01-31',
  minProblemRating: '1800',
  maxProblemRating: '2400',
};

function stubFetch(body: unknown) {
  const fetchMock = vi.fn().mockResolvedValue(
    new Response(JSON.stringify(body), {
      headers: { 'Content-Type': 'application/json' },
      status: 200,
    }),
  );
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

describe('platform API query parameters', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('passes date, rating range and pagination to the student submission endpoint', async () => {
    const fetchMock = stubFetch({
      studentIdentity: '230511213黄炳睿',
      authorHandle: 'tourist',
      page: 2,
      limit: 50,
      total: 120,
      totalPages: 3,
      hasMore: true,
      submissions: [],
    });

    await getStudentSubmissions('230511213黄炳睿', range, { page: 2, limit: 50 });

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    expect(url.pathname).toBe('/api/training-data/codeforces/submissions/by-student');
    expect(url.searchParams.get('ojName')).toBe(OJ_NAMES.CODEFORCES);
    expect(url.searchParams.get('studentIdentity')).toBe('230511213黄炳睿');
    expect(url.searchParams.get('submittedFromUtcPlus8')).toBe('2024-01-01T00:00:00');
    expect(url.searchParams.get('submittedToUtcPlus8')).toBe('2024-01-31T23:59:59');
    expect(url.searchParams.get('minProblemRating')).toBe('1800');
    expect(url.searchParams.get('maxProblemRating')).toBe('2400');
    expect(url.searchParams.get('page')).toBe('2');
    expect(url.searchParams.get('limit')).toBe('50');
  });

  it('passes date, rating range and pagination to the problem submission endpoint', async () => {
    const fetchMock = stubFetch({
      problemKey: '2242:C',
      page: 2,
      limit: 50,
      total: 120,
      totalPages: 3,
      hasMore: true,
      submissions: [],
    });

    await getProblemSubmissions('2242:C', range, { page: 2, limit: 50 }, OJ_NAMES.ATCODER);

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    expect(url.pathname).toBe('/api/training-data/codeforces/submissions/by-problem');
    expect(url.searchParams.get('ojName')).toBe(OJ_NAMES.ATCODER);
    expect(url.searchParams.get('problemKey')).toBe('2242:C');
    expect(url.searchParams.get('submittedFromUtcPlus8')).toBe('2024-01-01T00:00:00');
    expect(url.searchParams.get('submittedToUtcPlus8')).toBe('2024-01-31T23:59:59');
    expect(url.searchParams.get('minProblemRating')).toBe('1800');
    expect(url.searchParams.get('maxProblemRating')).toBe('2400');
    expect(url.searchParams.get('page')).toBe('2');
    expect(url.searchParams.get('limit')).toBe('50');
  });

  it('passes date, rating range and pagination to the first accepted endpoint', async () => {
    const fetchMock = stubFetch({
      studentIdentity: '230511213黄炳睿',
      authorHandle: 'tourist',
      totalAcceptedProblemCount: 0,
      page: 2,
      limit: 50,
      total: 0,
      totalPages: 0,
      hasMore: false,
      problems: [],
    });

    await getFirstAcceptedProblems('230511213黄炳睿', range, { page: 2, limit: 50 });

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    expect(url.pathname).toBe('/api/training-data/codeforces/first-accepted/by-student');
    expect(url.searchParams.get('ojName')).toBe(OJ_NAMES.CODEFORCES);
    expect(url.searchParams.get('studentIdentity')).toBe('230511213黄炳睿');
    expect(url.searchParams.get('firstAcceptedFromUtcPlus8')).toBe('2024-01-01T00:00:00');
    expect(url.searchParams.get('firstAcceptedToUtcPlus8')).toBe('2024-01-31T23:59:59');
    expect(url.searchParams.get('minProblemRating')).toBe('1800');
    expect(url.searchParams.get('maxProblemRating')).toBe('2400');
    expect(url.searchParams.get('page')).toBe('2');
    expect(url.searchParams.get('limit')).toBe('50');
  });

  it('passes date, rating range and pagination to the problem first accepted endpoint', async () => {
    const fetchMock = stubFetch({
      problemKey: '2242:C',
      acceptedHandleCount: 0,
      page: 2,
      limit: 50,
      total: 0,
      totalPages: 0,
      hasMore: false,
      acceptedHandles: [],
    });

    await getProblemFirstAcceptedHandles('2242:C', range, { page: 2, limit: 50 }, OJ_NAMES.ATCODER);

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    expect(url.pathname).toBe('/api/training-data/codeforces/first-accepted/by-problem');
    expect(url.searchParams.get('ojName')).toBe(OJ_NAMES.ATCODER);
    expect(url.searchParams.get('problemKey')).toBe('2242:C');
    expect(url.searchParams.get('firstAcceptedFromUtcPlus8')).toBe('2024-01-01T00:00:00');
    expect(url.searchParams.get('firstAcceptedToUtcPlus8')).toBe('2024-01-31T23:59:59');
    expect(url.searchParams.get('minProblemRating')).toBe('1800');
    expect(url.searchParams.get('maxProblemRating')).toBe('2400');
    expect(url.searchParams.get('page')).toBe('2');
    expect(url.searchParams.get('limit')).toBe('50');
  });

  it('lists auth users without requiring an admin token', async () => {
    const fetchMock = stubFetch([]);

    await listUsers();

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(url.pathname).toBe('/api/auth/users');
    expect(url.search).toBe('');
    expect(init.headers).toEqual({ Accept: 'application/json' });
  });

  it('posts remember-me flag when logging in', async () => {
    const fetchMock = stubFetch({
      tokenType: 'Bearer',
      accessToken: 'token',
      expiresInSeconds: 2592000,
      user: { studentIdentity: '230511213黄炳睿', role: 'player' },
    });

    await login('230511213黄炳睿', 'secret', true);

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(url.pathname).toBe('/api/auth/login');
    expect(init.method).toBe('POST');
    expect(JSON.parse(String(init.body))).toEqual({
      studentIdentity: '230511213黄炳睿',
      password: 'secret',
      rememberMe: true,
    });
  });

  it('loads auth module-info through the frontend proxy path', async () => {
    const fetchMock = stubFetch({
      module: 'platform-auth',
      service: 'auth-web',
      features: ['login'],
    });

    await getAuthModuleInfo();

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    expect(url.pathname).toBe('/module-info/auth');
  });

  it('loads training-data module-info through the frontend proxy path', async () => {
    const fetchMock = stubFetch({
      module: 'platform-training-data',
      service: 'training-data-web',
      features: ['oj-handles'],
    });

    await getTrainingDataModuleInfo();

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    expect(url.pathname).toBe('/module-info/training-data');
  });

  it('posts admin batch user creation commands to auth-web', async () => {
    const fetchMock = stubFetch([]);

    await batchCreateUsers('admin-token', [
      {
        studentIdentity: '230511213黄炳睿',
        role: 'player',
        password: 'initialPass123',
      },
    ]);

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(url.pathname).toBe('/api/auth/admin/users:batch-create');
    expect(init.method).toBe('POST');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer admin-token');
    expect(JSON.parse(init.body as string)).toEqual({
      users: [
        {
          studentIdentity: '230511213黄炳睿',
          role: 'player',
          password: 'initialPass123',
        },
      ],
    });
  });

  it('patches current account password through auth-web', async () => {
    const fetchMock = stubFetch(null);

    await changeCurrentPassword('player-token', {
      oldPassword: 'old-pass',
      newPassword: 'new-pass',
      confirmNewPassword: 'new-pass',
    });

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(url.pathname).toBe('/api/auth/player/me/password');
    expect(init.method).toBe('PATCH');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer player-token');
    expect(JSON.parse(init.body as string)).toEqual({
      oldPassword: 'old-pass',
      newPassword: 'new-pass',
      confirmNewPassword: 'new-pass',
    });
  });

  it('posts OJ handle bindings to training-data-web', async () => {
    const fetchMock = stubFetch({
      studentIdentity: '230511213黄炳睿',
      handles: { [OJ_NAMES.CODEFORCES]: 'tourist' },
      needCollect: true,
    });

    await createOjHandleAccount('admin-token', '230511213黄炳睿', {
      [OJ_NAMES.CODEFORCES]: 'tourist',
    });

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(url.pathname).toBe('/api/training-data/admin/oj-handles');
    expect(init.method).toBe('POST');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer admin-token');
    expect(JSON.parse(init.body as string)).toEqual({
      studentIdentity: '230511213黄炳睿',
      handles: {
        [OJ_NAMES.CODEFORCES]: 'tourist',
      },
    });
  });

  it('patches Codeforces handle automatic collection flag to training-data-web', async () => {
    const fetchMock = stubFetch({
      studentIdentity: '230511213黄炳睿',
      handles: { [OJ_NAMES.CODEFORCES]: 'tourist' },
      needCollect: false,
    });

    await updateOjHandleAccount('admin-token', '230511213黄炳睿', false);

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(url.pathname).toBe('/api/training-data/admin/oj-handles:change-identity');
    expect(init.method).toBe('PATCH');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer admin-token');
    expect(JSON.parse(init.body as string)).toEqual({
      oldStudentIdentity: '230511213黄炳睿',
      newStudentIdentity: '230511213黄炳睿',
      needCollect: false,
    });
  });

  it('patches OJ handle identity migrations to training-data-web', async () => {
    const fetchMock = stubFetch({
      studentIdentity: '230511214新同学',
      handles: {
        [OJ_NAMES.CODEFORCES]: 'newtourist',
        [OJ_NAMES.ATCODER]: 'atcoder_id',
      },
      needCollect: true,
    });

    await changeOjHandleIdentity(
      'admin-token',
      '230511213黄炳睿',
      '230511214新同学',
      true,
      {
        [OJ_NAMES.CODEFORCES]: 'newtourist',
        [OJ_NAMES.ATCODER]: 'atcoder_id',
      },
    );

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(url.pathname).toBe('/api/training-data/admin/oj-handles:change-identity');
    expect(init.method).toBe('PATCH');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer admin-token');
    expect(JSON.parse(init.body as string)).toEqual({
      oldStudentIdentity: '230511213黄炳睿',
      newStudentIdentity: '230511214新同学',
      needCollect: true,
      handles: {
        [OJ_NAMES.CODEFORCES]: 'newtourist',
        [OJ_NAMES.ATCODER]: 'atcoder_id',
      },
    });
  });

  it('loads OJ handle bindings in one frontend query', async () => {
    const fetchMock = stubFetch({});

    await listOjHandleAccounts();

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    expect(url.pathname).toBe('/api/training-data/oj-handles');
    expect(url.search).toBe('');
  });

  it('patches admin user role and password updates to auth-web', async () => {
    const fetchMock = stubFetch({
      success: true,
      studentIdentity: '230511213黄炳睿',
      user: null,
      plainPassword: null,
      errorCode: null,
      message: 'user updated',
    });

    await updateAdminUser('admin-token', '230511213黄炳睿', {
      role: 'disable',
      newPassword: '',
    });

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(url.pathname).toBe('/api/auth/admin/users/230511213%E9%BB%84%E7%82%B3%E7%9D%BF');
    expect(init.method).toBe('PATCH');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer admin-token');
    expect(JSON.parse(init.body as string)).toEqual({
      role: 'disable',
      newPassword: '',
    });
  });

  it('deletes admin users through auth-web', async () => {
    const fetchMock = stubFetch({
      success: true,
      studentIdentity: '230511213黄炳睿',
      user: null,
      plainPassword: null,
      errorCode: null,
      message: 'user deleted',
    });

    await deleteAdminUser('admin-token', '230511213黄炳睿');

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(url.pathname).toBe('/api/auth/admin/users/230511213%E9%BB%84%E7%82%B3%E7%9D%BF');
    expect(init.method).toBe('DELETE');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer admin-token');
  });

  it('purges OJ student data through training-data-web', async () => {
    const fetchMock = stubFetch({
      studentIdentity: '230511213黄炳睿',
      ojName: OJ_NAMES.CODEFORCES,
      handle: 'tourist',
      handles: { [OJ_NAMES.CODEFORCES]: 'tourist' },
      ojResults: [{
        ojName: OJ_NAMES.CODEFORCES,
        handle: 'tourist',
        odsSubmissionRows: 2,
        dwdSubmissionRows: 3,
        dwmFirstAcceptedRows: 4,
        dwsAcceptedSummaryRows: 5,
        totalDeletedRows: 14,
      }],
      handleAccountRows: 1,
      odsSubmissionRows: 2,
      dwdSubmissionRows: 3,
      dwmFirstAcceptedRows: 4,
      dwsAcceptedSummaryRows: 5,
      totalDeletedRows: 15,
    });

    await purgeOjStudentData('admin-token', '230511213黄炳睿', OJ_NAMES.CODEFORCES);

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(url.pathname).toBe(
      '/api/training-data/admin/students/230511213%E9%BB%84%E7%82%B3%E7%9D%BF/oj-data',
    );
    expect(url.searchParams.get('ojName')).toBe(OJ_NAMES.CODEFORCES);
    expect(init.method).toBe('DELETE');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer admin-token');
  });

  it('passes requested OJ name when purging student data', async () => {
    const fetchMock = stubFetch({
      studentIdentity: '230511213黄炳睿',
      ojName: OJ_NAMES.ATCODER,
      handle: 'tourist_atcoder',
      handles: { [OJ_NAMES.ATCODER]: 'tourist_atcoder' },
      ojResults: [],
      handleAccountRows: 0,
      odsSubmissionRows: 0,
      dwdSubmissionRows: 0,
      dwmFirstAcceptedRows: 0,
      dwsAcceptedSummaryRows: 0,
      totalDeletedRows: 0,
    });

    await purgeOjStudentData('admin-token', '230511213黄炳睿', OJ_NAMES.ATCODER);

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    expect(url.searchParams.get('ojName')).toBe(OJ_NAMES.ATCODER);
  });

  it('collects submissions with an explicit OJ selector by default', async () => {
    const fetchMock = stubFetch({
      ojName: OJ_NAMES.CODEFORCES,
      status: 'SUCCESS',
      windowStartInclusive: '2026-07-05T00:00:00Z',
      windowEndExclusive: '2026-07-06T00:00:00Z',
      requestedHandleCount: 1,
      succeededHandleCount: 1,
      failedHandleCount: 0,
      fetchedSubmissionCount: 2,
      matchedSubmissionCount: 2,
      batchId: 'collector-codeforces-1',
      tableName: 'ods_codeforces__submission',
      writtenRows: 2,
      fetchedAt: '2026-07-06T00:00:00Z',
      message: null,
      handles: [],
    });

    await collectCodeforcesSubmissions('admin-token', '230511213黄炳睿', 24);

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(url.pathname).toBe('/api/training-data/admin/codeforces/submissions:collect');
    expect(init.method).toBe('POST');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer admin-token');
    expect(JSON.parse(init.body as string)).toEqual({
      studentIdentity: '230511213黄炳睿',
      lookbackHours: 24,
      ojName: OJ_NAMES.CODEFORCES,
    });
  });

  it('starts Codeforces submission collection jobs through training-data-web', async () => {
    const fetchMock = stubFetch({
      jobId: 'job-1',
      ojName: OJ_NAMES.CODEFORCES,
      status: 'RUNNING',
      requestedCount: 1,
      completedCount: 0,
      collectedCount: 0,
      failedCount: 0,
      refreshedCount: 0,
      writtenRows: 0,
      batchIds: [],
      startedAt: '2026-07-06T03:00:00Z',
      finishedAt: null,
      message: '采集任务运行中',
      items: [],
    });

    await startCodeforcesSubmissionCollectionJob('admin-token', {
      studentIdentities: ['230511213黄炳睿'],
      lookbackHours: 24,
      refreshWarehouse: true,
      ojName: OJ_NAMES.CODEFORCES,
    });

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(url.pathname).toBe('/api/training-data/admin/codeforces/submissions:collect-batch-jobs');
    expect(init.method).toBe('POST');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer admin-token');
    expect(JSON.parse(init.body as string)).toEqual({
      studentIdentities: ['230511213黄炳睿'],
      lookbackHours: 24,
      refreshWarehouse: true,
      ojName: OJ_NAMES.CODEFORCES,
    });
  });

  it('lists Codeforces submission collection jobs through training-data-web', async () => {
    const fetchMock = stubFetch([]);

    await listCodeforcesSubmissionCollectionJobs('admin-token');

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(url.pathname).toBe('/api/training-data/admin/codeforces/submissions/collect-batch-jobs');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer admin-token');
  });

  it('includes gateway timeout detail in API errors', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      new Response('upstream request timed out', {
        status: 504,
        statusText: 'Gateway Timeout',
      }),
    ));

    try {
      await listCodeforcesSubmissionCollectionJobs('admin-token');
      throw new Error('expected request to fail');
    } catch (error) {
      expect(error).toBeInstanceOf(ApiError);
      expect((error as Error).message).toMatch(/HTTP 504 网关超时：upstream request timed out/);
    }
  });
});
