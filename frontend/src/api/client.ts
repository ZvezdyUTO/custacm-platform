export interface BlogResult<T> {
  code: number;
  errorCode: string | null;
  msg: string;
  data: T;
}

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly errorCode: string | null,
    message: string,
    readonly body: unknown,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

const API_BASE = import.meta.env.VITE_API_BASE ?? '/api';

export function authHeaders(token?: string): HeadersInit {
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function requestData<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json');
  }
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers,
  });
  const body = await response.json().catch(() => null) as BlogResult<T> | null;
  if (!response.ok || !body || body.code !== 200) {
    throw new ApiError(
      response.status,
      body?.errorCode ?? null,
      body?.msg || `HTTP ${response.status}`,
      body,
    );
  }
  return body.data;
}
