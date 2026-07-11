import { describe, expect, it } from 'vitest';
import { parseTrainingRoute, safeReturnPath } from '../routing';

describe('training route parsing', () => {
  it.each([
    ['/training', { page: 'multiple' }],
    ['/training/single', { page: 'single' }],
    ['/training/problem', { page: 'problem' }],
    ['/training/admin', { page: 'admin', section: 'users' }],
    ['/training/admin/create-users', { page: 'admin', section: 'create' }],
    ['/training/admin/users', { page: 'admin', section: 'users' }],
    ['/training/admin/training', { page: 'admin', section: 'training' }],
    ['/training/admin/appearance', { page: 'admin', section: 'appearance' }],
  ] as const)('maps %s to its training page', (pathname, expected) => {
    expect(parseTrainingRoute(pathname)).toEqual(expected);
  });

  it('reads the training admin section from the query string', () => {
    expect(parseTrainingRoute('/training/admin', '?section=training')).toEqual({
      page: 'admin',
      section: 'training',
    });
  });

  it.each([
    '/admin',
    '/other/admin',
    '/query/single',
    '/training/unknown',
    '/training/admin/extra',
    '/training/problem/extra',
  ])('canonicalizes a non-whitelisted full path to multiple: %s', (pathname) => {
    expect(parseTrainingRoute(pathname)).toEqual({ page: 'multiple' });
  });
});

describe('safe training return paths', () => {
  it.each([
    'https://evil.example/',
    '//evil.example/training/problem',
  ])('falls back when the return value is external: %s', (value) => {
    expect(safeReturnPath(value)).toBe('/training/multiple');
  });

  it('keeps a local training path', () => {
    expect(safeReturnPath('/training/problem')).toBe('/training/problem');
    expect(safeReturnPath('/training/admin/create-users')).toBe('/training/admin/create-users');
    expect(safeReturnPath('/training/admin/appearance')).toBe('/training/admin/appearance');
  });

  it('preserves a supported admin section', () => {
    expect(safeReturnPath('/training/admin?section=training')).toBe(
      '/training/admin?section=training',
    );
  });

  it.each([
    '/training/login',
    '/training/%2e%2e/%2e%2e//evil.example',
    '/training/problem/../login',
  ])('rejects an unsupported or non-canonical path: %s', (value) => {
    expect(safeReturnPath(value)).toBe('/training/multiple');
  });
});
