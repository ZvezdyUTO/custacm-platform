// Author: huangbingrui.awa
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  THEME_CHANGE_EVENT,
  THEME_MESSAGE_TYPE,
  THEME_STORAGE_KEY,
  applyTheme,
  initializeTheme,
  installThemeSync,
  resolveTheme,
  setTheme,
  subscribeTheme,
  toggleTheme,
} from '../theme';

function memoryLocalStorage(): Storage {
  const values = new Map<string, string>();
  return {
    clear: () => values.clear(),
    getItem: (key) => values.get(key) ?? null,
    key: (index) => Array.from(values.keys())[index] ?? null,
    get length() { return values.size; },
    removeItem: (key) => values.delete(key),
    setItem: (key, value) => values.set(key, value),
  };
}

const browserSelf = window.self;

beforeEach(() => {
  vi.stubGlobal('localStorage', memoryLocalStorage());
  document.documentElement.removeAttribute('data-theme');
  document.documentElement.classList.remove('dark');
});

afterEach(() => {
  window.self = browserSelf;
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe('Training color theme', () => {
  it('defaults to light and ignores the operating-system preference', () => {
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: true })));
    expect(resolveTheme()).toBe('light');
    expect(initializeTheme()).toBe('light');
  });

  it('persists, applies and broadcasts an explicit choice', () => {
    const listener = vi.fn();
    const stop = subscribeTheme(listener);
    setTheme('dark');
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
    expect(listener).toHaveBeenCalledWith('dark');
    expect(toggleTheme()).toBe('light');
    stop();
  });

  it('syncs storage updates and defaults a removed choice to light', () => {
    applyTheme('dark');
    const stop = installThemeSync();
    window.dispatchEvent(new StorageEvent('storage', { key: THEME_STORAGE_KEY, newValue: null }));
    expect(document.documentElement.dataset.theme).toBe('light');
    stop();
  });

  it('accepts only a legal same-origin parent-frame message', () => {
    window.self = {} as Window & typeof globalThis;
    applyTheme('light');
    const stop = installThemeSync();
    const send = (data: unknown, origin = window.location.origin, source: MessageEventSource | null = window.parent) => {
      window.dispatchEvent(new MessageEvent('message', { data, origin, source }));
    };

    send({ type: THEME_MESSAGE_TYPE, theme: 'dark' }, 'https://evil.example');
    send({ type: THEME_MESSAGE_TYPE, theme: 'sepia' });
    expect(document.documentElement.dataset.theme).toBe('light');
    send({ type: THEME_MESSAGE_TYPE, theme: 'dark' });
    expect(document.documentElement.dataset.theme).toBe('dark');
    stop();
  });

  it('uses the public theme event name for same-document updates', () => {
    const listener = vi.fn();
    window.addEventListener(THEME_CHANGE_EVENT, listener);
    setTheme('dark');
    expect(listener).toHaveBeenCalledOnce();
    window.removeEventListener(THEME_CHANGE_EVENT, listener);
  });
});
