// Author: huangbingrui.awa

export const THEME_STORAGE_KEY = 'custacm.theme';
export const THEME_CHANGE_EVENT = 'custacm:theme-change';
export const THEME_MESSAGE_TYPE = 'custacm:theme';

export type ColorTheme = 'light' | 'dark';

type ThemeChangeDetail = { theme: ColorTheme };

function mediaQueryOf(targetWindow: Window): MediaQueryList | null {
  try {
    return typeof targetWindow.matchMedia === 'function'
      ? targetWindow.matchMedia('(prefers-color-scheme: dark)')
      : null;
  } catch {
    return null;
  }
}

export function isColorTheme(value: unknown): value is ColorTheme {
  return value === 'light' || value === 'dark';
}

export function readStoredTheme(targetWindow: Window = window): ColorTheme | null {
  try {
    const stored = targetWindow.localStorage.getItem(THEME_STORAGE_KEY);
    return isColorTheme(stored) ? stored : null;
  } catch {
    return null;
  }
}

export function systemTheme(targetWindow: Window = window): ColorTheme {
  return mediaQueryOf(targetWindow)?.matches ? 'dark' : 'light';
}

export function resolveTheme(targetWindow: Window = window): ColorTheme {
  return readStoredTheme(targetWindow) ?? systemTheme(targetWindow);
}

export function applyTheme(theme: ColorTheme, targetDocument: Document = document): ColorTheme {
  const root = targetDocument.documentElement;
  root.dataset.theme = theme;
  root.classList.toggle('dark', theme === 'dark');
  root.style.colorScheme = theme;
  return theme;
}

export function currentTheme(
  targetWindow: Window = window,
  targetDocument: Document = document,
): ColorTheme {
  const applied = targetDocument.documentElement.dataset.theme;
  return isColorTheme(applied) ? applied : resolveTheme(targetWindow);
}

function emitThemeChange(theme: ColorTheme, targetWindow: Window): void {
  const event = targetWindow.document.createEvent('CustomEvent') as CustomEvent<ThemeChangeDetail>;
  event.initCustomEvent(THEME_CHANGE_EVENT, false, false, { theme });
  targetWindow.dispatchEvent(event);
}

function applyAndNotify(theme: ColorTheme, targetWindow: Window, targetDocument: Document): void {
  applyTheme(theme, targetDocument);
  emitThemeChange(theme, targetWindow);
}

export function initializeTheme(
  targetWindow: Window = window,
  targetDocument: Document = document,
): ColorTheme {
  return applyTheme(resolveTheme(targetWindow), targetDocument);
}

export function setTheme(
  theme: ColorTheme,
  targetWindow: Window = window,
  targetDocument: Document = document,
): void {
  try {
    targetWindow.localStorage.setItem(THEME_STORAGE_KEY, theme);
  } catch {
    // Applying the requested theme still works when storage is unavailable.
  }
  applyAndNotify(theme, targetWindow, targetDocument);
}

export function toggleTheme(
  targetWindow: Window = window,
  targetDocument: Document = document,
): ColorTheme {
  const nextTheme = currentTheme(targetWindow, targetDocument) === 'dark' ? 'light' : 'dark';
  setTheme(nextTheme, targetWindow, targetDocument);
  return nextTheme;
}

export function subscribeTheme(
  listener: (theme: ColorTheme) => void,
  targetWindow: Window = window,
): () => void {
  const handleThemeChange = (event: Event) => {
    const theme = (event as CustomEvent<ThemeChangeDetail>).detail?.theme;
    if (isColorTheme(theme)) listener(theme);
  };
  targetWindow.addEventListener(THEME_CHANGE_EVENT, handleThemeChange);
  return () => targetWindow.removeEventListener(THEME_CHANGE_EVENT, handleThemeChange);
}

export function installThemeSync(
  targetWindow: Window = window,
  targetDocument: Document = document,
): () => void {
  const mediaQuery = mediaQueryOf(targetWindow);

  const handleStorage = (event: StorageEvent) => {
    if (event.key !== null && event.key !== THEME_STORAGE_KEY) return;
    const theme = isColorTheme(event.newValue) ? event.newValue : systemTheme(targetWindow);
    applyAndNotify(theme, targetWindow, targetDocument);
  };

  const handleSystemTheme = (event: MediaQueryListEvent) => {
    if (readStoredTheme(targetWindow)) return;
    applyAndNotify(event.matches ? 'dark' : 'light', targetWindow, targetDocument);
  };

  const handleParentMessage = (event: MessageEvent<unknown>) => {
    if (targetWindow.self === targetWindow.top) return;
    if (event.origin !== targetWindow.location.origin || event.source !== targetWindow.parent) return;
    if (!event.data || typeof event.data !== 'object') return;
    const message = event.data as { type?: unknown; theme?: unknown };
    if (message.type !== THEME_MESSAGE_TYPE || !isColorTheme(message.theme)) return;
    applyAndNotify(message.theme, targetWindow, targetDocument);
  };

  targetWindow.addEventListener('storage', handleStorage);
  targetWindow.addEventListener('message', handleParentMessage);
  if (mediaQuery && typeof mediaQuery.addEventListener === 'function') {
    mediaQuery.addEventListener('change', handleSystemTheme);
  } else if (mediaQuery && typeof mediaQuery.addListener === 'function') {
    mediaQuery.addListener(handleSystemTheme);
  }

  return () => {
    targetWindow.removeEventListener('storage', handleStorage);
    targetWindow.removeEventListener('message', handleParentMessage);
    if (mediaQuery && typeof mediaQuery.removeEventListener === 'function') {
      mediaQuery.removeEventListener('change', handleSystemTheme);
    } else if (mediaQuery && typeof mediaQuery.removeListener === 'function') {
      mediaQuery.removeListener(handleSystemTheme);
    }
  };
}
