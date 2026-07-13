// Author: huangbingrui.awa
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  THEME_CHANGE_EVENT,
  THEME_MESSAGE_TYPE,
  THEME_STORAGE_KEY,
  applyTheme,
  initializeTheme,
  installThemeSync,
  readStoredTheme,
  resolveTheme,
  setTheme,
  subscribeTheme,
  toggleTheme,
} from '../theme';

type MediaController = {
  emit(matches: boolean): void;
  listenerCount(): number;
};

const browserSelf = window.self;

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

beforeEach(() => vi.stubGlobal('localStorage', memoryLocalStorage()));

function installMatchMedia(initialMatches: boolean, legacy = false): MediaController {
  let matches = initialMatches;
  const listeners = new Set<(event: MediaQueryListEvent) => void>();
  const mediaQuery = {
    get matches() { return matches; },
    media: '(prefers-color-scheme: dark)',
    onchange: null,
    addEventListener: legacy ? undefined : (_type: string, listener: (event: MediaQueryListEvent) => void) => listeners.add(listener),
    removeEventListener: legacy ? undefined : (_type: string, listener: (event: MediaQueryListEvent) => void) => listeners.delete(listener),
    addListener: (listener: (event: MediaQueryListEvent) => void) => listeners.add(listener),
    removeListener: (listener: (event: MediaQueryListEvent) => void) => listeners.delete(listener),
    dispatchEvent: () => true,
  } as unknown as MediaQueryList;
  vi.stubGlobal('matchMedia', vi.fn(() => mediaQuery));
  return {
    emit(nextMatches: boolean) {
      matches = nextMatches;
      const event = { matches: nextMatches, media: mediaQuery.media } as MediaQueryListEvent;
      listeners.forEach((listener) => listener(event));
    },
    listenerCount: () => listeners.size,
  };
}

afterEach(() => {
  window.self = browserSelf;
  localStorage.clear();
  document.documentElement.removeAttribute('data-theme');
  document.documentElement.classList.remove('dark');
  document.documentElement.style.removeProperty('color-scheme');
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe('training color theme', () => {
  it('prefers a valid stored choice and otherwise follows the system', () => {
    installMatchMedia(true);
    expect(resolveTheme()).toBe('dark');

    localStorage.setItem(THEME_STORAGE_KEY, 'light');
    expect(readStoredTheme()).toBe('light');
    expect(resolveTheme()).toBe('light');

    localStorage.setItem(THEME_STORAGE_KEY, 'sepia');
    expect(readStoredTheme()).toBeNull();
    expect(resolveTheme()).toBe('dark');
  });

  it('falls back to light when matchMedia is missing or throws', () => {
    vi.stubGlobal('matchMedia', undefined);
    expect(resolveTheme()).toBe('light');

    vi.stubGlobal('matchMedia', vi.fn(() => { throw new Error('blocked'); }));
    expect(resolveTheme()).toBe('light');
  });

  it('applies the root attribute, class and native control color scheme together', () => {
    applyTheme('dark');
    expect(document.documentElement.dataset.theme).toBe('dark');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
    expect(document.documentElement.style.colorScheme).toBe('dark');

    applyTheme('light');
    expect(document.documentElement.dataset.theme).toBe('light');
    expect(document.documentElement.classList.contains('dark')).toBe(false);
    expect(document.documentElement.style.colorScheme).toBe('light');
  });

  it('applies and broadcasts a same-document choice immediately', () => {
    const listener = vi.fn();
    const stop = subscribeTheme(listener);

    setTheme('dark');
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark');
    expect(document.documentElement.dataset.theme).toBe('dark');
    expect(listener).toHaveBeenCalledWith('dark');

    expect(toggleTheme()).toBe('light');
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('light');
    expect(listener).toHaveBeenLastCalledWith('light');

    stop();
    window.dispatchEvent(new CustomEvent(THEME_CHANGE_EVENT, { detail: { theme: 'dark' } }));
    expect(listener).toHaveBeenCalledTimes(2);
  });

  it('syncs storage and system changes while preserving an explicit choice', () => {
    const media = installMatchMedia(false);
    initializeTheme();
    const stop = installThemeSync();
    expect(media.listenerCount()).toBe(1);

    window.dispatchEvent(new StorageEvent('storage', {
      key: THEME_STORAGE_KEY,
      newValue: 'dark',
    }));
    expect(document.documentElement.dataset.theme).toBe('dark');

    localStorage.setItem(THEME_STORAGE_KEY, 'light');
    media.emit(true);
    expect(document.documentElement.dataset.theme).toBe('dark');

    localStorage.removeItem(THEME_STORAGE_KEY);
    media.emit(false);
    expect(document.documentElement.dataset.theme).toBe('light');

    applyTheme('dark');
    window.dispatchEvent(new StorageEvent('storage', { key: null, newValue: null }));
    expect(document.documentElement.dataset.theme).toBe('light');

    stop();
    expect(media.listenerCount()).toBe(0);
    media.emit(true);
    expect(document.documentElement.dataset.theme).toBe('light');
  });

  it('supports legacy MediaQueryList listeners', () => {
    const media = installMatchMedia(false, true);
    const stop = installThemeSync();
    media.emit(true);
    expect(document.documentElement.dataset.theme).toBe('dark');
    stop();
    expect(media.listenerCount()).toBe(0);
  });

  it('does not require MediaQueryList listener methods', () => {
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: true })));
    const stop = installThemeSync();
    expect(resolveTheme()).toBe('dark');
    expect(() => stop()).not.toThrow();
  });

  it('accepts only a legal same-origin message from the parent frame', () => {
    const originalSelf = window.self;
    window.self = {} as Window & typeof globalThis;
    applyTheme('light');
    const stop = installThemeSync();

    const send = (data: unknown, origin = window.location.origin, source: MessageEventSource | null = window.parent) => {
      window.dispatchEvent(new MessageEvent('message', { data, origin, source }));
    };

    send({ type: THEME_MESSAGE_TYPE, theme: 'dark' }, 'https://evil.example');
    send({ type: THEME_MESSAGE_TYPE, theme: 'dark' }, window.location.origin, null);
    send({ type: THEME_MESSAGE_TYPE, theme: 'sepia' });
    send({ type: 'custacm:theme-change', theme: 'dark' });
    expect(document.documentElement.dataset.theme).toBe('light');

    send({ type: THEME_MESSAGE_TYPE, theme: 'dark' });
    expect(document.documentElement.dataset.theme).toBe('dark');

    stop();
    send({ type: THEME_MESSAGE_TYPE, theme: 'light' });
    expect(document.documentElement.dataset.theme).toBe('dark');
    window.self = originalSelf;
  });
});
