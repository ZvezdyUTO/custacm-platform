import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { TrainingDataCollectionPanel, TrainingDataMaintenancePanel } from '../components/TrainingDataOpsPanel';
import { UNLIMITED_LOOKBACK_HOURS } from '../types';
import type {
  BatchCollectSummary,
  CodeforcesSubmissionCollectionJobResponse,
  CodeforcesOdsBatchUpsertResponse,
  FullUserDataDeleteSummary,
  StudentTrainingRecord,
} from '../types';

const collectableRecords: StudentTrainingRecord[] = [
  {
    studentIdentity: '230511213黄炳睿',
    role: 'player',
    handle: 'tourist',
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

const lastBatch: CodeforcesOdsBatchUpsertResponse = {
  batchId: 'ods-codeforces-20260706',
  tableName: 'ods_codeforces__submission',
  writtenRows: 200,
  fetchedAt: '2026-07-06T00:00:00Z',
};

const runningCollectionJob: CodeforcesSubmissionCollectionJobResponse = {
  jobId: 'job-running-1',
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

const deleteSummary: FullUserDataDeleteSummary = {
  trainingDataResult: {
    studentIdentity: '230511213黄炳睿',
    handle: 'tourist',
    handleAccountRows: 1,
    odsSubmissionRows: 2,
    dwdSubmissionRows: 3,
    dwmFirstAcceptedRows: 4,
    dwsAcceptedSummaryRows: 5,
    totalDeletedRows: 15,
  },
  authUserResult: {
    success: true,
    studentIdentity: '230511213黄炳睿',
    user: null,
    plainPassword: null,
    errorCode: null,
    message: 'user deleted',
  },
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
    expect(screen.getAllByText('窗口：不限')).toHaveLength(2);
    expect(screen.queryByText('230511215王强')).toBeNull();

    await user.click(screen.getAllByRole('button', { name: '执行采集' })[0]);

    expect(confirm).toHaveBeenCalledWith(expect.stringContaining('确认采集 230511213黄炳睿不限时间范围'));
    await waitFor(() => {
      expect(onBatchCollect).toHaveBeenCalledWith({
        studentIdentities: ['230511213黄炳睿'],
        lookbackHours: UNLIMITED_LOOKBACK_HOURS,
        refreshWarehouse: true,
      });
    });
    expect(screen.getByText(/采集完成：采集 1\/1，刷新 1，写入 10 行/)).not.toBeNull();
    const resultTable = screen.getByRole('table', { name: '数据采集结果' });
    expect(within(resultTable).getByRole('columnheader', { name: '选手 / handle' })).not.toBeNull();
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

    expect(confirm).toHaveBeenCalledWith(expect.stringContaining('确认采集全部 2 个选手最近 72 小时'));
    await waitFor(() => {
      expect(onBatchCollect).toHaveBeenCalledWith({
        studentIdentities: ['230511213黄炳睿', '230511214李明'],
        lookbackHours: 72,
        refreshWarehouse: true,
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

    expect(screen.getByText('窗口：最近 24 小时')).not.toBeNull();

    await user.click(screen.getByRole('button', { name: '执行采集' }));

    expect(confirm).toHaveBeenCalledWith(expect.stringContaining('确认采集 230511213黄炳睿最近 24 小时'));
    await waitFor(() => {
      expect(onBatchCollect).toHaveBeenCalledWith({
        studentIdentities: ['230511213黄炳睿'],
        lookbackHours: 24,
        refreshWarehouse: true,
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

  it('keeps ODS upload and warehouse refresh actions on the maintenance page', async () => {
    const user = userEvent.setup();
    const onImportOds = vi.fn();
    const onRefreshWarehouse = vi.fn();
    render(
      <TrainingDataMaintenancePanel
        canRefreshWarehouse
        currentUserIdentity="root"
        isRefreshing={false}
        lastBatch={lastBatch}
        onDeleteFullUserData={vi.fn().mockResolvedValue(deleteSummary)}
        onImportOds={onImportOds}
        onRefreshWarehouse={onRefreshWarehouse}
        userStudentOptions={['root']}
      />,
    );

    await user.click(screen.getByRole('button', { name: '上传 ODS JSON' }));
    await user.click(screen.getByRole('button', { name: '刷新最近批次' }));

    expect(onImportOds).toHaveBeenCalledTimes(1);
    expect(onRefreshWarehouse).toHaveBeenCalledTimes(1);
    expect(screen.getByText('ods-codeforces-20260706')).not.toBeNull();
  });

  it('confirms and submits full user data deletion for non-current users', async () => {
    const user = userEvent.setup();
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true);
    const onDeleteFullUserData = vi.fn().mockResolvedValue(deleteSummary);
    render(
      <TrainingDataMaintenancePanel
        canRefreshWarehouse
        currentUserIdentity="root"
        isRefreshing={false}
        lastBatch={lastBatch}
        onDeleteFullUserData={onDeleteFullUserData}
        onImportOds={vi.fn()}
        onRefreshWarehouse={vi.fn()}
        userStudentOptions={['root', '230511213黄炳睿']}
      />,
    );

    await user.click(screen.getByRole('button', { name: '删除用户所有数据' }));

    expect(confirm).toHaveBeenCalledWith(expect.stringContaining('确认彻底删除 230511213黄炳睿'));
    await waitFor(() => {
      expect(onDeleteFullUserData).toHaveBeenCalledWith('230511213黄炳睿');
    });
    expect(screen.getByText('已删除 230511213黄炳睿，训练数据 15 行')).not.toBeNull();
  });

  it('does not keep a stale deletion target after the selected user disappears', async () => {
    const user = userEvent.setup();
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true);
    const onDeleteFullUserData = vi.fn().mockResolvedValue(deleteSummary);
    const { rerender } = render(
      <TrainingDataMaintenancePanel
        canRefreshWarehouse
        currentUserIdentity="root"
        isRefreshing={false}
        lastBatch={lastBatch}
        onDeleteFullUserData={onDeleteFullUserData}
        onImportOds={vi.fn()}
        onRefreshWarehouse={vi.fn()}
        userStudentOptions={['root', '230511213黄炳睿', '230511214李明']}
      />,
    );

    await user.selectOptions(screen.getByLabelText('选择删除用户'), '230511214李明');
    expect((screen.getByLabelText('选择删除用户') as HTMLSelectElement).value).toBe('230511214李明');
    expect(screen.getByText('目标：230511214李明')).not.toBeNull();

    rerender(
      <TrainingDataMaintenancePanel
        canRefreshWarehouse
        currentUserIdentity="root"
        isRefreshing={false}
        lastBatch={lastBatch}
        onDeleteFullUserData={onDeleteFullUserData}
        onImportOds={vi.fn()}
        onRefreshWarehouse={vi.fn()}
        userStudentOptions={['root', '230511213黄炳睿']}
      />,
    );

    expect((screen.getByLabelText('选择删除用户') as HTMLSelectElement).value).toBe('230511213黄炳睿');
    expect(screen.getByText('目标：230511213黄炳睿')).not.toBeNull();

    await user.click(screen.getByRole('button', { name: '删除用户所有数据' }));

    expect(confirm).toHaveBeenCalledWith(expect.stringContaining('确认彻底删除 230511213黄炳睿'));
    await waitFor(() => {
      expect(onDeleteFullUserData).toHaveBeenCalledWith('230511213黄炳睿');
    });
    expect(onDeleteFullUserData).not.toHaveBeenCalledWith('230511214李明');
  });
});
