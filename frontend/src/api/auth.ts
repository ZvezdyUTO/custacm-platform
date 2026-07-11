import type { CurrentUser } from '../types';
import { authHeaders, requestData } from './client';

function jsonHeaders(token?: string): HeadersInit {
  return {
    ...authHeaders(token),
    'Content-Type': 'application/json',
  };
}

export function login(
  username: string,
  password: string,
  signal?: AbortSignal,
): Promise<{ token: string; user: CurrentUser }> {
  return requestData('/login', {
    method: 'POST',
    headers: jsonHeaders(),
    body: JSON.stringify({ username, password }),
    signal,
  });
}

export function getCurrentUser(token: string, signal?: AbortSignal): Promise<CurrentUser> {
  return requestData('/player/me', {
    headers: authHeaders(token),
    signal,
  });
}

export function changeCurrentPassword(
  token: string,
  oldPassword: string,
  newPassword: string,
): Promise<void> {
  return requestData('/player/me/password', {
    method: 'PATCH',
    headers: jsonHeaders(token),
    body: JSON.stringify({ oldPassword, newPassword }),
  });
}
