import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  batchCreateUsers,
  createHomepageFeaturedGroup,
  createCategory,
  createTag,
  deleteUser,
  deleteHomepageFeaturedImage,
  deleteHomepageFeaturedGroup,
  deleteCategory,
  deleteTag,
  deleteArticle,
	downloadAllArticlesBackup,
	restoreArticle,
  getCollectionJob,
  listAdminUsers,
  listCollectionJobs,
  listHomepageFeaturedImages,
  listHomepageFeaturedGroups,
  listAdminCategories,
	listAdminRecycleBinArticles,
  listAdminTags,
  updateUser,
  reorderHomepageFeaturedImages,
  reorderHomepageFeaturedGroups,
  searchHomepageFeaturedArticleCandidates,
  startCollectionJob,
  updateCategory,
  uploadHomepageFeaturedImage,
  updateHomepageFeaturedGroup,
} from '../api/admin';
import { changeCurrentPassword, getCurrentUser, login } from '../api/auth';
import { ApiError, authHeaders, requestData } from '../api/client';
import {
  getAcceptedSummaries,
  getAcceptedSummary,
  getProblemFirstAccepted,
  getProblemSubmissions,
  getUserFirstAccepted,
  getUserSubmissions,
  listTrainingUsers,
} from '../api/training';
import { OJ_NAMES, type PageQuery, type TrainingQueryRange } from '../types';

const range: TrainingQueryRange = {
  acceptedFromDateUtcPlus8: '2024-01-01',
  acceptedToDateUtcPlus8: '2024-01-31',
  minProblemRating: '1800',
  maxProblemRating: '2400',
};

const page: PageQuery = { page: 2, limit: 50 };

function stubFetch(body: unknown, status = 200) {
  const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(
    new Response(JSON.stringify(body), {
      headers: { 'Content-Type': 'application/json' },
      status,
    }),
  ));
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

function requestAt(fetchMock: ReturnType<typeof vi.fn>, index = 0) {
  const [input, init] = fetchMock.mock.calls[index] ?? [];
  return {
    init: (init ?? {}) as RequestInit,
    url: new URL(String(input), 'http://localhost'),
  };
}

function expectBearer(init: RequestInit, token = 'token') {
  expect(new Headers(init.headers).get('Authorization')).toBe(`Bearer ${token}`);
}

describe('Blog Result client', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('unwraps Blog Result data', async () => {
    stubFetch({ code: 200, errorCode: null, msg: 'ok', data: { username: 'player-a' } });

    await expect(requestData<{ username: string }>('/player/me'))
      .resolves.toEqual({ username: 'player-a' });
  });

  it('throws stable Blog errorCode even when the HTTP response is JSON', async () => {
    stubFetch(
      { code: 403, errorCode: 'AUTH_FORBIDDEN', msg: '权限不足', data: null },
      403,
    );

    await expect(requestData('/admin/users')).rejects.toMatchObject({
      status: 403,
      errorCode: 'AUTH_FORBIDDEN',
      message: '权限不足',
    });
  });

  it('exposes Retry-After seconds for login cooldown handling', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      code: 429,
      errorCode: 'AUTH_LOGIN_COOLDOWN',
      msg: '登录冷却中，请 5 秒后再试',
      data: null,
    }), {
      status: 429,
      headers: { 'Content-Type': 'application/json', 'Retry-After': '5' },
    }));
    vi.stubGlobal('fetch', fetchMock);

    await expect(requestData('/login')).rejects.toMatchObject({
      status: 429,
      errorCode: 'AUTH_LOGIN_COOLDOWN',
      retryAfterSeconds: 5,
    });
  });

  it('exposes the Blog API error and bearer-header contracts', () => {
    const error = new ApiError(401, 'AUTH_TOKEN_INVALID', 'expired', null);

    expect(error.name).toBe('ApiError');
    expect(authHeaders()).toEqual({});
    expect(authHeaders('jwt')).toEqual({ Authorization: 'Bearer jwt' });
  });

  it('preserves a Headers instance and an explicit Accept value', async () => {
    const fetchMock = stubFetch({ code: 200, errorCode: null, msg: 'ok', data: null });
    const headers = new Headers({
      Accept: 'application/vnd.custacm+json',
      Authorization: 'Bearer jwt',
      'Content-Type': 'application/json',
    });

    await requestData('/player/me', { headers });

    const sentHeaders = new Headers(requestAt(fetchMock).init.headers);
    expect(sentHeaders.get('Accept')).toBe('application/vnd.custacm+json');
    expect(sentHeaders.get('Authorization')).toBe('Bearer jwt');
    expect(sentHeaders.get('Content-Type')).toBe('application/json');
  });

  it('preserves tuple headers while adding the default Accept value', async () => {
    const fetchMock = stubFetch({ code: 200, errorCode: null, msg: 'ok', data: null });
    const headers: [string, string][] = [
      ['Authorization', 'Bearer jwt'],
      ['Content-Type', 'application/json'],
    ];

    await requestData('/player/me', { headers });

    const sentHeaders = new Headers(requestAt(fetchMock).init.headers);
    expect(sentHeaders.get('Accept')).toBe('application/json');
    expect(sentHeaders.get('Authorization')).toBe('Bearer jwt');
    expect(sentHeaders.get('Content-Type')).toBe('application/json');
  });
});

describe('focused Blog auth API', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('logs in with username and password through the shared API base', async () => {
    const user = {
      username: 'player-a',
      nickname: 'A',
      avatar: '',
      email: '',
      role: 'ROLE_player',
    } as const;
    const fetchMock = stubFetch({
      code: 200,
      errorCode: null,
      msg: 'ok',
      data: { token: 'jwt', user },
    });
    const controller = new AbortController();

    await expect(login('player-a', 'secret', controller.signal))
      .resolves.toEqual({ token: 'jwt', user });

    const { init, url } = requestAt(fetchMock);
    expect(url.pathname).toBe('/api/login');
    expect(init.method).toBe('POST');
    expect(init.signal).toBe(controller.signal);
    const sentHeaders = new Headers(init.headers);
    expect(sentHeaders.get('Accept')).toBe('application/json');
    expect(sentHeaders.get('Content-Type')).toBe('application/json');
    expect(JSON.parse(String(init.body))).toEqual({ username: 'player-a', password: 'secret' });
  });

  it('loads the current profile with bearer authentication', async () => {
    const fetchMock = stubFetch({
      code: 200,
      errorCode: null,
      msg: 'ok',
      data: { username: 'player-a', nickname: 'A', avatar: '', email: '', role: 'ROLE_player' },
    });
    const controller = new AbortController();

    await getCurrentUser('token', controller.signal);

    const { init, url } = requestAt(fetchMock);
    expect(url.pathname).toBe('/api/player/me');
    expect(init.signal).toBe(controller.signal);
    expectBearer(init);
  });

  it('changes the current password with the Blog request body', async () => {
    const fetchMock = stubFetch({ code: 200, errorCode: null, msg: 'ok', data: null });

    await changeCurrentPassword('token', 'old-password', 'new-password');

    const { init, url } = requestAt(fetchMock);
    expect(url.pathname).toBe('/api/player/me/password');
    expect(init.method).toBe('PATCH');
    expectBearer(init);
    expect(new Headers(init.headers).get('Content-Type')).toBe('application/json');
    expect(JSON.parse(String(init.body))).toEqual({
      oldPassword: 'old-password',
      newPassword: 'new-password',
    });
  });
});

describe('focused Blog training API', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('lists the authenticated training-user directory', async () => {
    const fetchMock = stubFetch({ code: 200, errorCode: null, msg: 'ok', data: [] });
    const controller = new AbortController();

    await listTrainingUsers('token', false, controller.signal);

    const { init, url } = requestAt(fetchMock);
    expect(url.pathname).toBe('/api/player/training-data/users');
    expect(url.searchParams.get('includeRetired')).toBeNull();
    expect(init.signal).toBe(controller.signal);
    expectBearer(init);
  });

  it('requests retired users only when the training view enables them', async () => {
    const fetchMock = stubFetch({ code: 200, errorCode: null, msg: 'ok', data: [] });

    await listTrainingUsers('token', true);

    const { url } = requestAt(fetchMock);
    expect(url.pathname).toBe('/api/player/training-data/users');
    expect(url.searchParams.get('includeRetired')).toBe('true');
  });

  it('passes username, OJ, date and rating filters to accepted-summary', async () => {
    const fetchMock = stubFetch({ code: 200, errorCode: null, msg: 'ok', data: {} });

    await getAcceptedSummary('token', 'player-a', range, OJ_NAMES.ATCODER);

    const { init, url } = requestAt(fetchMock);
    expect(url.pathname).toBe('/api/player/training-data/accepted-summary');
    expect(url.searchParams.get('username')).toBe('player-a');
    expect(url.searchParams.get('ojName')).toBe(OJ_NAMES.ATCODER);
    expect(url.searchParams.get('acceptedFromDateUtcPlus8')).toBe('2024-01-01');
    expect(url.searchParams.get('acceptedToDateUtcPlus8')).toBe('2024-01-31');
    expect(url.searchParams.get('minProblemRating')).toBe('1800');
    expect(url.searchParams.get('maxProblemRating')).toBe('2400');
    expectBearer(init);
  });

  it('loads all accepted summaries through one batch endpoint', async () => {
    const fetchMock = stubFetch({ code: 200, errorCode: null, msg: 'ok', data: [] });

    await getAcceptedSummaries('token', range, OJ_NAMES.ATCODER, true);

    const { init, url } = requestAt(fetchMock);
    expect(url.pathname).toBe('/api/player/training-data/accepted-summaries');
    expect(url.searchParams.get('ojName')).toBe('ATCODER');
    expect(url.searchParams.get('includeRetired')).toBe('true');
    expect(url.searchParams.get('acceptedFromDateUtcPlus8')).toBe('2024-01-01');
    expectBearer(init);
  });

  it('passes converted date, rating and page filters to user submissions', async () => {
    const fetchMock = stubFetch({ code: 200, errorCode: null, msg: 'ok', data: {} });

    await getUserSubmissions('token', 'player-a', range, page, OJ_NAMES.CODEFORCES);

    const { url } = requestAt(fetchMock);
    expect(url.pathname).toBe('/api/player/training-data/submissions/by-user');
    expect(url.searchParams.get('username')).toBe('player-a');
    expect(url.searchParams.get('submittedFromUtcPlus8')).toBe('2024-01-01T00:00:00');
    expect(url.searchParams.get('submittedToUtcPlus8')).toBe('2024-01-31T23:59:59');
    expect(url.searchParams.get('minProblemRating')).toBe('1800');
    expect(url.searchParams.get('maxProblemRating')).toBe('2400');
    expect(url.searchParams.get('page')).toBe('2');
    expect(url.searchParams.get('limit')).toBe('50');
  });

  it('passes supported filters to problem submissions', async () => {
    const fetchMock = stubFetch({ code: 200, errorCode: null, msg: 'ok', data: {} });

    await getProblemSubmissions('token', '2242:C', range, page, OJ_NAMES.CODEFORCES);

    const { url } = requestAt(fetchMock);
    expect(url.pathname).toBe('/api/player/training-data/submissions/by-problem');
    expect(url.searchParams.get('problemKey')).toBe('2242:C');
    expect(url.searchParams.get('submittedFromUtcPlus8')).toBe('2024-01-01T00:00:00');
    expect(url.searchParams.get('submittedToUtcPlus8')).toBe('2024-01-31T23:59:59');
    expect(url.searchParams.get('minProblemRating')).toBeNull();
    expect(url.searchParams.get('maxProblemRating')).toBeNull();
    expect(url.searchParams.get('page')).toBe('2');
  });

  it('passes converted filters to user first-accepted queries', async () => {
    const fetchMock = stubFetch({ code: 200, errorCode: null, msg: 'ok', data: {} });

    await getUserFirstAccepted('token', 'player-a', range, page, OJ_NAMES.ATCODER);

    const { url } = requestAt(fetchMock);
    expect(url.pathname).toBe('/api/player/training-data/first-accepted/by-user');
    expect(url.searchParams.get('username')).toBe('player-a');
    expect(url.searchParams.get('firstAcceptedFromUtcPlus8')).toBe('2024-01-01T00:00:00');
    expect(url.searchParams.get('firstAcceptedToUtcPlus8')).toBe('2024-01-31T23:59:59');
    expect(url.searchParams.get('minProblemRating')).toBe('1800');
    expect(url.searchParams.get('maxProblemRating')).toBe('2400');
  });

  it('passes supported filters to problem first-accepted queries', async () => {
    const fetchMock = stubFetch({ code: 200, errorCode: null, msg: 'ok', data: {} });

    await getProblemFirstAccepted('token', '2242:C', range, page, OJ_NAMES.ATCODER);

    const { url } = requestAt(fetchMock);
    expect(url.pathname).toBe('/api/player/training-data/first-accepted/by-problem');
    expect(url.searchParams.get('problemKey')).toBe('2242:C');
    expect(url.searchParams.get('firstAcceptedFromUtcPlus8')).toBe('2024-01-01T00:00:00');
    expect(url.searchParams.get('firstAcceptedToUtcPlus8')).toBe('2024-01-31T23:59:59');
    expect(url.searchParams.get('minProblemRating')).toBeNull();
    expect(url.searchParams.get('page')).toBe('2');
    expect(url.searchParams.get('limit')).toBe('50');
  });
});

describe('focused Blog admin API', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('sends the batch-create body as a raw array', async () => {
    const fetchMock = stubFetch({ code: 200, errorCode: null, msg: 'ok', data: [] });

    await batchCreateUsers('token', [{ username: 'player-a', role: 'ROLE_player' }]);

    const { init, url } = requestAt(fetchMock);
    expect(url.pathname).toBe('/api/admin/users:batch-create');
    expect(init.method).toBe('POST');
    expectBearer(init);
    expect(JSON.parse(String(init.body))).toEqual([
      { username: 'player-a', role: 'ROLE_player' },
    ]);
  });

  it('maps list, atomic update and delete to Blog user routes', async () => {
    const fetchMock = stubFetch({ code: 200, errorCode: null, msg: 'ok', data: {} });

    await listAdminUsers('token');
    await updateUser('token', 'player/a', {
      newUsername: 'player/a', nickname: 'A', email: '', role: 'ROLE_player',
      handles: { [OJ_NAMES.CODEFORCES]: 'tourist' },
      needCollect: true,
    });
    await deleteUser('token', 'player/a');

    expect(requestAt(fetchMock, 0).url.pathname).toBe('/api/admin/users');
    expect(requestAt(fetchMock, 0).init.method).toBeUndefined();
    expect(requestAt(fetchMock, 1).url.pathname).toBe('/api/admin/users/player%2Fa');
    expect(requestAt(fetchMock, 1).init.method).toBe('PUT');
    expect(JSON.parse(String(requestAt(fetchMock, 1).init.body))).toEqual({
      newUsername: 'player/a', nickname: 'A', email: '', role: 'ROLE_player',
      handles: { CODEFORCES: 'tourist' }, needCollect: true,
    });
    expect(requestAt(fetchMock, 2).url.pathname).toBe('/api/admin/users/player%2Fa');
    expect(requestAt(fetchMock, 2).init.method).toBe('DELETE');
    expectBearer(requestAt(fetchMock, 2).init);
  });

  it('passes the refresh abort signal to the admin user list request', async () => {
    const fetchMock = stubFetch({ code: 200, errorCode: null, msg: 'ok', data: [] });
    const controller = new AbortController();

    await listAdminUsers('token', controller.signal);

    expect(requestAt(fetchMock).init.signal).toBe(controller.signal);
  });

  it('maps category list, create, update and delete to Blog admin routes', async () => {
    const fetchMock = stubFetch({ code: 200, errorCode: null, msg: 'ok', data: {} });

    await listAdminCategories('token', 2, 20);
    await createCategory('token', '算法');
    await updateCategory('token', { id: 7, name: '赛事题解' });
    await deleteCategory('token', 7);

    expect(requestAt(fetchMock, 0).url.pathname).toBe('/api/admin/categories');
    expect(requestAt(fetchMock, 0).url.searchParams.get('pageNum')).toBe('2');
    expect(requestAt(fetchMock, 0).url.searchParams.get('pageSize')).toBe('20');
    expect(requestAt(fetchMock, 1).init.method).toBe('POST');
    expect(JSON.parse(String(requestAt(fetchMock, 1).init.body))).toEqual({ name: '算法', color: '#8B1E3F' });
    expect(requestAt(fetchMock, 2).init.method).toBe('PUT');
    expect(JSON.parse(String(requestAt(fetchMock, 2).init.body))).toEqual({ id: 7, name: '赛事题解' });
    expect(requestAt(fetchMock, 3).url.searchParams.get('id')).toBe('7');
    expect(requestAt(fetchMock, 3).init.method).toBe('DELETE');
    expectBearer(requestAt(fetchMock, 3).init);
  });

	it('moves, lists and restores articles through the Blog admin recycle bin routes', async () => {
		const fetchMock = stubFetch({ code: 200, errorCode: null, msg: 'ok', data: null });

		await deleteArticle('token', 42);
		await listAdminRecycleBinArticles('token', { pageNum: 2 });
		await restoreArticle('token', 42);

		const { init, url } = requestAt(fetchMock, 0);
		expect(url.pathname).toBe('/api/admin/blog');
    expect(url.searchParams.get('id')).toBe('42');
		expect(init.method).toBe('DELETE');
		expectBearer(init);
		expect(requestAt(fetchMock, 1).url.pathname).toBe('/api/admin/blogs/recycle-bin');
		expect(requestAt(fetchMock, 1).url.searchParams.get('pageNum')).toBe('2');
		expect(requestAt(fetchMock, 2).url.pathname).toBe('/api/admin/blog/restore');
		expect(requestAt(fetchMock, 2).init.method).toBe('PUT');
		expectBearer(requestAt(fetchMock, 2).init);
	});

	it('downloads the complete article backup as an authenticated zip', async () => {
		const fetchMock = vi.fn().mockResolvedValue(new Response('zip-data', {
			status: 200,
			headers: {
				'Content-Type': 'application/zip',
				'Content-Disposition': 'attachment; filename="custacm-article-backup-20260713-223500.zip"',
			},
		}));
		vi.stubGlobal('fetch', fetchMock);

		const download = await downloadAllArticlesBackup('token');

		expect(download.filename).toBe('custacm-article-backup-20260713-223500.zip');
		expect(await download.blob.text()).toBe('zip-data');
		expect(requestAt(fetchMock).url.pathname).toBe('/api/admin/blogs/backup');
		expectBearer(requestAt(fetchMock).init);
		expect(new Headers(requestAt(fetchMock).init.headers).get('Accept')).toBe('application/zip');
	});

  it('lists, creates without a color choice and deletes tags', async () => {
    const fetchMock = stubFetch({ code: 200, errorCode: null, msg: 'ok', data: {} });

    await listAdminTags('token', 2, 20);
    await createTag('token', '图论');
    await deleteTag('token', 9);

    expect(requestAt(fetchMock, 0).url.pathname).toBe('/api/admin/tags');
    expect(requestAt(fetchMock, 0).url.searchParams.get('pageNum')).toBe('2');
    expect(JSON.parse(String(requestAt(fetchMock, 1).init.body))).toEqual({ name: '图论' });
    expect(requestAt(fetchMock, 2).url.pathname).toBe('/api/admin/tag');
    expect(requestAt(fetchMock, 2).url.searchParams.get('id')).toBe('9');
    expect(requestAt(fetchMock, 2).init.method).toBe('DELETE');
  });

  it('maps collection jobs to Blog admin routes', async () => {
    const fetchMock = stubFetch({ code: 200, errorCode: null, msg: 'ok', data: {} });
    const collectionListController = new AbortController();
    const collectionJobController = new AbortController();

    await startCollectionJob('token', {
      usernames: ['player-a'],
      lookbackHours: 24,
      refreshWarehouse: true,
      ojName: OJ_NAMES.CODEFORCES,
    });
    await listCollectionJobs('token', collectionListController.signal);
    await getCollectionJob('token', 'job/a', collectionJobController.signal);

    expect(requestAt(fetchMock, 0).url.pathname)
      .toBe('/api/admin/training-data/submission-collection-jobs');
    expect(requestAt(fetchMock, 0).init.method).toBe('POST');
    expect(requestAt(fetchMock, 1).url.pathname)
      .toBe('/api/admin/training-data/submission-collection-jobs');
    expect(requestAt(fetchMock, 1).init.signal).toBe(collectionListController.signal);
    expect(requestAt(fetchMock, 2).url.pathname)
      .toBe('/api/admin/training-data/submission-collection-jobs/job%2Fa');
    expect(requestAt(fetchMock, 2).init.signal).toBe(collectionJobController.signal);
  });

  it('uses multipart upload and complete ordering for homepage featured images', async () => {
    const fetchMock = stubFetch({ code: 200, errorCode: null, msg: 'ok', data: [] });
    const image = new Blob(['jpeg'], { type: 'image/jpeg' });

    await listHomepageFeaturedImages('token');
    await uploadHomepageFeaturedImage('token', image);
    await reorderHomepageFeaturedImages('token', [3, 1, 2]);
    await deleteHomepageFeaturedImage('token', 3);

    expect(requestAt(fetchMock, 0).url.pathname).toBe('/api/admin/homepage-featured-images');
    expect(requestAt(fetchMock, 1).init.body).toBeInstanceOf(FormData);
    expect(new Headers(requestAt(fetchMock, 1).init.headers).has('Content-Type')).toBe(false);
    expect(requestAt(fetchMock, 2).url.pathname).toBe('/api/admin/homepage-featured-images/order');
    expect(JSON.parse(String(requestAt(fetchMock, 2).init.body))).toEqual({ ids: [3, 1, 2] });
    expect(requestAt(fetchMock, 3).url.pathname).toBe('/api/admin/homepage-featured-images/3');
    expect(requestAt(fetchMock, 3).init.method).toBe('DELETE');
    expectBearer(requestAt(fetchMock, 3).init);
  });

  it('maps homepage featured-group composition to the admin contracts', async () => {
    const fetchMock = stubFetch({ code: 200, errorCode: null, msg: 'ok', data: [] });
    const request = { title: '训练方法', articleIds: [11, 12, 13] };

    await listHomepageFeaturedGroups('token');
    await searchHomepageFeaturedArticleCandidates('token', '复盘');
    await createHomepageFeaturedGroup('token', request);
    await updateHomepageFeaturedGroup('token', 7, request);
    await reorderHomepageFeaturedGroups('token', [7, 3]);
    await deleteHomepageFeaturedGroup('token', 7);

    expect(requestAt(fetchMock, 0).url.pathname).toBe('/api/admin/homepage-featured-groups');
    expect(requestAt(fetchMock, 1).url.pathname).toBe('/api/admin/homepage-featured-groups/candidates');
    expect(requestAt(fetchMock, 1).url.searchParams.get('query')).toBe('复盘');
    expect(requestAt(fetchMock, 2).init.method).toBe('POST');
    expect(JSON.parse(String(requestAt(fetchMock, 2).init.body))).toEqual(request);
    expect(requestAt(fetchMock, 3).url.pathname).toBe('/api/admin/homepage-featured-groups/7');
    expect(requestAt(fetchMock, 3).init.method).toBe('PUT');
    expect(JSON.parse(String(requestAt(fetchMock, 4).init.body))).toEqual({ ids: [7, 3] });
    expect(requestAt(fetchMock, 5).init.method).toBe('DELETE');
    expectBearer(requestAt(fetchMock, 5).init);
  });
});
