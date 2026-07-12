import { describe, expect, it, vi } from 'vitest';
import { navigateToBlogReturnPath, safeReturnPath } from '../routing';

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
    expect(safeReturnPath('/training/admin/articles')).toBe('/training/admin/articles');
    expect(safeReturnPath('/training/admin/categories')).toBe('/training/admin/categories');
  });

	it('keeps the supported Blog writing return paths', () => {
		expect(safeReturnPath('/about')).toBe('/about');
		expect(safeReturnPath('/write')).toBe('/write');
		expect(safeReturnPath('/write/42')).toBe('/write/42');
		expect(safeReturnPath('/blog/42#comment-7')).toBe('/blog/42#comment-7');
	});

	it('keeps public Blog locations', () => {
		expect(safeReturnPath('/home')).toBe('/home');
		expect(safeReturnPath('/moments?tab=latest')).toBe('/moments?tab=latest');
		expect(safeReturnPath('/friends#team')).toBe('/friends#team');
		expect(safeReturnPath('/category/题解')).toBe('/category/%E9%A2%98%E8%A7%A3');
		expect(safeReturnPath('/tag/dp')).toBe('/tag/dp');
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
		'/write/not-an-id',
  ])('rejects an unsupported or non-canonical path: %s', (value) => {
    expect(safeReturnPath(value)).toBe('/training/multiple');
  });
});

describe('Blog login return navigation', () => {
  it('navigates the top-level Blog window instead of loading Blog inside the training frame', () => {
    const topAssign = vi.fn();
    const frameAssign = vi.fn();
    type TestNavigationWindow = { location: { assign(path: string): void }; top: TestNavigationWindow | null };
    const topWindow: TestNavigationWindow = { location: { assign: topAssign }, top: null };
    topWindow.top = topWindow;
    const frameWindow = { location: { assign: frameAssign }, top: topWindow };

    navigateToBlogReturnPath('/blog/42', frameWindow);

    expect(topAssign).toHaveBeenCalledWith('/blog/42');
    expect(frameAssign).not.toHaveBeenCalled();
  });
});
