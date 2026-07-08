import { OJ_NAMES } from '../types';
import type {
  AccountRole,
  AlertItem,
  AuthUser,
  CodeforcesFirstAcceptedReport,
  CodeforcesStudentSubmissionReport,
  DashboardOperation,
  DashboardTask,
  Metric,
  OperationsStatus,
  PlatformModuleInfo,
  PermissionSummary,
  ServiceHealth,
  StudentTrainingRecord,
  TimelineItem,
} from '../types';

const roleOrder: Record<AccountRole, number> = {
  admin: 0,
  player: 1,
  disable: 2,
};

export function buildMetrics(
  users: AuthUser[],
  records: StudentTrainingRecord[],
  submissions: CodeforcesStudentSubmissionReport | null,
  firstAccepted: CodeforcesFirstAcceptedReport | null,
  health: ServiceHealth[],
): Metric[] {
  const roleCounts = countRoles(users);
  const boundCount = records.filter((record) => record.handleStatus === 'bound').length;
  const totalAccepted = records.reduce(
    (sum, record) => sum + (record.acceptedSummary?.totalAcceptedProblemCount ?? 0),
    0,
  );
  const failedServices = health.filter((service) => service.status !== 'UP').length;

  return [
    {
      id: 'accounts',
      label: '平台账号',
      value: String(users.length),
      delta: `管理员 ${roleCounts.admin} / 队员 ${roleCounts.player} / 禁用 ${roleCounts.disable}`,
      tone: failedServices > 0 ? 'amber' : 'blue',
      iconKey: 'users',
    },
    {
      id: 'handles',
      label: 'OJ 绑定',
      value: String(boundCount),
      delta: `未绑定 ${Math.max(users.length - boundCount, 0)} 个学号姓名`,
      tone: 'green',
      iconKey: 'refresh',
    },
    {
      id: 'accepted',
      label: '首次通过题目',
      value: String(totalAccepted),
      delta: firstAccepted ? `当前队员 ${firstAccepted.totalAcceptedProblemCount} 题` : '等待选择队员',
      tone: 'violet',
      iconKey: 'trophy',
    },
    {
      id: 'submissions',
      label: '提交明细',
      value: String(submissions?.submissions.length ?? 0),
      delta: submissions ? `来自 ${submissions.authorHandle}` : '公开查询接口',
      tone: 'amber',
      iconKey: 'file-clock',
    },
    {
      id: 'health',
      label: '服务健康',
      value: failedServices === 0 ? '正常' : `${failedServices} 异常`,
      delta: health.map((item) => `${item.service}:${item.status}`).join(' / ') || '等待探活',
      tone: failedServices === 0 ? 'slate' : 'red',
      iconKey: 'shield-check',
    },
    {
      id: 'errors',
      label: '接口异常',
      value: String(records.filter((record) => record.handleStatus === 'error' || record.summaryStatus === 'error').length),
      delta: '来自查询结果',
      tone: 'red',
      iconKey: 'key-round',
    },
  ];
}

export function buildOperationsStatuses(
  health: ServiceHealth[],
  tokenPresent: boolean,
  lastBatchId: string | null,
  moduleInfo: PlatformModuleInfo[] = [],
): OperationsStatus[] {
  const authHealth = health.find((item) => item.service === 'auth-web');
  const trainingDataHealth = health.find((item) => item.service === 'training-data-web');
  const authModuleInfo = moduleInfo.find((item) => item.service === 'auth-web');
  const trainingDataModuleInfo = moduleInfo.find((item) => item.service === 'training-data-web');

  return [
    {
      id: 'auth-health',
      title: `auth-web ${authHealth?.status ?? 'UNKNOWN'}`,
      detail: authModuleInfo
        ? `${authModuleInfo.module} / ${authModuleInfo.features.length} features`
        : tokenPresent ? '已持有平台 JWT，可调用 admin 接口' : '未登录，只能探活和查看登录入口',
      tone: authHealth?.status === 'UP' ? 'green' : 'red',
    },
    {
      id: 'training-health',
      title: `training-data-web ${trainingDataHealth?.status ?? 'UNKNOWN'}`,
      detail: trainingDataModuleInfo
        ? `${trainingDataModuleInfo.module} / ${trainingDataModuleInfo.features.length} features`
        : trainingDataHealth?.detail ?? '等待训练数据服务探活',
      tone: trainingDataHealth?.status === 'UP' ? 'green' : 'red',
    },
    {
      id: 'warehouse-batch',
      title: lastBatchId ? '最近采集批次' : '等待采集批次',
      detail: lastBatchId ?? '通过训练数据采集生成 batchId 后可查看最近批次',
      tone: lastBatchId ? 'blue' : 'amber',
    },
  ];
}

export function buildPermissionSummary(users: AuthUser[]): PermissionSummary {
  const counts = countRoles(users);
  const total = Math.max(users.length, 1);

  return {
    total: String(users.length),
    segments: [
      { id: 'ok', label: '队员', value: `${counts.player} (${percentage(counts.player, total)})` },
      { id: 'pending', label: '管理员', value: `${counts.admin} (${percentage(counts.admin, total)})` },
      { id: 'danger', label: '禁用', value: `${counts.disable} (${percentage(counts.disable, total)})` },
      {
        id: 'muted',
        label: '未登录',
        value: `${users.length === 0 ? 1 : 0} (${users.length === 0 ? '100%' : '0%'})`,
      },
    ],
  };
}

export function buildTimeline(operations: DashboardOperation[], records: StudentTrainingRecord[]): TimelineItem[] {
  const operationItems = operations.slice(0, 5).map<TimelineItem>((operation) => ({
    id: operation.id,
    title: operation.title,
    meta: operation.detail,
    status: operation.status,
    time: operation.time,
  }));

  const recordItems = records
    .filter((record) => record.handleStatus !== 'bound' || record.summaryStatus !== 'loaded')
    .slice(0, Math.max(0, 5 - operationItems.length))
    .map<TimelineItem>((record) => ({
      id: `record-${record.studentIdentity}`,
      title: record.studentIdentity,
      meta: record.errorMessage ?? (record.handle ? '训练数据待刷新' : '未绑定 OJ handle'),
      status: record.errorMessage ? 'failed' : 'pending',
      time: formatShortTime(record.updatedAt),
    }));

  return [...operationItems, ...recordItems];
}

export function buildAlerts(
  health: ServiceHealth[],
  records: StudentTrainingRecord[],
  operations: DashboardOperation[],
): AlertItem[] {
  const serviceAlerts = health
    .filter((service) => service.status !== 'UP')
    .map<AlertItem>((service) => ({
      id: `service-${service.service}`,
      title: `${service.service} 不可用`,
      detail: service.detail,
      severity: 'error',
      time: 'now',
    }));

  const dataAlerts = records
    .filter((record) => record.handleStatus !== 'bound' || record.summaryStatus === 'error')
    .slice(0, 4)
    .map<AlertItem>((record) => ({
      id: `data-${record.studentIdentity}`,
      title: record.handle ? '训练数据查询异常' : '缺少 OJ handle 绑定',
      detail: record.errorMessage ?? record.studentIdentity,
      severity: record.errorMessage ? 'error' : 'warning',
      time: formatShortTime(record.updatedAt),
    }));

  const failedOperations = operations
    .filter((operation) => operation.status === 'failed')
    .slice(0, 3)
    .map<AlertItem>((operation) => ({
      id: `operation-${operation.id}`,
      title: operation.title,
      detail: operation.detail,
      severity: 'error',
      time: operation.time,
    }));

  return [...serviceAlerts, ...failedOperations, ...dataAlerts];
}

export function buildTasks(
  users: AuthUser[],
  records: StudentTrainingRecord[],
  operations: DashboardOperation[],
): DashboardTask[] {
  const recordByIdentity = new Map(records.map((record) => [record.studentIdentity, record]));
  const userRows = [...users]
    .sort((first, second) => {
      const roleDiff = roleOrder[first.role] - roleOrder[second.role];
      return roleDiff === 0
        ? first.studentIdentity.localeCompare(second.studentIdentity, 'zh-Hans-CN')
        : roleDiff;
    })
    .map<DashboardTask>((user) => {
      const record = recordByIdentity.get(user.studentIdentity);
      const hasError = record?.handleStatus === 'error' || record?.summaryStatus === 'error';
      const unbound = !record || record.handleStatus === 'missing';
      const disabled = user.role === 'disable';
      const acceptedCount = record?.acceptedSummary?.totalAcceptedProblemCount ?? 0;

      return {
        id: `user-${user.studentIdentity}`,
        title: `${roleLabel(user.role)}账号：${user.studentIdentity}`,
        module: disabled || hasError ? 'system' : record?.handle ? 'codeforces' : 'accounts',
        status: disabled ? 'disabled' : hasError ? 'failed' : unbound ? 'pending' : 'completed',
        priority: hasError ? 'P1' : unbound ? 'P2' : disabled ? 'P2' : 'P3',
        owner: {
          name: record?.handle ?? '平台账号',
          role: user.role,
          avatar: avatarOf(user.studentIdentity),
        },
        subjectLabel: record?.handle ? `${user.studentIdentity} / ${record.handle}` : user.studentIdentity,
        studentIdentity: user.studentIdentity,
        source: sourceForRecord(record),
        updatedAt: user.updatedAt,
        action: record?.handle ? '查询' : '绑定',
        detail: record?.handle
          ? `DWS 首次通过 ${acceptedCount} 题，handle=${record.handle}`
          : 'auth-web 中存在账号，training-data 暂无 OJ handle 绑定。',
      };
    });

  const operationRows = operations.slice(0, 4).map<DashboardTask>((operation) => {
    const isCollectionOperation = operation.title.includes('采集') || operation.title.includes('批次');
    return {
      id: `operation-${operation.id}`,
      title: operation.title,
      module: isCollectionOperation ? 'ods-import' : 'system',
      status: operation.status,
      priority: operation.status === 'failed' ? 'P1' : 'P2',
      owner: { name: '接口操作', role: 'admin', avatar: 'API' },
      subjectLabel: operation.detail,
      source: sourceForOperation(operation, isCollectionOperation),
      updatedAt: new Date().toISOString(),
      action: '结果',
      detail: operation.detail,
    };
  });

  return [...operationRows, ...userRows];
}

function sourceForRecord(record: StudentTrainingRecord | undefined) {
  if (!record?.handle) {
    return 'Auth';
  }
  return record.handles[OJ_NAMES.ATCODER] === record.handle ? 'AtCoder' : 'Codeforces';
}

function sourceForOperation(operation: DashboardOperation, isCollectionOperation: boolean) {
  const searchableText = `${operation.title} ${operation.detail}`;
  if (searchableText.includes('AtCoder')) {
    return 'AtCoder';
  }
  if (searchableText.includes('Codeforces')) {
    return 'Codeforces';
  }
  return isCollectionOperation ? 'ODS' : '系统';
}

function countRoles(users: AuthUser[]) {
  return users.reduce(
    (counts, user) => ({
      ...counts,
      [user.role]: counts[user.role] + 1,
    }),
    { admin: 0, player: 0, disable: 0 } satisfies Record<AccountRole, number>,
  );
}

function percentage(value: number, total: number) {
  return `${Math.round((value / total) * 100)}%`;
}

function roleLabel(role: AccountRole) {
  return role === 'admin' ? '管理员' : role === 'player' ? '队员' : '禁用';
}

function avatarOf(studentIdentity: string) {
  return Array.from(studentIdentity.trim()).at(-1) ?? 'U';
}

function formatShortTime(value: string) {
  const date = new Date(value);
  if (!Number.isNaN(date.getTime())) {
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
  }
  return value.slice(11, 16) || 'now';
}
