import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  ApiError,
  batchCreateUsers,
  createCodeforcesHandle,
  deleteAdminUser,
  getAutoCollectAcceptedSummaries,
  getCodeforcesSubmissionCollectionJob,
  getFirstAcceptedProblems,
  getStudentSubmissions,
  listCodeforcesSubmissionCollectionJobs,
  listUsers,
  purgeCodeforcesStudentData,
  startCodeforcesSubmissionCollectionJob,
  updateAdminUser,
  updateCodeforcesHandleAccount,
} from '../api/platform';
import type { TrainingQueryRange } from '../types';

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
    expect(url.searchParams.get('studentIdentity')).toBe('230511213黄炳睿');
    expect(url.searchParams.get('submittedFromUtcPlus8')).toBe('2024-01-01T00:00:00');
    expect(url.searchParams.get('submittedToUtcPlus8')).toBe('2024-01-31T23:59:59');
    expect(url.searchParams.get('minProblemRating')).toBe('1800');
    expect(url.searchParams.get('maxProblemRating')).toBe('2400');
    expect(url.searchParams.get('page')).toBe('2');
    expect(url.searchParams.get('limit')).toBe('50');
  });

  it('passes date and rating range to the first accepted endpoint', async () => {
    const fetchMock = stubFetch({
      studentIdentity: '230511213黄炳睿',
      authorHandle: 'tourist',
      totalAcceptedProblemCount: 0,
      problems: [],
    });

    await getFirstAcceptedProblems('230511213黄炳睿', range);

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    expect(url.pathname).toBe('/api/training-data/codeforces/first-accepted/by-student');
    expect(url.searchParams.get('studentIdentity')).toBe('230511213黄炳睿');
    expect(url.searchParams.get('firstAcceptedFromUtcPlus8')).toBe('2024-01-01T00:00:00');
    expect(url.searchParams.get('firstAcceptedToUtcPlus8')).toBe('2024-01-31T23:59:59');
    expect(url.searchParams.get('minProblemRating')).toBe('1800');
    expect(url.searchParams.get('maxProblemRating')).toBe('2400');
  });

  it('passes date and rating range to the automatic collection summary endpoint', async () => {
    const fetchMock = stubFetch([]);

    await getAutoCollectAcceptedSummaries(range);

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    expect(url.pathname).toBe('/api/training-data/codeforces/accepted-summary/auto-collect-users');
    expect(url.searchParams.get('acceptedFromDateUtcPlus8')).toBe('2024-01-01');
    expect(url.searchParams.get('acceptedToDateUtcPlus8')).toBe('2024-01-31');
    expect(url.searchParams.get('minProblemRating')).toBe('1800');
    expect(url.searchParams.get('maxProblemRating')).toBe('2400');
    expect(url.searchParams.has('studentIdentity')).toBe(false);
  });

  it('lists auth users without requiring an admin token', async () => {
    const fetchMock = stubFetch([]);

    await listUsers();

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(url.pathname).toBe('/api/auth/users');
    expect(url.search).toBe('');
    expect(init.headers).toEqual({});
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

  it('posts Codeforces handle bindings to training-data-web', async () => {
    const fetchMock = stubFetch({
      studentIdentity: '230511213黄炳睿',
      handle: 'tourist',
      needCollect: true,
    });

    await createCodeforcesHandle('admin-token', '230511213黄炳睿', 'tourist');

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(url.pathname).toBe('/api/training-data/admin/codeforces/handles');
    expect(init.method).toBe('POST');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer admin-token');
    expect(JSON.parse(init.body as string)).toEqual({
      studentIdentity: '230511213黄炳睿',
      handle: 'tourist',
    });
  });

  it('patches Codeforces handle automatic collection flag to training-data-web', async () => {
    const fetchMock = stubFetch({
      studentIdentity: '230511213黄炳睿',
      handle: 'tourist',
      needCollect: false,
    });

    await updateCodeforcesHandleAccount('admin-token', '230511213黄炳睿', false);

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(url.pathname).toBe('/api/training-data/admin/codeforces/handles:change-identity');
    expect(init.method).toBe('PATCH');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer admin-token');
    expect(JSON.parse(init.body as string)).toEqual({
      oldStudentIdentity: '230511213黄炳睿',
      newStudentIdentity: '230511213黄炳睿',
      needCollect: false,
    });
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

  it('purges Codeforces student data through training-data-web', async () => {
    const fetchMock = stubFetch({
      studentIdentity: '230511213黄炳睿',
      handle: 'tourist',
      handleAccountRows: 1,
      odsSubmissionRows: 2,
      dwdSubmissionRows: 3,
      dwmFirstAcceptedRows: 4,
      dwsAcceptedSummaryRows: 5,
      totalDeletedRows: 15,
    });

    await purgeCodeforcesStudentData('admin-token', '230511213黄炳睿');

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(url.pathname).toBe(
      '/api/training-data/admin/codeforces/users/230511213%E9%BB%84%E7%82%B3%E7%9D%BF/data',
    );
    expect(init.method).toBe('DELETE');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer admin-token');
  });

  it('starts Codeforces submission collection jobs through training-data-web', async () => {
    const fetchMock = stubFetch({
      jobId: 'job-1',
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
    });
  });

  it('gets Codeforces submission collection jobs through training-data-web', async () => {
    const fetchMock = stubFetch({
      jobId: 'job-1',
      status: 'SUCCESS',
      requestedCount: 1,
      completedCount: 1,
      collectedCount: 1,
      failedCount: 0,
      refreshedCount: 0,
      writtenRows: 10,
      batchIds: ['collector-codeforces-1'],
      startedAt: '2026-07-06T03:00:00Z',
      finishedAt: '2026-07-06T03:01:00Z',
      message: '采集任务已完成',
      items: [],
    });

    await getCodeforcesSubmissionCollectionJob('admin-token', 'job-1');

    const url = new URL(fetchMock.mock.calls[0]?.[0] as string);
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(url.pathname).toBe('/api/training-data/admin/codeforces/submissions/collect-batch-jobs/job-1');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer admin-token');
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
      await getCodeforcesSubmissionCollectionJob('admin-token', 'job-1');
      throw new Error('expected request to fail');
    } catch (error) {
      expect(error).toBeInstanceOf(ApiError);
      expect((error as Error).message).toMatch(/HTTP 504 网关超时：upstream request timed out/);
    }
  });
});
