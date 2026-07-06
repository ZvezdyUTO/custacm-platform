import { FileText, Plus, Save, Trash2, UserCog, UserPlus, Users } from 'lucide-react';
import type { ChangeEvent, FormEvent } from 'react';
import { Fragment, useEffect, useMemo, useState } from 'react';
import type {
  AccountRole,
  AuthUser,
  BatchStudentImportRow,
  BatchStudentImportSummary,
  StudentIdentity,
  StudentTrainingRecord,
  UserInfoUpdateInput,
  UserInfoUpdateSummary,
} from '../types';

const roleOptions: Array<{ label: string; value: AccountRole }> = [
  { label: '选手', value: 'player' },
  { label: '管理员', value: 'admin' },
  { label: '禁用', value: 'disable' },
];

interface EditableStudentRow {
  id: string;
  studentIdentity: string;
  role: AccountRole;
  password: string;
  handle: string;
}

interface AdminUserManagementPanelProps {
  currentUserIdentity: StudentIdentity | null;
  isRefreshing: boolean;
  onBatchImportStudents: (rows: BatchStudentImportRow[]) => Promise<BatchStudentImportSummary>;
  onUpdateStudentInfo: (input: UserInfoUpdateInput) => Promise<UserInfoUpdateSummary>;
  records: StudentTrainingRecord[];
  users: AuthUser[];
}

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
  onUpdateStudentInfo,
  records,
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
  const [generatePassword, setGeneratePassword] = useState(false);
  const [editHandle, setEditHandle] = useState('');
  const [editNeedCollect, setEditNeedCollect] = useState(true);
  const [editError, setEditError] = useState<string | null>(null);
  const [editSummary, setEditSummary] = useState<UserInfoUpdateSummary | null>(null);
  const existingHandle = selectedRecord?.handle ?? '';
  const canEditNeedCollect = Boolean(existingHandle || editHandle.trim());
  const userSuccessCount = createSummary?.userResults.filter((item) => item.success).length ?? 0;
  const handleSuccessCount = createSummary?.handleResults.filter((item) => item.success).length ?? 0;
  const createResultRows = useMemo(
    () => (createSummary ? buildCreateResultRows(createSummary) : []),
    [createSummary],
  );

  useEffect(() => {
    if (!selectedUser) {
      return;
    }
    setEditRole(selectedUser.role);
    setEditPassword('');
    setGeneratePassword(false);
    setEditHandle(selectedRecord?.handle ?? '');
    setEditNeedCollect(selectedRecord?.needCollect ?? true);
    setEditError(null);
    setEditSummary(null);
  }, [selectedRecord?.handle, selectedRecord?.needCollect, selectedUser]);

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
    const nextHandle = editHandle.trim();
    if (existingHandle && nextHandle !== existingHandle) {
      setEditError('当前接口只支持新增未绑定 handle，暂不支持修改已有 handle。');
      return;
    }

    const payload: UserInfoUpdateInput = {
      studentIdentity: selectedUser.studentIdentity,
      role: editRole,
      newPassword: generatePassword ? '' : editPassword.trim() || undefined,
      handle: existingHandle ? undefined : nextHandle || undefined,
    };
    if (canEditNeedCollect) {
      payload.needCollect = editNeedCollect;
    }
    try {
      const summary = await onUpdateStudentInfo(payload);
      setEditSummary(summary);
      setEditPassword('');
      setGeneratePassword(false);
    } catch (error) {
      setEditError(error instanceof Error ? error.message : '修改用户信息失败。');
    }
  }

  return (
    <section className="admin-user-management-panel" aria-label="用户信息管理">
      <form className="admin-management-card user-create-card" onSubmit={handleCreateSubmit}>
        <header>
          <span className="admin-action-icon">
            <UserPlus size={18} aria-hidden="true" />
          </span>
          <div>
            <h2>创建用户</h2>
            <p>文本导入会先填入信息栏；提交时创建账号，并为填写 handle 的行新增 Codeforces 绑定。</p>
          </div>
        </header>

        <div className="user-create-import-grid">
          <label className="batch-textarea-field">
            文本导入
            <textarea
              aria-describedby={createError ? 'create-user-error' : undefined}
              aria-invalid={createError ? true : undefined}
              rows={7}
              value={studentInput}
              onChange={(event) => setStudentInput(event.target.value)}
            />
          </label>
          <div className="import-format-panel">
            <div>
              <span>字段顺序</span>
              <code>学号姓名, role, password, handle</code>
            </div>
            <button className="secondary-button" disabled={isRefreshing} onClick={handleTextImport} type="button">
              <FileText size={16} aria-hidden="true" />
              填入信息栏
            </button>
          </div>
        </div>

        <div className="editable-user-list" aria-label="创建用户信息栏">
          <div className="editable-user-list-header">
            <strong>创建用户信息栏</strong>
            <button className="secondary-button compact" disabled={isRefreshing} onClick={addCreateRow} type="button">
              <Plus size={14} aria-hidden="true" />
              增加一行
            </button>
          </div>
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
                Codeforces handle
                <input
                  aria-label={`第 ${index + 1} 行 Codeforces handle`}
                  value={row.handle}
                  onChange={(event) => updateCreateRow(row.id, 'handle', event.target.value)}
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

        <div className="admin-card-actions">
          <button className="primary-button" disabled={isRefreshing} type="submit">
            <UserPlus size={16} aria-hidden="true" />
            创建用户
          </button>
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
                    <th scope="col">Codeforces handle</th>
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

      <section className="admin-management-card user-overview-card" aria-labelledby="all-users-heading">
        <header>
          <span className="admin-action-icon">
            <Users size={18} aria-hidden="true" />
          </span>
          <div>
            <h2 id="all-users-heading">所有用户</h2>
            <p>按学号姓名中的学号前缀降序展示；在列表中直接修改角色、密码、Codeforces handle 和自动采集状态。</p>
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
              <col className="user-col-role" />
              <col className="user-col-handle" />
              <col className="user-col-collect" />
              <col className="user-col-created" />
              <col className="user-col-updated" />
              <col className="user-col-action" />
            </colgroup>
            <thead>
              <tr>
                <th scope="col">学号姓名</th>
                <th scope="col">角色</th>
                <th scope="col">Codeforces handle</th>
                <th scope="col">自动采集</th>
                <th scope="col">创建时间</th>
                <th scope="col">更新时间</th>
                <th scope="col">操作</th>
              </tr>
            </thead>
            <tbody>
              {sortedUsers.length === 0 ? (
                <tr>
                  <td colSpan={7}>暂无用户</td>
                </tr>
              ) : (
                sortedUsers.map((user, index) => {
                  const record = recordByIdentity.get(user.studentIdentity);
                  const isEditing = selectedUser?.studentIdentity === user.studentIdentity;
                  const editPanelId = `user-edit-panel-${index}`;
                  return (
                    <Fragment key={user.studentIdentity}>
                      <tr
                        className={isEditing ? 'user-overview-row is-editing' : 'user-overview-row'}
                      >
                        <td data-label="学号姓名">
                          <strong>{user.studentIdentity}</strong>
                          {user.studentIdentity === currentUserIdentity ? <small>当前登录</small> : null}
                        </td>
                        <td data-label="角色">{roleLabel(user.role)}</td>
                        <td data-label="Codeforces handle">{record?.handle ?? '-'}</td>
                        <td data-label="自动采集">{formatNeedCollect(record)}</td>
                        <td data-label="创建时间">{formatTime(user.createdAt)}</td>
                        <td data-label="更新时间">{formatTime(user.updatedAt)}</td>
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
                          <td colSpan={7}>
                            <form className="user-list-edit-form" onSubmit={handleEditSubmit}>
                              <div className="user-list-edit-header">
                                <strong>修改 {user.studentIdentity}</strong>
                                <span>最后更新：{formatTime(user.updatedAt)}</span>
                              </div>

                              <div className="user-edit-grid">
                                <label className="user-edit-field">
                                  角色
                                  <select
                                    aria-label="修改用户角色"
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
                                    aria-label="修改用户新密码"
                                    disabled={generatePassword || isRefreshing}
                                    onChange={(event) => setEditPassword(event.target.value)}
                                    placeholder="不填则不修改"
                                    type="text"
                                    value={editPassword}
                                  />
                                </label>
                                <label className="checkbox-field user-edit-checkbox">
                                  <input
                                    checked={generatePassword}
                                    disabled={isRefreshing}
                                    onChange={(event) => setGeneratePassword(event.target.checked)}
                                    type="checkbox"
                                  />
                                  自动生成新密码
                                </label>
                                <label className="user-edit-field wide">
                                  Codeforces handle
                                  <input
                                    aria-label="修改用户 Codeforces handle"
                                    disabled={Boolean(existingHandle) || isRefreshing}
                                    onChange={(event) => setEditHandle(event.target.value)}
                                    placeholder={existingHandle ? '已绑定' : '可选'}
                                    value={editHandle}
                                  />
                                </label>
                                <label className="checkbox-field user-edit-checkbox">
                                  <input
                                    aria-label="是否需要自动采集"
                                    checked={editNeedCollect}
                                    disabled={!canEditNeedCollect || isRefreshing}
                                    onChange={(event) => setEditNeedCollect(event.target.checked)}
                                    type="checkbox"
                                  />
                                  需要自动采集
                                </label>
                              </div>

                              <div className="admin-card-actions">
                                <button className="primary-button" disabled={isRefreshing} type="submit">
                                  <Save size={16} aria-hidden="true" />
                                  保存修改
                                </button>
                              </div>

                              {editError ? (
                                <p className="form-error" role="alert">
                                  {editError}
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
                                      Codeforces handle：
                                      {editSummary.handleResult.success
                                        ? `${editSummary.handleResult.handle}${
                                          editSummary.handleResult.needCollect === null
                                          || editSummary.handleResult.needCollect === undefined
                                            ? ''
                                            : `，自动采集：${editSummary.handleResult.needCollect ? '是' : '否'}`
                                        }`
                                        : editSummary.handleResult.errorCode ?? editSummary.handleResult.message}
                                    </span>
                                  ) : null}
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
    return normalizeStudentRow({
      studentIdentity: toStringField(source.studentIdentity),
      role: toStringField(source.role) || 'player',
      password: toStringField(source.password),
      handle: toStringField(source.handle),
    }, index + 1);
  });

  if (result.length === 0) {
    throw new Error('JSON 中没有学生信息。');
  }
  return result;
}

function parseDelimitedStudentRow(line: string, lineNumber: number): BatchStudentImportRow[] {
  const columns = line.includes('\t') ? line.split('\t') : line.split(',');
  const [studentIdentity = '', role = 'player', password = '', handle = ''] = columns.map((item) => item.trim());

  if (studentIdentity.toLowerCase() === 'studentidentity' || studentIdentity === '学号姓名') {
    return [];
  }

  return [normalizeStudentRow({ studentIdentity, role: role || 'player', password, handle }, lineNumber)];
}

function normalizeStudentRow(
  row: { studentIdentity: string; role: string; password: string; handle: string },
  lineNumber: number,
): BatchStudentImportRow {
  if (!row.studentIdentity) {
    throw new Error(`第 ${lineNumber} 行缺少学号姓名。`);
  }

  if (!isAccountRole(row.role)) {
    throw new Error(`第 ${lineNumber} 行 role 必须是 admin、player 或 disable。`);
  }

  return {
    studentIdentity: row.studentIdentity,
    role: row.role,
    password: row.password || undefined,
    handle: row.handle || undefined,
  };
}

function editableRowsToImportRows(rows: EditableStudentRow[]) {
  const filledRows = rows.filter((row) =>
    [row.studentIdentity, row.password, row.handle].some((value) => value.trim().length > 0),
  );
  if (filledRows.length === 0) {
    throw new Error('请先填写至少一条用户信息。');
  }
  return filledRows.map((row, index) =>
    normalizeStudentRow({
      studentIdentity: row.studentIdentity.trim(),
      role: row.role,
      password: row.password.trim(),
      handle: row.handle.trim(),
    }, index + 1),
  );
}

function toEditableRows(rows: BatchStudentImportRow[]) {
  return rows.map((row) => ({
    id: nextRowId(),
    studentIdentity: row.studentIdentity,
    role: row.role,
    password: row.password ?? '',
    handle: row.handle ?? '',
  }));
}

function emptyEditableRow(): EditableStudentRow {
  return {
    id: nextRowId(),
    studentIdentity: '',
    role: 'player',
    password: '',
    handle: '',
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
    return result.handle ?? '已绑定';
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

function isAccountRole(value: string): value is AccountRole {
  return value === 'admin' || value === 'player' || value === 'disable';
}

function roleLabel(role: AccountRole) {
  return roleOptions.find((option) => option.value === role)?.label ?? role;
}

function formatNeedCollect(record: StudentTrainingRecord | undefined) {
  if (!record || record.handleStatus !== 'bound') {
    return '未绑定';
  }
  return record.needCollect === false ? '否' : '是';
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
