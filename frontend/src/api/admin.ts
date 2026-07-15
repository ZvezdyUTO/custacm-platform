import type {
  AdminUserCreateRequest,
  AdminUserMutationResponse,
  AdminUserUpdateRequest,
  AdminArticleListResponse,
  AdminCategory,
  AdminCategoryPage,
  AdminTagPage,
  CollectionJob,
  CollectionJobStartRequest,
  Competition,
  CompetitionAwardCreateRequest,
  CompetitionCreateRequest,
  CompetitionListQuery,
  CompetitionPageResponse,
  CompetitionParticipantsCreateRequest,
  HomepageFeaturedImage,
  HomepageFeaturedArticleCandidate,
  HomepageFeaturedGroup,
  HomepageFeaturedGroupUpsertRequest,
} from '../types';
import { authHeaders, requestData, requestDownload, type FileDownload } from './client';

function jsonHeaders(token: string): HeadersInit {
  return {
    ...authHeaders(token),
    'Content-Type': 'application/json',
  };
}

function jsonRequest<T>(
  token: string,
  path: string,
  method: 'PATCH' | 'POST' | 'PUT',
  body: unknown,
): Promise<T> {
  return requestData(path, {
    method,
    headers: jsonHeaders(token),
    body: JSON.stringify(body),
  });
}

function competitionListPath(path: string, query: CompetitionListQuery = {}) {
  const params = new URLSearchParams({
    pageNum: String(query.pageNum ?? 1),
    pageSize: String(query.pageSize ?? 15),
  });
  if (query.startYear !== null && query.startYear !== undefined) {
    params.set('startYear', String(query.startYear));
  }
  if (query.endYear !== null && query.endYear !== undefined) {
    params.set('endYear', String(query.endYear));
  }
  if (query.category) params.set('category', query.category);
  return `${path}?${params.toString()}`;
}

export function listAdminUsers(
  token: string,
  signal?: AbortSignal,
): Promise<AdminUserMutationResponse[]> {
  return requestData('/admin/users', { headers: authHeaders(token), signal });
}

export function batchCreateUsers(
  token: string,
  requests: AdminUserCreateRequest[],
): Promise<AdminUserMutationResponse[]> {
  return jsonRequest(token, '/admin/users:batch-create', 'POST', requests);
}

export function updateUser(
  token: string,
  username: string,
  request: AdminUserUpdateRequest,
): Promise<AdminUserMutationResponse> {
  return jsonRequest(token, `/admin/users/${encodeURIComponent(username)}`, 'PUT', request);
}

export function deleteUser(token: string, username: string): Promise<void> {
  return requestData(`/admin/users/${encodeURIComponent(username)}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  });
}

export function startCollectionJob(
  token: string,
  request: CollectionJobStartRequest,
): Promise<CollectionJob> {
  return jsonRequest(token, '/admin/training-data/submission-collection-jobs', 'POST', request);
}

export function listCollectionJobs(
  token: string,
  signal?: AbortSignal,
): Promise<CollectionJob[]> {
  return requestData('/admin/training-data/submission-collection-jobs', {
    headers: authHeaders(token),
    signal,
  });
}

export function getCollectionJob(
  token: string,
  jobId: string,
  signal?: AbortSignal,
): Promise<CollectionJob> {
  return requestData(
    `/admin/training-data/submission-collection-jobs/${encodeURIComponent(jobId)}`,
    { headers: authHeaders(token), signal },
  );
}

export function listHomepageFeaturedImages(token: string): Promise<HomepageFeaturedImage[]> {
  return requestData('/admin/homepage-featured-images', { headers: authHeaders(token) });
}

export function uploadHomepageFeaturedImage(
  token: string,
  image: Blob,
): Promise<HomepageFeaturedImage> {
  const formData = new FormData();
  formData.append('file', image, 'homepage-featured.jpg');
  return requestData('/admin/homepage-featured-images', {
    method: 'POST',
    headers: authHeaders(token),
    body: formData,
  });
}

export function reorderHomepageFeaturedImages(
  token: string,
  ids: number[],
): Promise<HomepageFeaturedImage[]> {
  return jsonRequest(token, '/admin/homepage-featured-images/order', 'PUT', { ids });
}

export function deleteHomepageFeaturedImage(
  token: string,
  id: number,
): Promise<HomepageFeaturedImage[]> {
  return requestData(`/admin/homepage-featured-images/${encodeURIComponent(id)}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  });
}

export function listHomepageFeaturedGroups(token: string): Promise<HomepageFeaturedGroup[]> {
  return requestData('/admin/homepage-featured-groups', { headers: authHeaders(token) });
}

export function searchHomepageFeaturedArticleCandidates(
  token: string,
  query = '',
): Promise<HomepageFeaturedArticleCandidate[]> {
  const params = new URLSearchParams({ query: query.trim() });
  return requestData(`/admin/homepage-featured-groups/candidates?${params.toString()}`, {
    headers: authHeaders(token),
  });
}

export function createHomepageFeaturedGroup(
  token: string,
  request: HomepageFeaturedGroupUpsertRequest,
): Promise<HomepageFeaturedGroup[]> {
  return jsonRequest(token, '/admin/homepage-featured-groups', 'POST', request);
}

export function updateHomepageFeaturedGroup(
  token: string,
  id: number,
  request: HomepageFeaturedGroupUpsertRequest,
): Promise<HomepageFeaturedGroup[]> {
  return jsonRequest(
    token,
    `/admin/homepage-featured-groups/${encodeURIComponent(id)}`,
    'PUT',
    request,
  );
}

export function reorderHomepageFeaturedGroups(
  token: string,
  ids: number[],
): Promise<HomepageFeaturedGroup[]> {
  return jsonRequest(token, '/admin/homepage-featured-groups/order', 'PUT', { ids });
}

export function deleteHomepageFeaturedGroup(
  token: string,
  id: number,
): Promise<HomepageFeaturedGroup[]> {
  return requestData(`/admin/homepage-featured-groups/${encodeURIComponent(id)}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  });
}

export function listCompetitions(
  token: string,
  query: CompetitionListQuery = {},
  signal?: AbortSignal,
): Promise<CompetitionPageResponse> {
  return requestData(competitionListPath('/admin/competitions', query), {
    headers: authHeaders(token),
    signal,
  });
}

export function listCompetitionRecycleBin(
  token: string,
  query: CompetitionListQuery = {},
  signal?: AbortSignal,
): Promise<CompetitionPageResponse> {
  return requestData(competitionListPath('/admin/competitions/recycle-bin', query), {
    headers: authHeaders(token),
    signal,
  });
}

export function createCompetition(
  token: string,
  request: CompetitionCreateRequest,
): Promise<Competition> {
  return jsonRequest(token, '/admin/competitions', 'POST', request);
}

export function addCompetitionParticipants(
  token: string,
  competitionId: number,
  request: CompetitionParticipantsCreateRequest,
): Promise<Competition> {
  return jsonRequest(
    token,
    `/admin/competitions/${encodeURIComponent(competitionId)}/participants`,
    'POST',
    request,
  );
}

export function deleteCompetitionParticipant(
  token: string,
  competitionId: number,
  participantId: number,
): Promise<void> {
  return requestData(
    `/admin/competitions/${encodeURIComponent(competitionId)}/participants/${encodeURIComponent(participantId)}`,
    { method: 'DELETE', headers: authHeaders(token) },
  );
}

export function addCompetitionAward(
  token: string,
  competitionId: number,
  request: CompetitionAwardCreateRequest,
): Promise<Competition> {
  return jsonRequest(
    token,
    `/admin/competitions/${encodeURIComponent(competitionId)}/awards`,
    'POST',
    request,
  );
}

export function deleteCompetitionAward(
  token: string,
  competitionId: number,
  awardId: number,
): Promise<void> {
  return requestData(
    `/admin/competitions/${encodeURIComponent(competitionId)}/awards/${encodeURIComponent(awardId)}`,
    { method: 'DELETE', headers: authHeaders(token) },
  );
}

export function updateCompetitionAwardLoginRequirement(
  token: string,
  competitionId: number,
  awardId: number,
  requiresLogin: boolean,
): Promise<Competition> {
  return jsonRequest(
    token,
    `/admin/competitions/${encodeURIComponent(competitionId)}/awards/${encodeURIComponent(awardId)}/login-requirement`,
    'PUT',
    { requiresLogin },
  );
}

export function moveCompetitionToRecycleBin(token: string, id: number): Promise<void> {
  return requestData(`/admin/competitions/${encodeURIComponent(id)}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  });
}

export function restoreCompetition(token: string, id: number): Promise<Competition> {
  return requestData(`/admin/competitions/${encodeURIComponent(id)}/restore`, {
    method: 'PUT',
    headers: authHeaders(token),
  });
}

export function listAdminArticles(
  token: string,
  query: { title?: string; categoryId?: number | null; pageNum?: number; pageSize?: number } = {},
): Promise<AdminArticleListResponse> {
  const params = new URLSearchParams({
    title: query.title || '',
    pageNum: String(query.pageNum || 1),
    pageSize: String(query.pageSize || 10),
  });
  if (query.categoryId) params.set('categoryId', String(query.categoryId));
  return requestData(`/admin/blogs?${params.toString()}`, { headers: authHeaders(token) });
}

export function listAdminRecycleBinArticles(
  token: string,
  query: { title?: string; categoryId?: number | null; pageNum?: number; pageSize?: number } = {},
): Promise<AdminArticleListResponse> {
  const params = new URLSearchParams({
    title: query.title || '',
    pageNum: String(query.pageNum || 1),
    pageSize: String(query.pageSize || 10),
  });
  if (query.categoryId) params.set('categoryId', String(query.categoryId));
  return requestData(`/admin/blogs/recycle-bin?${params.toString()}`, { headers: authHeaders(token) });
}

export function downloadAllArticlesBackup(token: string): Promise<FileDownload> {
  return requestDownload('/admin/blogs/backup', 'custacm-article-backup.zip', {
    headers: { ...authHeaders(token), Accept: 'application/zip' },
  });
}

export function deleteArticle(token: string, id: number): Promise<void> {
  const params = new URLSearchParams({ id: String(id) });
  return requestData(`/admin/blog?${params.toString()}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  });
}

export function restoreArticle(token: string, id: number): Promise<void> {
  const params = new URLSearchParams({ id: String(id) });
  return requestData(`/admin/blog/restore?${params.toString()}`, {
    method: 'PUT',
    headers: authHeaders(token),
  });
}

export function listAdminCategories(
  token: string,
  pageNum = 1,
  pageSize = 10,
): Promise<AdminCategoryPage> {
  const params = new URLSearchParams({ pageNum: String(pageNum), pageSize: String(pageSize) });
  return requestData(`/admin/categories?${params.toString()}`, { headers: authHeaders(token) });
}

export function createCategory(token: string, name: string, color = '#8B1E3F'): Promise<void> {
  return jsonRequest(token, '/admin/category', 'POST', { name, color });
}

export function listAdminTags(token: string, pageNum = 1, pageSize = 10): Promise<AdminTagPage> {
  const params = new URLSearchParams({ pageNum: String(pageNum), pageSize: String(pageSize) });
  return requestData(`/admin/tags?${params.toString()}`, { headers: authHeaders(token) });
}

export function createTag(token: string, name: string): Promise<void> {
  return jsonRequest(token, '/admin/tag', 'POST', { name });
}

export function deleteTag(token: string, id: number): Promise<void> {
  return requestData(`/admin/tag?id=${encodeURIComponent(id)}`, { method: 'DELETE', headers: authHeaders(token) });
}

export function updateCategory(token: string, category: AdminCategory): Promise<void> {
  return jsonRequest(token, '/admin/category', 'PUT', category);
}

export function deleteCategory(token: string, id: number): Promise<void> {
  const params = new URLSearchParams({ id: String(id) });
  return requestData(`/admin/category?${params.toString()}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  });
}
