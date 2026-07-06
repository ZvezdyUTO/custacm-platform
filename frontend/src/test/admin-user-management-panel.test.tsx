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
  StudentTrainingRecord,
  UserInfoUpdateSummary,
} from '../types';

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
    needCollect: true,
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

describe('AdminUserManagementPanel', () => {
  afterEach(() => cleanup());

  it('parses comma-separated student rows with optional Codeforces handles', () => {
    expect(parseBatchStudentInput('230511213黄炳睿,player,,tourist')).toEqual([
      {
        studentIdentity: '230511213黄炳睿',
        role: 'player',
        password: undefined,
        handle: 'tourist',
      },
    ]);
  });

  it('starts the create form without sample text or sample rows', () => {
    render(
      <AdminUserManagementPanel
        currentUserIdentity={null}
        isRefreshing={false}
        onBatchImportStudents={vi.fn().mockResolvedValue(importSummary)}
        onUpdateStudentInfo={vi.fn().mockResolvedValue(updateSummary)}
        records={records}
        users={users}
      />,
    );

    expect((screen.getByLabelText('文本导入') as HTMLTextAreaElement).value).toBe('');
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

    await user.click(screen.getByRole('button', { name: '创建用户' }));

    await waitFor(() => {
      expect(onBatchImportStudents).toHaveBeenCalledWith([
        {
          studentIdentity: '230511213黄炳睿',
          role: 'player',
          password: 'initialPass123',
          handle: 'tourist',
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
    await user.click(screen.getByRole('button', { name: '创建用户' }));

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
        onUpdateStudentInfo={onUpdateStudentInfo}
        records={records}
        users={users}
      />,
    );

    await user.click(screen.getByRole('button', { name: '编辑 230511213黄炳睿' }));
    fireEvent.change(screen.getByLabelText('修改用户角色'), { target: { value: 'disable' } });
    await user.click(screen.getByRole('button', { name: '保存修改' }));

    await waitFor(() => {
      expect(onUpdateStudentInfo).toHaveBeenCalledWith({
        studentIdentity: '230511213黄炳睿',
        role: 'disable',
        newPassword: undefined,
        handle: undefined,
      });
    });
    expect(screen.getByText('230511213黄炳睿 / disable')).not.toBeNull();
  });

  it('submits Codeforces automatic collection flag changes for bound users', async () => {
    const user = userEvent.setup();
    const onUpdateStudentInfo = vi.fn().mockResolvedValue({
      ...updateSummary,
      handleResult: {
        success: true,
        studentIdentity: '230511213黄炳睿',
        handle: 'tourist',
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
        handle: undefined,
        needCollect: false,
      });
    });
    expect(screen.getByText('Codeforces handle：tourist，自动采集：否')).not.toBeNull();
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
        onUpdateStudentInfo={vi.fn().mockResolvedValue(updateSummary)}
        records={[
          ...boundRecords,
          {
            ...records[0],
            studentIdentity: '230511215王强',
            handle: 'wang',
            handleStatus: 'bound',
            needCollect: false,
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
  });
});
