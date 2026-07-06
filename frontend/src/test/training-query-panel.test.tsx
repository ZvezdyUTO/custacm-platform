import { cleanup, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { TrainingQueryPanel } from '../components/TrainingQueryPanel';
import type {
  CodeforcesAcceptedSummary,
  CodeforcesFirstAcceptedReport,
  CodeforcesStudentSubmissionReport,
  StudentTrainingRecord,
  TrainingQueryMode,
  TrainingQueryRange,
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

const sampleAutoCollectSummaries: CodeforcesAcceptedSummary[] = [
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
    onSubmissionPageChange?: (page: number, limit: number) => Promise<void>;
    record?: StudentTrainingRecord;
    submissionLimit?: number;
    submissionPage?: number;
    submissions?: CodeforcesStudentSubmissionReport | null;
    autoCollectSummaries?: CodeforcesAcceptedSummary[];
    queryMode?: TrainingQueryMode;
    selectedIdentity?: string | null;
    studentOptions?: string[];
  } = {},
) {
  function TrainingQueryPanelHarness() {
    const [queryMode, setQueryMode] = useState<TrainingQueryMode>(options.queryMode ?? 'multiple');
    return (
      <TrainingQueryPanel
        autoCollectSummaries={options.autoCollectSummaries ?? sampleAutoCollectSummaries}
        firstAccepted={options.firstAccepted ?? null}
        isRefreshing={false}
        onApplyQuery={onApplyQuery}
        onQueryModeChange={setQueryMode}
        onRefresh={vi.fn()}
        onSubmissionPageChange={options.onSubmissionPageChange ?? vi.fn()}
        onSelectedIdentityChange={vi.fn()}
        query={query}
        queryMode={queryMode}
        record={options.record ?? sampleRecord}
        selectedIdentity={options.selectedIdentity === undefined ? '230511213黄炳睿' : options.selectedIdentity}
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

  it('defaults to automatic collection user summary table with multiple statistics first', () => {
    renderTrainingQueryPanel();

    const tabs = screen.getAllByRole('tab');
    expect(tabs.map((tab) => tab.textContent)).toEqual(['多人统计', '单人查询']);
    expect(screen.getByRole('tab', { name: '多人统计' }).getAttribute('aria-selected')).toBe('true');

    expect(screen.getByRole('table', { name: '全部选手做题量统计' })).not.toBeNull();
    expect(screen.queryByRole('columnheader', { name: '排名' })).toBeNull();
    expect(screen.getByRole('columnheader', { name: '选手' })).not.toBeNull();
    expect(screen.queryByRole('columnheader', { name: 'Codeforces' })).toBeNull();
    expect(screen.getByRole('columnheader', { name: '总计' })).not.toBeNull();
    expect(screen.getByRole('columnheader', { name: '1600' })).not.toBeNull();
    expect(screen.getByRole('columnheader', { name: '1800' })).not.toBeNull();
    expect(screen.getByRole('columnheader', { name: '2100' })).not.toBeNull();
    const table = screen.getByRole('table', { name: '全部选手做题量统计' });
    const rows = within(table).getAllByRole('row');
    expect(within(rows[1]!).getByRole('rowheader').textContent).toContain('230511214李明');
    expect(within(rows[1]!).getByRole('rowheader').textContent).toContain('Benq');
    expect(within(rows[1]!).getAllByRole('cell').map((cell) => cell.textContent)).toEqual(['5', '3', '2', '-']);
    expect(rows[2]?.textContent).toContain('230511213黄炳睿');
    expect(screen.queryByRole('table', { name: '最近提交明细' })).toBeNull();
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

  it('starts single-user query without prefilled identity or range', async () => {
    const user = userEvent.setup();
    renderTrainingQueryPanel({
      acceptedFromDateUtcPlus8: '2026-06-30',
      acceptedToDateUtcPlus8: '2026-07-06',
      minProblemRating: '1200',
      maxProblemRating: '2400',
    }, vi.fn(), {
      firstAccepted: null,
      selectedIdentity: null,
      submissions: null,
      studentOptions: ['9999999托宝', '230511213黄炳睿'],
    });

    await user.click(screen.getByRole('tab', { name: '单人查询' }));

    expect((screen.getByLabelText('选手') as HTMLSelectElement).value).toBe('');
    expect(screen.getByRole('option', { name: '请选择选手' })).not.toBeNull();
    expect((screen.getByLabelText('通过起始日期') as HTMLInputElement).value).toBe('');
    expect((screen.getByLabelText('通过结束日期') as HTMLInputElement).value).toBe('');
    expect((screen.getByLabelText('最低 rating') as HTMLInputElement).value).toBe('');
    expect((screen.getByLabelText('最高 rating') as HTMLInputElement).value).toBe('');
    expect(screen.getByText('当前范围：全量日期 / 全 rating')).not.toBeNull();
    expect((screen.getByRole('button', { name: '查询' }) as HTMLButtonElement).disabled).toBe(true);
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

  it('keeps edited range as a draft until the query action is submitted', async () => {
    const user = userEvent.setup();
    const onApplyQuery = vi.fn().mockResolvedValue(undefined);
    renderTrainingQueryPanel(emptyQuery, onApplyQuery);

    await user.type(screen.getByLabelText('最低 rating'), '2000');

    expect(screen.getByText('当前范围：全量日期 / 全 rating')).not.toBeNull();
    expect(screen.getByText('已编辑为 全量日期 / 2000 ~ 不限，点击查询后生效。')).not.toBeNull();

    await user.click(screen.getByRole('button', { name: '查询' }));

    expect(onApplyQuery).toHaveBeenCalledWith({
      ...emptyQuery,
      minProblemRating: '2000',
    }, 'multiple');
  });

  it('submits single-user queries with single mode after switching tabs', async () => {
    const user = userEvent.setup();
    const onApplyQuery = vi.fn().mockResolvedValue(undefined);
    renderTrainingQueryPanel(emptyQuery, onApplyQuery, {
      firstAccepted: null,
      submissions: null,
    });

    await user.click(screen.getByRole('tab', { name: '单人查询' }));
    await user.click(screen.getByRole('button', { name: '查询' }));

    expect(onApplyQuery).toHaveBeenCalledWith(emptyQuery, 'single');
  });

  it('resets the applied query range to the full dataset', async () => {
    const user = userEvent.setup();
    const onApplyQuery = vi.fn().mockResolvedValue(undefined);
    renderTrainingQueryPanel({ ...emptyQuery, minProblemRating: '2000' }, onApplyQuery);

    await user.click(screen.getByRole('button', { name: '重置' }));

    expect(onApplyQuery).toHaveBeenCalledWith(emptyQuery, 'multiple');
  });

  it('blocks reversed rating ranges before calling the query API', async () => {
    const user = userEvent.setup();
    const onApplyQuery = vi.fn().mockResolvedValue(undefined);
    renderTrainingQueryPanel(emptyQuery, onApplyQuery);

    await user.type(screen.getByLabelText('最低 rating'), '2500');
    await user.type(screen.getByLabelText('最高 rating'), '2000');

    const queryButton = screen.getByRole('button', { name: '查询' }) as HTMLButtonElement;
    expect(screen.getByText('最低 rating 不能大于最高 rating。')).not.toBeNull();
    expect(queryButton.disabled).toBe(true);

    await user.click(queryButton);

    expect(onApplyQuery).not.toHaveBeenCalled();
  });
});
