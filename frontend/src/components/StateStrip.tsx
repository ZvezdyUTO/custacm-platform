import { AlertCircle, CheckCircle2, Clock3 } from 'lucide-react';
import type { OperationsStatus } from '../types';

interface StateStripProps {
  isRefreshing: boolean;
  statuses: OperationsStatus[];
}

const statusIcon = {
  blue: Clock3,
  green: CheckCircle2,
  amber: AlertCircle,
  red: AlertCircle,
} satisfies Record<OperationsStatus['tone'], typeof AlertCircle>;

export function StateStrip({ isRefreshing, statuses }: StateStripProps) {
  return (
    <section className="state-strip" aria-label="运行状态">
      {statuses.map((status, index) => {
        const Icon = isRefreshing && index === 0 ? Clock3 : statusIcon[status.tone];
        return (
          <article className={`state-${isRefreshing && index === 0 ? 'blue' : status.tone}`} key={status.id}>
            <Icon size={17} aria-hidden="true" />
            <div>
              <strong>{isRefreshing && index === 0 ? '刷新中：正在汇总任务视图' : status.title}</strong>
              <span>{isRefreshing && index === 0 ? '同步任务、权限异常和采集状态正在重新计算' : status.detail}</span>
            </div>
          </article>
        );
      })}
    </section>
  );
}
