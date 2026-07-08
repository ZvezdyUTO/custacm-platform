import { FileText, Plus, Save, Trash2, UserCog, UserPlus, Users } from 'lucide-react';
import type { ChangeEvent, FormEvent } from 'react';
import { Fragment, useEffect, useMemo, useState } from 'react';
import { OJ_LABELS, OJ_NAMES } from '../types';
import type {
  AccountRole,
  AuthUser,
  BatchStudentImportRow,
  BatchStudentImportSummary,
  FullUserDataDeleteSummary,
  OjName,
  StudentIdentity,
  StudentTrainingRecord,
  UserInfoUpdateInput,
  UserInfoUpdateSummary,
} from '../types';

const roleOptions: Array<{ label: string; value: AccountRole }> = [
  { label: '队员', value: 'player' },
  { label: '管理员', value: 'admin' },
  { label: '禁用', value: 'disable' },
];
const studentImportPlaceholder = [
  '格式：学号姓名, role, password, Codeforces handle, AtCoder handle',
  '230511213队员甲,player,,tourist,',
  '230511214队员乙,player,initialPass123,jiangly,',
  '230511215队员丙,admin,,,atcoder_user',
].join('\n');

interface EditableStudentRow {
  id: string;
  studentIdentity: string;
  role: AccountRole;
  password: string;
  codeforcesHandle: string;
  atcoderHandle: string;
}

interface AdminUserManagementPanelProps {
  currentUserIdentity: StudentIdentity | null;
  isRefreshing: boolean;
  onBatchImportStudents: (rows: BatchStudentImportRow[]) => Promise<BatchStudentImportSummary>;
  onDeleteFullUserData: (studentIdentity: StudentIdentity) => Promise<FullUserDataDeleteSummary>;
  onUpdateStudentInfo: (input: UserInfoUpdateInput) => Promise<UserInfoUpdateSummary>;
  records: StudentTrainingRecord[];
  view?: AdminUserManagementView;
  users: AuthUser[];
}

type AdminUserManagementView = 'all' | 'create' | 'edit';

let nextEditableRowId = 0;

export function parseBatchStudentInput(value: string): BatchStudentImportRow[] {
  const trimmed = value.trim();
  if (!trimmed) {
    throw new Error('请先输入至少一条学生信息。');
  }

  if (trimmed.startsWith('[') || trimmed.startsWith('{')) {
    return parseJsonStudentRows(trimmed);
  }

  const rows = trimmed
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0 && !line.startsWith('#'))
    .flatMap((line, index) => parseDelimitedStudentRow(line, index + 1));

  if (rows.length === 0) {
    throw new Error('没有解析到有效学生信息。');
  }
  return rows;
}

export function AdminUserManagementPanel({
  currentUserIdentity,
  isRefreshing,
  onBatchImportStudents,
  onDeleteFullUserData,
  onUpdateStudentInfo,
  records,
  view = 'all',
  users,
}: AdminUserManagementPanelProps) {
  const [studentInput, setStudentInput] = useState('');
  const [createRows, setCreateRows] = useState<EditableStudentRow[]>(() => [emptyEditableRow()]);
  const [createError, setCreateError] = useState<string | null>(null);
  const [createSummary, setCreateSummary] = useState<BatchStudentImportSummary | null>(null);
  const [selectedEditIdentity, setSelectedEditIdentity] = useState('');
  const selectedUser = useMemo(
    () => users.find((user) => user.studentIdentity === selectedEditIdentity) ?? null,
    [selectedEditIdentity, users],
  );
  const selectedRecord = useMemo(
    () => records.find((record) => record.studentIdentity === selectedUser?.studentIdentity) ?? null,
    [records, selectedUser],
  );
  const recordByIdentity = useMemo(
    () => new Map(records.map((record) => [record.studentIdentity, record])),
    [records],
  );
  const sortedUsers = useMemo(
    () => [...users].sort(compareUsersByStudentNumberDesc),
    [users],
  );
  const [editRole, setEditRole] = useState<AccountRole>('player');
  const [editPassword, setEditPassword] = useState('');
  const [editNewStudentIdentity, setEditNewStudentIdentity] = useState('');
  const [editCodeforcesHandle, setEditCodeforcesHandle] = useState('');
  const [editAtcoderHandle, setEditAtcoderHandle] = useState('');
  const [editNeedCollect, setEditNeedCollect] = useState(true);
  const [editError, setEditError] = useState<string | null>(null);
  const [editSummary, setEditSummary] = useState<UserInfoUpdateSummary | null>(null);
  const [editDeleteError, setEditDeleteError] = useState<string | null>(null);
  const [editDeleteSummary, setEditDeleteSummary] = useState<FullUserDataDeleteSummary | null>(null);
  const existingCodeforcesHandle = selectedRecord?.handles[OJ_NAMES.CODEFORCES] ?? '';
  const existingAtcoderHandle = selectedRecord?.handles[OJ_NAMES.ATCODER] ?? '';
  const hasExistingHandle = Boolean(existingCodeforcesHandle || existingAtcoderHandle);
  const canManageSelectedOjBinding = selectedUser ? canManageOjBindingFor(selectedUser.studentIdentity) : false;
  const canEditNeedCollect = Boolean(
    canManageSelectedOjBinding
    && (hasExistingHandle || editCodeforcesHandle.trim() || editAtcoderHandle.trim()),
  );
  const userSuccessCount = createSummary?.userResults.filter((item) => item.success).length ?? 0;
  const handleSuccessCount = createSummary?.handleResults.filter((item) => item.success).length ?? 0;
  const createResultRows = useMemo(
    () => (createSummary ? buildCreateResultRows(createSummary) : []),
    [createSummary],
  );
  const shouldShowCreate = view === 'all' || view === 'create';
  const shouldShowEdit = view === 'all' || view === 'edit';
  const panelLabel = view === 'create'
    ? '创建用户'
    : view === 'edit'
      ? '管理用户信息'
      : '用户信息管理';
  const overviewHeading = view === 'edit' ? '管理用户信息' : '所有用户';

  useEffect(() => {
    if (!selectedUser) {
      return;
    }
    setEditRole(selectedUser.role);
    setEditPassword('');
    setEditNewStudentIdentity(selectedUser.studentIdentity);
    setEditCodeforcesHandle(existingCodeforcesHandle);
    setEditAtcoderHandle(existingAtcoderHandle);
    setEditNeedCollect(selectedRecord?.needCollect ?? true);
    setEditError(null);
    setEditSummary(null);
    setEditDeleteError(null);
    setEditDeleteSummary(null);
  }, [existingAtcoderHandle, existingCodeforcesHandle, selectedRecord?.needCollect, selectedUser]);

  function handleTextImport() {
    setCreateError(null);
    setCreateSummary(null);
    try {
      setCreateRows(toEditableRows(parseBatchStudentInput(studentInput)));
    } catch (error) {
      setCreateError(error instanceof Error ? error.message : '文本导入失败。');
    }
  }

  function updateCreateRow(id: string, field: keyof Omit<EditableStudentRow, 'id'>, value: string) {
    setCreateRows((current) =>
      current.map((row) => (row.id === id ? { ...row, [field]: value } : row)),
    );
  }

  function handleRoleChange(id: string, event: ChangeEvent<HTMLSelectElement>) {
    updateCreateRow(id, 'role', event.target.value as AccountRole);
  }

  function addCreateRow() {
    setCreateRows((current) => [...current, emptyEditableRow()]);
  }

  function removeCreateRow(id: string) {
    setCreateRows((current) => {
      const next = current.filter((row) => row.id !== id);
      return next.length > 0 ? next : [emptyEditableRow()];
    });
  }

  async function handleCreateSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setCreateError(null);
    setCreateSummary(null);
    try {
      const rows = editableRowsToImportRows(createRows);
      const summary = await onBatchImportStudents(rows);
      setCreateSummary(summary);
    } catch (error) {
      setCreateError(error instanceof Error ? error.message : '创建用户失败。');
    }
  }

  async function handleEditSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setEditError(null);
    setEditSummary(null);
    if (!selectedUser) {
      setEditError('请先选择一个用户。');
      return;
    }
    const canManageOjBinding = canManageOjBindingFor(selectedUser.studentIdentity);
    const nextStudentIdentity = editNewStudentIdentity.trim();
    if (canManageOjBinding && !nextStudentIdentity) {
      setEditError('OJ 绑定学号姓名不能为空。');
      return;
    }
    const nextCodeforcesHandle = editCodeforcesHandle.trim();
    const nextAtcoderHandle = editAtcoderHandle.trim();
    const nextHandles = canManageOjBinding
      ? normalizeHandles({
        ...(nextCodeforcesHandle ? { [OJ_NAMES.CODEFORCES]: nextCodeforcesHandle } : {}),
        ...(nextAtcoderHandle ? { [OJ_NAMES.ATCODER]: nextAtcoderHandle } : {}),
      })
      : {};
    const shouldUpdateHandles = canManageOjBinding
      && Object.keys(nextHandles).length > 0
      && (!selectedRecord || !areHandlesEqual(nextHandles, selectedRecord.handles));
    const shouldMigrateIdentity = canManageOjBinding && nextStudentIdentity !== selectedUser.studentIdentity;

    const payload: UserInfoUpdateInput = {
      studentIdentity: selectedUser.studentIdentity,
      role: editRole,
      newPassword: editPassword.trim() || undefined,
      handles: shouldUpdateHandles || shouldMigrateIdentity ? nextHandles : undefined,
    };
    if (shouldMigrateIdentity) {
      payload.newStudentIdentity = nextStudentIdentity;
    }
    if (canEditNeedCollect) {
      payload.needCollect = editNeedCollect;
    }
    try {
      const summary = await onUpdateStudentInfo(payload);
      setEditSummary(summary);
      setEditPassword('');
    } catch (error) {
      setEditError(error instanceof Error ? error.message : '管理用户信息失败。');
    }
  }

  async function handleEditDelete(studentIdentity: StudentIdentity) {
    setEditDeleteError(null);
    setEditDeleteSummary(null);
    if (studentIdentity === currentUserIdentity) {
      setEditDeleteError('不能删除当前登录用户。');
      return;
    }
    if (!confirmHighCostAction(
      `确认彻底删除 ${studentIdentity} 的全部 OJ 训练数据和 auth 账号？OJ handle 绑定会保留。此操作不可恢复。`,
    )) {
      return;
    }
    try {
      const summary = await onDeleteFullUserData(studentIdentity);
      setEditDeleteSummary(summary);
    } catch (error) {
      setEditDeleteError(error instanceof Error ? error.message : '删除用户信息失败。');
    }
  }

  return (
    <section className="admin-user-management-panel" aria-label={panelLabel}>
      {shouldShowCreate ? (
        <form className="admin-management-card user-create-card" onSubmit={handleCreateSubmit}>
        <header>
          <span className="admin-action-icon">
            <UserPlus size={18} aria-hidden="true" />
          </span>
          <div>
            <h2>创建用户</h2>
            <p>文本导入会先填入信息栏；提交时创建账号，并为填写 handle 的行新增 OJ handle 绑定。</p>
          </div>
        </header>

        <div className="user-create-import-grid">
          <div className="batch-textarea-field">
            <label htmlFor="student-batch-import">文本导入</label>
            <textarea
              aria-describedby={createError ? 'create-user-error' : undefined}
              aria-invalid={createError ? true : undefined}
              id="student-batch-import"
              placeholder={studentImportPlaceholder}
              rows={7}
              value={studentInput}
              onChange={(event) => setStudentInput(event.target.value)}
            />
            <div className="batch-textarea-actions">
              <button className="secondary-button" disabled={isRefreshing} onClick={handleTextImport} type="button">
                <FileText size={16} aria-hidden="true" />
                填入信息栏
              </button>
            </div>
          </div>
        </div>

        <div className="editable-user-list" aria-label="创建用户行列表">
          {createRows.map((row, index) => (
            <div className="editable-user-row" key={row.id}>
              <label>
                学号姓名
                <input
                  aria-label={`第 ${index + 1} 行学号姓名`}
                  value={row.studentIdentity}
                  onChange={(event) => updateCreateRow(row.id, 'studentIdentity', event.target.value)}
                />
              </label>
              <label>
                角色
                <select
                  aria-label={`第 ${index + 1} 行角色`}
                  value={row.role}
                  onChange={(event) => handleRoleChange(row.id, event)}
                >
                  {roleOptions.map((role) => (
                    <option key={role.value} value={role.value}>
                      {role.label}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                初始密码
                <input
                  aria-label={`第 ${index + 1} 行初始密码`}
                  value={row.password}
                  onChange={(event) => updateCreateRow(row.id, 'password', event.target.value)}
                  placeholder="留空自动生成"
                  type="text"
                />
              </label>
              <label>
                Codeforces
                <input
                  aria-label={`第 ${index + 1} 行 Codeforces handle`}
                  value={row.codeforcesHandle}
                  onChange={(event) => updateCreateRow(row.id, 'codeforcesHandle', event.target.value)}
                  placeholder="可选"
                />
              </label>
              <label>
                AtCoder
                <input
                  aria-label={`第 ${index + 1} 行 AtCoder handle`}
                  value={row.atcoderHandle}
                  onChange={(event) => updateCreateRow(row.id, 'atcoderHandle', event.target.value)}
                  placeholder="可选"
                />
              </label>
              <button
                aria-label={`删除第 ${index + 1} 行`}
                className="icon-button"
                disabled={isRefreshing || createRows.length === 1}
                onClick={() => removeCreateRow(row.id)}
                type="button"
              >
                <Trash2 size={16} aria-hidden="true" />
              </button>
            </div>
          ))}
        </div>

        <div className="admin-card-actions create-user-actions">
          <div className="create-user-submit-actions">
            <button className="primary-button" disabled={isRefreshing} type="submit">
              <UserPlus size={16} aria-hidden="true" />
              执行创建
            </button>
            <button className="secondary-button compact" disabled={isRefreshing} onClick={addCreateRow} type="button">
              <Plus size={14} aria-hidden="true" />
              增加一名队员
            </button>
          </div>
          <span>{createRows.length} 行待提交</span>
        </div>

        {createError ? (
          <p className="form-error" id="create-user-error" role="alert">
            {createError}
          </p>
        ) : null}
        {createSummary ? (
          <section className="admin-result" aria-label="创建用户结果摘要" aria-live="polite">
            <strong>
              账号 {userSuccessCount}/{createSummary.userResults.length}，绑定 {handleSuccessCount}/
              {createSummary.handleResults.length}
            </strong>
            <div className="admin-result-table-scroll">
              <table className="admin-result-table" aria-label="创建用户结果">
                <colgroup>
                  <col className="admin-result-col-identity" />
                  <col className="admin-result-col-account" />
                  <col className="admin-result-col-handle" />
                  <col className="admin-result-col-password" />
                </colgroup>
                <thead>
                  <tr>
                    <th scope="col">学号姓名</th>
                    <th scope="col">账号</th>
                    <th scope="col">OJ handle</th>
                    <th scope="col">初始密码</th>
                  </tr>
                </thead>
                <tbody>
                  {createResultRows.map((row) => (
                    <tr key={row.studentIdentity}>
                      <th scope="row">{row.studentIdentity}</th>
                      <td>
                        <span className={`admin-result-status is-${row.accountTone}`} title={row.accountTitle}>
                          {row.accountLabel}
                        </span>
                      </td>
                      <td>
                        <span className={`admin-result-status is-${row.handleTone}`} title={row.handleTitle}>
                          {row.handleLabel}
                        </span>
                      </td>
                      <td>
                        {row.plainPassword ? (
                          <code>{row.plainPassword}</code>
                        ) : (
                          <span className="admin-result-empty">无返回</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        ) : null}
        </form>
      ) : null}

      {shouldShowEdit ? (
        <section className="admin-management-card user-overview-card" aria-labelledby="all-users-heading">
        <header>
          <span className="admin-action-icon">
            <Users size={18} aria-hidden="true" />
          </span>
          <div>
            <h2 id="all-users-heading">{overviewHeading}</h2>
            <p>按学号姓名中的学号前缀降序展示；在列表中直接修改角色、密码、OJ handle、现役/退役标记，并查看历史最早数据覆盖情况。</p>
          </div>
        </header>

        <div className="user-overview-meta">
          <strong>{sortedUsers.length}</strong>
          <span>个账号</span>
        </div>

        <div className="user-overview-table-scroll">
          <table className="user-overview-table" aria-label="所有用户">
            <colgroup>
              <col className="user-col-identity" />
              <col className="user-col-handle" />
              <col className="user-col-history" />
              <col className="user-col-collected" />
              <col className="user-col-action" />
            </colgroup>
            <thead>
              <tr>
                <th scope="col">学号姓名</th>
                <th scope="col">OJ handle</th>
                <th scope="col">最早采集</th>
                <th scope="col">最近采集</th>
                <th scope="col">操作</th>
              </tr>
            </thead>
            <tbody>
              {sortedUsers.length === 0 ? (
                <tr>
                  <td colSpan={5}>暂无用户</td>
                </tr>
              ) : (
                sortedUsers.map((user, index) => {
                  const record = recordByIdentity.get(user.studentIdentity);
                  const isEditing = selectedUser?.studentIdentity === user.studentIdentity;
                  const canManageUserOjBinding = canManageOjBindingFor(user.studentIdentity);
                  const editPanelId = `user-edit-panel-${index}`;
                  return (
                    <Fragment key={user.studentIdentity}>
                      <tr
                        className={isEditing ? 'user-overview-row is-editing' : 'user-overview-row'}
                      >
                        <td data-label="学号姓名">
                          <strong>{user.studentIdentity}</strong>
                          <span className="user-overview-tags" aria-label={`${user.studentIdentity} 标签`}>
                            <span className={`user-overview-tag user-role-${user.role}`}>{roleLabel(user.role)}</span>
                            {canManageUserOjBinding ? (
                              <span className={`user-overview-tag user-collect-${needCollectTone(record)}`}>
                                {formatMemberStatus(record)}
                              </span>
                            ) : null}
                          </span>
                        </td>
                        <td data-label="OJ handle">
                          {canManageUserOjBinding ? <HandleBadges handles={record?.handles} /> : '-'}
                        </td>
                        <td data-label="最早采集">{canManageUserOjBinding ? formatHistoryCoverage(record) : '-'}</td>
                        <td data-label="最近采集">{canManageUserOjBinding ? formatLastCollectedAt(record) : '-'}</td>
                        <td data-label="操作">
                          <button
                            aria-controls={editPanelId}
                            aria-expanded={isEditing}
                            aria-label={`${isEditing ? '收起' : '编辑'} ${user.studentIdentity}`}
                            className="secondary-button compact user-overview-edit-button"
                            disabled={isRefreshing}
                            onClick={() => setSelectedEditIdentity(isEditing ? '' : user.studentIdentity)}
                            type="button"
                          >
                            <UserCog size={14} aria-hidden="true" />
                            {isEditing ? '收起' : '编辑'}
                          </button>
                        </td>
                      </tr>
                      {isEditing ? (
                        <tr className="user-overview-edit-row" id={editPanelId}>
                          <td colSpan={5}>
                            <form className="user-list-edit-form" onSubmit={handleEditSubmit}>
                              <div className="user-list-edit-header">
                                <strong>管理用户信息</strong>
                                <span>最后更新：{formatTime(user.updatedAt)}</span>
                              </div>

                              <div className="user-edit-grid">
                                {canManageUserOjBinding ? (
                                  <label className="user-edit-field user-edit-identity-field">
                                    修改学号姓名
                                    <input
                                      aria-label="修改学号姓名"
                                      disabled={isRefreshing}
                                      onChange={(event) => setEditNewStudentIdentity(event.target.value)}
                                      placeholder={user.studentIdentity}
                                      value={editNewStudentIdentity}
                                    />
                                  </label>
                                ) : null}
                                <label className="user-edit-field">
                                  角色
                                  <select
                                    aria-label="管理用户角色"
                                    disabled={isRefreshing}
                                    value={editRole}
                                    onChange={(event) => setEditRole(event.target.value as AccountRole)}
                                  >
                                    {roleOptions.map((role) => (
                                      <option key={role.value} value={role.value}>
                                        {role.label}
                                      </option>
                                    ))}
                                  </select>
                                </label>
                                <label className="user-edit-field">
                                  新密码
                                  <input
                                    aria-label="管理用户新密码"
                                    disabled={isRefreshing}
                                    onChange={(event) => setEditPassword(event.target.value)}
                                    placeholder="不填则不修改"
                                    type="text"
                                    value={editPassword}
                                  />
                                </label>
                                {canManageUserOjBinding ? (
                                  <Fragment>
                                    <label className="user-edit-field">
                                      Codeforces handle
                                      <input
                                        aria-label="管理用户 Codeforces handle"
                                        disabled={Boolean(existingCodeforcesHandle) || isRefreshing}
                                        onChange={(event) => setEditCodeforcesHandle(event.target.value)}
                                        placeholder="可选"
                                        value={editCodeforcesHandle}
                                      />
                                    </label>
                                    <label className="user-edit-field">
                                      AtCoder handle
                                      <input
                                        aria-label="管理用户 AtCoder handle"
                                        disabled={Boolean(existingAtcoderHandle) || isRefreshing}
                                        onChange={(event) => setEditAtcoderHandle(event.target.value)}
                                        placeholder="可选"
                                        value={editAtcoderHandle}
                                      />
                                    </label>
                                    <label className="checkbox-field user-edit-checkbox">
                                      <input
                                        aria-label="是否为现役队员"
                                        checked={editNeedCollect}
                                        disabled={!canEditNeedCollect || isRefreshing}
                                        onChange={(event) => setEditNeedCollect(event.target.checked)}
                                        type="checkbox"
                                      />
                                      现役队员
                                    </label>
                                  </Fragment>
                                ) : null}
                              </div>

                              <div className="admin-card-actions user-edit-actions">
                                <button className="primary-button" disabled={isRefreshing} type="submit">
                                  <Save size={16} aria-hidden="true" />
                                  保存修改
                                </button>
                                <button
                                  className="danger-button subtle"
                                  disabled={isRefreshing || user.studentIdentity === currentUserIdentity}
                                  onClick={() => {
                                    void handleEditDelete(user.studentIdentity);
                                  }}
                                  type="button"
                                >
                                  <Trash2 size={16} aria-hidden="true" />
                                  删除用户信息
                                </button>
                              </div>

                              {editError ? (
                                <p className="form-error" role="alert">
                                  {editError}
                                </p>
                              ) : null}
                              {editDeleteError ? (
                                <p className="form-error" role="alert">
                                  {editDeleteError}
                                </p>
                              ) : null}
                              {editSummary ? (
                                <output className="admin-result compact" aria-live="polite">
                                  <strong>
                                    {editSummary.userResult.studentIdentity} /{' '}
                                    {editSummary.userResult.user?.role ?? editRole}
                                  </strong>
                                  {editSummary.userResult.plainPassword ? (
                                    <code>{editSummary.userResult.plainPassword}</code>
                                  ) : null}
                                  {editSummary.handleResult ? (
                                    <span>
                                      OJ handle：
                                      {editSummary.handleResult.success
                                        ? `${formatHandlesText(editSummary.handleResult.handles)}${
                                          editSummary.handleResult.needCollect === null
                                          || editSummary.handleResult.needCollect === undefined
                                            ? ''
                                            : `，${formatMemberStatusFromNeedCollect(editSummary.handleResult.needCollect)}`
                                        }`
                                        : editSummary.handleResult.errorCode ?? editSummary.handleResult.message}
                                    </span>
                                  ) : null}
                                </output>
                              ) : null}
                              {editDeleteSummary ? (
                                <output className="admin-result compact" aria-live="polite">
                                  <strong>
                                    已删除 {editDeleteSummary.authUserResult.studentIdentity}，训练数据{' '}
                                    {editDeleteSummary.trainingDataResult.totalDeletedRows} 行
                                  </strong>
                                </output>
                              ) : null}
                            </form>
                          </td>
                        </tr>
                      ) : null}
                    </Fragment>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
        </section>
      ) : null}
    </section>
  );
}

function parseJsonStudentRows(value: string): BatchStudentImportRow[] {
  const parsed = JSON.parse(value) as unknown;
  const rows = Array.isArray(parsed)
    ? parsed
    : typeof parsed === 'object' && parsed !== null && Array.isArray((parsed as { users?: unknown }).users)
      ? (parsed as { users: unknown[] }).users
      : null;

  if (!rows) {
    throw new Error('JSON 需要是数组，或包含 users 数组。');
  }

  const result = rows.map((row, index) => {
    if (typeof row !== 'object' || row === null) {
      throw new Error(`第 ${index + 1} 条不是对象。`);
    }
    const source = row as Record<string, unknown>;
    const codeforcesHandle = toStringField(source.codeforcesHandle) || toStringField(source.handle);
    const atcoderHandle = toStringField(source.atcoderHandle);
    return normalizeStudentRow({
      studentIdentity: toStringField(source.studentIdentity),
      role: toStringField(source.role) || 'player',
      password: toStringField(source.password),
      handles: normalizeHandles({
        ...toHandlesRecord(source.handles),
        ...(codeforcesHandle ? { [OJ_NAMES.CODEFORCES]: codeforcesHandle } : {}),
        ...(atcoderHandle ? { [OJ_NAMES.ATCODER]: atcoderHandle } : {}),
      }),
    }, index + 1);
  });

  if (result.length === 0) {
    throw new Error('JSON 中没有学生信息。');
  }
  return result;
}

function parseDelimitedStudentRow(line: string, lineNumber: number): BatchStudentImportRow[] {
  const columns = line.includes('\t') ? line.split('\t') : line.split(',');
  const [
    studentIdentity = '',
    role = 'player',
    password = '',
    codeforcesHandle = '',
    atcoderHandle = '',
  ] = columns.map((item) => item.trim());

  if (studentIdentity.toLowerCase() === 'studentidentity' || studentIdentity === '学号姓名') {
    return [];
  }

  return [normalizeStudentRow({
    studentIdentity,
    role: role || 'player',
    password,
    handles: normalizeHandles({
      [OJ_NAMES.CODEFORCES]: codeforcesHandle,
      [OJ_NAMES.ATCODER]: atcoderHandle,
    }),
  }, lineNumber)];
}

function normalizeStudentRow(
  row: { studentIdentity: string; role: string; password: string; handles: Partial<Record<OjName, string>> },
  lineNumber: number,
): BatchStudentImportRow {
  if (!row.studentIdentity) {
    throw new Error(`第 ${lineNumber} 行缺少学号姓名。`);
  }

  if (!isAccountRole(row.role)) {
    throw new Error(`第 ${lineNumber} 行 role 必须是 admin、player 或 disable。`);
  }

  const handles = normalizeHandles(row.handles);
  return {
    studentIdentity: row.studentIdentity,
    role: row.role,
    password: row.password || undefined,
    handle: handles[OJ_NAMES.CODEFORCES],
    codeforcesHandle: handles[OJ_NAMES.CODEFORCES],
    atcoderHandle: handles[OJ_NAMES.ATCODER],
    handles: hasHandles(handles) ? handles : undefined,
  };
}

function editableRowsToImportRows(rows: EditableStudentRow[]) {
  const filledRows = rows.filter((row) =>
    [row.studentIdentity, row.password, row.codeforcesHandle, row.atcoderHandle].some(
      (value) => value.trim().length > 0,
    ),
  );
  if (filledRows.length === 0) {
    throw new Error('请先填写至少一条用户信息。');
  }
  return filledRows.map((row, index) =>
    normalizeStudentRow({
      studentIdentity: row.studentIdentity.trim(),
      role: row.role,
      password: row.password.trim(),
      handles: normalizeHandles({
        [OJ_NAMES.CODEFORCES]: row.codeforcesHandle.trim(),
        [OJ_NAMES.ATCODER]: row.atcoderHandle.trim(),
      }),
    }, index + 1),
  );
}

function toEditableRows(rows: BatchStudentImportRow[]) {
  return rows.map((row) => ({
    id: nextRowId(),
    studentIdentity: row.studentIdentity,
    role: row.role,
    password: row.password ?? '',
    codeforcesHandle: row.handles?.[OJ_NAMES.CODEFORCES] ?? row.codeforcesHandle ?? row.handle ?? '',
    atcoderHandle: row.handles?.[OJ_NAMES.ATCODER] ?? row.atcoderHandle ?? '',
  }));
}

function emptyEditableRow(): EditableStudentRow {
  return {
    id: nextRowId(),
    studentIdentity: '',
    role: 'player',
    password: '',
    codeforcesHandle: '',
    atcoderHandle: '',
  };
}

type CreateResultTone = 'success' | 'failed' | 'muted';

interface CreateResultRow {
  studentIdentity: string;
  accountLabel: string;
  accountTitle: string;
  accountTone: CreateResultTone;
  handleLabel: string;
  handleTitle: string;
  handleTone: CreateResultTone;
  plainPassword: string | null;
}

function buildCreateResultRows(summary: BatchStudentImportSummary): CreateResultRow[] {
  const handleResultByIdentity = new Map(
    summary.handleResults.map((item) => [item.studentIdentity, item]),
  );
  const userIdentities = new Set(summary.userResults.map((item) => item.studentIdentity));
  const rows = summary.userResults.map((userResult) => {
    const handleResult = handleResultByIdentity.get(userResult.studentIdentity) ?? null;
    return {
      studentIdentity: userResult.studentIdentity,
      accountLabel: userResult.success ? '已创建' : resultErrorLabel(userResult.errorCode, userResult.message, '创建失败'),
      accountTitle: userResult.message ?? userResult.errorCode ?? '',
      accountTone: userResult.success ? 'success' as const : 'failed' as const,
      handleLabel: handleResult ? handleResultLabel(handleResult) : '未填写',
      handleTitle: handleResult?.message ?? handleResult?.errorCode ?? '',
      handleTone: handleResult ? resultTone(handleResult.success) : 'muted' as const,
      plainPassword: userResult.plainPassword,
    };
  });
  const handleOnlyRows = summary.handleResults
    .filter((item) => !userIdentities.has(item.studentIdentity))
    .map((handleResult) => ({
      studentIdentity: handleResult.studentIdentity,
      accountLabel: '未返回',
      accountTitle: '',
      accountTone: 'muted' as const,
      handleLabel: handleResultLabel(handleResult),
      handleTitle: handleResult.message ?? handleResult.errorCode ?? '',
      handleTone: resultTone(handleResult.success),
      plainPassword: null,
    }));
  return [...rows, ...handleOnlyRows];
}

function handleResultLabel(result: BatchStudentImportSummary['handleResults'][number]) {
  if (result.success) {
    return formatHandlesText(result.handles, result.handle ?? '已绑定');
  }
  return resultErrorLabel(result.errorCode, result.message, '绑定失败');
}

function resultErrorLabel(errorCode: string | null, message: string | null, fallback: string) {
  return errorCode ?? message ?? fallback;
}

function resultTone(success: boolean): CreateResultTone {
  return success ? 'success' : 'failed';
}

function nextRowId() {
  nextEditableRowId += 1;
  return `student-row-${nextEditableRowId}`;
}

function toStringField(value: unknown) {
  return value === null || value === undefined ? '' : String(value).trim();
}

function toHandlesRecord(value: unknown): Partial<Record<OjName, string>> {
  if (typeof value !== 'object' || value === null) {
    return {};
  }
  const source = value as Record<string, unknown>;
  return normalizeHandles({
    [OJ_NAMES.CODEFORCES]: toStringField(source[OJ_NAMES.CODEFORCES] ?? source.codeforces),
    [OJ_NAMES.ATCODER]: toStringField(source[OJ_NAMES.ATCODER] ?? source.atcoder),
  });
}

function normalizeHandles(handles: Partial<Record<OjName, string>>): Partial<Record<OjName, string>> {
  return Object.fromEntries(
    Object.entries(handles)
      .map(([ojName, handle]) => [ojName, handle?.trim() ?? ''] as const)
      .filter(([, handle]) => handle.length > 0),
  ) as Partial<Record<OjName, string>>;
}

function hasHandles(handles: Partial<Record<OjName, string>>) {
  return Object.keys(handles).length > 0;
}

function areHandlesEqual(
  left: Partial<Record<OjName, string>>,
  right: Partial<Record<OjName, string>>,
) {
  const normalizedLeft = normalizeHandles(left);
  const normalizedRight = normalizeHandles(right);
  const ojNames = new Set([...Object.keys(normalizedLeft), ...Object.keys(normalizedRight)]);
  return Array.from(ojNames).every((ojName) => (
    normalizedLeft[ojName as OjName] ?? ''
  ) === (
    normalizedRight[ojName as OjName] ?? ''
  ));
}

function isAccountRole(value: string): value is AccountRole {
  return value === 'admin' || value === 'player' || value === 'disable';
}

function roleLabel(role: AccountRole) {
  return roleOptions.find((option) => option.value === role)?.label ?? role;
}

function canManageOjBindingFor(studentIdentity: StudentIdentity) {
  return studentIdentity.trim().toLowerCase() !== 'root';
}

function handleEntries(handles: Partial<Record<OjName, string>> | undefined) {
  return Object.entries(normalizeHandles(handles ?? {})) as Array<[OjName, string]>;
}

function formatHandlesText(handles: Partial<Record<OjName, string>> | undefined, fallback = '-') {
  const labels = handleEntries(handles)
    .map(([ojName, handle]) => `${OJ_LABELS[ojName] ?? ojName}：${handle}`);
  return labels.length > 0 ? labels.join('，') : fallback;
}

function HandleBadges({
  fallback = '-',
  handles,
}: {
  fallback?: string;
  handles: Partial<Record<OjName, string>> | undefined;
}) {
  const entries = handleEntries(handles);
  if (entries.length === 0) {
    return <span className="handle-badge-empty">{fallback}</span>;
  }

  return (
    <span className="handle-badge-list" aria-label={formatHandlesText(handles, fallback)}>
      {entries.map(([ojName, handle]) => (
        <span className="handle-badge" key={ojName}>
          <span className="handle-badge-oj">{OJ_LABELS[ojName] ?? ojName}：</span>
          <span className="handle-badge-value">{handle}</span>
        </span>
      ))}
    </span>
  );
}

function formatMemberStatus(record: StudentTrainingRecord | undefined) {
  if (!record || !hasHandles(record.handles)) {
    return '未绑定';
  }
  return formatMemberStatusFromNeedCollect(record.needCollect);
}

function formatMemberStatusFromNeedCollect(needCollect: boolean | null | undefined) {
  return needCollect === false ? '已退役' : '现役队员';
}

function needCollectTone(record: StudentTrainingRecord | undefined) {
  if (!record || !hasHandles(record.handles)) {
    return 'unbound';
  }
  return record.needCollect === false ? 'off' : 'on';
}

function formatHistoryCoverage(record: StudentTrainingRecord | undefined) {
  if (!record || !hasHandles(record.handles)) {
    return '未绑定';
  }
  const labels = (Object.keys(record.handles) as OjName[]).map((ojName) => {
    const state = record.collectionStates?.[ojName];
    if (!state) {
      return `${OJ_LABELS[ojName] ?? ojName}：未采集`;
    }
    const coverage = state.historyStartReached ? '已到最早' : '未到最早';
    return `${OJ_LABELS[ojName] ?? ojName}：${coverage}`;
  });
  return labels.length > 0 ? labels.join(' / ') : '未绑定';
}

function formatLastCollectedAt(record: StudentTrainingRecord | undefined) {
  if (!record || !hasHandles(record.handles)) {
    return '未绑定';
  }
  const labels = (Object.keys(record.handles) as OjName[]).map((ojName) => {
    const state = record.collectionStates?.[ojName];
    const collectedAt = state?.lastCollectedAt ? formatTime(state.lastCollectedAt) : '未采集';
    return `${OJ_LABELS[ojName] ?? ojName}：${collectedAt}`;
  });
  return labels.length > 0 ? labels.join(' / ') : '未绑定';
}

function compareUsersByStudentNumberDesc(left: AuthUser, right: AuthUser) {
  const leftNumber = extractStudentNumber(left.studentIdentity);
  const rightNumber = extractStudentNumber(right.studentIdentity);
  if (leftNumber && rightNumber) {
    const numberCompare = compareNumericStringDesc(leftNumber, rightNumber);
    return numberCompare === 0
      ? right.studentIdentity.localeCompare(left.studentIdentity, 'zh-CN')
      : numberCompare;
  }
  if (leftNumber) {
    return -1;
  }
  if (rightNumber) {
    return 1;
  }
  return right.studentIdentity.localeCompare(left.studentIdentity, 'zh-CN');
}

function extractStudentNumber(studentIdentity: StudentIdentity) {
  return studentIdentity.match(/^\d+/)?.[0] ?? '';
}

function compareNumericStringDesc(left: string, right: string) {
  const normalizedLeft = left.replace(/^0+/, '') || '0';
  const normalizedRight = right.replace(/^0+/, '') || '0';
  if (normalizedLeft.length !== normalizedRight.length) {
    return normalizedRight.length - normalizedLeft.length;
  }
  return normalizedRight.localeCompare(normalizedLeft);
}

function formatTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date);
}

function confirmHighCostAction(message: string) {
  if (typeof window === 'undefined' || typeof window.confirm !== 'function') {
    return true;
  }
  return window.confirm(message);
}
