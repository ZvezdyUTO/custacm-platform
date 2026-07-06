import type { AccountRole, Priority, TaskStatus } from '../types';

const statusLabel: Record<TaskStatus, string> = {
  syncing: '同步中',
  pending: '待处理',
  failed: '失败',
  completed: '已完成',
  disabled: '禁用',
};

const roleLabel: Record<AccountRole, string> = {
  admin: '管理员',
  player: '选手',
  disable: '禁用',
};

export function StatusBadge({ status }: { status: TaskStatus }) {
  return <span className={`badge status-${status}`}>{statusLabel[status]}</span>;
}

export function PriorityBadge({ priority }: { priority: Priority }) {
  return <span className={`badge priority-${priority.toLowerCase()}`}>{priority}</span>;
}

export function RoleBadge({ role }: { role: AccountRole }) {
  return <span className={`badge role-${role}`}>{roleLabel[role]}</span>;
}
