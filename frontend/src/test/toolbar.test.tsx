import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Toolbar } from '../components/Toolbar';
import type { Filters } from '../types';

const filters: Filters = {
  query: 'Codeforces',
  status: 'failed',
  priority: 'P1',
  role: 'admin',
  source: 'ODS',
  view: 'ods-import',
};

describe('Toolbar', () => {
  afterEach(() => cleanup());

  it('exposes an accessible dashboard search input', () => {
    render(
      <Toolbar
        filters={filters}
        isRefreshing={false}
        onClearFilters={vi.fn()}
        onExport={vi.fn()}
        onFiltersChange={vi.fn()}
        onRefresh={vi.fn()}
        onSelectedIdentityChange={vi.fn()}
        selectedIdentity="230511214李明"
        studentOptions={['230511214李明']}
      />,
    );

    expect(screen.getByLabelText('搜索任务、学号姓名或来源')).not.toBeNull();
  });

  it('clears all filters through the clear action', async () => {
    const user = userEvent.setup();
    const onClearFilters = vi.fn();
    render(
      <Toolbar
        filters={filters}
        isRefreshing={false}
        onClearFilters={onClearFilters}
        onExport={vi.fn()}
        onFiltersChange={vi.fn()}
        onRefresh={vi.fn()}
        onSelectedIdentityChange={vi.fn()}
        selectedIdentity="230511214李明"
        studentOptions={['230511214李明']}
      />,
    );

    await user.click(screen.getByRole('button', { name: '清除筛选' }));

    expect(onClearFilters).toHaveBeenCalledTimes(1);
  });
});
