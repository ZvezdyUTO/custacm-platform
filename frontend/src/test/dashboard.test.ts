import { describe, expect, it } from 'vitest';
import { filterAndSortTasks } from '../hooks/useDashboardFilters';
import type { Filters } from '../types';
import { dashboardTasksFixture } from './fixtures';

const defaultFilters: Filters = {
  query: '',
  status: 'all',
  priority: 'all',
  role: 'all',
  source: 'all',
  view: 'all',
};

describe('dashboard filtering', () => {
  it('keeps failed high-priority tasks at the top of the work queue', () => {
    const result = filterAndSortTasks(dashboardTasksFixture, defaultFilters);

    expect(result[0]?.status).toBe('failed');
    expect(result[0]?.priority).toBe('P1');
  });

  it('filters by dashboard view and query', () => {
    const result = filterAndSortTasks(dashboardTasksFixture, {
      ...defaultFilters,
      view: 'ods-import',
      query: '第 55 批',
    });

    expect(result).toHaveLength(1);
    expect(result[0]?.title).toContain('Codeforces');
  });

  it('returns an empty collection for unmatched account searches', () => {
    const result = filterAndSortTasks(dashboardTasksFixture, {
      ...defaultFilters,
      query: 'not-exist-identity',
    });

    expect(result).toEqual([]);
  });
});
