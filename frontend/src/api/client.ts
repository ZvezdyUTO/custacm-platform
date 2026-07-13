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
    readonly retryAfterSeconds: number | null = null,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export interface FileDownload {
  blob: Blob;
  filename: string;
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
      parseRetryAfter(response.headers.get('Retry-After')),
    );
  }
  return body.data;
}

function parseRetryAfter(value: string | null): number | null {
  if (!value) return null;
  const seconds = Number.parseInt(value, 10);
  return Number.isFinite(seconds) && seconds > 0 ? seconds : null;
}

export async function requestDownload(
  path: string,
  fallbackFilename: string,
  init: RequestInit = {},
): Promise<FileDownload> {
  const headers = new Headers(init.headers);
  if (!headers.has('Accept')) headers.set('Accept', 'application/octet-stream');
  const response = await fetch(`${API_BASE}${path}`, { ...init, headers });
  if (!response.ok) {
    const body = await response.json().catch(() => null) as BlogResult<unknown> | null;
    throw new ApiError(
      response.status,
      body?.errorCode ?? null,
      body?.msg || `HTTP ${response.status}`,
      body,
    );
  }
  return {
    blob: await response.blob(),
    filename: responseFilename(response.headers.get('Content-Disposition')) || fallbackFilename,
  };
}

function responseFilename(contentDisposition: string | null): string | null {
  if (!contentDisposition) return null;
  const encoded = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
  if (encoded) {
    try {
      return decodeURIComponent(encoded.replace(/^"|"$/g, ''));
    } catch {
      // Fall through to the ASCII filename when the upstream header is malformed.
    }
  }
  return contentDisposition.match(/filename="?([^";]+)"?/i)?.[1]?.trim() || null;
}
