export type TaskStatus = 'syncing' | 'pending' | 'failed' | 'completed' | 'disabled';
export type Priority = 'P0' | 'P1' | 'P2' | 'P3';
export type AccountRole = 'admin' | 'player' | 'disable';
export type DataSource = 'Auth' | 'ODS' | 'Codeforces' | 'AtCoder' | '系统';
export type DashboardView = 'all' | 'accounts' | 'codeforces' | 'ods-import' | 'system';
export type StudentIdentity = string;
export type WorkspaceView = 'query' | 'admin';
export const UNLIMITED_LOOKBACK_HOURS = 1_000_000_000;
export const OJ_NAMES = {
  CODEFORCES: 'CODEFORCES',
  ATCODER: 'ATCODER',
} as const;
export type OjName = typeof OJ_NAMES[keyof typeof OJ_NAMES];
export const OJ_LABELS: Record<OjName, string> = {
  [OJ_NAMES.CODEFORCES]: 'Codeforces',
  [OJ_NAMES.ATCODER]: 'AtCoder',
};
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

export interface PlatformModuleInfo {
  module: string;
  service: ServiceHealth['service'];
  features: string[];
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

export interface ChangeCurrentPasswordRequest {
  oldPassword: string;
  newPassword: string;
  confirmNewPassword: string;
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
  codeforcesHandle?: string;
  atcoderHandle?: string;
  handles?: Partial<Record<OjName, string>>;
}

export interface OjHandleOperationResult {
  success: boolean;
  studentIdentity: StudentIdentity;
  handle: string | null;
  handles: Partial<Record<OjName, string>>;
  needCollect?: boolean | null;
  errorCode: string | null;
  message: string | null;
}
export type CodeforcesHandleOperationResult = OjHandleOperationResult;

export interface BatchStudentImportSummary {
  userResults: AdminUserOperationResult[];
  handleResults: CodeforcesHandleOperationResult[];
}

export interface UserInfoUpdateInput {
  studentIdentity: StudentIdentity;
  newStudentIdentity?: StudentIdentity;
  role: AccountRole;
  newPassword?: string;
  handle?: string;
  handles?: Partial<Record<OjName, string>>;
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
  ojName?: OjName | null;
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

export type BatchCollectRefreshStatus = 'SUCCESS' | 'FAILED' | 'NOT_REQUESTED' | 'NO_BATCH';
export type BatchCollectStudentStatus = CodeforcesSubmissionCollectionResponse['status'] | 'PENDING' | 'RUNNING';

export interface BatchCollectStudentResult {
  studentIdentity: StudentIdentity;
  ojName: OjName | null;
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

export interface OjDataPurgeResult {
  ojName: OjName;
  handle: string;
  odsSubmissionRows: number;
  dwdSubmissionRows: number;
  dwmFirstAcceptedRows: number;
  dwsAcceptedSummaryRows: number;
  totalDeletedRows: number;
}

export interface OjStudentDataPurgeResult {
  studentIdentity: StudentIdentity;
  ojName: OjName | null;
  handle: string | null;
  handles: Partial<Record<OjName, string>>;
  ojResults: OjDataPurgeResult[];
  handleAccountRows: number;
  odsSubmissionRows: number;
  dwdSubmissionRows: number;
  dwmFirstAcceptedRows: number;
  dwsAcceptedSummaryRows: number;
  totalDeletedRows: number;
}

export interface FullUserDataDeleteSummary {
  trainingDataResult: OjStudentDataPurgeResult;
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

export interface OjHandleAccount {
  studentIdentity: StudentIdentity;
  handles: Partial<Record<OjName, string>>;
  needCollect: boolean;
  collectionStates?: Partial<Record<OjName, OjCollectionState>>;
}

export interface OjCollectionState {
  historyStartReached: boolean;
  lastCollectedAt: string | null;
}

export type OjHandleAccountMap = Record<StudentIdentity, OjHandleAccount>;

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

export type TrainingQueryMode = 'single' | 'multiple' | 'problem';

export interface SubmissionPageQuery {
  page: number;
  limit: number;
}

export interface CodeforcesSubmissionItem {
  submissionId?: string;
  codeforcesSubmissionId?: number;
  studentIdentity: StudentIdentity;
  handle?: string;
  authorHandle?: string;
  contestId?: number | null;
  submittedAtUtcPlus8: string | null;
  submittedDateUtcPlus8: string | null;
  relativeTimeSeconds?: number | null;
  problemKey: string | null;
  problemContestId?: number | null;
  problemIndex: string | null;
  problemName: string | null;
  problemType?: string | null;
  problemPoints?: number | null;
  problemRating?: number | null;
  problemTagsJson?: string | null;
  authorParticipantType?: string | null;
  difficulty?: string | null;
  language?: string | null;
  programmingLanguage?: string | null;
  verdict: string | null;
  accepted: boolean;
  testset?: string | null;
  passedTestCount?: number | null;
  timeConsumedMillis: number | null;
  memoryConsumedBytes?: number | null;
  sourceUrl?: string | null;
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

export interface CodeforcesProblemSubmissionReport {
  problemKey: string;
  page: number;
  limit: number;
  total: number;
  totalPages: number;
  hasMore: boolean;
  submissions: CodeforcesSubmissionItem[];
}

export interface CodeforcesFirstAcceptedProblem {
  problemKey: string;
  problemContestId?: number;
  problemIndex: string | null;
  problemName: string;
  problemType?: string | null;
  problemPoints?: number | null;
  problemRating?: number | null;
  problemTagsJson?: string | null;
  difficulty?: string | null;
  firstAcceptedSubmissionId: string | number;
  firstAcceptedAtUtcPlus8: string;
  firstAcceptedDateUtcPlus8: string;
  firstAcceptedLanguage: string | null;
  firstAcceptedSourceUrl?: string | null;
}

export interface CodeforcesFirstAcceptedReport {
  studentIdentity: StudentIdentity;
  authorHandle: string;
  totalAcceptedProblemCount: number;
  page: number;
  limit: number;
  total: number;
  totalPages: number;
  hasMore: boolean;
  problems: CodeforcesFirstAcceptedProblem[];
}

export interface CodeforcesProblemFirstAcceptedHandle {
  studentIdentity: StudentIdentity;
  handle: string;
  firstAcceptedAtUtcPlus8: string;
}

export interface CodeforcesProblemFirstAcceptedReport {
  problemKey: string;
  acceptedHandleCount: number;
  page: number;
  limit: number;
  total: number;
  totalPages: number;
  hasMore: boolean;
  acceptedHandles: CodeforcesProblemFirstAcceptedHandle[];
}

export interface TrainingDataBatchSummary {
  batchId: string;
  tableName: string;
  writtenRows: number;
  fetchedAt: string;
}

export interface CodeforcesSubmissionCollectionResponse {
  ojName: OjName;
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
  ojName: OjName | null;
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
  ojName: OjName | null;
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
  handles: Partial<Record<OjName, string>>;
  needCollect?: boolean | null;
  collectionStates?: Partial<Record<OjName, OjCollectionState>>;
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
