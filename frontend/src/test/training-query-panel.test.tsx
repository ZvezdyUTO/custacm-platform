import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { TrainingQueryPanel } from '../components/TrainingQueryPanel';
import {
  OJ_NAMES,
  type CodeforcesAcceptedSummary,
  type CodeforcesFirstAcceptedReport,
  type CodeforcesProblemFirstAcceptedReport,
  type CodeforcesProblemSubmissionReport,
  type CodeforcesStudentSubmissionReport,
  type OjName,
  type StudentIdentity,
  type StudentTrainingRecord,
  type TrainingQueryMode,
  type TrainingQueryRange,
} from '../types';

const emptyQuery: TrainingQueryRange = {
  acceptedFromDateUtcPlus8: '',
  acceptedToDateUtcPlus8: '',
  minProblemRating: '',
  maxProblemRating: '',
};

const sampleRecord: StudentTrainingRecord = {
  studentIdentity: '230511213黄炳睿',
  role: 'player',
  handle: 'tourist',
  handles: { [OJ_NAMES.CODEFORCES]: 'tourist' },
  handleStatus: 'bound',
  acceptedSummary: {
    studentIdentity: '230511213黄炳睿',
    authorHandle: 'tourist',
    totalAcceptedProblemCount: 2,
    ratingCounts: [
      { problemRating: '1800', acceptedProblemCount: 1 },
      { problemRating: '2100', acceptedProblemCount: 1 },
    ],
  },
  summaryStatus: 'loaded',
  errorMessage: null,
  updatedAt: '2026-07-06T00:00:00',
};

const sampleSubmissions: CodeforcesStudentSubmissionReport = {
  studentIdentity: '230511213黄炳睿',
  authorHandle: 'tourist',
  page: 1,
  limit: 100,
  total: 2,
  totalPages: 1,
  hasMore: false,
  submissions: [
    {
      codeforcesSubmissionId: 1,
      studentIdentity: '230511213黄炳睿',
      authorHandle: 'tourist',
      contestId: 2053,
      submittedAtUtcPlus8: '2023-12-11T22:49:21',
      submittedDateUtcPlus8: '2023-12-11',
      relativeTimeSeconds: null,
      problemKey: '2053:D',
      problemContestId: 2053,
      problemIndex: 'D',
      problemName: 'Remove and Add',
      problemType: 'PROGRAMMING',
      problemPoints: null,
      problemRating: 1800,
      problemTagsJson: null,
      authorParticipantType: 'CONTESTANT',
      programmingLanguage: 'Kotlin 1.9',
      verdict: 'OK',
      accepted: true,
      testset: 'TESTS',
      passedTestCount: 12,
      timeConsumedMillis: 93,
      memoryConsumedBytes: 1024000,
    },
    {
      codeforcesSubmissionId: 2,
      studentIdentity: '230511213黄炳睿',
      authorHandle: 'tourist',
      contestId: 2053,
      submittedAtUtcPlus8: '2023-12-11T22:55:31',
      submittedDateUtcPlus8: '2023-12-11',
      relativeTimeSeconds: null,
      problemKey: '2053:E',
      problemContestId: 2053,
      problemIndex: 'E',
      problemName: 'Maximum Sum Subarrays',
      problemType: 'PROGRAMMING',
      problemPoints: null,
      problemRating: 2100,
      problemTagsJson: null,
      authorParticipantType: 'CONTESTANT',
      programmingLanguage: 'Kotlin 1.9',
      verdict: 'WRONG_ANSWER',
      accepted: false,
      testset: 'TESTS',
      passedTestCount: 4,
      timeConsumedMillis: 124,
      memoryConsumedBytes: 2048000,
    },
  ],
};

const sampleFirstAccepted: CodeforcesFirstAcceptedReport = {
  studentIdentity: '230511213黄炳睿',
  authorHandle: 'tourist',
  totalAcceptedProblemCount: 2,
  problems: [
    {
      problemKey: '1000:A',
      problemContestId: 1000,
      problemIndex: 'A',
      problemName: 'Older Accepted Problem',
      problemType: 'PROGRAMMING',
      problemPoints: null,
      problemRating: 1400,
      problemTagsJson: null,
      firstAcceptedSubmissionId: 10,
      firstAcceptedAtUtcPlus8: '2024-01-02T10:00:00',
      firstAcceptedDateUtcPlus8: '2024-01-02',
      firstAcceptedLanguage: 'C++20',
    },
    {
      problemKey: '1001:B',
      problemContestId: 1001,
      problemIndex: 'B',
      problemName: 'Newest Accepted Problem',
      problemType: 'PROGRAMMING',
      problemPoints: null,
      problemRating: 1800,
      problemTagsJson: null,
      firstAcceptedSubmissionId: 11,
      firstAcceptedAtUtcPlus8: '2024-01-05T12:30:00',
      firstAcceptedDateUtcPlus8: '2024-01-05',
      firstAcceptedLanguage: 'Kotlin 1.9',
    },
  ],
};

const sampleProblemSubmissions: CodeforcesProblemSubmissionReport = {
  problemKey: '2242:C',
  page: 1,
  limit: 100,
  total: 2,
  totalPages: 1,
  hasMore: false,
  submissions: [
    {
      codeforcesSubmissionId: 21,
      studentIdentity: '240212224苏可航',
      authorHandle: 'Apeiron_24',
      contestId: 2242,
      submittedAtUtcPlus8: '2026-06-01T08:30:00',
      submittedDateUtcPlus8: '2026-06-01',
      relativeTimeSeconds: null,
      problemKey: '2242:C',
      problemContestId: 2242,
      problemIndex: 'C',
      problemName: 'Sample Problem',
      problemType: 'PROGRAMMING',
      problemPoints: null,
      problemRating: 1900,
      problemTagsJson: null,
      authorParticipantType: 'CONTESTANT',
      programmingLanguage: 'GNU C++20',
      verdict: 'WRONG_ANSWER',
      accepted: false,
      testset: 'TESTS',
      passedTestCount: 4,
      timeConsumedMillis: 124,
      memoryConsumedBytes: 2048000,
    },
    {
      codeforcesSubmissionId: 22,
      studentIdentity: '240521409李权澎',
      authorHandle: 'orange_lov_gouzi',
      contestId: 2242,
      submittedAtUtcPlus8: '2026-06-02T10:00:00',
      submittedDateUtcPlus8: '2026-06-02',
      relativeTimeSeconds: null,
      problemKey: '2242:C',
      problemContestId: 2242,
      problemIndex: 'C',
      problemName: 'Sample Problem',
      problemType: 'PROGRAMMING',
      problemPoints: null,
      problemRating: 1900,
      problemTagsJson: null,
      authorParticipantType: 'CONTESTANT',
      programmingLanguage: 'GNU C++20',
      verdict: 'OK',
      accepted: true,
      testset: 'TESTS',
      passedTestCount: 12,
      timeConsumedMillis: 93,
      memoryConsumedBytes: 1024000,
    },
  ],
};

const sampleProblemFirstAccepted: CodeforcesProblemFirstAcceptedReport = {
  problemKey: '2242:C',
  acceptedHandleCount: 2,
  acceptedHandles: [
    {
      studentIdentity: '240212224苏可航',
      handle: 'Apeiron_24',
      firstAcceptedAtUtcPlus8: '2026-06-01T09:30:00',
    },
    {
      studentIdentity: '240521409李权澎',
      handle: 'orange_lov_gouzi',
      firstAcceptedAtUtcPlus8: '2026-06-02T10:00:00',
    },
  ],
};

const sampleMultiUserSummaries: CodeforcesAcceptedSummary[] = [
  {
    studentIdentity: '230511214李明',
    authorHandle: 'Benq',
    totalAcceptedProblemCount: 5,
    ratingCounts: [
      { problemRating: '1600', acceptedProblemCount: 3 },
      { problemRating: '1800', acceptedProblemCount: 2 },
    ],
  },
  {
    studentIdentity: '230511213黄炳睿',
    authorHandle: 'tourist',
    totalAcceptedProblemCount: 2,
    ratingCounts: [
      { problemRating: '1800', acceptedProblemCount: 1 },
      { problemRating: '2100', acceptedProblemCount: 1 },
    ],
  },
];

function renderTrainingQueryPanel(
  query: TrainingQueryRange = emptyQuery,
  onApplyQuery = vi.fn(),
  options: {
    firstAccepted?: CodeforcesFirstAcceptedReport | null;
    onOjNameChange?: (ojName: OjName) => void;
    onSubmissionPageChange?: (page: number, limit: number) => Promise<void>;
    onProblemKeyChange?: (problemKey: string) => void;
    onProblemSubmissionPageChange?: (page: number, limit: number) => Promise<void>;
    problemFirstAccepted?: CodeforcesProblemFirstAcceptedReport | null;
    problemKey?: string;
    problemSubmissionLimit?: number;
    problemSubmissionPage?: number;
    problemSubmissions?: CodeforcesProblemSubmissionReport | null;
    record?: StudentTrainingRecord;
    submissionLimit?: number;
    submissionPage?: number;
    submissions?: CodeforcesStudentSubmissionReport | null;
    multiUserSummaries?: CodeforcesAcceptedSummary[];
    queryMode?: TrainingQueryMode;
    onSelectedIdentityChange?: (studentIdentity: StudentIdentity) => Promise<void> | void;
    selectedIdentity?: string | null;
    studentOptions?: string[];
  } = {},
) {
  function TrainingQueryPanelHarness() {
    const [queryMode, setQueryMode] = useState<TrainingQueryMode>(options.queryMode ?? 'multiple');
    const [ojName, setOjName] = useState<OjName>(OJ_NAMES.CODEFORCES);
    const [problemKey, setProblemKey] = useState(options.problemKey ?? '2242:C');
    const [selectedIdentity, setSelectedIdentity] = useState<StudentIdentity | null>(
      options.selectedIdentity === undefined ? '230511213黄炳睿' : options.selectedIdentity,
    );
    function handleOjNameChange(nextOjName: OjName) {
      setOjName(nextOjName);
      options.onOjNameChange?.(nextOjName);
    }
    function handleProblemKeyChange(nextProblemKey: string) {
      setProblemKey(nextProblemKey);
      options.onProblemKeyChange?.(nextProblemKey);
    }
    async function handleSelectedIdentityChange(nextIdentity: StudentIdentity) {
      setSelectedIdentity(nextIdentity);
      await options.onSelectedIdentityChange?.(nextIdentity);
    }
    return (
      <TrainingQueryPanel
        multiUserSummaries={options.multiUserSummaries ?? sampleMultiUserSummaries}
        firstAccepted={options.firstAccepted ?? null}
        isRefreshing={false}
        ojName={ojName}
        onApplyQuery={onApplyQuery}
        onOjNameChange={handleOjNameChange}
        onProblemKeyChange={handleProblemKeyChange}
        onProblemSubmissionPageChange={options.onProblemSubmissionPageChange ?? vi.fn()}
        onQueryModeChange={setQueryMode}
        onRefresh={vi.fn()}
        onSubmissionPageChange={options.onSubmissionPageChange ?? vi.fn()}
        onSelectedIdentityChange={handleSelectedIdentityChange}
        problemFirstAccepted={options.problemFirstAccepted ?? null}
        problemKey={problemKey}
        problemSubmissionLimit={options.problemSubmissionLimit ?? 100}
        problemSubmissionPage={options.problemSubmissionPage ?? 1}
        problemSubmissions={options.problemSubmissions ?? null}
        query={query}
        queryMode={queryMode}
        record={options.record ?? sampleRecord}
        selectedIdentity={selectedIdentity}
        submissionLimit={options.submissionLimit ?? 100}
        submissionPage={options.submissionPage ?? 1}
        studentOptions={options.studentOptions ?? ['230511213黄炳睿']}
        submissions={'submissions' in options ? options.submissions ?? null : sampleSubmissions}
        updatedAt="2026/07/06 01:30:00"
      />
    );
  }

  return render(
    <TrainingQueryPanelHarness />,
  );
}

describe('TrainingQueryPanel', () => {
  afterEach(() => cleanup());

  it('renders the full submission list with problem names as the primary text', async () => {
    const user = userEvent.setup();
    renderTrainingQueryPanel();

    await user.click(screen.getByRole('tab', { name: '单人查询' }));

    expect(screen.getByRole('table', { name: '最近提交明细' })).not.toBeNull();
    expect(screen.getByRole('columnheader', { name: '题目' })).not.toBeNull();
    expect(screen.getByRole('columnheader', { name: '判题' })).not.toBeNull();
    expect(screen.getByRole('columnheader', { name: '提交时间' })).not.toBeNull();
    expect(screen.getByText('Remove and Add')).not.toBeNull();
    expect(screen.getByText('Maximum Sum Subarrays')).not.toBeNull();
    const rows = within(screen.getByRole('table', { name: '最近提交明细' })).getAllByRole('row');
    expect(rows[1]?.textContent).toContain('Maximum Sum Subarrays');
    expect(rows[2]?.textContent).toContain('Remove and Add');
    expect(screen.getByText('Accept')).not.toBeNull();
    expect(screen.getByText('个人信息')).not.toBeNull();
    expect(screen.getByText('通过题目数')).not.toBeNull();
    expect(screen.getByText('tourist')).not.toBeNull();
    expect(screen.getByText('本页 0 题 / 合计 0 题')).not.toBeNull();
    expect(screen.getByText('本页 2 条，合计 2 条')).not.toBeNull();
    expect(screen.queryByText('绑定状态')).toBeNull();
    expect(screen.queryByText('区间通过题数')).toBeNull();
    expect(screen.queryByText('2 个区间')).toBeNull();
    expect(screen.queryByText('OK')).toBeNull();
    expect(screen.queryByText('2053:D')).toBeNull();
  });

  it('shortens the unrated rating bucket label to UNR', async () => {
    const user = userEvent.setup();
    renderTrainingQueryPanel(emptyQuery, vi.fn(), {
      record: {
        ...sampleRecord,
        acceptedSummary: {
          ...sampleRecord.acceptedSummary!,
          ratingCounts: [
            ...sampleRecord.acceptedSummary!.ratingCounts,
            { problemRating: 'UNRATED', acceptedProblemCount: 3 },
          ],
        },
      },
    });

    await user.click(screen.getByRole('tab', { name: '单人查询' }));

    expect(screen.getByText('UNR')).not.toBeNull();
    expect(screen.queryByText('UNRATED')).toBeNull();
  });

  it('sends backend pagination parameters from the submission pager', async () => {
    const user = userEvent.setup();
    const onSubmissionPageChange = vi.fn().mockResolvedValue(undefined);
    renderTrainingQueryPanel(emptyQuery, vi.fn(), {
      onSubmissionPageChange,
      submissions: {
        ...sampleSubmissions,
        total: 120,
        totalPages: 2,
        hasMore: true,
      },
    });

    await user.click(screen.getByRole('tab', { name: '单人查询' }));
    await user.click(screen.getByRole('button', { name: '下一页' }));
    await user.selectOptions(screen.getByLabelText('每页提交数'), '50');

    expect(onSubmissionPageChange).toHaveBeenCalledWith(2, 100);
    expect(onSubmissionPageChange).toHaveBeenCalledWith(1, 50);
  });

  it('defaults to the multi-user summary table with multiple statistics first', () => {
    renderTrainingQueryPanel();

    const tabs = screen.getAllByRole('tab');
    expect(tabs.map((tab) => tab.textContent)).toEqual(['多人统计', '单人查询', '题目查询']);
    expect(screen.getByRole('tab', { name: '多人统计' }).getAttribute('aria-selected')).toBe('true');

    expect(screen.getByRole('table', { name: '全部队员做题量统计' })).not.toBeNull();
    expect(screen.queryByRole('columnheader', { name: '排名' })).toBeNull();
    expect(screen.getByRole('columnheader', { name: '队员' })).not.toBeNull();
    expect(screen.queryByRole('columnheader', { name: 'Codeforces' })).toBeNull();
    expect(screen.getByRole('columnheader', { name: '总计' })).not.toBeNull();
    expect(screen.getByRole('columnheader', { name: '1600' })).not.toBeNull();
    expect(screen.getByRole('columnheader', { name: '1800' })).not.toBeNull();
    expect(screen.getByRole('columnheader', { name: '2100' })).not.toBeNull();
    const table = screen.getByRole('table', { name: '全部队员做题量统计' });
    const rows = within(table).getAllByRole('row');
    expect(within(rows[1]!).getByRole('rowheader').textContent).toContain('230511214李明');
    expect(within(rows[1]!).getByRole('rowheader').textContent).toContain('Benq');
    expect(within(rows[1]!).getAllByRole('cell').map((cell) => cell.textContent)).toEqual(['5', '3', '2', '-']);
    expect(rows[2]?.textContent).toContain('230511213黄炳睿');
    expect(screen.queryByRole('table', { name: '最近提交明细' })).toBeNull();
    expect(screen.queryByRole('button', { name: '查询' })).toBeNull();
  });

  it('renders problem-level submissions and first accepted handles', async () => {
    const user = userEvent.setup();
    renderTrainingQueryPanel(emptyQuery, vi.fn(), {
      problemFirstAccepted: sampleProblemFirstAccepted,
      problemSubmissions: sampleProblemSubmissions,
      queryMode: 'problem',
    });

    expect(screen.getByLabelText('题目维度统计')).not.toBeNull();
    expect(screen.getAllByText('题目编号').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('2242:C')).not.toBeNull();
    expect(screen.getByRole('table', { name: '题目提交明细' })).not.toBeNull();
    const submissionRows = within(screen.getByRole('table', { name: '题目提交明细' })).getAllByRole('row');
    expect(submissionRows[1]?.textContent).toContain('240521409李权澎');
    expect(submissionRows[2]?.textContent).toContain('240212224苏可航');
    expect(screen.getByText('Accept')).not.toBeNull();
    expect(screen.getByText('WRONG_ANSWER')).not.toBeNull();

    await user.click(screen.getByRole('tab', { name: '首 AC handle' }));

    expect(screen.getByRole('table', { name: '题目首 AC handle' })).not.toBeNull();
    expect(screen.getByText('orange_lov_gouzi')).not.toBeNull();
    expect(screen.getByText('Apeiron_24')).not.toBeNull();
  });

  it('auto-runs problem queries only after a problem key is filled', async () => {
    const user = userEvent.setup();
    const onApplyQuery = vi.fn().mockResolvedValue(undefined);
    renderTrainingQueryPanel(emptyQuery, onApplyQuery, {
      problemKey: '',
      queryMode: 'problem',
    });

    expect(screen.queryByRole('button', { name: '查询' })).toBeNull();
    expect(onApplyQuery).not.toHaveBeenCalled();

    await user.type(screen.getByLabelText('题目编号'), '2242:C');

    await waitFor(() => expect(onApplyQuery).toHaveBeenLastCalledWith(emptyQuery, 'problem'));
  });

  it('sends backend pagination parameters from the problem submission pager', async () => {
    const user = userEvent.setup();
    const onProblemSubmissionPageChange = vi.fn().mockResolvedValue(undefined);
    renderTrainingQueryPanel(emptyQuery, vi.fn(), {
      onProblemSubmissionPageChange,
      problemSubmissions: {
        ...sampleProblemSubmissions,
        total: 120,
        totalPages: 2,
        hasMore: true,
      },
      queryMode: 'problem',
    });

    await user.click(screen.getByRole('button', { name: '下一页' }));
    await user.selectOptions(screen.getByLabelText('题目每页提交数'), '50');

    expect(onProblemSubmissionPageChange).toHaveBeenCalledWith(2, 100);
    expect(onProblemSubmissionPageChange).toHaveBeenCalledWith(1, 50);
  });

  it('keeps single-user details hidden before a single query is submitted', async () => {
    const user = userEvent.setup();
    renderTrainingQueryPanel(emptyQuery, vi.fn(), {
      firstAccepted: null,
      submissions: null,
    });

    await user.click(screen.getByRole('tab', { name: '单人查询' }));

    expect(screen.queryByLabelText('训练数据统计')).toBeNull();
    expect(screen.queryByRole('table', { name: '最近提交明细' })).toBeNull();
    expect(screen.queryByRole('table', { name: '最近通过明细' })).toBeNull();
  });

  it('keeps single-user auto-query idle without prefilled identity or range', async () => {
    const user = userEvent.setup();
    const onApplyQuery = vi.fn().mockResolvedValue(undefined);
    renderTrainingQueryPanel({
      acceptedFromDateUtcPlus8: '2026-06-30',
      acceptedToDateUtcPlus8: '2026-07-06',
      minProblemRating: '1200',
      maxProblemRating: '2400',
    }, onApplyQuery, {
      firstAccepted: null,
      selectedIdentity: null,
      submissions: null,
      studentOptions: ['9999999托宝', '230511213黄炳睿'],
    });

    await user.click(screen.getByRole('tab', { name: '单人查询' }));

    expect((screen.getByLabelText('队员') as HTMLSelectElement).value).toBe('');
    expect(screen.getByRole('option', { name: '请选择队员' })).not.toBeNull();
    expect((screen.getByLabelText('通过起始日期') as HTMLInputElement).value).toBe('');
    expect((screen.getByLabelText('通过结束日期') as HTMLInputElement).value).toBe('');
    expect((screen.getByLabelText('最低 rating') as HTMLInputElement).value).toBe('');
    expect((screen.getByLabelText('最高 rating') as HTMLInputElement).value).toBe('');
    expect(screen.getByText('当前范围：全量日期 / 全 rating')).not.toBeNull();
    expect(screen.queryByRole('button', { name: '查询' })).toBeNull();
    expect(onApplyQuery).not.toHaveBeenCalled();
  });

  it('switches to recently accepted problems with problem name, metadata and accepted time', async () => {
    const user = userEvent.setup();
    renderTrainingQueryPanel(emptyQuery, vi.fn(), {
      firstAccepted: sampleFirstAccepted,
    });

    await user.click(screen.getByRole('tab', { name: '单人查询' }));
    await user.click(screen.getByRole('tab', { name: '最近通过' }));

    expect(screen.getByRole('table', { name: '最近通过明细' })).not.toBeNull();
    expect(screen.getByRole('columnheader', { name: '题目' })).not.toBeNull();
    expect(screen.getByRole('columnheader', { name: '通过时间' })).not.toBeNull();
    expect(screen.getByText('Newest Accepted Problem')).not.toBeNull();
    expect(screen.getByText('Older Accepted Problem')).not.toBeNull();
    const rows = within(screen.getByRole('table', { name: '最近通过明细' })).getAllByRole('row');
    expect(rows[1]?.textContent).toContain('Newest Accepted Problem');
    expect(rows[2]?.textContent).toContain('Older Accepted Problem');
    expect(screen.getByText('1800 rating / Kotlin 1.9')).not.toBeNull();
    expect(screen.getByText('1400 rating / C++20')).not.toBeNull();
    expect(screen.getByText('2024-01-05T12:30:00')).not.toBeNull();
    expect(screen.queryByText('判题')).toBeNull();
    expect(screen.queryByText('Accept')).toBeNull();
    expect(screen.queryByLabelText('每页提交数')).toBeNull();
  });

  it('auto-applies edited range without a query action', async () => {
    const user = userEvent.setup();
    const onApplyQuery = vi.fn().mockResolvedValue(undefined);
    renderTrainingQueryPanel(emptyQuery, onApplyQuery);

    await user.type(screen.getByLabelText('最低 rating'), '2000');

    expect(screen.getByText('当前范围：全量日期 / 2000 ~ 不限')).not.toBeNull();
    expect(screen.queryByText(/点击查询后生效/)).toBeNull();
    expect(screen.queryByRole('button', { name: '查询' })).toBeNull();

    await waitFor(() => expect(onApplyQuery).toHaveBeenLastCalledWith({
      ...emptyQuery,
      minProblemRating: '2000',
    }, 'multiple'));
  });

  it('auto-runs single-user queries with single mode after switching tabs', async () => {
    const user = userEvent.setup();
    const onApplyQuery = vi.fn().mockResolvedValue(undefined);
    renderTrainingQueryPanel(emptyQuery, onApplyQuery, {
      firstAccepted: null,
      submissions: null,
    });

    await user.click(screen.getByRole('tab', { name: '单人查询' }));

    await waitFor(() => expect(onApplyQuery).toHaveBeenCalledWith(emptyQuery, 'single'));
  });

  it('auto-runs single-user query after changing the selected member', async () => {
    const user = userEvent.setup();
    const onApplyQuery = vi.fn().mockResolvedValue(undefined);
    renderTrainingQueryPanel(emptyQuery, onApplyQuery, {
      firstAccepted: null,
      queryMode: 'single',
      selectedIdentity: '230511213黄炳睿',
      studentOptions: ['230511213黄炳睿', '9999999托宝'],
      submissions: null,
    });

    await user.selectOptions(screen.getByLabelText('队员'), '9999999托宝');

    await waitFor(() => expect(onApplyQuery).toHaveBeenLastCalledWith(emptyQuery, 'single'));
  });

  it('auto-runs the current query after switching OJ', async () => {
    const user = userEvent.setup();
    const onApplyQuery = vi.fn().mockResolvedValue(undefined);
    const onOjNameChange = vi.fn();
    renderTrainingQueryPanel(emptyQuery, onApplyQuery, {
      onOjNameChange,
    });

    await user.selectOptions(screen.getByLabelText('选择 OJ'), OJ_NAMES.ATCODER);

    expect(onOjNameChange).toHaveBeenCalledWith(OJ_NAMES.ATCODER);
    await waitFor(() => expect(onApplyQuery).toHaveBeenLastCalledWith(emptyQuery, 'multiple'));
  });

  it('blocks reversed rating ranges before calling the query API', async () => {
    const user = userEvent.setup();
    const onApplyQuery = vi.fn().mockResolvedValue(undefined);
    renderTrainingQueryPanel(emptyQuery, onApplyQuery);

    await user.type(screen.getByLabelText('最低 rating'), '2500');
    await waitFor(() => expect(onApplyQuery).toHaveBeenLastCalledWith({
      ...emptyQuery,
      minProblemRating: '2500',
    }, 'multiple'));
    onApplyQuery.mockClear();
    await user.type(screen.getByLabelText('最高 rating'), '2000');

    expect(screen.getByText('最低 rating 不能大于最高 rating。')).not.toBeNull();
    expect(screen.queryByRole('button', { name: '查询' })).toBeNull();

    expect(onApplyQuery).not.toHaveBeenCalled();
  });
});
