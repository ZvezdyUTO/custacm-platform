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

export interface HomepageFeaturedImage {
  id: number;
  imageUrl: string;
  thumbnailUrl: string;
  sortOrder: number;
}

export interface HomepageFeaturedArticle {
  id: number;
  title: string;
  description: string;
  firstPicture: string;
  createTime: string;
  categoryName: string | null;
  authorUsername: string | null;
  authorNickname: string;
  authorAvatar: string | null;
  sortOrder: number;
  available: boolean;
}

export interface HomepageFeaturedGroup {
  id: number;
  title: string;
  sortOrder: number;
  complete: boolean;
  articles: HomepageFeaturedArticle[];
}

export interface HomepageFeaturedArticleCandidate extends Omit<HomepageFeaturedArticle, 'sortOrder'> {
  sortOrder: number | null;
  featuredGroupId: number | null;
}

export interface HomepageFeaturedGroupUpsertRequest {
  title: string;
  articleIds: number[];
}

export const COMPETITION_CATEGORY_GROUPS = [
  {
    label: '省级赛事',
    options: [{ code: 'PROVINCIAL', label: '省赛' }],
  },
  {
    label: '全国邀请赛',
    options: [
      { code: 'ICPC_NATIONAL_INVITATIONAL', label: 'ICPC 全国邀请赛' },
      { code: 'CCPC_NATIONAL_INVITATIONAL', label: 'CCPC 全国邀请赛' },
    ],
  },
  {
    label: '区域赛事',
    options: [
      { code: 'ICPC_ASIA_REGIONAL', label: 'ICPC 亚洲区域赛' },
      { code: 'CCPC_REGIONAL', label: 'CCPC 区域赛' },
    ],
  },
  {
    label: '总决赛',
    options: [
      { code: 'EC_FINAL', label: 'EC-Final' },
      { code: 'CCPC_FINAL', label: 'CCPC-Final' },
    ],
  },
  {
    label: '专项赛事',
    options: [
      { code: 'BAIDU_STAR', label: '百度之星' },
      { code: 'GPLT_NATIONAL', label: 'GPLT 团体程序设计天梯赛（国赛）' },
      { code: 'LANQIAO_CUP_NATIONAL', label: '蓝桥杯程序设计竞赛（国奖）' },
    ],
  },
] as const;

export type CompetitionCategory = typeof COMPETITION_CATEGORY_GROUPS[number]['options'][number]['code'];

export const MEDAL_AWARD_TIER_OPTIONS = [
  { code: 'MEDAL_GOLD', label: '金牌' },
  { code: 'MEDAL_SILVER', label: '银牌' },
  { code: 'MEDAL_BRONZE', label: '铜牌' },
  { code: 'MEDAL_HONORABLE_MENTION', label: '优胜奖' },
] as const;

export const BAIDU_AWARD_TIER_OPTIONS = [
  { code: 'BAIDU_NATIONAL_FIRST', label: '国赛一等奖' },
  { code: 'BAIDU_NATIONAL_SECOND', label: '国赛二等奖' },
  { code: 'BAIDU_NATIONAL_THIRD', label: '国赛三等奖' },
  { code: 'BAIDU_NATIONAL_FOURTH', label: '国赛四等奖' },
  { code: 'BAIDU_PROVINCIAL_FIRST', label: '省赛一等奖' },
  { code: 'BAIDU_PROVINCIAL_SECOND', label: '省赛二等奖' },
  { code: 'BAIDU_PROVINCIAL_THIRD', label: '省赛三等奖' },
] as const;

export const PRIZE_AWARD_TIER_OPTIONS = [
  { code: 'FIRST_PRIZE', label: '一等奖' },
  { code: 'SECOND_PRIZE', label: '二等奖' },
  { code: 'THIRD_PRIZE', label: '三等奖' },
] as const;

export type CompetitionAwardTier =
  | typeof MEDAL_AWARD_TIER_OPTIONS[number]['code']
  | typeof BAIDU_AWARD_TIER_OPTIONS[number]['code']
  | typeof PRIZE_AWARD_TIER_OPTIONS[number]['code'];
export type CompetitionParticipationMode = 'INDIVIDUAL' | 'TEAM' | 'MIXED';
export type CompetitionAwardMode = 'INDIVIDUAL' | 'TEAM';

export interface CompetitionTypeTag {
  code: string;
  label: string;
}

export interface CompetitionParticipant {
  id: number;
  username: Username | null;
  displayName: string;
  articles: Array<{ id: number; title: string }>;
}

export interface CompetitionAwardRecipient {
  participantId: number;
  username: Username | null;
  displayName: string;
}

export interface CompetitionAward {
  id: number;
  awardMode: CompetitionAwardMode;
  awardModeLabel: string;
  teamName: string | null;
  awardTier: CompetitionAwardTier | null;
  awardTierLabel: string | null;
  rankPosition: number | null;
  rankTotal: number | null;
  rank: string | null;
  requiresLogin: boolean;
  recipients: CompetitionAwardRecipient[];
}

export interface Competition {
  id: number;
  fullName: string;
  competitionDate: string | null;
  year: number | null;
  category: CompetitionCategory | null;
  categoryLabel: string | null;
  participationMode: CompetitionParticipationMode;
  participationModeLabel: string;
  types: CompetitionTypeTag[];
  createTime: string;
  deletedAt: string | null;
  participants: CompetitionParticipant[];
  awards: CompetitionAward[];
}

export interface CompetitionPageResponse {
  pageNum: number;
  pageSize: number;
  total: number;
  totalPages: number;
  list: Competition[];
}

export interface CompetitionListQuery {
  startYear?: number | null;
  endYear?: number | null;
  category?: CompetitionCategory | null;
  pageNum?: number;
  pageSize?: number;
}

export interface CompetitionCreateRequest {
  fullName: string;
  competitionDate: string | null;
  category: CompetitionCategory;
  participationMode: CompetitionParticipationMode;
}

export interface CompetitionParticipantsCreateRequest {
  usernames: Username[];
}

export interface CompetitionAwardCreateRequest {
  awardMode: CompetitionAwardMode;
  teamName: string | null;
  awardTier: CompetitionAwardTier;
  rankPosition: number | null;
  rankTotal: number | null;
  requiresLogin: boolean;
  recipientUsernames: Username[];
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
