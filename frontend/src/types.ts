export type Username = string;
export type AccountRole = 'ROLE_admin' | 'ROLE_player';

export const OJ_NAMES = {
  CODEFORCES: 'CODEFORCES',
  ATCODER: 'ATCODER',
} as const;

export type OjName = typeof OJ_NAMES[keyof typeof OJ_NAMES];

export const OJ_LABELS: Record<OjName, string> = {
  [OJ_NAMES.CODEFORCES]: 'Codeforces',
  [OJ_NAMES.ATCODER]: 'AtCoder',
};

export interface CurrentUser {
  username: Username;
  nickname: string;
  avatar: string;
  email: string;
  role: AccountRole;
}

export interface TrainingUser {
  username: Username;
  nickname: string;
  ojNames: OjName[];
}

export interface AdminUserMutationResponse {
  user: CurrentUser & { id: number; createTime: string; updateTime: string };
  handles: Partial<Record<OjName, string>>;
  needCollect: boolean | null;
  collectionStates: Partial<Record<OjName, {
    lastCollectedAt: string | null;
  }>>;
  generatedPassword: string | null;
  reloginRequired: boolean;
}

export interface AdminUserCreateRequest {
  username: Username;
  password?: string | null;
  nickname?: string | null;
  email?: string | null;
  role: AccountRole;
  handles?: Partial<Record<OjName, string>> | null;
  needCollect?: boolean | null;
}

export interface AdminUserUpdateRequest {
  newUsername: Username;
  nickname: string;
  email: string;
  role: AccountRole;
  password?: string | null;
  handles?: Partial<Record<OjName, string>> | null;
  needCollect?: boolean | null;
}

export interface CollectionJobStartRequest {
  usernames: Username[];
  lookbackHours: number;
  refreshWarehouse: boolean;
  ojName: OjName | null;
}

export type CollectionJobStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'PARTIAL_SUCCESS' | 'FAILED';
export type CollectionJobItemStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED';
export type CollectionStatus = 'SUCCESS' | 'PARTIAL_SUCCESS' | 'FAILED' | 'SKIPPED';
export type CollectionRefreshStatus = 'NOT_REQUESTED' | 'NO_BATCH' | 'SUCCESS' | 'FAILED';

export interface CollectionJobItem {
  username: Username;
  ojName: OjName | null;
  itemStatus: CollectionJobItemStatus;
  collectionStatus: CollectionStatus | null;
  handle: string | null;
  batchId: string | null;
  tableName: string | null;
  writtenRows: number;
  fetchedSubmissionCount: number;
  matchedSubmissionCount: number;
  fetchedAt: string | null;
  message: string | null;
  refreshStatus: CollectionRefreshStatus;
  refreshMessage: string | null;
}

export interface CollectionJob {
  jobId: string;
  ojName: OjName | null;
  status: CollectionJobStatus;
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
  items: CollectionJobItem[];
}

export type WarehouseRefreshStatus = 'SUCCESS' | 'FAILED';
export type WarehouseRefreshTaskStatus = 'SUCCESS' | 'FAILED' | 'SKIPPED';

export interface WarehouseRefreshTaskResult {
  taskId: string;
  description: string;
  sqlLocation: string;
  status: WarehouseRefreshTaskStatus;
  startedAt: string | null;
  finishedAt: string | null;
  durationMillis: number;
  affectedRows: number;
  errorCode: string | null;
  message: string | null;
}

export interface WarehouseRefreshResult {
  runId: string;
  status: WarehouseRefreshStatus;
  manifestLocation: string;
  startFromTaskId: string | null;
  failedTaskId: string | null;
  startedAt: string;
  finishedAt: string;
  durationMillis: number;
  tasks: WarehouseRefreshTaskResult[];
}

export interface HomepageBannerImage {
  id: number;
  imageUrl: string;
  sortOrder: number;
}

export interface AdminArticle {
  id: number;
  title: string;
  firstPicture: string;
  createTime: string;
  updateTime: string;
	deletedAt?: string;
  published: boolean;
  recommend: boolean;
  top: boolean;
  category: { id: number; name: string } | null;
	user?: { username: string; nickname?: string | null } | null;
}

export interface AdminArticlePage {
  list: AdminArticle[];
  pageNum: number;
  pageSize: number;
  pages: number;
  total: number;
}

export interface AdminArticleListResponse {
  blogs: AdminArticlePage;
  categories: Array<{ id: number; name: string }>;
}

export interface AdminCategory {
  id: number;
  name: string;
  color?: string;
}

export interface AdminCategoryPage {
  list: AdminCategory[];
  pageNum: number;
  pageSize: number;
  pages: number;
  total: number;
}

export type AdminTag = AdminCategory;
export type AdminTagPage = AdminCategoryPage;

export interface TrainingQueryRange {
  acceptedFromDateUtcPlus8: string;
  acceptedToDateUtcPlus8: string;
  minProblemRating: string;
  maxProblemRating: string;
}

export interface PageQuery {
  page: number;
  limit: number;
}

export type SubmissionPageQuery = PageQuery;

export interface AcceptedSummary {
  username: Username;
  authorHandle: string;
  totalAcceptedProblemCount: number;
  ratingCounts: Array<{
    problemRating: string;
    acceptedProblemCount: number;
  }>;
}

export interface SubmissionItem {
  submissionId: string;
  username: Username;
  handle: string;
  submittedAtUtcPlus8: string | null;
  submittedDateUtcPlus8: string | null;
  problemKey: string | null;
  problemIndex: string | null;
  problemName: string | null;
  difficulty: string | null;
  language: string | null;
  verdict: string | null;
  accepted: boolean;
  timeConsumedMillis: number | null;
  sourceUrl: string | null;
}

export interface UserSubmissionReport {
  username: Username;
  authorHandle: string;
  page: number;
  limit: number;
  total: number;
  totalPages: number;
  hasMore: boolean;
  submissions: SubmissionItem[];
}

export interface ProblemSubmissionReport {
  problemKey: string;
  page: number;
  limit: number;
  total: number;
  totalPages: number;
  hasMore: boolean;
  submissions: SubmissionItem[];
}

export interface FirstAcceptedProblem {
  problemKey: string;
  problemIndex: string | null;
  problemName: string | null;
  difficulty: string | null;
  firstAcceptedSubmissionId: string;
  firstAcceptedAtUtcPlus8: string;
  firstAcceptedDateUtcPlus8: string;
  firstAcceptedLanguage: string | null;
  firstAcceptedSourceUrl: string | null;
}

export interface UserFirstAcceptedReport {
  username: Username;
  authorHandle: string;
  totalAcceptedProblemCount: number;
  page: number;
  limit: number;
  total: number;
  totalPages: number;
  hasMore: boolean;
  problems: FirstAcceptedProblem[];
}

export interface ProblemFirstAcceptedHandle {
  username: Username;
  handle: string;
  firstAcceptedAtUtcPlus8: string;
}

export interface ProblemFirstAcceptedReport {
  problemKey: string;
  acceptedHandleCount: number;
  page: number;
  limit: number;
  total: number;
  totalPages: number;
  hasMore: boolean;
  acceptedHandles: ProblemFirstAcceptedHandle[];
}

export type TrainingQueryMode = 'single' | 'multiple' | 'problem';
