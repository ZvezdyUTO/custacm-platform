// Author: huangbingrui.awa
import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  addCompetitionAward,
  addCompetitionParticipants,
  createCompetition,
  deleteCompetitionAward,
  deleteCompetitionParticipant,
  listCompetitionRecycleBin,
  listCompetitions,
  moveCompetitionToRecycleBin,
  restoreCompetition,
} from '../api/admin';
import type { Competition } from '../types';

const competition: Competition = {
  id: 7,
  fullName: '2026 ICPC 亚洲区域赛（合肥）',
  year: 2026,
  category: 'ICPC_ASIA_REGIONAL',
  categoryLabel: 'ICPC 亚洲区域赛',
  participationMode: 'TEAM',
  participationModeLabel: '团队',
  types: [{ code: 'ICPC', label: 'ICPC' }, { code: 'ASIA_REGIONAL', label: 'ICPC 亚洲区域赛' }],
  createTime: '2026-07-14T08:00:00Z',
  deletedAt: null,
  participants: [],
  awards: [],
};

function stubFetch(data: unknown) {
  const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(new Response(JSON.stringify({
    code: 200,
    errorCode: null,
    msg: 'ok',
    data,
  }), { status: 200, headers: { 'Content-Type': 'application/json' } })));
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

function requestAt(fetchMock: ReturnType<typeof vi.fn>, index = 0) {
  const [input, init] = fetchMock.mock.calls[index] ?? [];
  return {
    url: new URL(String(input), 'http://localhost'),
    init: (init ?? {}) as RequestInit,
  };
}

describe('competition admin API contract', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('loads the active public list without attaching the admin token', async () => {
    const fetchMock = stubFetch({ pageNum: 2, pageSize: 10, total: 1, totalPages: 1, list: [competition] });

    await listCompetitions({
      startYear: 2024,
      endYear: 2026,
      category: 'ICPC_ASIA_REGIONAL',
      pageNum: 2,
      pageSize: 10,
    });

    const { url, init } = requestAt(fetchMock);
    expect(url.pathname).toBe('/api/competitions');
    expect(Object.fromEntries(url.searchParams)).toEqual({
      startYear: '2024', endYear: '2026', category: 'ICPC_ASIA_REGIONAL', pageNum: '2', pageSize: '10',
    });
    expect(new Headers(init.headers).get('Authorization')).toBeNull();
  });

  it('loads the recycle bin with an explicit bearer token', async () => {
    const fetchMock = stubFetch({ pageNum: 1, pageSize: 15, total: 0, totalPages: 0, list: [] });

    await listCompetitionRecycleBin('jwt', { pageNum: 1, pageSize: 15 });

    const { url, init } = requestAt(fetchMock);
    expect(url.pathname).toBe('/api/admin/competitions/recycle-bin');
    expect(Object.fromEntries(url.searchParams)).toEqual({ pageNum: '1', pageSize: '15' });
    expect(new Headers(init.headers).get('Authorization')).toBe('Bearer jwt');
  });

  it('uses the add-only competition, participant, and award write contracts', async () => {
    const fetchMock = stubFetch(competition);

    await createCompetition('jwt', {
      fullName: competition.fullName,
      year: 2026,
      category: 'ICPC_ASIA_REGIONAL',
      participationMode: 'TEAM',
    });
    await addCompetitionParticipants('jwt', 7, { usernames: ['alice', 'bob'] });
    await addCompetitionAward('jwt', 7, {
      awardMode: 'TEAM',
      teamName: 'CUST ACM',
      awardTier: 'MEDAL_GOLD',
      rankPosition: 3,
      rankTotal: 280,
      recipientUsernames: ['alice', 'bob'],
    });

    const create = requestAt(fetchMock, 0);
    expect(create.url.pathname).toBe('/api/admin/competitions');
    expect(create.init.method).toBe('POST');
    expect(JSON.parse(String(create.init.body))).toEqual({
      fullName: competition.fullName,
      year: 2026,
      category: 'ICPC_ASIA_REGIONAL',
      participationMode: 'TEAM',
    });

    const participants = requestAt(fetchMock, 1);
    expect(participants.url.pathname).toBe('/api/admin/competitions/7/participants');
    expect(participants.init.method).toBe('POST');
    expect(JSON.parse(String(participants.init.body))).toEqual({ usernames: ['alice', 'bob'] });

    const award = requestAt(fetchMock, 2);
    expect(award.url.pathname).toBe('/api/admin/competitions/7/awards');
    expect(award.init.method).toBe('POST');
    expect(JSON.parse(String(award.init.body))).toMatchObject({
      awardMode: 'TEAM', awardTier: 'MEDAL_GOLD', rankPosition: 3, rankTotal: 280,
      recipientUsernames: ['alice', 'bob'],
    });
    expect(JSON.parse(String(award.init.body))).not.toHaveProperty('awardScope');
    expect(JSON.parse(String(award.init.body))).not.toHaveProperty('awardName');
    for (let index = 0; index < 3; index += 1) {
      expect(new Headers(requestAt(fetchMock, index).init.headers).get('Authorization')).toBe('Bearer jwt');
    }
  });

  it('keeps ordinary award ranks explicitly nullable in the request body', async () => {
    const fetchMock = stubFetch(competition);

    await addCompetitionAward('jwt', 7, {
      awardMode: 'INDIVIDUAL',
      teamName: null,
      awardTier: 'BAIDU_NATIONAL_FIRST',
      rankPosition: null,
      rankTotal: null,
      recipientUsernames: ['alice'],
    });

    expect(JSON.parse(String(requestAt(fetchMock).init.body))).toEqual({
      awardMode: 'INDIVIDUAL',
      teamName: null,
      awardTier: 'BAIDU_NATIONAL_FIRST',
      rankPosition: null,
      rankTotal: null,
      recipientUsernames: ['alice'],
    });
  });

  it('uses explicit destructive and restore endpoints without inventing edit APIs', async () => {
    const fetchMock = stubFetch(competition);

    await deleteCompetitionParticipant('jwt', 7, 11);
    await deleteCompetitionAward('jwt', 7, 21);
    await moveCompetitionToRecycleBin('jwt', 7);
    await restoreCompetition('jwt', 7);

    expect(requestAt(fetchMock, 0).url.pathname).toBe('/api/admin/competitions/7/participants/11');
    expect(requestAt(fetchMock, 0).init.method).toBe('DELETE');
    expect(requestAt(fetchMock, 1).url.pathname).toBe('/api/admin/competitions/7/awards/21');
    expect(requestAt(fetchMock, 1).init.method).toBe('DELETE');
    expect(requestAt(fetchMock, 2).url.pathname).toBe('/api/admin/competitions/7');
    expect(requestAt(fetchMock, 2).init.method).toBe('DELETE');
    expect(requestAt(fetchMock, 3).url.pathname).toBe('/api/admin/competitions/7/restore');
    expect(requestAt(fetchMock, 3).init.method).toBe('PUT');
  });
});
