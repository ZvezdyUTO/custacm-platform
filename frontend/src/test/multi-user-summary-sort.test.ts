// Author: huangbingrui.awa
import { describe, expect, it } from 'vitest';
import { compareMultiUserSummaryRows, type MultiUserSummaryRow } from '../composables/usePlatformDashboard';
import { OJ_NAMES, type AcceptedSummary, type TrainingUser } from '../types';

function row(username: string, total: number | null): MultiUserSummaryRow {
  const user: TrainingUser = { username, nickname: username, ojNames: [OJ_NAMES.CODEFORCES] };
  const summary: AcceptedSummary | null = total === null ? null : {
    username,
    authorHandle: username,
    totalAcceptedProblemCount: total,
    ratingCounts: [],
  };
  return { user, summary, status: summary ? 'ready' : 'error', message: summary ? null : '查询失败' };
}

describe('compareMultiUserSummaryRows', () => {
  it('sorts accepted totals descending and uses username for ties', () => {
    const rows = [row('player-c', 0), row('player-b', 156), row('player-a', 156)];

    expect(rows.sort(compareMultiUserSummaryRows).map((item) => item.user.username))
      .toEqual(['player-a', 'player-b', 'player-c']);
  });

  it('puts failed queries after successful rows', () => {
    const rows = [row('player-a', null), row('player-b', 0)];

    expect(rows.sort(compareMultiUserSummaryRows).map((item) => item.user.username))
      .toEqual(['player-b', 'player-a']);
  });
});
