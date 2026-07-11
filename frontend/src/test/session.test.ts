import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { clearSession, readSession, writeSession } from '../auth/session';

function memoryLocalStorage(): Storage {
  const values = new Map<string, string>();
  return {
    clear: () => values.clear(),
    getItem: (key) => values.get(key) ?? null,
    key: (index) => Array.from(values.keys())[index] ?? null,
    get length() {
      return values.size;
    },
    removeItem: (key) => values.delete(key),
    setItem: (key, value) => values.set(key, value),
  };
}

describe('shared Blog session', () => {
  beforeEach(() => vi.stubGlobal('localStorage', memoryLocalStorage()));
  afterEach(() => vi.unstubAllGlobals());

  it('stores and removes the shared Blog/Training login keys', () => {
    writeSession('jwt', {
      username: 'player-a',
      nickname: 'A',
      avatar: '',
      email: '',
      role: 'ROLE_player',
    });

    expect(localStorage.getItem('custacm.accessToken')).toBe('jwt');
    expect(readSession()?.user.username).toBe('player-a');

    clearSession();

    expect(readSession()).toBeNull();
    expect(localStorage.getItem('custacm.accessToken')).toBeNull();
    expect(localStorage.getItem('custacm.user')).toBeNull();
  });

  it('stores only the shared display fields from runtime user data', () => {
    const runtimeUser = {
      username: 'admin-a',
      nickname: 'Admin A',
      avatar: '/avatar.png',
      email: 'private@example.com',
      role: 'ROLE_admin',
      id: 7,
      createTime: '2026-07-11T00:00:00Z',
      updateTime: '2026-07-11T01:00:00Z',
      password: null,
    } as const;

    writeSession('jwt', runtimeUser);

    expect(JSON.parse(localStorage.getItem('custacm.user') ?? 'null')).toEqual({
      username: 'admin-a',
      nickname: 'Admin A',
      avatar: '/avatar.png',
      role: 'ROLE_admin',
    });
    expect(readSession()).toEqual({
      token: 'jwt',
      user: {
        username: 'admin-a',
        nickname: 'Admin A',
        avatar: '/avatar.png',
        email: '',
        role: 'ROLE_admin',
      },
    });
  });

  it.each([
    ['a blank username', { username: '   ', nickname: 'A', avatar: '', role: 'ROLE_player' }],
    ['a non-string nickname', { username: 'player-a', nickname: 1, avatar: '', role: 'ROLE_player' }],
    ['an invalid role', { username: 'player-a', nickname: 'A', avatar: '', role: 'player' }],
  ])('clears a parseable stored user with %s', (_label, storedUser) => {
    localStorage.setItem('custacm.accessToken', 'jwt');
    localStorage.setItem('custacm.user', JSON.stringify(storedUser));

    expect(readSession()).toBeNull();
    expect(localStorage.getItem('custacm.accessToken')).toBeNull();
    expect(localStorage.getItem('custacm.user')).toBeNull();
  });

  it('clears either orphaned session key before returning null', () => {
    localStorage.setItem('custacm.accessToken', 'jwt');

    expect(readSession()).toBeNull();
    expect(localStorage.getItem('custacm.accessToken')).toBeNull();
    expect(localStorage.getItem('custacm.user')).toBeNull();

    localStorage.setItem('custacm.user', JSON.stringify({
      username: 'player-a',
      nickname: 'A',
      avatar: '',
      role: 'ROLE_player',
    }));

    expect(readSession()).toBeNull();
    expect(localStorage.getItem('custacm.accessToken')).toBeNull();
    expect(localStorage.getItem('custacm.user')).toBeNull();
  });

  it('drops malformed user JSON without throwing', () => {
    localStorage.setItem('custacm.accessToken', 'jwt');
    localStorage.setItem('custacm.user', '{bad-json');

    expect(readSession()).toBeNull();
    expect(localStorage.getItem('custacm.accessToken')).toBeNull();
    expect(localStorage.getItem('custacm.user')).toBeNull();
  });
});
