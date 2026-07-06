import { ChevronLeft, ChevronRight, RefreshCw, RotateCcw, Search } from 'lucide-react';
import { FormEvent, useEffect, useMemo, useState } from 'react';
import type {
  CodeforcesAcceptedSummary,
  CodeforcesFirstAcceptedReport,
  CodeforcesStudentSubmissionReport,
  StudentIdentity,
  StudentTrainingRecord,
  TrainingQueryMode,
  TrainingQueryRange,
} from '../types';

interface TrainingQueryPanelProps {
  autoCollectSummaries: CodeforcesAcceptedSummary[];
  firstAccepted: CodeforcesFirstAcceptedReport | null;
  isRefreshing: boolean;
  onApplyQuery: (query: TrainingQueryRange, mode: TrainingQueryMode) => Promise<void>;
  onQueryModeChange: (mode: TrainingQueryMode) => void;
  onRefresh: (mode: TrainingQueryMode) => Promise<void>;
  onSubmissionPageChange: (page: number, limit: number) => Promise<void>;
  onSelectedIdentityChange: (studentIdentity: StudentIdentity) => Promise<void>;
  query: TrainingQueryRange;
  queryMode: TrainingQueryMode;
  record: StudentTrainingRecord | null;
  selectedIdentity: StudentIdentity | null;
  submissionLimit: number;
  submissionPage: number;
  studentOptions: StudentIdentity[];
  submissions: CodeforcesStudentSubmissionReport | null;
  updatedAt: string;
}

const submissionPageSizes = [50, 100, 200];
const autoSummaryPlayerColumnWidth = 204;
const autoSummaryTotalColumnWidth = 72;
const autoSummaryRatingColumnWidth = 56;

type ActivityView = 'submissions' | 'accepted';
const emptyTrainingQuery: TrainingQueryRange = {
  acceptedFromDateUtcPlus8: '',
  acceptedToDateUtcPlus8: '',
  minProblemRating: '',
  maxProblemRating: '',
};

function normalizeQueryRange(query: TrainingQueryRange): TrainingQueryRange {
  return {
    acceptedFromDateUtcPlus8: query.acceptedFromDateUtcPlus8,
    acceptedToDateUtcPlus8: query.acceptedToDateUtcPlus8,
    minProblemRating: query.minProblemRating.trim(),
    maxProblemRating: query.maxProblemRating.trim(),
  };
}

function formatQuerySummary(query: TrainingQueryRange) {
  const dateRange = query.acceptedFromDateUtcPlus8 || query.acceptedToDateUtcPlus8
    ? `${query.acceptedFromDateUtcPlus8 || '不限'} ~ ${query.acceptedToDateUtcPlus8 || '不限'}`
    : '全量日期';
  const ratingRange = query.minProblemRating || query.maxProblemRating
    ? `${query.minProblemRating || '不限'} ~ ${query.maxProblemRating || '不限'}`
    : '全 rating';
  return `${dateRange} / ${ratingRange}`;
}

function isSameQueryRange(left: TrainingQueryRange, right: TrainingQueryRange) {
  return left.acceptedFromDateUtcPlus8 === right.acceptedFromDateUtcPlus8
    && left.acceptedToDateUtcPlus8 === right.acceptedToDateUtcPlus8
    && left.minProblemRating === right.minProblemRating
    && left.maxProblemRating === right.maxProblemRating;
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
  const rating = Number(problemRating);
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
  const rating = Number(problemRating);
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
  const leftRating = Number(left);
  const rightRating = Number(right);
  if (Number.isFinite(leftRating) && Number.isFinite(rightRating)) {
    return leftRating - rightRating;
  }
  return left.localeCompare(right);
}

export function TrainingQueryPanel({
  autoCollectSummaries = [],
  firstAccepted,
  isRefreshing,
  onApplyQuery,
  onQueryModeChange,
  onRefresh,
  onSubmissionPageChange,
  onSelectedIdentityChange,
  query,
  queryMode,
  record,
  selectedIdentity,
  submissionLimit,
  submissionPage,
  studentOptions,
  submissions,
  updatedAt,
}: TrainingQueryPanelProps) {
  const [multipleDraft, setMultipleDraft] = useState(query);
  const [singleDraft, setSingleDraft] = useState(emptyTrainingQuery);
  const [activityView, setActivityView] = useState<ActivityView>('submissions');
  const hasSingleQueryResult = Boolean(submissions || firstAccepted);
  const activeDraft = queryMode === 'single' ? singleDraft : multipleDraft;
  const appliedQuery = queryMode === 'single' && !hasSingleQueryResult ? emptyTrainingQuery : query;
  const acceptedCount = record?.acceptedSummary?.totalAcceptedProblemCount ?? 0;
  const autoCollectUserCount = autoCollectSummaries.length;
  const autoCollectAcceptedCount = autoCollectSummaries.reduce(
    (sum, summary) => sum + summary.totalAcceptedProblemCount,
    0,
  );
  const currentPageSubmissionCount = submissions?.submissions.length ?? 0;
  const submissionCount = submissions?.total ?? currentPageSubmissionCount;
  const firstAcceptedCount = firstAccepted?.totalAcceptedProblemCount ?? 0;
  const acceptedSubmissions = submissions?.submissions.filter((submission) => submission.accepted).length ?? 0;
  const ratingCounts = record?.acceptedSummary?.ratingCounts ?? [];
  const maxRatingCount = Math.max(...ratingCounts.map((item) => item.acceptedProblemCount), 1);
  const ratingBucketCount = ratingCounts.length;
  const highestAcceptedRating = ratingCounts.reduce((highest, item) => {
    const rating = Number(item.problemRating);
    return Number.isFinite(rating) ? Math.max(highest, rating) : highest;
  }, 0);
  const recentSubmissions = useMemo(
    () => [...(submissions?.submissions ?? [])].sort((left, right) => {
      const timeOrder = compareTimeDesc(left.submittedAtUtcPlus8, right.submittedAtUtcPlus8);
      return timeOrder === 0
        ? right.codeforcesSubmissionId - left.codeforcesSubmissionId
        : timeOrder;
    }),
    [submissions],
  );
  const latestAcceptedProblems = useMemo(
    () => [...(firstAccepted?.problems ?? [])].sort((left, right) => (
      right.firstAcceptedAtUtcPlus8.localeCompare(left.firstAcceptedAtUtcPlus8)
    )),
    [firstAccepted],
  );
  const autoCollectRatingBuckets = useMemo(() => {
    const buckets = new Set<string>();
    autoCollectSummaries.forEach((summary) => {
      summary.ratingCounts.forEach((item) => buckets.add(item.problemRating));
    });
    return [...buckets].sort(compareRatingBucket);
  }, [autoCollectSummaries]);
  const autoCollectRows = useMemo(() => autoCollectSummaries.map((summary) => ({
    summary,
    counts: new Map(summary.ratingCounts.map((item) => [item.problemRating, item.acceptedProblemCount])),
  })), [autoCollectSummaries]);
  const autoSummaryTableWidth = Math.max(
    760,
    autoSummaryPlayerColumnWidth
      + autoSummaryTotalColumnWidth
      + autoCollectRatingBuckets.length * autoSummaryRatingColumnWidth,
  );
  const firstAcceptedPageCount = latestAcceptedProblems.length;
  const visibleSubmissionPage = submissions?.page ?? submissionPage;
  const visibleSubmissionLimit = submissions?.limit ?? submissionLimit;
  const totalSubmissionPages = Math.max(1, submissions?.totalPages ?? 1);
  const canGoPreviousSubmissionPage = Boolean(submissions) && visibleSubmissionPage > 1 && !isRefreshing;
  const canGoNextSubmissionPage = Boolean(submissions?.hasMore) && !isRefreshing;
  const normalizedDraft = useMemo(() => normalizeQueryRange(activeDraft), [activeDraft]);
  const appliedQuerySummary = useMemo(() => formatQuerySummary(appliedQuery), [appliedQuery]);
  const draftQuerySummary = useMemo(() => formatQuerySummary(normalizedDraft), [normalizedDraft]);
  const hasPendingQuery = !isSameQueryRange(normalizedDraft, appliedQuery);
  const canResetQuery = hasPendingQuery || !isSameQueryRange(appliedQuery, emptyTrainingQuery);
  const queryRangeError = useMemo(() => getQueryRangeError(normalizedDraft), [normalizedDraft]);

  useEffect(() => {
    setMultipleDraft(query);
  }, [query]);

  function setActiveDraft(nextDraft: TrainingQueryRange) {
    if (queryMode === 'single') {
      setSingleDraft(nextDraft);
    } else {
      setMultipleDraft(nextDraft);
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (queryRangeError) {
      return;
    }
    await onApplyQuery(normalizedDraft, queryMode);
  }

  async function handleReset() {
    setActiveDraft(emptyTrainingQuery);
    await onApplyQuery(emptyTrainingQuery, queryMode);
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

      <form className={`query-form${queryMode === 'multiple' ? ' multi-query-form' : ''}`} onSubmit={handleSubmit}>
        {queryMode === 'single' ? (
          <label className="query-field wide">
            选手
            <select
              disabled={studentOptions.length === 0 || isRefreshing}
              value={selectedIdentity ?? ''}
              onChange={(event) => onSelectedIdentityChange(event.target.value)}
            >
              <option disabled value="">
                {studentOptions.length === 0 ? '等待训练数据' : '请选择选手'}
              </option>
              {studentOptions.map((studentIdentity) => (
                <option key={studentIdentity} value={studentIdentity}>
                  {studentIdentity}
                </option>
              ))}
            </select>
          </label>
        ) : (
          <div className="query-field wide query-mode-field">
            <span>自动更新选手</span>
            <strong>{autoCollectUserCount} 人 / {autoCollectAcceptedCount} 题</strong>
          </div>
        )}
        <label className="query-field">
          通过起始日期
          <input
            aria-describedby={queryRangeError ? 'training-query-range-error' : undefined}
            aria-invalid={queryRangeError ? true : undefined}
            type="date"
            value={activeDraft.acceptedFromDateUtcPlus8}
            onChange={(event) => setActiveDraft({ ...activeDraft, acceptedFromDateUtcPlus8: event.target.value })}
          />
        </label>
        <label className="query-field">
          通过结束日期
          <input
            aria-describedby={queryRangeError ? 'training-query-range-error' : undefined}
            aria-invalid={queryRangeError ? true : undefined}
            type="date"
            value={activeDraft.acceptedToDateUtcPlus8}
            onChange={(event) => setActiveDraft({ ...activeDraft, acceptedToDateUtcPlus8: event.target.value })}
          />
        </label>
        <label className="query-field compact">
          最低 rating
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
          最高 rating
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
        <button
          className="primary-button"
          disabled={isRefreshing || (queryMode === 'single' && !selectedIdentity) || Boolean(queryRangeError)}
          type="submit"
        >
          <Search size={16} aria-hidden="true" />
          查询
        </button>
        <button
          className="secondary-button"
          disabled={isRefreshing || (queryMode === 'single' && !selectedIdentity) || !canResetQuery}
          onClick={handleReset}
          type="button"
        >
          <RotateCcw size={15} aria-hidden="true" />
          重置
        </button>
      </form>

      <div className="query-summary" role="status">
        <span>当前范围：{appliedQuerySummary}</span>
        {queryRangeError ? (
          <small className="query-error" id="training-query-range-error" role="alert">
            {queryRangeError}
          </small>
        ) : null}
        {!queryRangeError && hasPendingQuery ? <small>已编辑为 {draftQuerySummary}，点击查询后生效。</small> : null}
      </div>

      {queryMode === 'multiple' ? (
        <article className="multi-summary-panel">
          <header>
            <div>
              <h2>全部选手做题量统计</h2>
              <span>{autoCollectUserCount} 人 · 合计 {autoCollectAcceptedCount} 题 · 按通过题目数降序</span>
            </div>
          </header>
          <div className="auto-summary-table-scroll">
            <table
              className="auto-summary-table"
              style={{ width: autoSummaryTableWidth }}
              aria-label="全部选手做题量统计"
            >
              <colgroup>
                <col style={{ width: autoSummaryPlayerColumnWidth }} />
                <col style={{ width: autoSummaryTotalColumnWidth }} />
                {autoCollectRatingBuckets.map((bucket) => (
                  <col key={bucket} style={{ width: autoSummaryRatingColumnWidth }} />
                ))}
              </colgroup>
              <thead>
                <tr>
                  <th className="auto-summary-player-col" scope="col">选手</th>
                  <th className="auto-summary-total-col" scope="col">总计</th>
                  {autoCollectRatingBuckets.map((bucket) => (
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
                {autoCollectRows.map(({ summary, counts }) => (
                  <tr key={summary.studentIdentity}>
                    <th className="auto-summary-player-cell" data-label="选手" scope="row">
                      <span className="auto-summary-player">
                        <strong>{summary.studentIdentity}</strong>
                        <small>{summary.authorHandle}</small>
                      </span>
                    </th>
                    <td className="auto-summary-total-cell" data-label="总计">
                      <strong>{summary.totalAcceptedProblemCount}</strong>
                    </td>
                    {autoCollectRatingBuckets.map((bucket) => {
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
                {autoCollectSummaries.length === 0 ? (
                  <tr>
                    <td className="submission-empty" colSpan={2 + autoCollectRatingBuckets.length}>
                      当前范围暂无自动更新选手通过统计。
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </article>
      ) : hasSingleQueryResult ? (
        <>
          <div className="training-stat-grid" aria-label="训练数据统计">
            <article className="training-stat-card identity-card">
              <span>个人信息</span>
              <strong>{selectedIdentity ?? '未选择选手'}</strong>
              <small>{record?.handle ?? '未绑定 Codeforces'}</small>
            </article>
            <article className="training-stat-card primary">
              <span>通过题目数</span>
              <strong>{acceptedCount}</strong>
              <small>按当前范围统计</small>
            </article>
            <article className="training-stat-card">
              <span>提交明细</span>
              <strong>{submissionCount}</strong>
              <small>本页 {currentPageSubmissionCount} 条 / 通过 {acceptedSubmissions} 条</small>
            </article>
            <article className="training-stat-card">
              <span>首次通过明细</span>
              <strong>{firstAcceptedCount}</strong>
              <small>本页 {firstAcceptedPageCount} 题 / 合计 {firstAcceptedCount} 题</small>
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
                    <span><strong>{highestAcceptedRating || '-'}</strong>最高</span>
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
                {ratingCounts.length === 0 ? <p>当前范围暂无通过汇总。</p> : null}
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
                    : `${latestAcceptedProblems.length} 题`}
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
                    <tr key={submission.codeforcesSubmissionId}>
                      <td data-label="题目">
                        <div className="submission-problem">
                          <strong>{submission.problemName ?? '题目名称缺失'}</strong>
                          <span>
                            {[
                              submission.problemRating ? `${submission.problemRating} rating` : null,
                              submission.programmingLanguage,
                              submission.problemName ? null : submission.problemKey,
                            ].filter(Boolean).join(' / ') || '无 rating 信息'}
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
                        当前范围暂无提交明细。
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
                              problem.problemRating ? `${problem.problemRating} rating` : null,
                              problem.firstAcceptedLanguage,
                            ].filter(Boolean).join(' / ') || '无 rating 信息'}
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
                        当前范围暂无最新通过。
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
            </article>
          </div>
        </>
      ) : null}
    </section>
  );
}
