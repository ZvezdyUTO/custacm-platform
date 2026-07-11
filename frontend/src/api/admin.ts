import type {
  AdminUserCreateRequest,
  AdminUserMutationResponse,
  AdminUserPatchRequest,
  CollectionJob,
  CollectionJobStartRequest,
  HomepageBannerImage,
  OjHandlesUpdateRequest,
  OjName,
  WarehouseRefreshRequest,
  WarehouseRefreshResult,
} from '../types';
import { authHeaders, requestData } from './client';

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

export function createUser(
  token: string,
  request: AdminUserCreateRequest,
): Promise<AdminUserMutationResponse> {
  return jsonRequest(token, '/admin/users', 'POST', request);
}

export function batchCreateUsers(
  token: string,
  requests: AdminUserCreateRequest[],
): Promise<AdminUserMutationResponse[]> {
  return jsonRequest(token, '/admin/users:batch-create', 'POST', requests);
}

export function patchUser(
  token: string,
  username: string,
  request: AdminUserPatchRequest,
): Promise<AdminUserMutationResponse> {
  return jsonRequest(token, `/admin/users/${encodeURIComponent(username)}`, 'PATCH', request);
}

export function updateOjHandles(
  token: string,
  username: string,
  request: OjHandlesUpdateRequest,
): Promise<AdminUserMutationResponse> {
  return jsonRequest(
    token,
    `/admin/users/${encodeURIComponent(username)}/oj-handles`,
    'PUT',
    request,
  );
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

export function refreshWarehouse(
  token: string,
  ojName: OjName,
  request: WarehouseRefreshRequest,
): Promise<WarehouseRefreshResult> {
  return jsonRequest(
    token,
    `/admin/training-data/${encodeURIComponent(ojName)}/warehouse:refresh`,
    'POST',
    request,
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
