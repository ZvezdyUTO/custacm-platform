// Author: huangbingrui.awa
import { OJ_NAMES } from '../types';
import type { AccountRole, AdminUserCreateRequest, AdminUserMutationResponse, OjName } from '../types';

export interface UserFormState {
  username: string;
  nickname: string;
  email: string;
  role: AccountRole;
  password: string;
  codeforcesHandle: string;
  atcoderHandle: string;
  needCollect: boolean;
}

export function emptyUserForm(): UserFormState {
  return { username: '', nickname: '', email: '', role: 'ROLE_player', password: '', codeforcesHandle: '', atcoderHandle: '', needCollect: true };
}

export function userFormOf(response: AdminUserMutationResponse): UserFormState {
  return {
    username: response.user.username,
    nickname: response.user.nickname || '', email: response.user.email || '',
    role: response.user.role, password: '',
    codeforcesHandle: response.handles.CODEFORCES || '', atcoderHandle: response.handles.ATCODER || '',
    needCollect: response.needCollect ?? true,
  };
}

export function handlesOf(form: Pick<UserFormState, 'codeforcesHandle' | 'atcoderHandle'>) {
  const handles: Partial<Record<OjName, string>> = {};
  if (form.codeforcesHandle.trim()) handles[OJ_NAMES.CODEFORCES] = form.codeforcesHandle.trim();
  if (form.atcoderHandle.trim()) handles[OJ_NAMES.ATCODER] = form.atcoderHandle.trim();
  return handles;
}

export function createRequestOf(form: UserFormState): AdminUserCreateRequest {
  const username = form.username.trim();
  if (!username) throw new Error('username 不能为空。');
  if (form.password && (form.password.length < 6 || form.password.length > 128)) {
    throw new Error('初始密码长度需为 6 到 128 个字符。');
  }
  return {
    username, nickname: form.nickname.trim(), email: form.email.trim(), role: form.role,
    ...(form.password ? { password: form.password } : {}), handles: handlesOf(form), needCollect: form.needCollect,
  };
}

export function parseBatchUserInput(value: string): AdminUserCreateRequest[] {
  const rows = value.split(/\r?\n/).map((line) => line.trim()).filter((line) => line && !line.startsWith('#')).flatMap((line, index) => {
    const columns = (line.includes('\t') ? line.split('\t') : line.split(',')).map((column) => column.trim());
    const [username = '', nickname = '', email = '', role = 'ROLE_player', password = '', codeforcesHandle = '', atcoderHandle = '', needCollect = 'true'] = columns;
    if (username.toLowerCase() === 'username') return [];
    if (!username) throw new Error(`第 ${index + 1} 行缺少 username。`);
    if (role !== 'ROLE_admin' && role !== 'ROLE_player') throw new Error(`第 ${index + 1} 行 role 必须是 ROLE_admin 或 ROLE_player。`);
    if (!['', 'true', 'false'].includes(needCollect.toLowerCase())) throw new Error(`第 ${index + 1} 行 needCollect 必须是 true 或 false。`);
    return [{ username, nickname, email, role: role as AccountRole, ...(password ? { password } : {}), handles: handlesOf({ codeforcesHandle, atcoderHandle }), needCollect: needCollect.toLowerCase() !== 'false' }];
  });
  if (!rows.length) throw new Error('请至少输入一个用户。');
  return rows;
}

export function parseCreateUserRows(value: string): UserFormState[] {
  const lines = value.split(/\r?\n/).map((line) => line.trim()).filter((line) => line && !line.startsWith('#'));
  const rows = lines.flatMap((line, index) => {
    const columns = (line.includes('\t') ? line.split('\t') : line.split(',')).map((column) => column.trim());
    const [username = '', nickname = '', roleValue = 'player', password = '', codeforcesHandle = '', atcoderHandle = ''] = columns;
    if (username.toLowerCase() === 'username') return [];
    if (!username) throw new Error(`第 ${index + 1} 行缺少 username。`);
    const normalizedRole = roleValue.toLowerCase();
    const role: AccountRole = normalizedRole === 'admin' || normalizedRole === 'role_admin'
      ? 'ROLE_admin'
      : normalizedRole === 'player' || normalizedRole === 'role_player' || !normalizedRole
        ? 'ROLE_player'
        : (() => { throw new Error(`第 ${index + 1} 行角色必须是 player 或 admin。`); })();
    return [{ ...emptyUserForm(), username, nickname, role, password, codeforcesHandle, atcoderHandle }];
  });
  if (!rows.length) throw new Error('请至少输入一行用户数据。');
  return rows;
}
