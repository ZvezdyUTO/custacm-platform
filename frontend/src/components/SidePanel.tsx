import { AlertTriangle } from 'lucide-react';
import type { AlertItem, PermissionSummary, TimelineItem } from '../types';
import { StatusBadge } from './Badge';

interface SidePanelProps {
  alerts: AlertItem[];
  permissionSummary: PermissionSummary;
  timeline: TimelineItem[];
}

export function SidePanel({ alerts, permissionSummary, timeline }: SidePanelProps) {
  return (
    <aside className="side-panel" aria-label="近期任务和异常记录">
      <section className="side-section">
        <header>
          <h2>近期任务</h2>
        </header>
        <ul className="timeline-list">
          {timeline.map((item) => (
            <li key={item.id}>
              <div>
                <strong>{item.title}</strong>
                <span>{item.meta}</span>
              </div>
              <StatusBadge status={item.status} />
              <time>{item.time}</time>
            </li>
          ))}
        </ul>
      </section>

      <section className="side-section">
        <header>
          <h2>异常与告警</h2>
        </header>
        <ul className="alert-list">
          {alerts.map((alert) => (
            <li className={`alert-${alert.severity}`} key={alert.id}>
              <AlertTriangle size={15} aria-hidden="true" />
              <div>
                <strong>{alert.title}</strong>
                <span>{alert.detail}</span>
              </div>
              <time>{alert.time}</time>
            </li>
          ))}
        </ul>
      </section>

      <section className="side-section permission-summary">
        <header>
          <h2>权限状态概览</h2>
        </header>
        <div className="donut-row">
          <div className="donut" aria-label="权限状态总览：正常 76%，待处理 10%，异常 5%，未授权 9%">
            <span>{permissionSummary.total}</span>
            <small>总账号</small>
          </div>
          <dl>
            {permissionSummary.segments.map((segment) => (
              <div key={segment.id}>
                <dt className={segment.id}>{segment.label}</dt>
                <dd>{segment.value}</dd>
              </div>
            ))}
          </dl>
        </div>
      </section>
    </aside>
  );
}
