import { OJ_NAMES } from '../types';
import type {
  AcceptedSummary,
  ProblemFirstAcceptedReport,
  ProblemSubmissionReport,
  TrainingUser,
  UserFirstAcceptedReport,
  UserSubmissionReport,
} from '../types';

export const trainingUsersFixture: TrainingUser[] = [
  { username: 'player-a', nickname: '队员 A', ojNames: [OJ_NAMES.CODEFORCES] },
  { username: 'player-b', nickname: '', ojNames: [OJ_NAMES.CODEFORCES, OJ_NAMES.ATCODER] },
];

export const acceptedSummaryFixture: AcceptedSummary = {
  username: 'player-a',
  authorHandle: 'tourist',
  totalAcceptedProblemCount: 2,
  ratingCounts: [
    { problemRating: '1800', acceptedProblemCount: 1 },
    { problemRating: '2100', acceptedProblemCount: 1 },
  ],
};

export const userSubmissionsFixture: UserSubmissionReport = {
  username: 'player-a',
  authorHandle: 'tourist',
  page: 1,
  limit: 15,
  total: 1,
  totalPages: 1,
  hasMore: false,
  submissions: [{
    submissionId: '1',
    username: 'player-a',
    handle: 'tourist',
    submittedAtUtcPlus8: '2026-07-05T12:00:00',
    submittedDateUtcPlus8: '2026-07-05',
    problemKey: '2053:D',
    problemIndex: 'D',
    problemName: 'Remove and Add',
    difficulty: '1800',
    language: 'Kotlin 1.9',
    verdict: 'OK',
    accepted: true,
    timeConsumedMillis: 93,
    sourceUrl: 'https://codeforces.com/contest/2053/submission/1',
  }],
};

export const userFirstAcceptedFixture: UserFirstAcceptedReport = {
  username: 'player-a',
  authorHandle: 'tourist',
  totalAcceptedProblemCount: 1,
  page: 1,
  limit: 15,
  total: 1,
  totalPages: 1,
  hasMore: false,
  problems: [{
    problemKey: '2053:D',
    problemIndex: 'D',
    problemName: 'Remove and Add',
    difficulty: '1800',
    firstAcceptedSubmissionId: '1',
    firstAcceptedAtUtcPlus8: '2026-07-05T12:00:00',
    firstAcceptedDateUtcPlus8: '2026-07-05',
    firstAcceptedLanguage: 'Kotlin 1.9',
    firstAcceptedSourceUrl: 'https://codeforces.com/contest/2053/submission/1',
  }],
};

export const problemSubmissionsFixture: ProblemSubmissionReport = {
  problemKey: '2242:C',
  page: 1,
  limit: 15,
  total: 1,
  totalPages: 1,
  hasMore: false,
  submissions: [{
    ...userSubmissionsFixture.submissions[0]!,
    submissionId: '21',
    problemKey: '2242:C',
    problemIndex: 'C',
    problemName: 'Sample Problem',
  }],
};

export const problemFirstAcceptedFixture: ProblemFirstAcceptedReport = {
  problemKey: '2242:C',
  acceptedHandleCount: 1,
  page: 1,
  limit: 15,
  total: 1,
  totalPages: 1,
  hasMore: false,
  acceptedHandles: [{
    username: 'player-a',
    handle: 'tourist',
    firstAcceptedAtUtcPlus8: '2026-07-05T12:00:00',
  }],
};
