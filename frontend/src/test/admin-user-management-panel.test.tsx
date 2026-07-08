import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  AdminUserManagementPanel,
  parseBatchStudentInput,
} from '../components/AdminUserManagementPanel';
import type {
  AuthUser,
  BatchStudentImportSummary,
  FullUserDataDeleteSummary,
  StudentTrainingRecord,
  UserInfoUpdateSummary,
} from '../types';
import { OJ_NAMES } from '../types';

const users: AuthUser[] = [
  {
    studentIdentity: '230511213黄炳睿',
    role: 'player',
    createdAt: '2026-07-06T00:00:00Z',
    updatedAt: '2026-07-06T00:00:00Z',
  },
];

const records: StudentTrainingRecord[] = [
  {
    studentIdentity: '230511213黄炳睿',
    role: 'player',
    handle: null,
    handles: {},
    handleStatus: 'missing',
    acceptedSummary: null,
    summaryStatus: 'not-requested',
    errorMessage: null,
    updatedAt: '2026-07-06T00:00:00Z',
  },
];

const boundRecords: StudentTrainingRecord[] = [
  {
    ...records[0],
    handle: 'tourist',
    handles: { [OJ_NAMES.CODEFORCES]: 'tourist' },
    needCollect: true,
    collectionStates: {
      [OJ_NAMES.CODEFORCES]: {
        historyStartReached: true,
        lastCollectedAt: '2026-07-08T20:00:00',
      },
    },
    handleStatus: 'bound',
  },
];

const importSummary: BatchStudentImportSummary = {
  userResults: [
    {
      success: true,
      studentIdentity: '230511213黄炳睿',
      user: users[0],
      plainPassword: 'initialPass123',
      errorCode: null,
      message: 'user created',
    },
  ],
  handleResults: [
    {
      success: true,
      studentIdentity: '230511213黄炳睿',
      handle: 'tourist',
      handles: { [OJ_NAMES.CODEFORCES]: 'tourist' },
      errorCode: null,
      message: 'handle created',
    },
  ],
};

const multiImportSummary: BatchStudentImportSummary = {
  userResults: [
    {
      success: true,
      studentIdentity: '230511213黄炳睿',
      user: users[0],
      plainPassword: 'initialPass123',
      errorCode: null,
      message: 'user created',
    },
    {
      success: true,
      studentIdentity: '230511215王强',
      user: {
        studentIdentity: '230511215王强',
        role: 'player',
        createdAt: '2026-07-06T00:00:00Z',
        updatedAt: '2026-07-06T00:00:00Z',
      },
      plainPassword: 'generatedPass456',
      errorCode: null,
      message: 'user created',
    },
  ],
  handleResults: [
    {
      success: true,
      studentIdentity: '230511213黄炳睿',
      handle: 'tourist',
      handles: { [OJ_NAMES.CODEFORCES]: 'tourist' },
      errorCode: null,
      message: 'handle created',
    },
  ],
};

const updateSummary: UserInfoUpdateSummary = {
  userResult: {
    success: true,
    studentIdentity: '230511213黄炳睿',
    user: { ...users[0], role: 'disable' },
    plainPassword: null,
    errorCode: null,
    message: 'user updated',
  },
  handleResult: null,
};

const deleteSummary: FullUserDataDeleteSummary = {
  trainingDataResult: {
    studentIdentity: '230511213黄炳睿',
    ojName: OJ_NAMES.CODEFORCES,
    handle: 'tourist',
    handles: { [OJ_NAMES.CODEFORCES]: 'tourist' },
    ojResults: [
      {
        ojName: OJ_NAMES.CODEFORCES,
        handle: 'tourist',
        odsSubmissionRows: 5,
        dwdSubmissionRows: 4,
        dwmFirstAcceptedRows: 3,
        dwsAcceptedSummaryRows: 2,
        totalDeletedRows: 14,
      },
    ],
    handleAccountRows: 0,
    odsSubmissionRows: 5,
    dwdSubmissionRows: 4,
    dwmFirstAcceptedRows: 3,
    dwsAcceptedSummaryRows: 2,
    totalDeletedRows: 14,
  },
  authUserResult: {
    success: true,
    studentIdentity: '230511213黄炳睿',
    user: null,
    plainPassword: null,
    errorCode: null,
    message: 'deleted',
  },
};

describe('AdminUserManagementPanel', () => {
  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it('parses comma-separated student rows with optional Codeforces handles', () => {
    expect(parseBatchStudentInput('230511213黄炳睿,player,,tourist')).toEqual([
      {
        studentIdentity: '230511213黄炳睿',
        role: 'player',
        password: undefined,
        handle: 'tourist',
        codeforcesHandle: 'tourist',
        atcoderHandle: undefined,
        handles: { [OJ_NAMES.CODEFORCES]: 'tourist' },
      },
    ]);
  });

  it('starts the create form without sample text or sample rows', () => {
    render(
      <AdminUserManagementPanel
        currentUserIdentity={null}
        isRefreshing={false}
        onBatchImportStudents={vi.fn().mockResolvedValue(importSummary)}
        onDeleteFullUserData={vi.fn().mockResolvedValue(deleteSummary)}
        onUpdateStudentInfo={vi.fn().mockResolvedValue(updateSummary)}
        records={records}
        users={users}
      />,
    );

    const importTextarea = screen.getByLabelText('文本导入') as HTMLTextAreaElement;
    expect(importTextarea.value).toBe('');
    expect(importTextarea.placeholder).toContain('格式：学号姓名, role, password, Codeforces handle, AtCoder handle');
    expect(importTextarea.placeholder).toContain('230511213队员甲');
    expect(importTextarea.placeholder).not.toContain('黄炳睿');
    expect(importTextarea.placeholder).not.toContain('李明');
    expect(importTextarea.placeholder).not.toContain('王强');
    expect(screen.queryByText('字段顺序')).toBeNull();
    expect(screen.queryByText('创建用户信息栏')).toBeNull();
    expect(screen.getByRole('button', { name: '增加一名队员' })).not.toBeNull();
    expect((screen.getByLabelText('第 1 行学号姓名') as HTMLInputElement).value).toBe('');
    expect((screen.getByLabelText('第 1 行初始密码') as HTMLInputElement).value).toBe('');
    expect((screen.getByLabelText('第 1 行 Codeforces handle') as HTMLInputElement).value).toBe('');
    expect(screen.getByText('1 行待提交')).not.toBeNull();
  });

  it('fills editable user fields from text import before creating users', async () => {
    const user = userEvent.setup();
    const onBatchImportStudents = vi.fn().mockResolvedValue(importSummary);
    render(
      <AdminUserManagementPanel
        currentUserIdentity={null}
        isRefreshing={false}
        onBatchImportStudents={onBatchImportStudents}
        onDeleteFullUserData={vi.fn().mockResolvedValue(deleteSummary)}
        onUpdateStudentInfo={vi.fn().mockResolvedValue(updateSummary)}
        records={records}
        users={users}
      />,
    );

    fireEvent.change(screen.getByLabelText('文本导入'), {
      target: { value: '230511213黄炳睿,player,initialPass123,tourist' },
    });
    await user.click(screen.getByRole('button', { name: '填入信息栏' }));

    expect((screen.getByLabelText('第 1 行学号姓名') as HTMLInputElement).value).toBe('230511213黄炳睿');
    expect((screen.getByLabelText('第 1 行初始密码') as HTMLInputElement).value).toBe('initialPass123');
    expect((screen.getByLabelText('第 1 行 Codeforces handle') as HTMLInputElement).value).toBe('tourist');

    await user.click(screen.getByRole('button', { name: '执行创建' }));

    await waitFor(() => {
      expect(onBatchImportStudents).toHaveBeenCalledWith([
        {
          studentIdentity: '230511213黄炳睿',
          role: 'player',
          password: 'initialPass123',
          handle: 'tourist',
          codeforcesHandle: 'tourist',
          atcoderHandle: undefined,
          handles: { [OJ_NAMES.CODEFORCES]: 'tourist' },
        },
      ]);
    });
    expect(screen.getByText('账号 1/1，绑定 1/1')).not.toBeNull();
  });

  it('renders batch create results as one merged row per student', async () => {
    const user = userEvent.setup();
    render(
      <AdminUserManagementPanel
        currentUserIdentity={null}
        isRefreshing={false}
        onBatchImportStudents={vi.fn().mockResolvedValue(multiImportSummary)}
        onDeleteFullUserData={vi.fn().mockResolvedValue(deleteSummary)}
        onUpdateStudentInfo={vi.fn().mockResolvedValue(updateSummary)}
        records={records}
        users={users}
      />,
    );

    fireEvent.change(screen.getByLabelText('文本导入'), {
      target: {
        value: [
          '230511213黄炳睿,player,,tourist',
          '230511215王强,player,,',
        ].join('\n'),
      },
    });
    await user.click(screen.getByRole('button', { name: '填入信息栏' }));
    await user.click(screen.getByRole('button', { name: '执行创建' }));

    const resultTable = await screen.findByRole('table', { name: '创建用户结果' });
    const dataRows = within(resultTable).getAllByRole('row').slice(1);

    expect(dataRows).toHaveLength(2);
    expect(dataRows[0].textContent).toContain('230511213黄炳睿');
    expect(dataRows[0].textContent).toContain('tourist');
    expect(dataRows[0].textContent).toContain('initialPass123');
    expect(dataRows[1].textContent).toContain('230511215王强');
    expect(dataRows[1].textContent).toContain('未填写');
    expect(dataRows[1].textContent).toContain('generatedPass456');
  });

  it('submits selected user edits to the update handler', async () => {
    const user = userEvent.setup();
    const onUpdateStudentInfo = vi.fn().mockResolvedValue(updateSummary);
    render(
      <AdminUserManagementPanel
        currentUserIdentity={null}
        isRefreshing={false}
        onBatchImportStudents={vi.fn().mockResolvedValue(importSummary)}
        onDeleteFullUserData={vi.fn().mockResolvedValue(deleteSummary)}
        onUpdateStudentInfo={onUpdateStudentInfo}
        records={records}
        users={users}
      />,
    );

    await user.click(screen.getByRole('button', { name: '编辑 230511213黄炳睿' }));
    expect(screen.getByText('修改用户信息')).not.toBeNull();
    expect(screen.getByLabelText('修改学号姓名')).not.toBeNull();
    expect(screen.queryByText('自动生成新密码')).toBeNull();
    fireEvent.change(screen.getByLabelText('修改用户角色'), { target: { value: 'disable' } });
    await user.click(screen.getByRole('button', { name: '保存修改' }));

    await waitFor(() => {
      expect(onUpdateStudentInfo).toHaveBeenCalledWith({
        studentIdentity: '230511213黄炳睿',
        role: 'disable',
        newPassword: undefined,
        handles: undefined,
      });
    });
    expect(screen.getByText('230511213黄炳睿 / disable')).not.toBeNull();
  });

  it('deletes a user from the expanded edit panel after confirmation', async () => {
    const user = userEvent.setup();
    const confirm = vi.fn().mockReturnValue(true);
    vi.stubGlobal('confirm', confirm);
    const onDeleteFullUserData = vi.fn().mockResolvedValue(deleteSummary);
    render(
      <AdminUserManagementPanel
        currentUserIdentity={null}
        isRefreshing={false}
        onBatchImportStudents={vi.fn().mockResolvedValue(importSummary)}
        onDeleteFullUserData={onDeleteFullUserData}
        onUpdateStudentInfo={vi.fn().mockResolvedValue(updateSummary)}
        records={boundRecords}
        users={users}
      />,
    );

    await user.click(screen.getByRole('button', { name: '编辑 230511213黄炳睿' }));
    await user.click(screen.getByRole('button', { name: '删除用户信息' }));

    expect(confirm).toHaveBeenCalledWith(expect.stringContaining('确认彻底删除 230511213黄炳睿'));
    await waitFor(() => {
      expect(onDeleteFullUserData).toHaveBeenCalledWith('230511213黄炳睿');
    });
    expect(screen.getByText('已删除 230511213黄炳睿，训练数据 14 行')).not.toBeNull();
    vi.unstubAllGlobals();
  });

  it('submits Codeforces automatic collection flag changes for bound users', async () => {
    const user = userEvent.setup();
    const onUpdateStudentInfo = vi.fn().mockResolvedValue({
      ...updateSummary,
      handleResult: {
        success: true,
        studentIdentity: '230511213黄炳睿',
        handle: 'tourist',
        handles: { [OJ_NAMES.CODEFORCES]: 'tourist' },
        needCollect: false,
        errorCode: null,
        message: 'collection flag updated',
      },
    } satisfies UserInfoUpdateSummary);
    render(
      <AdminUserManagementPanel
        currentUserIdentity={null}
        isRefreshing={false}
        onBatchImportStudents={vi.fn().mockResolvedValue(importSummary)}
        onDeleteFullUserData={vi.fn().mockResolvedValue(deleteSummary)}
        onUpdateStudentInfo={onUpdateStudentInfo}
        records={boundRecords}
        users={users}
      />,
    );

    await user.click(screen.getByRole('button', { name: '编辑 230511213黄炳睿' }));
    await user.click(screen.getByLabelText('是否需要自动采集'));
    await user.click(screen.getByRole('button', { name: '保存修改' }));

    await waitFor(() => {
      expect(onUpdateStudentInfo).toHaveBeenCalledWith({
        studentIdentity: '230511213黄炳睿',
        role: 'player',
        newPassword: undefined,
        handles: undefined,
        needCollect: false,
      });
    });
    expect(screen.getByText('OJ handle：Codeforces：tourist，自动采集：否')).not.toBeNull();
  });

  it('locks existing handles while allowing missing handle and identity updates', async () => {
    const user = userEvent.setup();
    const onUpdateStudentInfo = vi.fn().mockResolvedValue({
      ...updateSummary,
      handleResult: {
        success: true,
        studentIdentity: '230511214新同学',
        handle: 'tourist',
        handles: {
          [OJ_NAMES.CODEFORCES]: 'tourist',
          [OJ_NAMES.ATCODER]: 'atcoder_id',
        },
        needCollect: true,
        errorCode: null,
        message: 'handle identity changed',
      },
    } satisfies UserInfoUpdateSummary);
    render(
      <AdminUserManagementPanel
        currentUserIdentity={null}
        isRefreshing={false}
        onBatchImportStudents={vi.fn().mockResolvedValue(importSummary)}
        onDeleteFullUserData={vi.fn().mockResolvedValue(deleteSummary)}
        onUpdateStudentInfo={onUpdateStudentInfo}
        records={boundRecords}
        users={users}
      />,
    );

    await user.click(screen.getByRole('button', { name: '编辑 230511213黄炳睿' }));
    const codeforcesInput = screen.getByLabelText('修改用户 Codeforces handle') as HTMLInputElement;
    expect(codeforcesInput.disabled).toBe(true);
    expect(codeforcesInput.value).toBe('tourist');
    await user.clear(screen.getByLabelText('修改学号姓名'));
    await user.type(screen.getByLabelText('修改学号姓名'), '230511214新同学');
    await user.type(screen.getByLabelText('修改用户 AtCoder handle'), 'atcoder_id');
    await user.click(screen.getByRole('button', { name: '保存修改' }));

    await waitFor(() => {
      expect(onUpdateStudentInfo).toHaveBeenCalledWith({
        studentIdentity: '230511213黄炳睿',
        newStudentIdentity: '230511214新同学',
        role: 'player',
        newPassword: undefined,
        handles: {
          [OJ_NAMES.CODEFORCES]: 'tourist',
          [OJ_NAMES.ATCODER]: 'atcoder_id',
        },
        needCollect: true,
      });
    });
    expect(screen.getByText('OJ handle：Codeforces：tourist / AtCoder：atcoder_id，自动采集：是')).not.toBeNull();
  });

  it('lists all users by descending student number at the bottom', () => {
    const allUsers: AuthUser[] = [
      users[0],
      {
        studentIdentity: '230511215王强',
        role: 'admin',
        createdAt: '2026-07-05T00:00:00Z',
        updatedAt: '2026-07-05T00:00:00Z',
      },
      {
        studentIdentity: '220000001李明',
        role: 'player',
        createdAt: '2026-07-04T00:00:00Z',
        updatedAt: '2026-07-04T00:00:00Z',
      },
      {
        studentIdentity: 'root',
        role: 'admin',
        createdAt: '2026-07-03T00:00:00Z',
        updatedAt: '2026-07-03T00:00:00Z',
      },
    ];
    render(
      <AdminUserManagementPanel
        currentUserIdentity="root"
        isRefreshing={false}
        onBatchImportStudents={vi.fn().mockResolvedValue(importSummary)}
        onDeleteFullUserData={vi.fn().mockResolvedValue(deleteSummary)}
        onUpdateStudentInfo={vi.fn().mockResolvedValue(updateSummary)}
        records={[
          ...boundRecords,
          {
            ...records[0],
            studentIdentity: '230511215王强',
            handle: 'wang',
            handles: { [OJ_NAMES.CODEFORCES]: 'wang' },
            handleStatus: 'bound',
            needCollect: false,
            collectionStates: {
              [OJ_NAMES.CODEFORCES]: {
                historyStartReached: false,
                lastCollectedAt: null,
              },
            },
          },
        ]}
        users={allUsers}
      />,
    );

    const table = screen.getByRole('table', { name: '所有用户' });
    const bodyRows = within(table).getAllByRole('row').slice(1);

    expect(bodyRows.map((row) => row.textContent)).toEqual([
      expect.stringContaining('230511215王强'),
      expect.stringContaining('230511213黄炳睿'),
      expect.stringContaining('220000001李明'),
      expect.stringContaining('root'),
    ]);
    expect(bodyRows[0].textContent).toContain('wang');
    expect(bodyRows[0].textContent).toContain('否');
    expect(bodyRows[0].textContent).toContain('Codeforces：未到最早');
    expect(bodyRows[0].textContent).toContain('Codeforces：未采集');
    expect(bodyRows[1]?.textContent).toContain('Codeforces：已到最早');
    expect(bodyRows[1]?.textContent).toContain('Codeforces：2026/07/08 20:00');
    expect(screen.queryByRole('columnheader', { name: '创建时间' })).toBeNull();
    expect(screen.queryByRole('columnheader', { name: '更新时间' })).toBeNull();
    expect(screen.queryByRole('columnheader', { name: '角色' })).toBeNull();
    expect(screen.queryByRole('columnheader', { name: '自动采集' })).toBeNull();
    expect(screen.getByRole('columnheader', { name: '最早采集' })).not.toBeNull();
    expect(screen.getByRole('columnheader', { name: '最近采集' })).not.toBeNull();
  });
});
