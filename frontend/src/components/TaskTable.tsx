import { useEffect, useRef } from 'react';
import type { DashboardTask } from '../types';
import { PriorityBadge, RoleBadge, StatusBadge } from './Badge';

interface TaskTableProps {
  tasks: DashboardTask[];
  isLoading: boolean;
  selectedIds: Set<string>;
  totalTasks: number;
  onSelect: (id: string) => void;
  onSelectAll: (checked: boolean) => void;
}

export function TaskTable({
  tasks,
  isLoading,
  selectedIds,
  totalTasks,
  onSelect,
  onSelectAll,
}: TaskTableProps) {
  const selectAllRef = useRef<HTMLInputElement>(null);
  const allSelected = tasks.length > 0 && tasks.every((task) => selectedIds.has(task.id));
  const partiallySelected = selectedIds.size > 0 && !allSelected;

  useEffect(() => {
    if (selectAllRef.current) {
      selectAllRef.current.indeterminate = partiallySelected;
    }
  }, [partiallySelected]);

  if (isLoading) {
    return (
      <div className="table-state" role="status" aria-live="polite">
        <div className="loading-bars" aria-hidden="true">
          <span />
          <span />
          <span />
        </div>
        <strong>正在调用本地后端接口</strong>
        <p>auth-web 用户列表、training-data handle 和 Codeforces 仓库查询正在刷新。</p>
      </div>
    );
  }

  if (tasks.length === 0) {
    return (
      <div className="table-state">
        <strong>没有符合条件的数据项</strong>
        <p>可以清除筛选条件，或先登录 admin 账号并运行本地种子数据脚本。</p>
      </div>
    );
  }

  return (
    <div className="table-shell">
      <table className="task-table">
        <colgroup>
          <col className="col-check" />
          <col className="col-task" />
          <col className="col-status" />
          <col className="col-priority" />
          <col className="col-owner" />
          <col className="col-identity" />
          <col className="col-source" />
          <col className="col-time" />
        </colgroup>
        <thead>
          <tr>
            <th className="check-cell" scope="col">
              <input
                aria-checked={partiallySelected ? 'mixed' : allSelected}
                aria-label="选择全部任务"
                checked={allSelected}
                onChange={(event) => onSelectAll(event.target.checked)}
                ref={selectAllRef}
                type="checkbox"
              />
            </th>
            <th scope="col">账号 / 数据项</th>
            <th scope="col">状态</th>
            <th scope="col">优先级</th>
            <th scope="col">责任域</th>
            <th scope="col">学号姓名 / handle</th>
            <th scope="col">来源</th>
            <th scope="col">更新时间</th>
          </tr>
        </thead>
        <tbody>
          {tasks.map((task) => (
            <tr key={task.id} className={selectedIds.has(task.id) ? 'is-selected' : ''}>
              <td className="check-cell">
                <input
                  aria-label={`选择 ${task.title}`}
                  checked={selectedIds.has(task.id)}
                  onChange={() => onSelect(task.id)}
                  type="checkbox"
                />
              </td>
              <td data-label="账号 / 数据项">
                <div className="task-title">
                  <strong>{task.title}</strong>
                  <span>{task.detail}</span>
                </div>
              </td>
              <td data-label="状态">
                <StatusBadge status={task.status} />
              </td>
              <td data-label="优先级">
                <PriorityBadge priority={task.priority} />
              </td>
              <td data-label="责任域">
                <div className="owner-cell">
                  <span className="avatar small">{task.owner.avatar}</span>
                  <span>
                    <strong>{task.owner.name}</strong>
                    <RoleBadge role={task.owner.role} />
                  </span>
                </div>
              </td>
              <td className="subject-cell" data-label="学号姓名 / handle" title={task.studentIdentity ?? task.subjectLabel}>
                {task.subjectLabel}
              </td>
              <td data-label="来源">{task.source}</td>
              <td className="time-cell" data-label="更新时间" title={task.updatedAt}>
                {formatUpdatedAt(task.updatedAt)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <footer className="table-footer">
        <span>
          当前 {tasks.length} / 共 {totalTasks} 条真实接口数据
        </span>
        <small>筛选和排序由上方控件即时完成。</small>
      </footer>
    </div>
  );
}

function formatUpdatedAt(value: string) {
  const [, month, day, hour, minute] =
    value.match(/^\d{4}-(\d{2})-(\d{2})\s+(\d{2}):(\d{2})/) ?? [];
  return month && day && hour && minute ? `${month}-${day} ${hour}:${minute}` : value;
}
