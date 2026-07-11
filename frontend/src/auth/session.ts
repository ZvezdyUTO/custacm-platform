import type { CurrentUser } from '../types';

const TOKEN_KEY = 'custacm.accessToken';
const USER_KEY = 'custacm.user';

type StoredUser = Pick<CurrentUser, 'username' | 'nickname' | 'avatar' | 'role'>;

function storedUserOf(user: StoredUser): StoredUser {
  return {
    username: user.username,
    nickname: user.nickname,
    avatar: user.avatar,
    role: user.role,
  };
}

function isStoredUser(value: unknown): value is StoredUser {
  if (typeof value !== 'object' || value === null) return false;
  const user = value as Record<string, unknown>;
  return typeof user.username === 'string'
    && user.username.trim().length > 0
    && typeof user.nickname === 'string'
    && typeof user.avatar === 'string'
    && (user.role === 'ROLE_admin' || user.role === 'ROLE_player');
}

export function writeSession(token: string, user: CurrentUser) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(storedUserOf(user)));
}

export function readSession(): { token: string; user: CurrentUser } | null {
  const token = localStorage.getItem(TOKEN_KEY);
  const rawUser = localStorage.getItem(USER_KEY);
  if (!token || !rawUser) {
    clearSession();
    return null;
  }
  try {
    const parsedUser: unknown = JSON.parse(rawUser);
    if (!isStoredUser(parsedUser)) {
      clearSession();
      return null;
    }
    const storedUser = storedUserOf(parsedUser);
    localStorage.setItem(USER_KEY, JSON.stringify(storedUser));
    return { token, user: { ...storedUser, email: '' } };
  } catch {
    clearSession();
    return null;
  }
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}
