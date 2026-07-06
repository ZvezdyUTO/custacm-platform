export type TaskStatus = 'syncing' | 'pending' | 'failed' | 'completed' | 'disabled';
export type Priority = 'P0' | 'P1' | 'P2' | 'P3';
export type AccountRole = 'admin' | 'player' | 'disable';
export type DataSource = 'Auth' | 'ODS' | 'Codeforces' | '系统';
export type DashboardView = 'all' | 'accounts' | 'codeforces' | 'ods-import' | 'system';
export type StudentIdentity = string;
export type WorkspaceView = 'query' | 'admin';
export const UNLIMITED_LOOKBACK_HOURS = 1_000_000_000;
export type IconKey =
  | 'activity'
  | 'bar-chart'
  | 'book-open'
  | 'clipboard-list'
  | 'database'
  | 'file-clock'
  | 'key-round'
  | 'layout-dashboard'
  | 'list-checks'
  | 'refresh'
  | 'shield-check'
  | 'trophy'
  | 'user-check'
  | 'users';

export interface Metric {
  id: string;
  label: string;
  value: string;
  delta: string;
  tone: 'blue' | 'green' | 'violet' | 'amber' | 'red' | 'slate';
  iconKey: IconKey;
}

export interface Owner {
  name: string;
  role: AccountRole;
  avatar: string;
}

export interface DashboardTask {
  id: string;
  title: string;
  module: DashboardView;
  status: TaskStatus;
  priority: Priority;
  owner: Owner;
  subjectLabel: string;
  studentIdentity?: StudentIdentity;
  source: DataSource;
  updatedAt: string;
  action: string;
  detail: string;
}

export interface DashboardMeta {
  updatedAt: string;
  totalTasks: number;
  pageSize: number;
}

export interface OperationsStatus {
  id: string;
  title: string;
  detail: string;
  tone: 'blue' | 'green' | 'amber' | 'red';
}

export interface Filters {
  query: string;
  status: 'all' | TaskStatus;
  priority: 'all' | Priority;
  role: 'all' | AccountRole;
  source: 'all' | DataSource;
  view: DashboardView;
}

export interface TimelineItem {
  id: string;
  title: string;
  meta: string;
  status: TaskStatus;
  time: string;
}

export interface AlertItem {
  id: string;
  title: string;
  detail: string;
  severity: 'error' | 'warning';
  time: string;
}

export interface PermissionSummary {
  total: string;
  segments: Array<{
    id: 'ok' | 'pending' | 'danger' | 'muted';
    label: string;
    value: string;
  }>;
}

export interface ServiceHealth {
  service: 'auth-web' | 'training-data-web';
  status: 'UP' | 'DOWN' | 'UNKNOWN';
  detail: string;
}

export interface AuthUser {
  studentIdentity: StudentIdentity;
  role: AccountRole;
  createdAt: string;
  updatedAt: string;
}

export interface AdminUserCreateRequest {
  studentIdentity: StudentIdentity;
  role: AccountRole;
  password?: string;
}

export interface AdminUserUpdateRequest {
  role?: AccountRole;
  newPassword?: string;
}

export interface AdminUserOperationResult {
  success: boolean;
  studentIdentity: StudentIdentity;
  user: AuthUser | null;
  plainPassword: string | null;
  errorCode: string | null;
  message: string | null;
}

export interface BatchStudentImportRow extends AdminUserCreateRequest {
  handle?: string;
}

export interface CodeforcesHandleOperationResult {
  success: boolean;
  studentIdentity: StudentIdentity;
  handle: string | null;
  needCollect?: boolean | null;
  errorCode: string | null;
  message: string | null;
}

export interface BatchStudentImportSummary {
  userResults: AdminUserOperationResult[];
  handleResults: CodeforcesHandleOperationResult[];
}

export interface UserInfoUpdateInput {
  studentIdentity: StudentIdentity;
  role: AccountRole;
  newPassword?: string;
  handle?: string;
  needCollect?: boolean;
}

export interface UserInfoUpdateSummary {
  userResult: AdminUserOperationResult;
  handleResult: CodeforcesHandleOperationResult | null;
}

export interface BatchCollectOptions {
  studentIdentities: StudentIdentity[];
  lookbackHours: number;
  refreshWarehouse: boolean;
}

export interface BatchCollectSummary {
  requestedCount: number;
  collectedCount: number;
  failedCount: number;
  refreshedCount: number;
  writtenRows: number;
  batchIds: string[];
  results: BatchCollectStudentResult[];
}

export type BatchCollectRefreshStatus = WarehouseRefreshResult['status'] | 'NOT_REQUESTED' | 'NO_BATCH';
export type BatchCollectStudentStatus = CodeforcesSubmissionCollectionResponse['status'] | 'PENDING' | 'RUNNING';

export interface BatchCollectStudentResult {
  studentIdentity: StudentIdentity;
  status: BatchCollectStudentStatus;
  handle: string | null;
  batchId: string | null;
  writtenRows: number;
  fetchedSubmissionCount: number;
  matchedSubmissionCount: number;
  message: string | null;
  refreshStatus: BatchCollectRefreshStatus;
  refreshMessage: string | null;
}

export interface CodeforcesStudentDataPurgeResult {
  studentIdentity: StudentIdentity;
  handle: string | null;
  handleAccountRows: number;
  odsSubmissionRows: number;
  dwdSubmissionRows: number;
  dwmFirstAcceptedRows: number;
  dwsAcceptedSummaryRows: number;
  totalDeletedRows: number;
}

export interface FullUserDataDeleteSummary {
  trainingDataResult: CodeforcesStudentDataPurgeResult;
  authUserResult: AdminUserOperationResult;
}

export interface CurrentUser {
  studentIdentity: StudentIdentity;
  role: Exclude<AccountRole, 'disable'>;
}

export interface LoginResponse {
  tokenType: 'Bearer';
  accessToken: string;
  expiresInSeconds: number;
  user: CurrentUser;
}

export interface CodeforcesHandleAccount {
  studentIdentity: StudentIdentity;
  handle: string;
  needCollect: boolean;
}

export interface CodeforcesAcceptedSummary {
  studentIdentity: StudentIdentity;
  authorHandle: string;
  totalAcceptedProblemCount: number;
  ratingCounts: Array<{
    problemRating: string;
    acceptedProblemCount: number;
  }>;
}

export interface TrainingQueryRange {
  acceptedFromDateUtcPlus8: string;
  acceptedToDateUtcPlus8: string;
  minProblemRating: string;
  maxProblemRating: string;
}

export type TrainingQueryMode = 'single' | 'multiple';

export interface SubmissionPageQuery {
  page: number;
  limit: number;
}

export interface CodeforcesSubmissionItem {
  codeforcesSubmissionId: number;
  studentIdentity: StudentIdentity;
  authorHandle: string;
  contestId: number | null;
  submittedAtUtcPlus8: string | null;
  submittedDateUtcPlus8: string | null;
  relativeTimeSeconds: number | null;
  problemKey: string | null;
  problemContestId: number | null;
  problemIndex: string | null;
  problemName: string | null;
  problemType: string | null;
  problemPoints: number | null;
  problemRating: number | null;
  problemTagsJson: string | null;
  authorParticipantType: string | null;
  programmingLanguage: string | null;
  verdict: string | null;
  accepted: boolean;
  testset: string | null;
  passedTestCount: number | null;
  timeConsumedMillis: number | null;
  memoryConsumedBytes: number | null;
}

export interface CodeforcesStudentSubmissionReport {
  studentIdentity: StudentIdentity;
  authorHandle: string;
  page: number;
  limit: number;
  total: number;
  totalPages: number;
  hasMore: boolean;
  submissions: CodeforcesSubmissionItem[];
}

export interface CodeforcesFirstAcceptedProblem {
  problemKey: string;
  problemContestId: number;
  problemIndex: string;
  problemName: string;
  problemType: string | null;
  problemPoints: number | null;
  problemRating: number | null;
  problemTagsJson: string | null;
  firstAcceptedSubmissionId: number;
  firstAcceptedAtUtcPlus8: string;
  firstAcceptedDateUtcPlus8: string;
  firstAcceptedLanguage: string | null;
}

export interface CodeforcesFirstAcceptedReport {
  studentIdentity: StudentIdentity;
  authorHandle: string;
  totalAcceptedProblemCount: number;
  problems: CodeforcesFirstAcceptedProblem[];
}

export interface CodeforcesOdsBatchUpsertResponse {
  batchId: string;
  tableName: string;
  writtenRows: number;
  fetchedAt: string;
}

export interface SqlTaskNodeResult {
  taskId: string;
  description: string;
  sqlLocation: string;
  status: 'SUCCESS' | 'FAILED' | 'SKIPPED';
  startedAt: string | null;
  finishedAt: string | null;
  durationMillis: number | null;
  affectedRows: number | null;
  errorCode: string | null;
  message: string | null;
}

export interface WarehouseRefreshResult {
  runId: string;
  status: 'SUCCESS' | 'FAILED' | 'SKIPPED';
  manifestLocation: string;
  startFromTaskId: string | null;
  failedTaskId: string | null;
  startedAt: string;
  finishedAt: string | null;
  durationMillis: number | null;
  tasks: SqlTaskNodeResult[];
}

export interface CodeforcesSubmissionCollectionResponse {
  status: 'SUCCESS' | 'PARTIAL_SUCCESS' | 'FAILED' | 'SKIPPED';
  windowStartInclusive: string;
  windowEndExclusive: string;
  requestedHandleCount: number;
  succeededHandleCount: number;
  failedHandleCount: number;
  fetchedSubmissionCount: number;
  matchedSubmissionCount: number;
  batchId: string | null;
  tableName: string | null;
  writtenRows: number;
  fetchedAt: string | null;
  message: string | null;
  handles: Array<{
    handle: string;
    status: 'SUCCESS' | 'FAILED';
    fetchedSubmissionCount: number;
    matchedSubmissionCount: number;
    errorCode: string | null;
    message: string | null;
  }>;
}

export type CodeforcesSubmissionCollectionJobStatus =
  | 'RUNNING'
  | 'SUCCESS'
  | 'PARTIAL_SUCCESS'
  | 'FAILED';

export type CodeforcesSubmissionCollectionJobItemStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED';
export type CodeforcesSubmissionCollectionJobRefreshStatus = BatchCollectRefreshStatus;

export interface CodeforcesSubmissionCollectionJobItem {
  studentIdentity: StudentIdentity;
  itemStatus: CodeforcesSubmissionCollectionJobItemStatus;
  collectionStatus: CodeforcesSubmissionCollectionResponse['status'] | null;
  handle: string | null;
  batchId: string | null;
  tableName: string | null;
  writtenRows: number;
  fetchedSubmissionCount: number;
  matchedSubmissionCount: number;
  fetchedAt: string | null;
  message: string | null;
  refreshStatus: CodeforcesSubmissionCollectionJobRefreshStatus;
  refreshMessage: string | null;
}

export interface CodeforcesSubmissionCollectionJobResponse {
  jobId: string;
  status: CodeforcesSubmissionCollectionJobStatus;
  requestedCount: number;
  completedCount: number;
  collectedCount: number;
  failedCount: number;
  refreshedCount: number;
  writtenRows: number;
  batchIds: string[];
  startedAt: string;
  finishedAt: string | null;
  message: string | null;
  items: CodeforcesSubmissionCollectionJobItem[];
}

export interface StudentTrainingRecord {
  studentIdentity: StudentIdentity;
  role: AccountRole;
  handle: string | null;
  needCollect?: boolean | null;
  handleStatus: 'bound' | 'missing' | 'error';
  acceptedSummary: CodeforcesAcceptedSummary | null;
  summaryStatus: 'loaded' | 'missing' | 'error' | 'not-requested';
  errorMessage: string | null;
  updatedAt: string;
}

export interface DashboardOperation {
  id: string;
  title: string;
  detail: string;
  status: TaskStatus;
  time: string;
}
