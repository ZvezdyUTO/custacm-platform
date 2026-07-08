import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { TrainingDataCollectionPanel } from '../components/TrainingDataOpsPanel';
import { OJ_NAMES, UNLIMITED_LOOKBACK_HOURS } from '../types';
import type {
  BatchCollectSummary,
  CodeforcesSubmissionCollectionJobResponse,
  StudentTrainingRecord,
} from '../types';

const collectableRecords: StudentTrainingRecord[] = [
  {
    studentIdentity: '230511213黄炳睿',
    role: 'player',
    handle: 'tourist',
    handles: { [OJ_NAMES.CODEFORCES]: 'tourist' },
    needCollect: true,
    handleStatus: 'bound',
    acceptedSummary: null,
    summaryStatus: 'not-requested',
    errorMessage: null,
    updatedAt: '2026-07-06T00:00:00Z',
  },
  {
    studentIdentity: '230511214李明',
    role: 'player',
    handle: 'benq',
    handles: { [OJ_NAMES.CODEFORCES]: 'benq' },
    needCollect: true,
    handleStatus: 'bound',
    acceptedSummary: null,
    summaryStatus: 'not-requested',
    errorMessage: null,
    updatedAt: '2026-07-06T00:00:00Z',
  },
];

const disabledCollectRecord: StudentTrainingRecord = {
  studentIdentity: '230511215王强',
  role: 'player',
  handle: 'jiangly',
  handles: { [OJ_NAMES.CODEFORCES]: 'jiangly' },
  needCollect: false,
  handleStatus: 'bound',
  acceptedSummary: null,
  summaryStatus: 'not-requested',
  errorMessage: null,
  updatedAt: '2026-07-06T00:00:00Z',
};

const collectSummary: BatchCollectSummary = {
  requestedCount: 1,
  collectedCount: 1,
  failedCount: 0,
  refreshedCount: 1,
  writtenRows: 10,
  batchIds: ['collector-codeforces-1'],
  results: [
    {
      studentIdentity: '230511213黄炳睿',
      ojName: OJ_NAMES.CODEFORCES,
      status: 'SUCCESS',
      handle: 'tourist',
      batchId: 'collector-codeforces-1',
      writtenRows: 10,
      fetchedSubmissionCount: 12,
      matchedSubmissionCount: 10,
      message: null,
      refreshStatus: 'SUCCESS',
      refreshMessage: null,
    },
  ],
};

const runningCollectionJob: CodeforcesSubmissionCollectionJobResponse = {
  jobId: 'job-running-1',
  ojName: OJ_NAMES.CODEFORCES,
  status: 'RUNNING',
  requestedCount: 2,
  completedCount: 1,
  collectedCount: 1,
  failedCount: 0,
  refreshedCount: 1,
  writtenRows: 10,
  batchIds: ['collector-codeforces-1'],
  startedAt: '2026-07-06T03:00:00Z',
  finishedAt: null,
  message: '采集任务运行中',
  items: [
    {
      studentIdentity: '230511213黄炳睿',
      ojName: OJ_NAMES.CODEFORCES,
      itemStatus: 'SUCCESS',
      collectionStatus: 'SUCCESS',
      handle: 'tourist',
      batchId: 'collector-codeforces-1',
      tableName: 'ods_codeforces__submission',
      writtenRows: 10,
      fetchedSubmissionCount: 12,
      matchedSubmissionCount: 10,
      fetchedAt: '2026-07-06T03:01:00Z',
      message: null,
      refreshStatus: 'SUCCESS',
      refreshMessage: null,
    },
    {
      studentIdentity: '230511214李明',
      ojName: OJ_NAMES.CODEFORCES,
      itemStatus: 'RUNNING',
      collectionStatus: null,
      handle: null,
      batchId: null,
      tableName: null,
      writtenRows: 0,
      fetchedSubmissionCount: 0,
      matchedSubmissionCount: 0,
      fetchedAt: null,
      message: null,
      refreshStatus: 'NOT_REQUESTED',
      refreshMessage: null,
    },
  ],
};

const runningCollectionSummary: BatchCollectSummary = {
  requestedCount: 2,
  collectedCount: 1,
  failedCount: 0,
  refreshedCount: 1,
  writtenRows: 10,
  batchIds: ['collector-codeforces-1'],
  results: [
    collectSummary.results[0],
    {
      studentIdentity: '230511214李明',
      ojName: OJ_NAMES.CODEFORCES,
      status: 'RUNNING',
      handle: null,
      batchId: null,
      writtenRows: 0,
      fetchedSubmissionCount: 0,
      matchedSubmissionCount: 0,
      message: null,
      refreshStatus: 'NOT_REQUESTED',
      refreshMessage: null,
    },
  ],
};

describe('training data operation panels', () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('runs collection for a single auto-collect student with unlimited lookback when hours are blank', async () => {
    const user = userEvent.setup();
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true);
    const onBatchCollect = vi.fn().mockResolvedValue(collectSummary);
    render(
      <TrainingDataCollectionPanel
        collectableRecords={[...collectableRecords, disabledCollectRecord]}
        collectionJob={null}
        collectionJobs={[]}
        collectionJobSummary={null}
        isRefreshing={false}
        onBatchCollect={onBatchCollect}
      />,
    );

    expect((screen.getByLabelText('230511213黄炳睿 回看小时数') as HTMLInputElement).value).toBe('');
    expect(screen.queryByText(/窗口：/)).toBeNull();
    expect(screen.queryByText('230511215王强')).toBeNull();

    await user.click(screen.getAllByRole('button', { name: '执行采集' })[0]);

    expect(confirm).toHaveBeenCalledWith(expect.stringContaining('确认采集 230511213黄炳睿不限时间范围'));
    await waitFor(() => {
      expect(onBatchCollect).toHaveBeenCalledWith({
        studentIdentities: ['230511213黄炳睿'],
        lookbackHours: UNLIMITED_LOOKBACK_HOURS,
        refreshWarehouse: true,
        ojName: OJ_NAMES.CODEFORCES,
      });
    });
    expect(screen.getByText(/采集完成：采集 1\/1，刷新 1，写入 10 行/)).not.toBeNull();
    const resultTable = screen.getByRole('table', { name: '数据采集结果' });
    expect(within(resultTable).getByRole('columnheader', { name: '队员 / handle' })).not.toBeNull();
    expect(within(resultTable).getByRole('columnheader', { name: '状态' })).not.toBeNull();
    expect(within(resultTable).queryByRole('columnheader', { name: '优先级' })).toBeNull();
    expect(within(resultTable).queryByRole('columnheader', { name: '责任域' })).toBeNull();
    expect(screen.getByText('采集成功')).not.toBeNull();
    expect(screen.getAllByText(/collector-codeforces-1/).length).toBeGreaterThan(0);
  });

  it('runs collection for all auto-collect students from the header action', async () => {
    const user = userEvent.setup();
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true);
    const onBatchCollect = vi.fn().mockResolvedValue({
      ...collectSummary,
      requestedCount: 2,
      collectedCount: 2,
      refreshedCount: 2,
      writtenRows: 18,
      batchIds: ['collector-codeforces-1', 'collector-codeforces-2'],
      results: [collectSummary.results[0], {
        studentIdentity: '230511214李明',
        ojName: OJ_NAMES.CODEFORCES,
        status: 'SUCCESS',
        handle: 'benq',
        batchId: 'collector-codeforces-2',
        writtenRows: 8,
        fetchedSubmissionCount: 8,
        matchedSubmissionCount: 8,
        message: null,
        refreshStatus: 'SUCCESS',
        refreshMessage: null,
      }],
    });
    render(
      <TrainingDataCollectionPanel
        collectableRecords={[...collectableRecords, disabledCollectRecord]}
        collectionJob={null}
        collectionJobs={[]}
        collectionJobSummary={null}
        isRefreshing={false}
        onBatchCollect={onBatchCollect}
      />,
    );

    const allLookbackInput = screen.getByLabelText('全部采集回看小时数') as HTMLInputElement;
    expect(allLookbackInput.value).toBe('1440');
    await user.clear(allLookbackInput);
    await user.type(allLookbackInput, '72');

    await user.click(screen.getByRole('button', { name: '全部采集' }));

    expect(confirm).toHaveBeenCalledWith(expect.stringContaining('确认采集全部 2 个队员最近 72 小时'));
    await waitFor(() => {
      expect(onBatchCollect).toHaveBeenCalledWith({
        studentIdentities: ['230511213黄炳睿', '230511214李明'],
        lookbackHours: 72,
        refreshWarehouse: true,
        ojName: OJ_NAMES.CODEFORCES,
      });
    });
    expect(screen.getByText(/采集完成：采集 2\/2，刷新 2，写入 18 行/)).not.toBeNull();
  });

  it('shows a running backend collection job and expands job details', async () => {
    const user = userEvent.setup();
    const onBatchCollect = vi.fn().mockResolvedValue(collectSummary);
    render(
      <TrainingDataCollectionPanel
        collectableRecords={collectableRecords}
        collectionJob={runningCollectionJob}
        collectionJobs={[runningCollectionJob]}
        collectionJobSummary={runningCollectionSummary}
        isRefreshing={false}
        onBatchCollect={onBatchCollect}
      />,
    );

    expect(screen.getByText('后台采集运行中：采集 1/2，刷新 1，写入 10 行')).not.toBeNull();
    expect(screen.getByText('采集中')).not.toBeNull();
    expect(screen.getAllByRole('button', { name: '正在采集' }).every((button) => button.hasAttribute('disabled'))).toBe(true);
    expect(screen.getByRole('button', { name: '全部采集' })).toHaveProperty('disabled', true);
    expect(onBatchCollect).not.toHaveBeenCalled();

    await user.click(screen.getByRole('button', { name: /job-running-1/ }));

    expect(screen.getByText('任务 ID')).not.toBeNull();
    expect(screen.getAllByText('job-running-1').length).toBeGreaterThan(0);
    expect(screen.getAllByText('230511214李明').length).toBeGreaterThan(0);
  });

  it('uses explicit lookback hours when a student row field is filled', async () => {
    const user = userEvent.setup();
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true);
    const onBatchCollect = vi.fn().mockResolvedValue(collectSummary);
    render(
      <TrainingDataCollectionPanel
        collectableRecords={[collectableRecords[0]]}
        collectionJob={null}
        collectionJobs={[]}
        collectionJobSummary={null}
        isRefreshing={false}
        onBatchCollect={onBatchCollect}
      />,
    );

    await user.type(screen.getByLabelText('230511213黄炳睿 回看小时数'), '24');

    expect(screen.queryByText(/窗口：/)).toBeNull();

    await user.click(screen.getByRole('button', { name: '执行采集' }));

    expect(confirm).toHaveBeenCalledWith(expect.stringContaining('确认采集 230511213黄炳睿最近 24 小时'));
    await waitFor(() => {
      expect(onBatchCollect).toHaveBeenCalledWith({
        studentIdentities: ['230511213黄炳睿'],
        lookbackHours: 24,
        refreshWarehouse: true,
        ojName: OJ_NAMES.CODEFORCES,
      });
    });
  });

  it('does not collect when the high-cost confirmation is cancelled', async () => {
    const user = userEvent.setup();
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    const onBatchCollect = vi.fn().mockResolvedValue(collectSummary);
    render(
      <TrainingDataCollectionPanel
        collectableRecords={[collectableRecords[0]]}
        collectionJob={null}
        collectionJobs={[]}
        collectionJobSummary={null}
        isRefreshing={false}
        onBatchCollect={onBatchCollect}
      />,
    );

    await user.click(screen.getByRole('button', { name: '执行采集' }));

    expect(onBatchCollect).not.toHaveBeenCalled();
  });

});
