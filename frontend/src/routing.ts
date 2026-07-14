export type TrainingPage = 'login' | 'multiple' | 'single' | 'problem' | 'admin';
export type AdminSection = 'create' | 'users' | 'articles' | 'categories' | 'competitions' | 'training' | 'appearance';

const DEFAULT_TRAINING_PATH = '/training/multiple';
const RETURN_PATH_ORIGIN = 'https://training-return.invalid';
const RETURN_PATHS = new Set([
	'/',
	'/home',
	'/profile',
	'/competitions',
  DEFAULT_TRAINING_PATH,
  '/training/single',
  '/training/problem',
  '/training/admin',
  '/training/admin/create-users',
  '/training/admin/users',
	'/training/admin/articles',
  '/training/admin/categories',
  '/training/admin/competitions',
  '/training/admin/training',
  '/training/admin/appearance',
	'/write',
]);

export function safeReturnPath(value: string | null): string {
  if (!value?.startsWith('/') || value.startsWith('//')) {
    return DEFAULT_TRAINING_PATH;
  }
	try {
		const target = new URL(value, RETURN_PATH_ORIGIN);
		const articlePath = /^\/(?:write|blog)\/[1-9]\d*$/.test(target.pathname);
		const competitionPath = /^\/competitions\/[1-9]\d*$/.test(target.pathname);
		const taxonomyPath = /^\/(?:tag|category)\/[^/]+$/.test(target.pathname);
		if (target.origin !== RETURN_PATH_ORIGIN || (!RETURN_PATHS.has(target.pathname) && !articlePath && !competitionPath && !taxonomyPath)) {
      return DEFAULT_TRAINING_PATH;
    }
    return `${target.pathname}${target.search}${target.hash}`;
  } catch {
    return DEFAULT_TRAINING_PATH;
  }
}

interface NavigationWindow {
  location: { assign(path: string): void };
  top: NavigationWindow | null;
}

export function navigateToBlogReturnPath(
  path: string,
  currentWindow: NavigationWindow = window as unknown as NavigationWindow,
) {
  const targetWindow = currentWindow.top ?? currentWindow;
  targetWindow.location.assign(path);
}
