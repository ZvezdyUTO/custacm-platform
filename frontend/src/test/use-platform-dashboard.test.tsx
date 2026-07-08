import { act, cleanup, renderHook, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { usePlatformDashboard } from '../hooks/usePlatformDashboard';
import { OJ_NAMES, type TrainingQueryRange } from '../types';

const users = [
  {
    studentIdentity: '230511213黄炳睿',
    role: 'player',
    createdAt: '2026-07-06T00:00:00Z',
    updatedAt: '2026-07-06T00:00:00Z',
  },
  {
    studentIdentity: '230511214李明',
    role: 'player',
    createdAt: '2026-07-06T00:00:00Z',
    updatedAt: '2026-07-06T00:00:00Z',
  },
  {
    studentIdentity: '9999999托宝',
    role: 'player',
    createdAt: '2026-07-06T00:00:00Z',
    updatedAt: '2026-07-06T00:00:00Z',
  },
] as const;

const queryRange: TrainingQueryRange = {
  acceptedFromDateUtcPlus8: '2026-07-02',
  acceptedToDateUtcPlus8: '2026-07-08',
  minProblemRating: '',
  maxProblemRating: '',
};

function jsonResponse(body: unknown) {
  return new Response(JSON.stringify(body), {
    headers: { 'Content-Type': 'application/json' },
    status: 200,
  });
}

function stubPlatformFetch() {
  const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
    const url = new URL(String(input), window.location.origin);
    if (url.pathname === '/health/auth') {
      return jsonResponse({ service: 'auth-web', status: 'UP' });
    }
    if (url.pathname === '/health/training-data') {
      return jsonResponse({ service: 'training-data-web', status: 'UP' });
    }
    if (url.pathname === '/module-info/auth') {
      return jsonResponse({ module: 'platform-auth', service: 'auth-web', features: ['login'] });
    }
    if (url.pathname === '/module-info/training-data') {
      return jsonResponse({ module: 'platform-training-data', service: 'training-data-web', features: ['oj-query'] });
    }
    if (url.pathname === '/api/auth/users') {
      return jsonResponse(users);
    }
    if (url.pathname === '/api/training-data/oj-handles') {
      return jsonResponse({
        '230511213黄炳睿': {
          studentIdentity: '230511213黄炳睿',
          handles: { [OJ_NAMES.CODEFORCES]: 'tourist' },
          needCollect: true,
          collectionStates: {},
        },
        '230511214李明': {
          studentIdentity: '230511214李明',
          handles: { [OJ_NAMES.CODEFORCES]: 'Benq' },
          needCollect: false,
          collectionStates: {},
        },
      });
    }
    if (url.pathname === '/api/training-data/codeforces/accepted-summary') {
      const studentIdentity = url.searchParams.get('studentIdentity');
      if (studentIdentity === '230511213黄炳睿') {
        return jsonResponse({
          studentIdentity,
          authorHandle: 'tourist',
          totalAcceptedProblemCount: 2,
          ratingCounts: [
            { problemRating: '1800', acceptedProblemCount: 1 },
            { problemRating: '2100', acceptedProblemCount: 1 },
          ],
        });
      }
      if (studentIdentity === '230511214李明') {
        return jsonResponse({
          studentIdentity,
          authorHandle: 'Benq',
          totalAcceptedProblemCount: 5,
          ratingCounts: [
            { problemRating: '1600', acceptedProblemCount: 3 },
            { problemRating: '1800', acceptedProblemCount: 2 },
          ],
        });
      }
    }
    throw new Error(`Unexpected request: ${url.pathname}${url.search}`);
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

function installMemoryLocalStorage() {
  const values = new Map<string, string>();
  Object.defineProperty(window, 'localStorage', {
    configurable: true,
    value: {
      clear: vi.fn(() => values.clear()),
      getItem: vi.fn((key: string) => values.get(key) ?? null),
      removeItem: vi.fn((key: string) => values.delete(key)),
      setItem: vi.fn((key: string, value: string) => values.set(key, value)),
    },
  });
}

describe('usePlatformDashboard', () => {
  afterEach(() => {
    cleanup();
    window.localStorage.clear();
    vi.unstubAllGlobals();
  });

  it('loads multi-user accepted summaries for automatic-collection users when applying a multiple query', async () => {
    installMemoryLocalStorage();
    const fetchMock = stubPlatformFetch();
    const { result } = renderHook(() => usePlatformDashboard());

    await waitFor(() => expect(result.current.status).toBe('ready'));
    fetchMock.mockClear();

    await act(async () => {
      await result.current.applyTrainingQuery(queryRange, 'multiple');
    });

    await waitFor(() => {
      expect(result.current.multiUserAcceptedSummaries.map((summary) => summary.studentIdentity)).toEqual([
        '230511213黄炳睿',
      ]);
    });
    expect(result.current.multiUserAcceptedSummaries.map((summary) => summary.totalAcceptedProblemCount)).toEqual([2]);
    const acceptedSummaryUrls = fetchMock.mock.calls
      .map(([input]) => new URL(String(input), window.location.origin))
      .filter((url) => url.pathname === '/api/training-data/codeforces/accepted-summary');
    expect(acceptedSummaryUrls).toHaveLength(1);
    expect(acceptedSummaryUrls[0]?.searchParams.get('studentIdentity')).toBe('230511213黄炳睿');
    expect(acceptedSummaryUrls.every((url) => url.searchParams.get('acceptedFromDateUtcPlus8') === '2026-07-02')).toBe(true);
    expect(acceptedSummaryUrls.every((url) => url.searchParams.get('acceptedToDateUtcPlus8') === '2026-07-08')).toBe(true);
  });

  it('loads automatic-collection summaries during the initial dashboard refresh', async () => {
    installMemoryLocalStorage();
    const fetchMock = stubPlatformFetch();
    const { result } = renderHook(() => usePlatformDashboard());

    await waitFor(() => {
      expect(result.current.multiUserAcceptedSummaries.map((summary) => summary.studentIdentity)).toEqual([
        '230511213黄炳睿',
      ]);
    });
    expect(result.current.status).toBe('ready');
    expect(result.current.trainingQuery.acceptedFromDateUtcPlus8).not.toBe('');
    expect(result.current.trainingQuery.acceptedToDateUtcPlus8).toBe('');

    const acceptedSummaryUrls = fetchMock.mock.calls
      .map(([input]) => new URL(String(input), window.location.origin))
      .filter((url) => url.pathname === '/api/training-data/codeforces/accepted-summary');
    expect(acceptedSummaryUrls).toHaveLength(1);
    expect(acceptedSummaryUrls[0]?.searchParams.get('studentIdentity')).toBe('230511213黄炳睿');
    expect(acceptedSummaryUrls[0]?.searchParams.get('acceptedToDateUtcPlus8')).toBeNull();
  });
});
