import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { TaskTable } from '../components/TaskTable';
import { dashboardTasksFixture } from './fixtures';

describe('TaskTable', () => {
  afterEach(() => cleanup());

  it('shows the loading state while refreshing', () => {
    render(
      <TaskTable
        isLoading
        onSelect={vi.fn()}
        onSelectAll={vi.fn()}
        selectedIds={new Set()}
        tasks={dashboardTasksFixture}
        totalTasks={128}
      />,
    );

    expect(screen.getByText('正在刷新数据')).not.toBeNull();
  });

  it('shows a useful empty state when filters match nothing', () => {
    render(
      <TaskTable
        isLoading={false}
        onSelect={vi.fn()}
        onSelectAll={vi.fn()}
        selectedIds={new Set()}
        tasks={[]}
        totalTasks={128}
      />,
    );

    expect(screen.getByText('没有符合条件的数据项')).not.toBeNull();
  });

  it('supports selecting all visible tasks', async () => {
    const user = userEvent.setup();
    const onSelectAll = vi.fn();
    render(
      <TaskTable
        isLoading={false}
        onSelect={vi.fn()}
        onSelectAll={onSelectAll}
        selectedIds={new Set()}
        tasks={dashboardTasksFixture.slice(0, 2)}
        totalTasks={128}
      />,
    );

    await user.click(screen.getByLabelText('选择全部任务'));

    expect(onSelectAll).toHaveBeenCalledWith(true);
  });
});
