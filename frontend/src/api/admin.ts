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
  HomepageBannerImage,
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

export function listHomepageBanners(token: string): Promise<HomepageBannerImage[]> {
  return requestData('/admin/homepage-banners', { headers: authHeaders(token) });
}

export function uploadHomepageBanner(token: string, image: Blob): Promise<HomepageBannerImage> {
  const formData = new FormData();
  formData.append('file', image, 'homepage-banner.jpg');
  return requestData('/admin/homepage-banners', {
    method: 'POST',
    headers: authHeaders(token),
    body: formData,
  });
}

export function reorderHomepageBanners(token: string, ids: number[]): Promise<HomepageBannerImage[]> {
  return jsonRequest(token, '/admin/homepage-banners/order', 'PUT', { ids });
}

export function deleteHomepageBanner(token: string, id: number): Promise<HomepageBannerImage[]> {
  return requestData(`/admin/homepage-banners/${encodeURIComponent(id)}`, {
    method: 'DELETE',
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

export function updateArticleFeatured(token: string, id: number, featured: boolean): Promise<void> {
  const params = new URLSearchParams({ id: String(id), recommend: String(featured) });
  return requestData(`/admin/blog/recommend?${params.toString()}`, {
    method: 'PUT',
    headers: authHeaders(token),
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
