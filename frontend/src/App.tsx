import type { ChangeEvent } from 'react';
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  BookOpen,
  ClipboardList,
  Database,
  Edit3,
  RefreshCw,
  Users,
  Wrench,
  X,
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import { AdminUserManagementPanel } from './components/AdminUserManagementPanel';
import { AppShell } from './components/AppShell';
import { LoginPanel } from './components/LoginPanel';
import { SidePanel } from './components/SidePanel';
import { StateStrip } from './components/StateStrip';
import { TaskTable } from './components/TaskTable';
import { Toolbar } from './components/Toolbar';
import { TrainingDataCollectionPanel, TrainingDataMaintenancePanel } from './components/TrainingDataOpsPanel';
import { TrainingQueryPanel } from './components/TrainingQueryPanel';
import { usePlatformDashboard } from './hooks/usePlatformDashboard';
import { filterAndSortTasks } from './hooks/useDashboardFilters';
import { UNLIMITED_LOOKBACK_HOURS } from './types';
import type {
  BatchCollectOptions,
  BatchStudentImportRow,
  DashboardView,
  Filters,
  StudentIdentity,
  TrainingQueryMode,
  UserInfoUpdateInput,
  WorkspaceView,
} from './types';
import {
  buildAlerts,
  buildOperationsStatuses,
  buildPermissionSummary,
  buildTasks,
  buildTimeline,
} from './utils/dashboardModels';

const tabs: Array<{ id: DashboardView; label: string }> = [
  { id: 'all', label: '全部数据项' },
  { id: 'accounts', label: '账号权限' },
  { id: 'codeforces', label: 'Codeforces' },
  { id: 'ods-import', label: 'ODS 导入' },
  { id: 'system', label: '系统状态' },
];

type AdminWorkspaceView = 'users' | 'collection' | 'maintenance' | 'records';

interface AppRouteState {
  adminView: AdminWorkspaceView;
  queryMode: TrainingQueryMode;
  workspaceView: WorkspaceView;
}

const adminTabs: Array<{ id: AdminWorkspaceView; label: string; detail: string }> = [
  { id: 'users', label: '用户信息', detail: '创建账号、角色、密码、CF 绑定' },
  { id: 'collection', label: '数据采集', detail: '提交采集、采集任务列表' },
  { id: 'maintenance', label: '数据维护', detail: 'ODS 导入、仓库刷新、删除数据' },
  { id: 'records', label: '操作记录', detail: '服务健康、系统任务、异常记录' },
];

const backendModuleItems = [
  { id: 'training-data', label: '训练数据管理模块', detail: '训练查询 / 管理员操作', supported: true },
  { id: 'blog', label: '博客模块', detail: '未支持', supported: false },
  { id: 'editor', label: '编辑器模块', detail: '未支持', supported: false },
];

const backendModuleIcons: Record<(typeof backendModuleItems)[number]['id'], LucideIcon> = {
  'training-data': Database,
  blog: BookOpen,
  editor: Edit3,
};

const adminTabIcons: Record<AdminWorkspaceView, LucideIcon> = {
  users: Users,
  collection: RefreshCw,
  maintenance: Wrench,
  records: ClipboardList,
};

const defaultFilters: Filters = {
  query: '',
  status: 'all',
  priority: 'all',
  role: 'all',
  source: 'all',
  view: 'all',
};

const defaultRouteState: AppRouteState = {
  adminView: 'users',
  queryMode: 'multiple',
  workspaceView: 'query',
};

function readRouteState(): AppRouteState {
  if (typeof window === 'undefined') {
    return defaultRouteState;
  }
  return routeStateFromPath(window.location.pathname);
}

function routeStateFromPath(pathname: string): AppRouteState {
  const [workspace, page] = pathname.split('/').filter(Boolean);
  if (workspace === 'admin') {
    return {
      ...defaultRouteState,
      adminView: isAdminWorkspaceView(page) ? page : 'users',
      workspaceView: 'admin',
    };
  }
  if (workspace === 'query') {
    return {
      ...defaultRouteState,
      queryMode: page === 'single' ? 'single' : 'multiple',
      workspaceView: 'query',
    };
  }
  return defaultRouteState;
}

function routePathFromState(routeState: AppRouteState) {
  if (routeState.workspaceView === 'admin') {
    return `/admin/${routeState.adminView}`;
  }
  return `/query/${routeState.queryMode}`;
}

function isAdminWorkspaceView(value: string | undefined): value is AdminWorkspaceView {
  return value === 'users' || value === 'collection' || value === 'maintenance' || value === 'records';
}

export function App() {
  const {
    token,
    currentUser,
    status,
    health,
    trainingQuery,
    submissionPage,
    submissionLimit,
    users,
    records,
    autoCollectAcceptedSummaries,
    boundRecords,
    selectedIdentity,
    submissions,
    firstAccepted,
    lastBatch,
    collectionJob,
    collectionJobSummary,
    collectionJobs,
    operations,
    errorMessage,
    signIn,
    signOut,
    refreshDashboard,
    applyTrainingQuery,
    changeSubmissionPage,
    chooseIdentity,
    batchCollectSubmissions,
    batchImportStudents,
    deleteFullUserData,
    importOdsFile,
    refreshWarehouse,
    updateStudentInfo,
  } = usePlatformDashboard();

  const fileInputRef = useRef<HTMLInputElement>(null);
  const [routeState, setRouteState] = useState<AppRouteState>(() => readRouteState());
  const [filters, setFilters] = useState<Filters>(defaultFilters);
  const [isLoginOpen, setIsLoginOpen] = useState(false);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [actionNotice, setActionNotice] = useState('已加载真实接口数据，等待本地后端响应。');
  const isRefreshing = status === 'loading';
  const isAdmin = currentUser?.role === 'admin';
  const { adminView, queryMode, workspaceView } = routeState;

  useEffect(() => {
    if (currentUser) {
      setIsLoginOpen(false);
    }
  }, [currentUser]);

  useEffect(() => {
    function handlePopState() {
      setRouteState(readRouteState());
    }

    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  function navigateRoute(nextRouteState: Partial<AppRouteState>) {
    const route = {
      ...routeState,
      ...nextRouteState,
    };
    const path = routePathFromState(route);
    if (window.location.pathname !== path) {
      window.history.pushState(null, '', path);
    }
    setRouteState(route);
  }

  const tasks = useMemo(
    () => buildTasks(users, records, operations),
    [operations, records, users],
  );
  const visibleTasks = useMemo(() => filterAndSortTasks(tasks, filters), [filters, tasks]);
  const stateStatuses = useMemo(
    () => buildOperationsStatuses(health, Boolean(token), lastBatch?.batchId ?? null),
    [health, lastBatch, token],
  );
  const permissionSummary = useMemo(() => buildPermissionSummary(users), [users]);
  const timeline = useMemo(() => buildTimeline(operations, records), [operations, records]);
  const alerts = useMemo(() => buildAlerts(health, records, operations), [health, operations, records]);
  const selectedRecord = useMemo(
    () => records.find((record) => record.studentIdentity === selectedIdentity) ?? null,
    [records, selectedIdentity],
  );
  const queryStudentOptions = useMemo(
    () => {
      const bound = boundRecords.map((record) => record.studentIdentity);
      return bound.length > 0 ? bound : records.map((record) => record.studentIdentity);
    },
    [boundRecords, records],
  );
  const collectableRecords = useMemo(
    () => boundRecords
      .filter((record) => record.needCollect !== false),
    [boundRecords],
  );
  const updatedAt = new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(new Date());

  function handleSelect(id: string) {
    setSelectedIds((current) => {
      const next = new Set(current);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  function handleSelectAll(checked: boolean) {
    setSelectedIds(checked ? new Set(visibleTasks.map((task) => task.id)) : new Set());
  }

  function updateFilters(nextFilters: Filters) {
    setFilters(nextFilters);
    setSelectedIds(new Set());
  }

  function clearFilters() {
    updateFilters(defaultFilters);
    setActionNotice('已清除管理员列表筛选。');
  }

  async function handleRefresh(mode: TrainingQueryMode = 'multiple') {
    setActionNotice('正在通过真实 HTTP 接口刷新数据。');
    await refreshDashboard(undefined, undefined, {
      loadStudentDetails: mode === 'single',
    });
  }

  async function handleSignIn(credentials: { studentIdentity: string; password: string }) {
    await signIn(credentials);
    setActionNotice('登录成功。');
    setIsLoginOpen(false);
  }

  async function handleChooseIdentity(studentIdentity: StudentIdentity) {
    setActionNotice(`已选择 ${studentIdentity}。`);
    await chooseIdentity(studentIdentity);
  }

  async function handleRefreshWarehouse() {
    setActionNotice('正在调用 admin warehouse:refresh 接口。');
    await refreshWarehouse();
  }

  async function handleBatchImportStudents(rows: BatchStudentImportRow[]) {
    setActionNotice(`正在批量创建 ${rows.length} 个学生账号并写入 Codeforces 绑定。`);
    return batchImportStudents(rows);
  }

  async function handleUpdateStudentInfo(input: UserInfoUpdateInput) {
    setActionNotice(`正在修改 ${input.studentIdentity} 的账号信息。`);
    return updateStudentInfo(input);
  }

  async function handleBatchCollect(options: BatchCollectOptions) {
    const lookbackLabel = options.lookbackHours >= UNLIMITED_LOOKBACK_HOURS ? '不限时间范围' : `最近 ${options.lookbackHours} 小时`;
    setActionNotice(
      `正在按${lookbackLabel}批量采集 ${options.studentIdentities.length} 个选手。`,
    );
    return batchCollectSubmissions(options);
  }

  async function handleDeleteFullUserData(studentIdentity: StudentIdentity) {
    setActionNotice(`正在彻底删除 ${studentIdentity} 的训练数据和 auth 账号。`);
    return deleteFullUserData(studentIdentity);
  }

  function handleImportClick() {
    fileInputRef.current?.click();
  }

  async function handleOdsFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) {
      return;
    }
    setActionNotice(`正在上传 ${file.name} 到 Codeforces ODS upsert 接口，成功后刷新仓库。`);
    await importOdsFile(file);
  }

  function exportVisibleTasks() {
    const selectedTasks = visibleTasks.filter((task) => selectedIds.has(task.id));
    const tasksToExport = selectedTasks.length > 0 ? selectedTasks : visibleTasks;
    const exportScope = selectedTasks.length > 0 ? '已选' : '当前';
    const payload = JSON.stringify(tasksToExport, null, 2);
    const blob = new Blob([payload], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `custacm-dashboard-${exportScope}-${Date.now()}.json`;
    anchor.click();
    URL.revokeObjectURL(url);
    setActionNotice(`已导出${exportScope} ${tasksToExport.length} 条管理员列表数据。`);
  }

  const tableTotal = Math.max(tasks.length, visibleTasks.length);
  const activeAdminTab = adminTabs.find((tab) => tab.id === adminView) ?? adminTabs[0];
  const currentPage = workspaceView === 'query'
    ? { eyebrow: '当前模块', title: '训练数据管理模块', detail: '训练查询' }
    : {
        eyebrow: '当前页面',
        title: '管理员操作',
        detail: isAdmin ? activeAdminTab.label : token ? '需要管理员权限' : '未登录',
      };
  const availableModuleItems = backendModuleItems.filter((item) => item.supported);
  const unavailableModuleItems = backendModuleItems.filter((item) => !item.supported);
  const sidebarContent = workspaceView === 'query' ? (
    <nav className="module-section" aria-label="功能模块">
      <h2>功能模块</h2>
      <span className="module-group-label">可用功能</span>
      <ul>
        {availableModuleItems.map((item) => {
          const ModuleIcon = backendModuleIcons[item.id];
          const tone = item.supported ? 'live' : 'passive';
          return (
            <li key={item.id}>
              <button
                className={`module-static-item module-nav-button tone-${tone}${
                  item.id === 'training-data' ? ' is-active' : ''
                }`}
                disabled={!item.supported}
                onClick={() => navigateRoute({ workspaceView: 'query' })}
                type="button"
              >
                <span className={`module-item-icon tone-${tone}`}>
                  <ModuleIcon size={17} aria-hidden="true" />
                </span>
                <span className="module-item-copy">
                  <strong>{item.label}</strong>
                  <small>{item.detail}</small>
                </span>
              </button>
            </li>
          );
        })}
      </ul>
      {unavailableModuleItems.length > 0 ? (
        <>
          <span className="module-group-label muted">暂未开放</span>
          <ul className="module-list-muted">
            {unavailableModuleItems.map((item) => {
              const ModuleIcon = backendModuleIcons[item.id];
              const tone = item.supported ? 'live' : 'passive';
              return (
                <li key={item.id}>
                  <button
                    className={`module-static-item module-nav-button tone-${tone}`}
                    disabled
                    onClick={() => navigateRoute({ workspaceView: 'query' })}
                    type="button"
                  >
                    <span className={`module-item-icon tone-${tone}`}>
                      <ModuleIcon size={17} aria-hidden="true" />
                    </span>
                    <span className="module-item-copy">
                      <strong>{item.label}</strong>
                      <small>{item.detail}</small>
                    </span>
                  </button>
                </li>
              );
            })}
          </ul>
        </>
      ) : null}
    </nav>
  ) : (
    <nav className="module-section" aria-label="管理员页面">
      <h2>训练数据管理模块</h2>
      <ul role="tablist">
        {adminTabs.map((tab) => {
          const TabIcon = adminTabIcons[tab.id];
          return (
            <li key={tab.id}>
              <button
                aria-controls={`admin-panel-${tab.id}`}
                aria-selected={adminView === tab.id}
                className={`module-static-item module-nav-button tone-ready${
                  adminView === tab.id ? ' is-active' : ''
                }`}
                disabled={!isAdmin}
                id={`admin-tab-${tab.id}`}
                onClick={() => navigateRoute({ adminView: tab.id, workspaceView: 'admin' })}
                role="tab"
                type="button"
              >
                <span className="module-item-icon tone-ready">
                  <TabIcon size={17} aria-hidden="true" />
                </span>
                <span className="module-item-copy">
                  <strong>{tab.label}</strong>
                  <small>{isAdmin ? tab.detail : '需要管理员权限'}</small>
                </span>
              </button>
            </li>
          );
        })}
      </ul>
    </nav>
  );

  return (
    <AppShell
      currentUser={currentUser}
      currentPage={currentPage}
      onSignIn={() => setIsLoginOpen(true)}
      onSignOut={currentUser ? signOut : undefined}
      sidebarContent={sidebarContent}
      workspaceSwitcher={(
        <div className="workspace-mode-tabs" role="tablist" aria-label="工作区" aria-orientation="horizontal">
          <button
            aria-controls="workspace-panel-query"
            aria-selected={workspaceView === 'query'}
            className={workspaceView === 'query' ? 'is-active' : ''}
            id="workspace-tab-query"
            onClick={() => navigateRoute({ workspaceView: 'query' })}
            role="tab"
            type="button"
          >
            训练查询
          </button>
          <button
            aria-controls="workspace-panel-admin"
            aria-selected={workspaceView === 'admin'}
            className={workspaceView === 'admin' ? 'is-active' : ''}
            id="workspace-tab-admin"
          onClick={() => navigateRoute({ workspaceView: 'admin' })}
          role="tab"
          type="button"
        >
            管理员操作
          </button>
        </div>
      )}
    >
      <main className="dashboard-main">
        <input
          accept="application/json,.json"
          aria-label="上传 Codeforces ODS JSON"
          aria-hidden="true"
          className="visually-hidden"
          onChange={handleOdsFileChange}
          ref={fileInputRef}
          tabIndex={-1}
          type="file"
        />
        {isLoginOpen ? (
          <div className="login-dialog-backdrop">
            <section
              aria-labelledby="login-title"
              aria-modal="true"
              className="login-dialog"
              role="dialog"
            >
              <button
                aria-label="关闭登录"
                className="icon-button compact login-dialog-close"
                onClick={() => setIsLoginOpen(false)}
                type="button"
              >
                <X size={16} aria-hidden="true" />
              </button>
              <LoginPanel isLoading={isRefreshing} errorMessage={errorMessage} onSubmit={handleSignIn} />
            </section>
          </div>
        ) : null}

        {workspaceView === 'query' ? (
          <section id="workspace-panel-query" role="tabpanel" aria-labelledby="workspace-tab-query">
            <TrainingQueryPanel
              autoCollectSummaries={autoCollectAcceptedSummaries}
              firstAccepted={firstAccepted}
              isRefreshing={isRefreshing}
              onApplyQuery={applyTrainingQuery}
              onQueryModeChange={(mode) => navigateRoute({ queryMode: mode, workspaceView: 'query' })}
              onRefresh={handleRefresh}
              onSubmissionPageChange={changeSubmissionPage}
              onSelectedIdentityChange={handleChooseIdentity}
              query={trainingQuery}
              queryMode={queryMode}
              record={selectedRecord}
              selectedIdentity={selectedIdentity}
              submissionLimit={submissionLimit}
              submissionPage={submissionPage}
              studentOptions={queryStudentOptions}
              submissions={submissions}
              updatedAt={updatedAt}
            />
            <div className="operation-toast" role="status" aria-live="polite">
              {errorMessage ? `接口提示：${errorMessage}` : actionNotice}
            </div>
          </section>
        ) : (
          <section
            aria-labelledby="workspace-tab-admin"
            className="admin-workspace"
            id="workspace-panel-admin"
            role="tabpanel"
          >
            {!token ? (
              <section className="admin-gate">
                <h1>未登录</h1>
                <p>登录后根据账号权限显示管理功能。</p>
              </section>
            ) : null}

            {token && !isAdmin ? (
              <section className="admin-gate">
                <h1>需要管理员权限</h1>
                <p>当前账号只能查看训练数据查询页，导入、采集和仓库刷新只对 admin 开放。</p>
              </section>
            ) : null}

            {isAdmin ? (
              <>
                <div className="admin-header">
                  <div>
                    <h1>管理员操作</h1>
                    <p>用户信息、数据采集、数据维护和运行记录分开处理，避免把创建、查询和高风险动作混在同一屏。</p>
                  </div>
                </div>

                {adminView === 'users' ? (
                  <section id="admin-panel-users" role="tabpanel" aria-labelledby="admin-tab-users">
                    <AdminUserManagementPanel
                      currentUserIdentity={currentUser?.studentIdentity ?? null}
                      isRefreshing={isRefreshing}
                      onBatchImportStudents={handleBatchImportStudents}
                      onUpdateStudentInfo={handleUpdateStudentInfo}
                      records={records}
                      users={users}
                    />
                  </section>
                ) : null}

                {adminView === 'collection' ? (
                  <section id="admin-panel-collection" role="tabpanel" aria-labelledby="admin-tab-collection">
                    <TrainingDataCollectionPanel
                      collectableRecords={collectableRecords}
                      collectionJob={collectionJob}
                      collectionJobs={collectionJobs}
                      collectionJobSummary={collectionJobSummary}
                      isRefreshing={isRefreshing}
                      onBatchCollect={handleBatchCollect}
                    />
                  </section>
                ) : null}

                {adminView === 'maintenance' ? (
                  <section id="admin-panel-maintenance" role="tabpanel" aria-labelledby="admin-tab-maintenance">
                    <TrainingDataMaintenancePanel
                      canRefreshWarehouse={Boolean(lastBatch)}
                      currentUserIdentity={currentUser?.studentIdentity ?? null}
                      isRefreshing={isRefreshing}
                      lastBatch={lastBatch}
                      onDeleteFullUserData={handleDeleteFullUserData}
                      onImportOds={handleImportClick}
                      onRefreshWarehouse={handleRefreshWarehouse}
                      userStudentOptions={users.map((user) => user.studentIdentity)}
                    />
                  </section>
                ) : null}

                {adminView === 'records' ? (
                  <section id="admin-panel-records" role="tabpanel" aria-labelledby="admin-tab-records">
                    <div className="view-header">
                      <nav className="tabs" aria-label="管理员列表视图" role="tablist">
                        {tabs.map((tab) => (
                          <button
                            aria-selected={filters.view === tab.id}
                            className={filters.view === tab.id ? 'is-active' : ''}
                            key={tab.id}
                            onClick={() => updateFilters({ ...filters, view: tab.id })}
                            role="tab"
                            type="button"
                          >
                            {tab.label}
                          </button>
                        ))}
                      </nav>
                    </div>

                    <Toolbar
                      exportLabel={selectedIds.size > 0 ? '导出已选' : '导出'}
                      filters={filters}
                      isRefreshing={isRefreshing}
                      onClearFilters={clearFilters}
                      onExport={exportVisibleTasks}
                      onFiltersChange={updateFilters}
                      onRefresh={handleRefresh}
                      onSelectedIdentityChange={handleChooseIdentity}
                      selectedIdentity={selectedIdentity}
                      studentOptions={queryStudentOptions}
                    />

                    <StateStrip isRefreshing={isRefreshing} statuses={stateStatuses} />

                    {selectedIds.size > 0 ? (
                      <div className="batch-bar" role="status">
                        <strong>已选择 {selectedIds.size} 项</strong>
                        <button type="button" onClick={exportVisibleTasks}>
                          导出已选
                        </button>
                        <button type="button" onClick={() => setSelectedIds(new Set())}>
                          取消选择
                        </button>
                      </div>
                    ) : null}

                    <section className="content-grid">
                      <section className="table-panel" aria-label="管理员数据列表">
                        <TaskTable
                          isLoading={isRefreshing}
                          onSelect={handleSelect}
                          onSelectAll={handleSelectAll}
                          selectedIds={selectedIds}
                          tasks={visibleTasks}
                          totalTasks={tableTotal}
                        />
                      </section>
                      <SidePanel alerts={alerts} permissionSummary={permissionSummary} timeline={timeline} />
                    </section>
                  </section>
                ) : null}

                <div className="operation-toast" role="status" aria-live="polite">
                  {errorMessage ? `接口提示：${errorMessage}` : actionNotice}
                </div>
              </>
            ) : null}
          </section>
        )}
      </main>
    </AppShell>
  );
}
