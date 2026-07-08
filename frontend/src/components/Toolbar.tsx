import {
  Download,
  RefreshCw,
  Search,
} from 'lucide-react';
import type { AccountRole, DataSource, Filters, Priority, StudentIdentity, TaskStatus } from '../types';

const statuses: Array<{ label: string; value: 'all' | TaskStatus }> = [
  { label: '全部状态', value: 'all' },
  { label: '同步中', value: 'syncing' },
  { label: '待处理', value: 'pending' },
  { label: '失败', value: 'failed' },
  { label: '已完成', value: 'completed' },
];

const priorities: Array<{ label: string; value: 'all' | Priority }> = [
  { label: '全部优先级', value: 'all' },
  { label: 'P0', value: 'P0' },
  { label: 'P1', value: 'P1' },
  { label: 'P2', value: 'P2' },
  { label: 'P3', value: 'P3' },
];

const roles: Array<{ label: string; value: 'all' | AccountRole }> = [
  { label: '全部角色', value: 'all' },
  { label: '管理员', value: 'admin' },
  { label: '队员', value: 'player' },
  { label: '禁用', value: 'disable' },
];

const sources: Array<{ label: string; value: 'all' | DataSource }> = [
  { label: '全部来源', value: 'all' },
  { label: 'Auth', value: 'Auth' },
  { label: 'Codeforces', value: 'Codeforces' },
  { label: 'AtCoder', value: 'AtCoder' },
  { label: 'ODS', value: 'ODS' },
  { label: '系统', value: '系统' },
];

interface ToolbarProps {
  filters: Filters;
  isRefreshing: boolean;
  selectedIdentity: StudentIdentity | null;
  studentOptions: StudentIdentity[];
  exportLabel?: string;
  onClearFilters: () => void;
  onExport: () => void;
  onFiltersChange: (filters: Filters) => void;
  onRefresh: () => void;
  onSelectedIdentityChange: (studentIdentity: StudentIdentity) => void;
}

export function Toolbar({
  filters,
  isRefreshing,
  selectedIdentity,
  studentOptions,
  exportLabel = '导出',
  onClearFilters,
  onExport,
  onFiltersChange,
  onRefresh,
  onSelectedIdentityChange,
}: ToolbarProps) {
  return (
    <section className="toolbar" aria-label="任务筛选和批量操作">
      <div className="filter-row">
        <label>
          状态
          <select
            value={filters.status}
            onChange={(event) =>
              onFiltersChange({ ...filters, status: event.target.value as Filters['status'] })
            }
          >
            {statuses.map((status) => (
              <option key={status.value} value={status.value}>
                {status.label}
              </option>
            ))}
          </select>
        </label>
        <label>
          优先级
          <select
            value={filters.priority}
            onChange={(event) =>
              onFiltersChange({ ...filters, priority: event.target.value as Filters['priority'] })
            }
          >
            {priorities.map((priority) => (
              <option key={priority.value} value={priority.value}>
                {priority.label}
              </option>
            ))}
          </select>
        </label>
        <label>
          负责人角色
          <select
            value={filters.role}
            onChange={(event) =>
              onFiltersChange({ ...filters, role: event.target.value as Filters['role'] })
            }
          >
            {roles.map((role) => (
              <option key={role.value} value={role.value}>
                {role.label}
              </option>
            ))}
          </select>
        </label>
        <label>
          来源
          <select
            value={filters.source}
            onChange={(event) =>
              onFiltersChange({ ...filters, source: event.target.value as Filters['source'] })
            }
          >
            {sources.map((source) => (
              <option key={source.value} value={source.value}>
                {source.label}
              </option>
            ))}
          </select>
        </label>
        <label className="search-control">
          <Search size={16} aria-hidden="true" />
          <input
            aria-label="搜索任务、学号姓名或来源"
            value={filters.query}
            onChange={(event) => onFiltersChange({ ...filters, query: event.target.value })}
            placeholder="搜索任务 / 学号姓名 / 来源"
          />
        </label>
        <label className="identity-control">
          当前队员
          <select
            aria-label="当前队员"
            disabled={studentOptions.length === 0}
            value={selectedIdentity ?? ''}
            onChange={(event) => onSelectedIdentityChange(event.target.value)}
          >
            {studentOptions.length === 0 ? <option value="">等待登录</option> : null}
            {studentOptions.map((studentIdentity) => (
              <option key={studentIdentity} value={studentIdentity}>
                {studentIdentity}
              </option>
            ))}
          </select>
        </label>
        <button className="secondary-button" type="button" onClick={onClearFilters}>
          清除筛选
        </button>
      </div>
      <div className="action-row">
        <button
          className="secondary-button"
          type="button"
          onClick={onExport}
        >
          <Download size={16} aria-hidden="true" />
          {exportLabel}
        </button>
        <button className="secondary-button" type="button" onClick={onRefresh} disabled={isRefreshing}>
          <RefreshCw size={16} aria-hidden="true" className={isRefreshing ? 'spin' : ''} />
          {isRefreshing ? '刷新中' : '刷新'}
        </button>
      </div>
    </section>
  );
}
