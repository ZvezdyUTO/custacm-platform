export type TrainingPage = 'login' | 'multiple' | 'single' | 'problem' | 'admin';
export type AdminSection = 'create' | 'users' | 'training' | 'appearance';

export interface TrainingRoute {
  page: TrainingPage;
  section?: AdminSection;
}

const DEFAULT_TRAINING_PATH = '/training/multiple';
const RETURN_PATH_ORIGIN = 'https://training-return.invalid';
const RETURN_PATHS = new Set([
  DEFAULT_TRAINING_PATH,
  '/training/single',
  '/training/problem',
  '/training/admin',
  '/training/admin/create-users',
  '/training/admin/users',
  '/training/admin/training',
  '/training/admin/appearance',
]);

export function parseTrainingRoute(pathname: string, search = ''): TrainingRoute {
  if (pathname === '/training/login') {
    return { page: 'login' };
  }
  if (pathname === '/training/single') {
    return { page: 'single' };
  }
  if (pathname === '/training/problem') {
    return { page: 'problem' };
  }
  if (pathname === '/training/admin') {
    const section = new URLSearchParams(search).get('section');
    return { page: 'admin', section: section === 'training' ? 'training' : 'users' };
  }
  if (pathname === '/training/admin/create-users') return { page: 'admin', section: 'create' };
  if (pathname === '/training/admin/users') return { page: 'admin', section: 'users' };
  if (pathname === '/training/admin/training') return { page: 'admin', section: 'training' };
  if (pathname === '/training/admin/appearance') return { page: 'admin', section: 'appearance' };
  return { page: 'multiple' };
}

export function safeReturnPath(value: string | null): string {
  if (!value?.startsWith('/') || value.startsWith('//')) {
    return DEFAULT_TRAINING_PATH;
  }
  try {
    const target = new URL(value, RETURN_PATH_ORIGIN);
    if (target.origin !== RETURN_PATH_ORIGIN || !RETURN_PATHS.has(target.pathname)) {
      return DEFAULT_TRAINING_PATH;
    }
    return `${target.pathname}${target.search}${target.hash}`;
  } catch {
    return DEFAULT_TRAINING_PATH;
  }
}
