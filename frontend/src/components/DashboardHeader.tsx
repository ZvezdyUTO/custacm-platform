import { RefreshCw } from 'lucide-react';
import type { Metric } from '../types';
import { dashboardIcons } from './iconRegistry';

interface DashboardHeaderProps {
  isRefreshing: boolean;
  metrics: Metric[];
  onRefresh: () => void;
  updatedAt: string;
}

export function DashboardHeader({ isRefreshing, metrics, onRefresh, updatedAt }: DashboardHeaderProps) {
  return (
    <section className="dashboard-header" aria-labelledby="page-title">
      <div className="title-row">
        <div>
          <h1 id="page-title">custacm wiki</h1>
          <p>训练数据管理面板用于集中管理账号权限、选手身份、数据采集与维护任务。</p>
        </div>
        <div className="refresh-meta">
          <span>数据更新时间：{updatedAt}</span>
          <button
            className="icon-button"
            disabled={isRefreshing}
            onClick={onRefresh}
            type="button"
            aria-label="刷新工作台数据"
          >
            <RefreshCw className={isRefreshing ? 'spin' : ''} size={16} />
          </button>
        </div>
      </div>
      <div className="metric-strip" aria-label="关键指标概览">
        {metrics.map((metric) => {
          const Icon = dashboardIcons[metric.iconKey];
          return (
            <article className="metric-cell" key={metric.id}>
              <span className={`metric-icon metric-${metric.tone}`}>
                <Icon size={18} aria-hidden="true" />
              </span>
              <div>
                <span>{metric.label}</span>
                <strong>{metric.value}</strong>
                <small>{metric.delta}</small>
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
}
