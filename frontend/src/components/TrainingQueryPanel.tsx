import { ChevronLeft, ChevronRight, RefreshCw } from 'lucide-react';
import { useEffect, useMemo, useRef, useState } from 'react';
import { OJ_LABELS, OJ_NAMES } from '../types';
import type {
  CodeforcesAcceptedSummary,
  CodeforcesFirstAcceptedProblem,
  CodeforcesFirstAcceptedReport,
  CodeforcesProblemFirstAcceptedReport,
  CodeforcesProblemSubmissionReport,
  CodeforcesSubmissionItem,
  CodeforcesStudentSubmissionReport,
  OjName,
  StudentIdentity,
  StudentTrainingRecord,
  TrainingQueryMode,
  TrainingQueryRange,
} from '../types';

interface TrainingQueryPanelProps {
  multiUserSummaries: CodeforcesAcceptedSummary[];
  firstAccepted: CodeforcesFirstAcceptedReport | null;
  isRefreshing: boolean;
  ojName: OjName;
  onApplyQuery: (query: TrainingQueryRange, mode: TrainingQueryMode) => Promise<void>;
  onFirstAcceptedPageChange: (page: number, limit: number) => Promise<void>;
  onOjNameChange: (ojName: OjName) => void;
  onProblemKeyChange: (problemKey: string) => void;
  onProblemFirstAcceptedPageChange: (page: number, limit: number) => Promise<void>;
  onProblemSubmissionPageChange: (page: number, limit: number) => Promise<void>;
  onQueryModeChange: (mode: TrainingQueryMode) => void;
  onRefresh: (mode: TrainingQueryMode) => Promise<void>;
  onSubmissionPageChange: (page: number, limit: number) => Promise<void>;
  onSelectedIdentityChange: (studentIdentity: StudentIdentity) => Promise<void>;
  problemFirstAccepted: CodeforcesProblemFirstAcceptedReport | null;
  problemFirstAcceptedLimit: number;
  problemFirstAcceptedPage: number;
  problemKey: string;
  problemSubmissionLimit: number;
  problemSubmissionPage: number;
  problemSubmissions: CodeforcesProblemSubmissionReport | null;
  query: TrainingQueryRange;
  queryMode: TrainingQueryMode;
  record: StudentTrainingRecord | null;
  selectedIdentity: StudentIdentity | null;
  firstAcceptedLimit: number;
  firstAcceptedPage: number;
  submissionLimit: number;
  submissionPage: number;
  studentOptions: StudentIdentity[];
  submissions: CodeforcesStudentSubmissionReport | null;
  updatedAt: string;
}

const submissionPageSizes = [15, 50, 100, 200];
const multiSummaryPlayerColumnWidth = 204;
const multiSummaryTotalColumnWidth = 72;
const multiSummaryRatingColumnWidth = 56;

type ActivityView = 'submissions' | 'accepted';

function normalizeQueryRange(query: TrainingQueryRange): TrainingQueryRange {
  return {
    acceptedFromDateUtcPlus8: query.acceptedFromDateUtcPlus8,
    acceptedToDateUtcPlus8: query.acceptedToDateUtcPlus8,
    minProblemRating: query.minProblemRating.trim(),
    maxProblemRating: query.maxProblemRating.trim(),
  };
}

function getQueryRangeError(query: TrainingQueryRange) {
  if (
    query.acceptedFromDateUtcPlus8
    && query.acceptedToDateUtcPlus8
    && query.acceptedFromDateUtcPlus8 > query.acceptedToDateUtcPlus8
  ) {
    return '通过起始日期不能晚于结束日期。';
  }

  const minRating = query.minProblemRating ? Number(query.minProblemRating) : null;
  const maxRating = query.maxProblemRating ? Number(query.maxProblemRating) : null;
  if (minRating !== null && maxRating !== null && minRating > maxRating) {
    return '最低 rating 不能大于最高 rating。';
  }

  return null;
}

function formatSubmissionVerdict(verdict: string | null, accepted: boolean) {
  if (accepted || verdict === 'OK') {
    return 'Accept';
  }
  return verdict ?? 'UNKNOWN';
}

function compareTimeDesc(left: string | null, right: string | null) {
  if (left && right) {
    return right.localeCompare(left);
  }
  if (left) {
    return -1;
  }
  if (right) {
    return 1;
  }
  return 0;
}

function ratingBarColor(problemRating: string) {
  const rating = ratingBucketStart(problemRating);
  if (!Number.isFinite(rating)) {
    return '#808080';
  }
  if (rating < 1200) {
    return '#808080';
  }
  if (rating < 1400) {
    return '#008000';
  }
  if (rating < 1600) {
    return '#03a89e';
  }
  if (rating < 1900) {
    return '#0000ff';
  }
  if (rating < 2100) {
    return '#aa00aa';
  }
  if (rating < 2400) {
    return '#ff8c00';
  }
  return '#ff0000';
}

function ratingBucketLabel(problemRating: string) {
  return problemRating === 'UNRATED' ? 'UNR' : problemRating;
}

function ratingToneClass(problemRating: string) {
  const rating = ratingBucketStart(problemRating);
  if (!Number.isFinite(rating)) {
    return 'rating-tone-gray';
  }
  if (rating < 1200) {
    return 'rating-tone-gray';
  }
  if (rating < 1400) {
    return 'rating-tone-green';
  }
  if (rating < 1600) {
    return 'rating-tone-cyan';
  }
  if (rating < 1900) {
    return 'rating-tone-blue';
  }
  if (rating < 2100) {
    return 'rating-tone-violet';
  }
  if (rating < 2400) {
    return 'rating-tone-orange';
  }
  return 'rating-tone-red';
}

function compareRatingBucket(left: string, right: string) {
  if (left === 'UNRATED' && right !== 'UNRATED') {
    return 1;
  }
  if (left !== 'UNRATED' && right === 'UNRATED') {
    return -1;
  }
  const leftRating = ratingBucketStart(left);
  const rightRating = ratingBucketStart(right);
  if (Number.isFinite(leftRating) && Number.isFinite(rightRating)) {
    return leftRating - rightRating;
  }
  return left.localeCompare(right);
}

function ratingBucketStart(problemRating: string) {
  if (problemRating === 'UNRATED') {
    return Number.NaN;
  }
  const match = problemRating.match(/^\d+/);
  if (!match) {
    return Number.NaN;
  }
  return Number(match[0]);
}

function submissionId(submission: CodeforcesSubmissionItem) {
  return submission.submissionId ?? String(submission.codeforcesSubmissionId ?? '');
}

function submissionDifficulty(submission: CodeforcesSubmissionItem) {
  const difficulty = submission.difficulty ?? (
    submission.problemRating === null || submission.problemRating === undefined
      ? null
      : String(submission.problemRating)
  );
  return difficulty && difficulty !== 'UNRATED' ? `${difficulty} rating` : difficulty;
}

function firstAcceptedDifficulty(problem: CodeforcesFirstAcceptedProblem) {
  const difficulty = problem.difficulty ?? (
    problem.problemRating === null || problem.problemRating === undefined
      ? null
      : String(problem.problemRating)
  );
  return difficulty && difficulty !== 'UNRATED' ? `${difficulty} rating` : difficulty;
}

function problemTitleFromSubmissions(submissions: CodeforcesSubmissionItem[]) {
  return submissions.find((submission) => submission.problemName?.trim())?.problemName?.trim() ?? null;
}

export function TrainingQueryPanel({
  multiUserSummaries = [],
  firstAccepted,
  isRefreshing,
  ojName,
  onApplyQuery,
  onFirstAcceptedPageChange,
  onOjNameChange,
  onProblemKeyChange,
  onProblemFirstAcceptedPageChange,
  onProblemSubmissionPageChange,
  onQueryModeChange,
  onRefresh,
  onSubmissionPageChange,
  onSelectedIdentityChange,
  problemFirstAccepted,
  problemFirstAcceptedLimit,
  problemFirstAcceptedPage,
  problemKey,
  problemSubmissionLimit,
  problemSubmissionPage,
  problemSubmissions,
  query,
  queryMode,
  record,
  selectedIdentity,
  firstAcceptedLimit,
  firstAcceptedPage,
  submissionLimit,
  submissionPage,
  studentOptions,
  submissions,
  updatedAt,
}: TrainingQueryPanelProps) {
  const [multipleDraft, setMultipleDraft] = useState(query);
  const [singleDraft, setSingleDraft] = useState(query);
  const [problemDraft, setProblemDraft] = useState(query);
  const [activityView, setActivityView] = useState<ActivityView>('submissions');
  const [problemActivityView, setProblemActivityView] = useState<ActivityView>('submissions');
  const hasMountedAutoQueryRef = useRef(false);
  const lastAutoQueryKeyRef = useRef<string | null>(null);
  const hasSingleQueryResult = Boolean(submissions || firstAccepted);
  const hasProblemQueryResult = Boolean(problemSubmissions || problemFirstAccepted);
  const activeDraft = queryMode === 'single'
    ? singleDraft
    : queryMode === 'problem' ? problemDraft : multipleDraft;
  const acceptedCount = record?.acceptedSummary?.totalAcceptedProblemCount ?? 0;
  const multiUserCount = multiUserSummaries.length;
  const multiUserAcceptedCount = multiUserSummaries.reduce(
    (sum, summary) => sum + summary.totalAcceptedProblemCount,
    0,
  );
  const currentPageSubmissionCount = submissions?.submissions.length ?? 0;
  const submissionCount = submissions?.total ?? currentPageSubmissionCount;
  const currentProblemPageSubmissionCount = problemSubmissions?.submissions.length ?? 0;
  const problemSubmissionCount = problemSubmissions?.total ?? currentProblemPageSubmissionCount;
  const firstAcceptedCount = firstAccepted?.total ?? firstAccepted?.totalAcceptedProblemCount ?? 0;
  const problemAcceptedHandleCount = problemFirstAccepted?.total ?? problemFirstAccepted?.acceptedHandleCount ?? 0;
  const acceptedSubmissions = submissions?.submissions.filter((submission) => submission.accepted).length ?? 0;
  const acceptedProblemSubmissions = problemSubmissions?.submissions.filter((submission) => submission.accepted).length ?? 0;
  const ratingCounts = record?.acceptedSummary?.ratingCounts ?? [];
  const maxRatingCount = Math.max(...ratingCounts.map((item) => item.acceptedProblemCount), 1);
  const ratingBucketCount = ratingCounts.length;
  const highestAcceptedRating = ratingCounts.reduce<string | null>((highest, item) => {
    if (!Number.isFinite(ratingBucketStart(item.problemRating))) {
      return highest;
    }
    if (!highest) {
      return item.problemRating;
    }
    return compareRatingBucket(item.problemRating, highest) > 0 ? item.problemRating : highest;
  }, null);
  const recentSubmissions = useMemo(
    () => [...(submissions?.submissions ?? [])].sort((left, right) => {
      const timeOrder = compareTimeDesc(left.submittedAtUtcPlus8, right.submittedAtUtcPlus8);
      return timeOrder === 0
        ? submissionId(right).localeCompare(submissionId(left))
        : timeOrder;
    }),
    [submissions],
  );
  const recentProblemSubmissions = useMemo(
    () => [...(problemSubmissions?.submissions ?? [])].sort((left, right) => {
      const timeOrder = compareTimeDesc(left.submittedAtUtcPlus8, right.submittedAtUtcPlus8);
      return timeOrder === 0
        ? submissionId(right).localeCompare(submissionId(left))
        : timeOrder;
    }),
    [problemSubmissions],
  );
  const problemDisplayKey = problemSubmissions?.problemKey ?? problemFirstAccepted?.problemKey ?? problemKey.trim();
  const problemDisplayTitle = problemTitleFromSubmissions(recentProblemSubmissions) ?? problemDisplayKey;
  const latestAcceptedProblems = useMemo(
    () => [...(firstAccepted?.problems ?? [])].sort((left, right) => (
      right.firstAcceptedAtUtcPlus8.localeCompare(left.firstAcceptedAtUtcPlus8)
    )),
    [firstAccepted],
  );
  const multiUserRatingBuckets = useMemo(() => {
    const buckets = new Set<string>();
    multiUserSummaries.forEach((summary) => {
      summary.ratingCounts.forEach((item) => buckets.add(item.problemRating));
    });
    return [...buckets].sort(compareRatingBucket);
  }, [multiUserSummaries]);
  const multiUserRows = useMemo(() => multiUserSummaries.map((summary) => ({
    summary,
    counts: new Map(summary.ratingCounts.map((item) => [item.problemRating, item.acceptedProblemCount])),
  })), [multiUserSummaries]);
  const multiSummaryTableWidth = Math.max(
    760,
    multiSummaryPlayerColumnWidth
      + multiSummaryTotalColumnWidth
      + multiUserRatingBuckets.length * multiSummaryRatingColumnWidth,
  );
  const latestProblemAcceptedHandles = useMemo(
    () => [...(problemFirstAccepted?.acceptedHandles ?? [])].sort((left, right) => (
      right.firstAcceptedAtUtcPlus8.localeCompare(left.firstAcceptedAtUtcPlus8)
    )),
    [problemFirstAccepted],
  );
  const acceptedProblemPageCount = latestAcceptedProblems.length;
  const visibleAcceptedProblemPage = firstAccepted?.page ?? firstAcceptedPage;
  const visibleAcceptedProblemLimit = firstAccepted?.limit ?? firstAcceptedLimit;
  const acceptedProblemTotalPages = Math.max(1, firstAccepted?.totalPages ?? 1);
  const visibleProblemAcceptedHandlePage = problemFirstAccepted?.page ?? problemFirstAcceptedPage;
  const visibleProblemAcceptedHandleLimit = problemFirstAccepted?.limit ?? problemFirstAcceptedLimit;
  const problemAcceptedHandleTotalPages = Math.max(1, problemFirstAccepted?.totalPages ?? 1);
  const visibleSubmissionPage = submissions?.page ?? submissionPage;
  const visibleSubmissionLimit = submissions?.limit ?? submissionLimit;
  const totalSubmissionPages = Math.max(1, submissions?.totalPages ?? 1);
  const visibleProblemSubmissionPage = problemSubmissions?.page ?? problemSubmissionPage;
  const visibleProblemSubmissionLimit = problemSubmissions?.limit ?? problemSubmissionLimit;
  const totalProblemSubmissionPages = Math.max(1, problemSubmissions?.totalPages ?? 1);
  const canGoPreviousSubmissionPage = Boolean(submissions) && visibleSubmissionPage > 1 && !isRefreshing;
  const canGoNextSubmissionPage = Boolean(submissions?.hasMore) && !isRefreshing;
  const canGoPreviousProblemSubmissionPage =
    Boolean(problemSubmissions) && visibleProblemSubmissionPage > 1 && !isRefreshing;
  const canGoNextProblemSubmissionPage = Boolean(problemSubmissions?.hasMore) && !isRefreshing;
  const normalizedDraft = useMemo(() => normalizeQueryRange(activeDraft), [activeDraft]);
  const queryRangeError = useMemo(() => getQueryRangeError(normalizedDraft), [normalizedDraft]);
  const canAutoApplyQuery =
    !isRefreshing
    && !queryRangeError
    && (queryMode !== 'single' || Boolean(selectedIdentity))
    && (queryMode !== 'problem' || Boolean(problemKey.trim()));
  const autoQueryKey = useMemo(() => JSON.stringify({
    mode: queryMode,
    ojName,
    problemKey: problemKey.trim(),
    query: normalizedDraft,
    selectedIdentity,
  }), [normalizedDraft, ojName, problemKey, queryMode, selectedIdentity]);

  useEffect(() => {
    setMultipleDraft(query);
  }, [query]);

  useEffect(() => {
    if (!hasMountedAutoQueryRef.current) {
      hasMountedAutoQueryRef.current = true;
      if (canAutoApplyQuery) {
        lastAutoQueryKeyRef.current = autoQueryKey;
      }
      return;
    }

    if (!canAutoApplyQuery || lastAutoQueryKeyRef.current === autoQueryKey) {
      return;
    }

    lastAutoQueryKeyRef.current = autoQueryKey;
    void onApplyQuery(normalizedDraft, queryMode);
  }, [autoQueryKey, canAutoApplyQuery, normalizedDraft, onApplyQuery, queryMode]);

  function setActiveDraft(nextDraft: TrainingQueryRange) {
    if (queryMode === 'single') {
      setSingleDraft(nextDraft);
    } else if (queryMode === 'problem') {
      setProblemDraft(nextDraft);
    } else {
      setMultipleDraft(nextDraft);
    }
  }

  return (
    <section className="training-query" aria-labelledby="training-query-title">
      <div className="query-header">
        <div className="query-title-row">
          <h1 id="training-query-title">训练数据查询</h1>
          <div className="query-mode-tabs" role="tablist" aria-label="训练数据查询方式">
            <button
              aria-selected={queryMode === 'multiple'}
              className={queryMode === 'multiple' ? 'is-active' : ''}
              onClick={() => onQueryModeChange('multiple')}
              role="tab"
              type="button"
            >
              多人统计
            </button>
            <button
              aria-selected={queryMode === 'single'}
              className={queryMode === 'single' ? 'is-active' : ''}
              onClick={() => onQueryModeChange('single')}
              role="tab"
              type="button"
            >
              单人查询
            </button>
            <button
              aria-selected={queryMode === 'problem'}
              className={queryMode === 'problem' ? 'is-active' : ''}
              onClick={() => onQueryModeChange('problem')}
              role="tab"
              type="button"
            >
              题目查询
            </button>
          </div>
        </div>
        <div className="refresh-meta">
          <span>数据更新时间：{updatedAt}</span>
          <button
            className="icon-button"
            disabled={isRefreshing}
            onClick={() => {
              void onRefresh(queryMode);
            }}
            type="button"
            aria-label="刷新训练数据"
          >
            <RefreshCw className={isRefreshing ? 'spin' : ''} size={16} />
          </button>
        </div>
      </div>

      <div className={`query-form${queryMode === 'multiple' ? ' multi-query-form' : ''}`}>
        <label className="query-field query-oj-field compact">
          <span className="query-field-label">OJ</span>
          <select
            aria-label="选择 OJ"
            disabled={isRefreshing}
            value={ojName}
            onChange={(event) => onOjNameChange(event.target.value as OjName)}
          >
            <option value={OJ_NAMES.CODEFORCES}>{OJ_LABELS[OJ_NAMES.CODEFORCES]}</option>
            <option value={OJ_NAMES.ATCODER}>{OJ_LABELS[OJ_NAMES.ATCODER]}</option>
          </select>
        </label>
        {queryMode === 'single' ? (
          <label className="query-field wide">
            <span className="query-field-label">队员</span>
            <select
              disabled={studentOptions.length === 0 || isRefreshing}
              value={selectedIdentity ?? ''}
              onChange={(event) => onSelectedIdentityChange(event.target.value)}
            >
              <option disabled value="">
                {studentOptions.length === 0 ? '等待训练数据' : '请选择队员'}
              </option>
              {studentOptions.map((studentIdentity) => (
                <option key={studentIdentity} value={studentIdentity}>
                  {studentIdentity}
                </option>
              ))}
            </select>
          </label>
        ) : queryMode === 'problem' ? (
          <label className="query-field wide">
            <span className="query-field-label">题目编号</span>
            <input
              disabled={isRefreshing}
              placeholder={ojName === OJ_NAMES.ATCODER ? '例如 abc443_c' : '例如 2242:C'}
              value={problemKey}
              onChange={(event) => onProblemKeyChange(event.target.value)}
            />
          </label>
        ) : (
          <div className="query-field wide query-mode-field">
            <span className="query-field-label">队员统计</span>
            <strong>{multiUserCount} 人 / {multiUserAcceptedCount} 题</strong>
          </div>
        )}
        <label className="query-field">
          <span className="query-field-label">通过起始日期</span>
          <input
            aria-describedby={queryRangeError ? 'training-query-range-error' : undefined}
            aria-invalid={queryRangeError ? true : undefined}
            type="date"
            value={activeDraft.acceptedFromDateUtcPlus8}
            onChange={(event) => setActiveDraft({ ...activeDraft, acceptedFromDateUtcPlus8: event.target.value })}
          />
        </label>
        <label className="query-field">
          <span className="query-field-label">通过结束日期</span>
          <input
            aria-describedby={queryRangeError ? 'training-query-range-error' : undefined}
            aria-invalid={queryRangeError ? true : undefined}
            type="date"
            value={activeDraft.acceptedToDateUtcPlus8}
            onChange={(event) => setActiveDraft({ ...activeDraft, acceptedToDateUtcPlus8: event.target.value })}
          />
        </label>
        <label className="query-field compact">
          <span className="query-field-label">最低 rating</span>
          <input
            aria-describedby={queryRangeError ? 'training-query-range-error' : undefined}
            aria-invalid={queryRangeError ? true : undefined}
            inputMode="numeric"
            min="0"
            placeholder="不限"
            type="number"
            value={activeDraft.minProblemRating}
            onChange={(event) => setActiveDraft({ ...activeDraft, minProblemRating: event.target.value })}
          />
        </label>
        <label className="query-field compact">
          <span className="query-field-label">最高 rating</span>
          <input
            aria-describedby={queryRangeError ? 'training-query-range-error' : undefined}
            aria-invalid={queryRangeError ? true : undefined}
            inputMode="numeric"
            min="0"
            placeholder="不限"
            type="number"
            value={activeDraft.maxProblemRating}
            onChange={(event) => setActiveDraft({ ...activeDraft, maxProblemRating: event.target.value })}
          />
        </label>
      </div>

      {queryRangeError ? (
        <div className="query-summary" role="status">
          <small className="query-error" id="training-query-range-error" role="alert">
            {queryRangeError}
          </small>
        </div>
      ) : null}

      {queryMode === 'multiple' ? (
        <article className="multi-summary-panel">
          <header>
            <div>
              <h2>全部队员做题量统计</h2>
              <span>{multiUserCount} 人 · 合计 {multiUserAcceptedCount} 题 · 按通过题目数降序</span>
            </div>
          </header>
          <div className="auto-summary-table-scroll">
            <table
              className="auto-summary-table"
              style={{ width: multiSummaryTableWidth }}
              aria-label="全部队员做题量统计"
            >
              <colgroup>
                <col style={{ width: multiSummaryPlayerColumnWidth }} />
                <col style={{ width: multiSummaryTotalColumnWidth }} />
                {multiUserRatingBuckets.map((bucket) => (
                  <col key={bucket} style={{ width: multiSummaryRatingColumnWidth }} />
                ))}
              </colgroup>
              <thead>
                <tr>
                  <th className="auto-summary-player-col" scope="col">队员</th>
                  <th className="auto-summary-total-col" scope="col">总计</th>
                  {multiUserRatingBuckets.map((bucket) => (
                    <th
                      className={`auto-summary-rating-col ${ratingToneClass(bucket)}`}
                      key={bucket}
                      scope="col"
                    >
                      {ratingBucketLabel(bucket)}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {multiUserRows.map(({ summary, counts }) => (
                  <tr key={summary.studentIdentity}>
                    <th className="auto-summary-player-cell" data-label="队员" scope="row">
                      <span className="auto-summary-player">
                        <strong>{summary.studentIdentity}</strong>
                        <small>{summary.authorHandle}</small>
                      </span>
                    </th>
                    <td className="auto-summary-total-cell" data-label="总计">
                      <strong>{summary.totalAcceptedProblemCount}</strong>
                    </td>
                    {multiUserRatingBuckets.map((bucket) => {
                      const count = counts.get(bucket);
                      return (
                        <td
                          className={`auto-summary-rating-cell ${ratingToneClass(bucket)}`}
                          data-label={ratingBucketLabel(bucket)}
                          key={bucket}
                        >
                          {count === undefined ? (
                            <span className="auto-rating-empty">-</span>
                          ) : (
                            <span className="auto-rating-count">{count}</span>
                          )}
                        </td>
                      );
                    })}
                  </tr>
                ))}
                {multiUserSummaries.length === 0 ? (
                  <tr>
                    <td className="submission-empty" colSpan={2 + multiUserRatingBuckets.length}>
                      暂无队员通过统计。
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </article>
      ) : queryMode === 'problem' ? (
        hasProblemQueryResult ? (
          <>
            <div className="training-stat-grid problem-stat-grid" aria-label="题目维度统计">
              <article className="training-stat-card identity-card">
                <span>题目</span>
                <strong title={problemDisplayTitle}>{problemDisplayTitle}</strong>
                <small>{OJ_LABELS[ojName]}</small>
              </article>
              <article className="training-stat-card primary">
                <span>提交总数</span>
                <strong>{problemSubmissionCount}</strong>
                <small>本页 {currentProblemPageSubmissionCount} 条 / 通过 {acceptedProblemSubmissions} 条</small>
              </article>
              <article className="training-stat-card">
                <span>首 AC 人数</span>
                <strong>{problemAcceptedHandleCount}</strong>
                <small>按筛选条件统计</small>
              </article>
              <article className="training-stat-card">
                <span>提交分页</span>
                <strong>{visibleProblemSubmissionPage}/{totalProblemSubmissionPages}</strong>
                <small>每页 {visibleProblemSubmissionLimit} 条</small>
              </article>
            </div>

            <article className="recent-submission-panel problem-query-panel">
              <header>
                <div className="activity-heading">
                  <div className="activity-switch" role="tablist" aria-label="题目查询记录视图">
                    <button
                      aria-selected={problemActivityView === 'submissions'}
                      className={problemActivityView === 'submissions' ? 'is-active' : ''}
                      onClick={() => setProblemActivityView('submissions')}
                      role="tab"
                      type="button"
                    >
                      提交明细
                    </button>
                    <button
                      aria-selected={problemActivityView === 'accepted'}
                      className={problemActivityView === 'accepted' ? 'is-active' : ''}
                      onClick={() => setProblemActivityView('accepted')}
                      role="tab"
                      type="button"
                    >
                      首 AC handle
                    </button>
                  </div>
                </div>
                <span>
                  {problemActivityView === 'submissions'
                    ? `${problemSubmissionCount} 条 · 第 ${visibleProblemSubmissionPage}/${totalProblemSubmissionPages} 页`
                    : `${problemAcceptedHandleCount} 人 · 第 ${visibleProblemAcceptedHandlePage}/${problemAcceptedHandleTotalPages} 页`}
                </span>
              </header>
              <div className="submission-table-scroll">
                {problemActivityView === 'submissions' ? (
                  <table className="submission-table problem-submission-table" aria-label="题目提交明细">
                    <thead>
                      <tr>
                        <th scope="col">队员</th>
                        <th scope="col">判题</th>
                        <th scope="col">提交时间</th>
                      </tr>
                    </thead>
                    <tbody>
                      {recentProblemSubmissions.map((submission) => (
                        <tr key={submissionId(submission)}>
                          <td data-label="队员">
                            <div className="submission-problem">
                              <strong>{submission.studentIdentity}</strong>
                              <span>
                                {[
                                  submission.handle ?? submission.authorHandle,
                                  submission.language ?? submission.programmingLanguage,
                                  submissionDifficulty(submission),
                                ].filter(Boolean).join(' / ') || '无提交元数据'}
                              </span>
                            </div>
                          </td>
                          <td data-label="判题">
                            <span className={submission.accepted ? 'submission-verdict accepted' : 'submission-verdict'}>
                              {formatSubmissionVerdict(submission.verdict, submission.accepted)}
                            </span>
                          </td>
                          <td className="submission-time" data-label="提交时间">
                            {submission.submittedAtUtcPlus8 ?? '-'}
                          </td>
                        </tr>
                      ))}
                      {recentProblemSubmissions.length === 0 ? (
                        <tr>
                          <td className="submission-empty" colSpan={3}>
                            暂无题目提交明细。
                          </td>
                        </tr>
                      ) : null}
                    </tbody>
                  </table>
                ) : (
                  <table className="submission-table accepted-table" aria-label="题目首 AC handle">
                    <thead>
                      <tr>
                        <th scope="col">队员</th>
                        <th scope="col">handle</th>
                        <th scope="col">首次通过时间</th>
                      </tr>
                    </thead>
                    <tbody>
                      {latestProblemAcceptedHandles.map((item) => (
                        <tr key={`${item.studentIdentity}-${item.handle}`}>
                          <td data-label="队员">
                            <strong>{item.studentIdentity}</strong>
                          </td>
                          <td data-label="handle">{item.handle}</td>
                          <td className="submission-time" data-label="首次通过时间">
                            {item.firstAcceptedAtUtcPlus8}
                          </td>
                        </tr>
                      ))}
                      {latestProblemAcceptedHandles.length === 0 ? (
                        <tr>
                          <td className="submission-empty" colSpan={3}>
                            暂无首 AC handle。
                          </td>
                        </tr>
                      ) : null}
                    </tbody>
                  </table>
                )}
              </div>
              {problemActivityView === 'submissions' ? (
                <div className="submission-pagination" aria-label="题目提交记录分页">
                  <span>
                    本页 {currentProblemPageSubmissionCount} 条，合计 {problemSubmissionCount} 条
                  </span>
                  <label>
                    每页
                    <select
                      aria-label="题目每页提交数"
                      disabled={isRefreshing || !problemSubmissions}
                      value={visibleProblemSubmissionLimit}
                      onChange={(event) => {
                        void onProblemSubmissionPageChange(1, Number(event.target.value));
                      }}
                    >
                      {submissionPageSizes.map((pageSize) => (
                        <option key={pageSize} value={pageSize}>
                          {pageSize}
                        </option>
                      ))}
                    </select>
                  </label>
                  <div className="submission-page-actions">
                    <button
                      className="secondary-button compact"
                      disabled={!canGoPreviousProblemSubmissionPage}
                      onClick={() => {
                        void onProblemSubmissionPageChange(
                          visibleProblemSubmissionPage - 1,
                          visibleProblemSubmissionLimit,
                        );
                      }}
                      type="button"
                    >
                      <ChevronLeft size={14} aria-hidden="true" />
                      上一页
                    </button>
                    <button
                      className="secondary-button compact"
                      disabled={!canGoNextProblemSubmissionPage}
                      onClick={() => {
                        void onProblemSubmissionPageChange(
                          visibleProblemSubmissionPage + 1,
                          visibleProblemSubmissionLimit,
                        );
                      }}
                      type="button"
                    >
                      下一页
                      <ChevronRight size={14} aria-hidden="true" />
                    </button>
                  </div>
                </div>
              ) : null}
              {problemActivityView === 'accepted' ? (
                <div className="submission-pagination" aria-label="题目首 AC handle 分页">
                  <span>
                    本页 {latestProblemAcceptedHandles.length} 人，合计 {problemAcceptedHandleCount} 人
                  </span>
                  <label>
                    每页
                    <select
                      aria-label="每页首 AC 数"
                      disabled={isRefreshing || !problemFirstAccepted}
                      value={visibleProblemAcceptedHandleLimit}
                      onChange={(event) => {
                        void onProblemFirstAcceptedPageChange(1, Number(event.target.value));
                      }}
                    >
                      {submissionPageSizes.map((pageSize) => (
                        <option key={pageSize} value={pageSize}>
                          {pageSize}
                        </option>
                      ))}
                    </select>
                  </label>
                  <div className="submission-page-actions">
                    <button
                      className="secondary-button compact"
                      disabled={isRefreshing || visibleProblemAcceptedHandlePage <= 1}
                      onClick={() => {
                        void onProblemFirstAcceptedPageChange(
                          visibleProblemAcceptedHandlePage - 1,
                          visibleProblemAcceptedHandleLimit,
                        );
                      }}
                      type="button"
                    >
                      <ChevronLeft size={14} aria-hidden="true" />
                      上一页
                    </button>
                    <button
                      className="secondary-button compact"
                      disabled={isRefreshing || visibleProblemAcceptedHandlePage >= problemAcceptedHandleTotalPages}
                      onClick={() => {
                        void onProblemFirstAcceptedPageChange(
                          visibleProblemAcceptedHandlePage + 1,
                          visibleProblemAcceptedHandleLimit,
                        );
                      }}
                      type="button"
                    >
                      下一页
                      <ChevronRight size={14} aria-hidden="true" />
                    </button>
                  </div>
                </div>
              ) : null}
            </article>
          </>
        ) : null
      ) : hasSingleQueryResult ? (
        <>
          <div className="training-stat-grid" aria-label="训练数据统计">
            <article className="training-stat-card identity-card">
              <span>个人信息</span>
              <strong>{selectedIdentity ?? '未选择队员'}</strong>
              <small>{record?.handle ?? `未绑定 ${OJ_LABELS[ojName]}`}</small>
            </article>
            <article className="training-stat-card primary">
              <span>通过题目数</span>
              <strong>{acceptedCount}</strong>
              <small>按筛选条件统计</small>
            </article>
            <article className="training-stat-card">
              <span>提交明细</span>
              <strong>{submissionCount}</strong>
              <small>本页 {currentPageSubmissionCount} 条 / 通过 {acceptedSubmissions} 条</small>
            </article>
            <article className="training-stat-card">
              <span>首次通过明细</span>
              <strong>{firstAcceptedCount}</strong>
              <small>本页 {acceptedProblemPageCount} 题 / 合计 {firstAcceptedCount} 题</small>
            </article>
          </div>

          <div className="training-result-grid">
            <article className="rating-panel">
              <header>
                <div className="rating-panel-title">
                  <h2>难度分布</h2>
                  <div className="rating-summary-row" aria-label="难度分布摘要">
                    <span><strong>{acceptedCount}</strong>通过</span>
                    <span><strong>{ratingBucketCount}</strong>档</span>
                    <span><strong>{highestAcceptedRating ? ratingBucketLabel(highestAcceptedRating) : '-'}</strong>最高</span>
                  </div>
                </div>
              </header>
              <div className="rating-bars">
                {ratingCounts.map((item) => (
                  <div className="rating-bar-row" key={item.problemRating}>
                    <span>{ratingBucketLabel(item.problemRating)}</span>
                    <div>
                      <i
                        style={{
                          background: ratingBarColor(item.problemRating),
                          width: `${Math.max(6, (item.acceptedProblemCount / maxRatingCount) * 100)}%`,
                        }}
                      />
                    </div>
                    <strong>{item.acceptedProblemCount}</strong>
                  </div>
                ))}
                {ratingCounts.length === 0 ? <p>暂无通过汇总。</p> : null}
              </div>
            </article>

            <article className="recent-submission-panel">
              <header>
                <div className="activity-heading">
                  <div className="activity-switch" role="tablist" aria-label="训练记录视图">
                    <button
                      aria-selected={activityView === 'submissions'}
                      className={activityView === 'submissions' ? 'is-active' : ''}
                      onClick={() => setActivityView('submissions')}
                      role="tab"
                      type="button"
                    >
                      最近提交
                    </button>
                    <button
                      aria-selected={activityView === 'accepted'}
                      className={activityView === 'accepted' ? 'is-active' : ''}
                      onClick={() => setActivityView('accepted')}
                      role="tab"
                      type="button"
                    >
                      最近通过
                    </button>
                  </div>
                </div>
                <span>
                  {activityView === 'submissions'
                    ? `${submissionCount} 条 · 第 ${visibleSubmissionPage}/${totalSubmissionPages} 页`
                    : `${firstAcceptedCount} 题 · 第 ${visibleAcceptedProblemPage}/${acceptedProblemTotalPages} 页`}
                </span>
              </header>
          <div className="submission-table-scroll">
            {activityView === 'submissions' ? (
              <table className="submission-table" aria-label="最近提交明细">
                <thead>
                  <tr>
                    <th scope="col">题目</th>
                    <th scope="col">判题</th>
                    <th scope="col">提交时间</th>
                  </tr>
                </thead>
                <tbody>
                  {recentSubmissions.map((submission) => (
                    <tr key={submissionId(submission)}>
                      <td data-label="题目">
                        <div className="submission-problem">
                          <strong>{submission.problemName ?? '题目名称缺失'}</strong>
                          <span>
                            {[
                              submissionDifficulty(submission),
                              submission.language ?? submission.programmingLanguage,
                              submission.problemName ? null : submission.problemKey,
                            ].filter(Boolean).join(' / ') || '无难度信息'}
                          </span>
                        </div>
                      </td>
                      <td data-label="判题">
                        <span className={submission.accepted ? 'submission-verdict accepted' : 'submission-verdict'}>
                          {formatSubmissionVerdict(submission.verdict, submission.accepted)}
                        </span>
                      </td>
                      <td className="submission-time" data-label="提交时间">
                        {submission.submittedAtUtcPlus8 ?? '-'}
                      </td>
                    </tr>
                  ))}
                  {recentSubmissions.length === 0 ? (
                    <tr>
                      <td className="submission-empty" colSpan={3}>
                        暂无提交明细。
                      </td>
                    </tr>
                  ) : null}
                </tbody>
              </table>
            ) : (
              <table className="submission-table accepted-table" aria-label="最近通过明细">
                <thead>
                  <tr>
                    <th scope="col">题目</th>
                    <th scope="col">通过时间</th>
                  </tr>
                </thead>
                <tbody>
                  {latestAcceptedProblems.map((problem) => (
                    <tr key={problem.problemKey}>
                      <td data-label="题目">
                        <div className="submission-problem">
                          <strong>{problem.problemName}</strong>
                          <span>
                            {[
                              firstAcceptedDifficulty(problem),
                              problem.firstAcceptedLanguage,
                            ].filter(Boolean).join(' / ') || '无难度信息'}
                          </span>
                        </div>
                      </td>
                      <td className="submission-time" data-label="通过时间">
                        {problem.firstAcceptedAtUtcPlus8}
                      </td>
                    </tr>
                  ))}
                  {latestAcceptedProblems.length === 0 ? (
                    <tr>
                      <td className="submission-empty" colSpan={2}>
                        暂无最新通过。
                      </td>
                    </tr>
                  ) : null}
                </tbody>
              </table>
            )}
          </div>
          {activityView === 'submissions' ? (
            <div className="submission-pagination" aria-label="提交记录分页">
              <span>
                本页 {currentPageSubmissionCount} 条，合计 {submissionCount} 条
              </span>
              <label>
                每页
                <select
                  aria-label="每页提交数"
                  disabled={isRefreshing || !submissions}
                  value={visibleSubmissionLimit}
                  onChange={(event) => {
                    void onSubmissionPageChange(1, Number(event.target.value));
                  }}
                >
                  {submissionPageSizes.map((pageSize) => (
                    <option key={pageSize} value={pageSize}>
                      {pageSize}
                    </option>
                  ))}
                </select>
              </label>
              <div className="submission-page-actions">
                <button
                  className="secondary-button compact"
                  disabled={!canGoPreviousSubmissionPage}
                  onClick={() => {
                    void onSubmissionPageChange(visibleSubmissionPage - 1, visibleSubmissionLimit);
                  }}
                  type="button"
                >
                  <ChevronLeft size={14} aria-hidden="true" />
                  上一页
                </button>
                <button
                  className="secondary-button compact"
                  disabled={!canGoNextSubmissionPage}
                  onClick={() => {
                    void onSubmissionPageChange(visibleSubmissionPage + 1, visibleSubmissionLimit);
                  }}
                  type="button"
                >
                  下一页
                  <ChevronRight size={14} aria-hidden="true" />
                </button>
              </div>
            </div>
              ) : null}
              {activityView === 'accepted' ? (
                <div className="submission-pagination" aria-label="通过题目分页">
                  <span>
                    本页 {acceptedProblemPageCount} 题，合计 {firstAcceptedCount} 题
                  </span>
                  <label>
                    每页
                    <select
                      aria-label="每页通过题目数"
                      disabled={isRefreshing || !firstAccepted}
                      value={visibleAcceptedProblemLimit}
                      onChange={(event) => {
                        void onFirstAcceptedPageChange(1, Number(event.target.value));
                      }}
                    >
                      {submissionPageSizes.map((pageSize) => (
                        <option key={pageSize} value={pageSize}>
                          {pageSize}
                        </option>
                      ))}
                    </select>
                  </label>
                  <div className="submission-page-actions">
                    <button
                      className="secondary-button compact"
                      disabled={isRefreshing || visibleAcceptedProblemPage <= 1}
                      onClick={() => {
                        void onFirstAcceptedPageChange(
                          visibleAcceptedProblemPage - 1,
                          visibleAcceptedProblemLimit,
                        );
                      }}
                      type="button"
                    >
                      <ChevronLeft size={14} aria-hidden="true" />
                      上一页
                    </button>
                    <button
                      className="secondary-button compact"
                      disabled={isRefreshing || visibleAcceptedProblemPage >= acceptedProblemTotalPages}
                      onClick={() => {
                        void onFirstAcceptedPageChange(
                          visibleAcceptedProblemPage + 1,
                          visibleAcceptedProblemLimit,
                        );
                      }}
                      type="button"
                    >
                      下一页
                      <ChevronRight size={14} aria-hidden="true" />
                    </button>
                  </div>
                </div>
              ) : null}
            </article>
          </div>
        </>
      ) : null}
    </section>
  );
}
