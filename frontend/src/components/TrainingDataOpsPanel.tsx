import { ChevronDown, Database, RefreshCw } from 'lucide-react';
import type { FormEvent } from 'react';
import { useEffect, useMemo, useState } from 'react';
import { OJ_LABELS, OJ_NAMES, UNLIMITED_LOOKBACK_HOURS } from '../types';
import type {
  BatchCollectOptions,
  BatchCollectSummary,
  CodeforcesSubmissionCollectionJobResponse,
  OjName,
  StudentIdentity,
  StudentTrainingRecord,
} from '../types';

interface TrainingDataCollectionPanelProps {
  collectableRecords: StudentTrainingRecord[];
  collectionJob: CodeforcesSubmissionCollectionJobResponse | null;
  collectionJobs: CodeforcesSubmissionCollectionJobResponse[];
  collectionJobSummary: BatchCollectSummary | null;
  isRefreshing: boolean;
  onBatchCollect: (options: BatchCollectOptions) => Promise<BatchCollectSummary>;
}

interface CollectionJobsCardProps {
  collectionJobs: CodeforcesSubmissionCollectionJobResponse[];
}

type CollectionResultStatus = BatchCollectSummary['results'][number]['status'];
type CollectionRefreshStatus = BatchCollectSummary['results'][number]['refreshStatus'];

interface CollectionResultTableRow {
  studentIdentity: StudentIdentity;
  ojName: BatchCollectSummary['results'][number]['ojName'];
  status: CollectionResultStatus;
  handle: string | null;
  batchId: string | null;
  writtenRows: number;
  fetchedSubmissionCount: number;
  matchedSubmissionCount: number;
  message: string | null;
  refreshStatus: CollectionRefreshStatus;
  refreshMessage: string | null;
}

export function TrainingDataCollectionPanel({
  collectableRecords,
  collectionJob,
  collectionJobs,
  collectionJobSummary,
  isRefreshing,
  onBatchCollect,
}: TrainingDataCollectionPanelProps) {
  const [collectionOjName, setCollectionOjName] = useState<OjName>(OJ_NAMES.CODEFORCES);
  const [collectAllLookbackHours, setCollectAllLookbackHours] = useState('1440');
  const [lookbackHoursByIdentity, setLookbackHoursByIdentity] = useState<Record<StudentIdentity, string>>({});
  const [refreshWarehouseByIdentity, setRefreshWarehouseByIdentity] = useState<Record<StudentIdentity, boolean>>({});
  const [collectError, setCollectError] = useState<string | null>(null);
  const [collectSummary, setCollectSummary] = useState<BatchCollectSummary | null>(null);

  const collectionRecords = useMemo(
    () => collectableRecords.filter((record) => handleForOj(record, collectionOjName) && record.needCollect !== false),
    [collectableRecords, collectionOjName],
  );
  const collectionIdentities = useMemo(
    () => collectionRecords.map((record) => record.studentIdentity),
    [collectionRecords],
  );
  const isCollectionRunning = collectionJob?.status === 'RUNNING';
  const effectiveCollectSummary = collectSummary ?? collectionJobSummary;
  const canCollectAll = collectionIdentities.length > 0 && !isRefreshing && !isCollectionRunning;
  const selectedOjLabel = OJ_LABELS[collectionOjName];
  const normalizedCollectAllLookbackHours = parseLookbackHours(collectAllLookbackHours);
  const effectiveCollectAllLookbackHours = normalizedCollectAllLookbackHours ?? UNLIMITED_LOOKBACK_HOURS;
  const collectAllLookbackConfirmationLabel = normalizedCollectAllLookbackHours === null
    ? '不限时间范围'
    : `最近 ${normalizedCollectAllLookbackHours} 小时`;
  const collectAllWindowLabel = normalizedCollectAllLookbackHours === null
    ? '不限'
    : `最近 ${normalizedCollectAllLookbackHours} 小时`;

  useEffect(() => {
    if (isCollectionRunning) {
      setCollectSummary(null);
      setCollectError(null);
    }
  }, [isCollectionRunning, collectionJob?.jobId]);

  function getLookbackHours(identity: StudentIdentity) {
    return lookbackHoursByIdentity[identity] ?? '';
  }

  function getRefreshWarehouse(identity: StudentIdentity) {
    return refreshWarehouseByIdentity[identity] ?? true;
  }

  function updateLookbackHours(identity: StudentIdentity, value: string) {
    setLookbackHoursByIdentity((current) => ({
      ...current,
      [identity]: value,
    }));
    setCollectError(null);
  }

  function updateRefreshWarehouse(identity: StudentIdentity, checked: boolean) {
    setRefreshWarehouseByIdentity((current) => ({
      ...current,
      [identity]: checked,
    }));
  }

  function updateCollectAllLookbackHours(value: string) {
    setCollectAllLookbackHours(value);
    setCollectError(null);
  }

  async function handleCollectOne(event: FormEvent<HTMLFormElement>, record: StudentTrainingRecord) {
    event.preventDefault();
    const identity = record.studentIdentity;
    const normalizedLookbackHours = parseLookbackHours(getLookbackHours(identity));
    const effectiveLookbackHours = normalizedLookbackHours ?? UNLIMITED_LOOKBACK_HOURS;
    const lookbackConfirmationLabel = normalizedLookbackHours === null
      ? '不限时间范围'
      : `最近 ${normalizedLookbackHours} 小时`;
    const refreshWarehouse = getRefreshWarehouse(identity);
    setCollectError(null);
    setCollectSummary(null);
    if (!confirmHighCostAction(
      `确认采集 ${identity}${lookbackConfirmationLabel}的 ${selectedOjLabel} 提交？${
        refreshWarehouse ? '采集成功后会刷新仓库。' : ''
      }`,
    )) {
      return;
    }
    try {
      const summary = await onBatchCollect({
        studentIdentities: [identity],
        lookbackHours: effectiveLookbackHours,
        refreshWarehouse,
        ojName: collectionOjName,
      });
      setCollectSummary(summary);
    } catch (error) {
      setCollectError(error instanceof Error ? error.message : '采集失败。');
    }
  }

  async function handleCollectAll() {
    setCollectError(null);
    setCollectSummary(null);
    if (collectionIdentities.length === 0) {
      setCollectError('暂无可采集的绑定队员。');
      return;
    }
    if (!confirmHighCostAction(
      `确认采集全部 ${collectionIdentities.length} 个队员${collectAllLookbackConfirmationLabel}的 ${selectedOjLabel} 提交？采集成功后会刷新仓库。`,
    )) {
      return;
    }
    try {
      const summary = await onBatchCollect({
        studentIdentities: collectionIdentities,
        lookbackHours: effectiveCollectAllLookbackHours,
        refreshWarehouse: true,
        ojName: collectionOjName,
      });
      setCollectSummary(summary);
    } catch (error) {
      setCollectError(error instanceof Error ? error.message : '全部采集失败。');
    }
  }

  return (
    <section className="training-data-collection-panel" aria-label="训练数据采集">
      <section className="admin-management-card data-collect-card">
        <header className="admin-card-header-with-actions">
          <div className="admin-card-heading">
            <span className="admin-action-icon">
              <Database size={18} aria-hidden="true" />
            </span>
            <div>
              <h2>训练数据采集</h2>
              <p>按 OJ 列出已开启自动采集且已绑定 handle 的队员，每行可单独设置回看窗口并执行采集。</p>
            </div>
          </div>
          <div className="admin-card-header-actions">
            <label className="query-field collect-oj-field">
              OJ
              <select
                aria-label="采集 OJ"
                disabled={isRefreshing || isCollectionRunning}
                value={collectionOjName}
                onChange={(event) => {
                  setCollectionOjName(event.target.value as OjName);
                  setCollectSummary(null);
                  setCollectError(null);
                }}
              >
                <option value={OJ_NAMES.CODEFORCES}>{OJ_LABELS[OJ_NAMES.CODEFORCES]}</option>
                <option value={OJ_NAMES.ATCODER}>{OJ_LABELS[OJ_NAMES.ATCODER]}</option>
              </select>
            </label>
            <label className="query-field collect-all-lookback-field">
              统一回看小时数
              <input
                aria-label="全部采集回看小时数"
                min="1"
                placeholder="不限"
                type="number"
                value={collectAllLookbackHours}
                onChange={(event) => updateCollectAllLookbackHours(event.target.value)}
              />
            </label>
            <button className="primary-button" disabled={!canCollectAll} onClick={handleCollectAll} type="button">
              <RefreshCw size={16} aria-hidden="true" className={isRefreshing || isCollectionRunning ? 'spin' : ''} />
              全部采集
            </button>
            <small className="collect-window-label">窗口：{collectAllWindowLabel}</small>
          </div>
        </header>
        {collectionRecords.length > 0 ? (
          <ul className="collect-student-list" aria-live="polite">
            {collectionRecords.map((record, index) => {
              const identity = record.studentIdentity;
              const inputId = `collect-lookback-${index}`;
              const refreshId = `collect-refresh-${index}`;
              const lookbackHours = getLookbackHours(identity);
              const normalizedLookbackHours = parseLookbackHours(lookbackHours);
              const lookbackWindowLabel = normalizedLookbackHours === null
                ? '不限'
                : `最近 ${normalizedLookbackHours} 小时`;
              const canCollect = !isRefreshing && !isCollectionRunning;
              const selectedHandle = handleForOj(record, collectionOjName);
              return (
                <li key={identity}>
                  <form className="collect-student-row" onSubmit={(event) => handleCollectOne(event, record)}>
                    <div className="collect-student-main">
                      <strong>{identity}</strong>
                      <small>{selectedHandle ? `${selectedOjLabel}：${selectedHandle}` : `${selectedOjLabel} handle 未解析`}</small>
                    </div>
                    <label className="query-field collect-lookback-field" htmlFor={inputId}>
                      回看小时数
                      <input
                        aria-label={`${identity} 回看小时数`}
                        id={inputId}
                        min="1"
                        placeholder="不限"
                        type="number"
                        value={lookbackHours}
                        onChange={(event) => updateLookbackHours(identity, event.target.value)}
                      />
                    </label>
                    <label className="checkbox-field collect-refresh-field" htmlFor={refreshId}>
                      <input
                        checked={getRefreshWarehouse(identity)}
                        id={refreshId}
                        type="checkbox"
                        onChange={(event) => updateRefreshWarehouse(identity, event.target.checked)}
                      />
                      采集后刷新仓库
                    </label>
                    <button className="primary-button" disabled={!canCollect} type="submit">
                      <RefreshCw size={16} aria-hidden="true" className={isRefreshing || isCollectionRunning ? 'spin' : ''} />
                      {isCollectionRunning ? '正在采集' : '执行采集'}
                    </button>
                    <small className="collect-window-label">窗口：{lookbackWindowLabel}</small>
                  </form>
                </li>
              );
            })}
          </ul>
        ) : (
          <p className="batch-target-empty" aria-live="polite">暂无开启自动采集且绑定 {selectedOjLabel} 的队员</p>
        )}
        {collectError ? (
          <p className="form-error" role="alert">
            {collectError}
          </p>
        ) : null}
        {effectiveCollectSummary ? (
          <output className="admin-result" aria-live="polite">
            <strong>
              {isCollectionRunning ? '后台采集运行中' : '采集完成'}：采集{' '}
              {effectiveCollectSummary.collectedCount}/{effectiveCollectSummary.requestedCount}，刷新{' '}
              {effectiveCollectSummary.refreshedCount}，写入 {effectiveCollectSummary.writtenRows} 行
            </strong>
            <span>
              {effectiveCollectSummary.batchIds.length > 0
                ? `批次：${effectiveCollectSummary.batchIds.slice(0, 3).join('、')}`
                : isCollectionRunning
                  ? `任务：${collectionJob?.jobId ?? '等待任务号'}`
                  : `失败 ${effectiveCollectSummary.failedCount} 个，无可刷新批次`}
            </span>
            <CollectionResultTable rows={effectiveCollectSummary.results} />
          </output>
        ) : null}
      </section>

      <CollectionJobsCard collectionJobs={collectionJobs} />
    </section>
  );
}

function handleForOj(record: StudentTrainingRecord, ojName: OjName) {
  return record.handles[ojName] ?? null;
}

function CollectionJobsCard({ collectionJobs }: CollectionJobsCardProps) {
  const [expandedJobIds, setExpandedJobIds] = useState<Set<string>>(new Set());

  function toggleJobDetails(jobId: string) {
    setExpandedJobIds((current) => {
      const next = new Set(current);
      if (next.has(jobId)) {
        next.delete(jobId);
      } else {
        next.add(jobId);
      }
      return next;
    });
  }

  return (
    <section className="admin-management-card collection-jobs-card">
      <header>
        <span className="admin-action-icon">
          <Database size={18} aria-hidden="true" />
        </span>
        <div>
          <h2>当前采集任务</h2>
          <p>展示后端保留的 OJ 采集任务，运行中任务会持续刷新状态。</p>
        </div>
      </header>
      {collectionJobs.length > 0 ? (
        <ul className="collection-job-list" aria-live="polite">
          {collectionJobs.map((job) => {
            const expanded = expandedJobIds.has(job.jobId);
            return (
              <li key={job.jobId}>
                <button
                  aria-expanded={expanded}
                  className="collection-job-row"
                  onClick={() => toggleJobDetails(job.jobId)}
                  type="button"
                >
                  <span>
                    <strong>{shortJobId(job.jobId)}</strong>
                    <small>{formatJobTime(job.startedAt)}</small>
                  </span>
                  <strong className={`result-status result-status-${job.status.toLowerCase().replace('_', '-')}`}>
                    {jobStatusLabel(job.status)}
                  </strong>
                  <small>
                    采集 {job.collectedCount}/{job.requestedCount}，失败 {job.failedCount}，刷新{' '}
                    {job.refreshedCount}，写入 {job.writtenRows} 行
                  </small>
                  <ChevronDown className={expanded ? 'is-expanded' : ''} size={16} aria-hidden="true" />
                </button>
                {expanded ? (
                  <div className="collection-job-detail">
                    <dl>
                      <div>
                        <dt>任务 ID</dt>
                        <dd>{job.jobId}</dd>
                      </div>
                      <div>
                        <dt>开始时间</dt>
                        <dd>{formatJobTime(job.startedAt)}</dd>
                      </div>
                      <div>
                        <dt>完成时间</dt>
                        <dd>{job.finishedAt ? formatJobTime(job.finishedAt) : '运行中'}</dd>
                      </div>
                      <div>
                        <dt>批次</dt>
                        <dd>{job.batchIds.length > 0 ? job.batchIds.join('、') : '暂无'}</dd>
                      </div>
                    </dl>
                    {job.items.length > 0 ? (
                      <CollectionResultTable
                        rows={job.items.map((item) => ({
                          studentIdentity: item.studentIdentity,
                          ojName: item.ojName,
                          status: collectStatusFromJobItem(item),
                          handle: item.handle,
                          batchId: item.batchId,
                          writtenRows: item.writtenRows,
                          fetchedSubmissionCount: item.fetchedSubmissionCount,
                          matchedSubmissionCount: item.matchedSubmissionCount,
                          message: item.message,
                          refreshStatus: item.refreshStatus,
                          refreshMessage: item.refreshMessage,
                        }))}
                      />
                    ) : (
                      <p className="batch-target-empty">任务还没有用户明细。</p>
                    )}
                  </div>
                ) : null}
              </li>
            );
          })}
        </ul>
      ) : (
        <p className="batch-target-empty" aria-live="polite">暂无采集任务。</p>
      )}
    </section>
  );
}

function CollectionResultTable({ rows }: { rows: CollectionResultTableRow[] }) {
  return (
    <div className="collection-result-table-scroll">
      <table className="collection-result-table" aria-label="数据采集结果">
        <thead>
          <tr>
            <th scope="col">队员 / handle</th>
            <th scope="col">状态</th>
            <th scope="col">写入</th>
            <th scope="col">匹配</th>
            <th scope="col">仓库</th>
            <th scope="col">批次</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={`${row.studentIdentity}-${row.ojName ?? 'ALL'}`}>
              <td data-label="队员 / handle">
                <strong>{row.studentIdentity}</strong>
                <small>
                  {row.ojName ?? '全部 OJ'}：{row.handle ?? row.message ?? collectStatusDescription(row.status)}
                </small>
              </td>
              <td data-label="状态">
                <strong className={`result-status result-status-${statusClassName(row.status)}`}>
                  {collectStatusLabel(row.status)}
                </strong>
              </td>
              <td data-label="写入">{row.writtenRows} 行</td>
              <td data-label="匹配">
                {row.matchedSubmissionCount}/{row.fetchedSubmissionCount}
              </td>
              <td data-label="仓库">
                <span className={`refresh-status refresh-status-${statusClassName(row.refreshStatus)}`}>
                  {refreshStatusLabel(row.refreshStatus)}
                </span>
                {row.refreshMessage ? <small>{row.refreshMessage}</small> : null}
              </td>
              <td data-label="批次">{row.batchId ?? '无批次'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function confirmHighCostAction(message: string) {
  if (typeof window === 'undefined' || typeof window.confirm !== 'function') {
    return true;
  }
  return window.confirm(message);
}

function parseLookbackHours(value: string) {
  const trimmed = value.trim();
  if (!trimmed) {
    return null;
  }
  const numericValue = Number(trimmed);
  if (!Number.isFinite(numericValue) || numericValue <= 0) {
    return null;
  }
  const flooredValue = Math.floor(numericValue);
  return flooredValue > 0 ? flooredValue : null;
}

function collectStatusLabel(status: BatchCollectSummary['results'][number]['status']) {
  switch (status) {
    case 'PENDING':
      return '等待中';
    case 'RUNNING':
      return '采集中';
    case 'SUCCESS':
      return '采集成功';
    case 'PARTIAL_SUCCESS':
      return '部分成功';
    case 'SKIPPED':
      return '已跳过';
    case 'FAILED':
      return '采集失败';
  }
}

function collectStatusDescription(status: BatchCollectSummary['results'][number]['status']) {
  switch (status) {
    case 'PENDING':
      return '等待采集';
    case 'RUNNING':
      return '正在采集';
    case 'SUCCESS':
    case 'PARTIAL_SUCCESS':
      return '已完成';
    case 'SKIPPED':
      return '已跳过';
    case 'FAILED':
      return '未解析 handle';
  }
}

function collectStatusFromJobItem(jobItem: CodeforcesSubmissionCollectionJobResponse['items'][number]) {
  if (jobItem.itemStatus === 'PENDING' || jobItem.itemStatus === 'RUNNING') {
    return jobItem.itemStatus;
  }
  return jobItem.collectionStatus ?? (jobItem.itemStatus === 'SUCCESS' ? 'SUCCESS' : 'FAILED');
}

function jobStatusLabel(status: CodeforcesSubmissionCollectionJobResponse['status']) {
  switch (status) {
    case 'RUNNING':
      return '正在执行';
    case 'SUCCESS':
      return '执行成功';
    case 'PARTIAL_SUCCESS':
      return '部分成功';
    case 'FAILED':
      return '执行失败';
  }
}

function shortJobId(jobId: string) {
  return jobId.length > 18 ? `${jobId.slice(0, 18)}...` : jobId;
}

function formatJobTime(value: string) {
  return formatTime(value);
}

function refreshStatusLabel(status: BatchCollectSummary['results'][number]['refreshStatus']) {
  switch (status) {
    case 'SUCCESS':
      return '仓库已刷新';
    case 'FAILED':
      return '仓库刷新失败';
    case 'NO_BATCH':
      return '无可刷新批次';
    case 'NOT_REQUESTED':
      return '未请求刷新';
  }
}

function statusClassName(status: string) {
  return status.toLowerCase().replaceAll('_', '-');
}

function formatTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date);
}
