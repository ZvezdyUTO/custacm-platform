import type { DashboardTask, Filters, Priority, TaskStatus } from '../types';

const statusRank: Record<TaskStatus, number> = {
  failed: 0,
  pending: 1,
  syncing: 2,
  completed: 3,
  disabled: 4,
};

const priorityRank: Record<Priority, number> = {
  P0: 0,
  P1: 1,
  P2: 2,
  P3: 3,
};

export function matchesDashboardFilters(task: DashboardTask, filters: Filters) {
  const query = filters.query.trim().toLowerCase();
  const searchable = [
    task.title,
    task.owner.name,
    task.subjectLabel,
    task.studentIdentity,
    task.source,
    task.status,
    task.priority,
  ]
    .join(' ')
    .toLowerCase();

  return (
    (filters.view === 'all' || task.module === filters.view) &&
    (filters.status === 'all' || task.status === filters.status) &&
    (filters.priority === 'all' || task.priority === filters.priority) &&
    (filters.role === 'all' || task.owner.role === filters.role) &&
    (filters.source === 'all' || task.source === filters.source) &&
    (query.length === 0 || searchable.includes(query))
  );
}

export function filterAndSortTasks(tasks: DashboardTask[], filters: Filters) {
  return [...tasks]
    .filter((task) => matchesDashboardFilters(task, filters))
    .sort((first, second) => {
      const statusDiff = statusRank[first.status] - statusRank[second.status];
      if (statusDiff !== 0) {
        return statusDiff;
      }

      const priorityDiff = priorityRank[first.priority] - priorityRank[second.priority];
      if (priorityDiff !== 0) {
        return priorityDiff;
      }

      return second.updatedAt.localeCompare(first.updatedAt);
    });
}
