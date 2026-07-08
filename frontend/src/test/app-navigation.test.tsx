import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { App } from '../App';

const dashboardMock = vi.hoisted(() => ({
  value: {
    token: 'token',
    currentUser: { studentIdentity: 'root', role: 'admin' },
    status: 'idle',
    health: [
      { service: 'auth-web', status: 'UP', detail: 'UP' },
      { service: 'training-data-web', status: 'UP', detail: 'UP' },
    ],
    moduleInfo: [],
    trainingQuery: {
      acceptedFromDateUtcPlus8: '',
      acceptedToDateUtcPlus8: '',
      minProblemRating: '',
      maxProblemRating: '',
    },
    selectedOjName: 'CODEFORCES',
    submissionPage: 1,
    submissionLimit: 50,
    problemKey: '',
    problemSubmissionPage: 1,
    problemSubmissionLimit: 50,
    users: [
      {
        studentIdentity: 'root',
        role: 'admin',
        createdAt: '2026-07-06T00:00:00Z',
        updatedAt: '2026-07-06T00:00:00Z',
      },
    ],
    records: [],
    boundRecords: [],
    multiUserAcceptedSummaries: [],
    selectedIdentity: null,
    submissions: null,
    firstAccepted: null,
    problemSubmissions: null,
    problemFirstAccepted: null,
    lastBatch: null,
    collectionJob: null,
    collectionJobSummary: null,
    collectionJobs: [],
    operations: [],
    errorMessage: null,
    signIn: vi.fn(),
    signOut: vi.fn(),
    changePassword: vi.fn(),
    refreshDashboard: vi.fn().mockResolvedValue(undefined),
    applyTrainingQuery: vi.fn().mockResolvedValue(undefined),
    changeSubmissionPage: vi.fn().mockResolvedValue(undefined),
    changeProblemSubmissionPage: vi.fn().mockResolvedValue(undefined),
    changeProblemKey: vi.fn(),
    chooseIdentity: vi.fn().mockResolvedValue(undefined),
    chooseOjName: vi.fn(),
    batchCollectSubmissions: vi.fn(),
    batchImportStudents: vi.fn(),
    deleteFullUserData: vi.fn(),
    updateStudentInfo: vi.fn(),
  },
}));

const deleteSummary = {
  trainingDataResult: {
    studentIdentity: '230511213黄炳睿',
    ojName: null,
    handle: null,
    handles: {},
    ojResults: [],
    handleAccountRows: 0,
    odsSubmissionRows: 4,
    dwdSubmissionRows: 4,
    dwmFirstAcceptedRows: 3,
    dwsAcceptedSummaryRows: 3,
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

vi.mock('../hooks/usePlatformDashboard', () => ({
  usePlatformDashboard: () => dashboardMock.value,
}));

describe('App navigation', () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
    window.history.pushState(null, '', '/');
  });

  it('shows feature modules in the query workspace sidebar and separates unavailable modules', () => {
    render(<App />);

    expect(screen.getByText('功能模块')).not.toBeNull();
    expect(screen.getByText('可用功能')).not.toBeNull();
    expect(screen.getByText('暂未开放')).not.toBeNull();
    expect(screen.getByRole('button', { name: /训练数据管理模块/ }).getAttribute('disabled')).toBeNull();
    expect(screen.getByRole('button', { name: /博客模块/ })).toHaveProperty('disabled', true);
    expect(screen.getByRole('button', { name: /编辑器模块/ })).toHaveProperty('disabled', true);
    expect(screen.getAllByText('未支持')).toHaveLength(2);
  });

  it('moves admin page tabs into the sidebar after switching to the admin workspace', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('tab', { name: '管理员操作' }));

    const collectionTab = screen.getByRole('tab', { name: /数据采集/ });
    expect(window.location.pathname).toBe('/admin/user-create');
    expect(screen.getByRole('tab', { name: /创建用户/ })).not.toBeNull();
    expect(screen.getByRole('tab', { name: /修改用户/ })).not.toBeNull();
    expect(screen.queryByRole('tab', { name: /数据维护/ })).toBeNull();
    expect(screen.getByRole('tab', { name: /操作记录/ })).not.toBeNull();

    await user.click(collectionTab);

    expect(window.location.pathname).toBe('/admin/collection');
    expect(collectionTab.getAttribute('aria-selected')).toBe('true');
    expect(screen.getByRole('heading', { name: '训练数据采集' })).not.toBeNull();
  });

  it('keeps create and edit user pages separate in admin navigation', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('tab', { name: '管理员操作' }));

    expect(screen.getByRole('heading', { name: '创建用户' })).not.toBeNull();
    expect(screen.queryByRole('heading', { name: '修改用户信息' })).toBeNull();

    await user.click(screen.getByRole('tab', { name: /修改用户/ }));

    expect(window.location.pathname).toBe('/admin/user-edit');
    expect(screen.getByRole('heading', { name: '修改用户信息' })).not.toBeNull();
    expect(screen.queryByRole('heading', { name: '创建用户' })).toBeNull();
  });

  it('opens the workspace page encoded in the URL path', () => {
    window.history.pushState(null, '', '/admin/collection');

    render(<App />);

    const collectionTab = screen.getByRole('tab', { name: /数据采集/ });
    expect(collectionTab.getAttribute('aria-selected')).toBe('true');
    expect(screen.getByRole('heading', { name: '训练数据采集' })).not.toBeNull();
  });

  it('writes the query mode into the URL path', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('tab', { name: '单人查询' }));

    expect(window.location.pathname).toBe('/query/single');
    expect(screen.getByRole('tab', { name: '单人查询' }).getAttribute('aria-selected')).toBe('true');
  });

  it('writes the problem query mode into the URL path', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('tab', { name: '题目查询' }));

    expect(window.location.pathname).toBe('/query/problem');
    expect(screen.getByRole('tab', { name: '题目查询' }).getAttribute('aria-selected')).toBe('true');
  });

  it('submits current account password changes from the top-right account area', async () => {
    const user = userEvent.setup();
    const changePassword = vi.fn().mockResolvedValue(undefined);
    const previousDashboard = dashboardMock.value;
    dashboardMock.value = {
      ...previousDashboard,
      changePassword,
    } as typeof previousDashboard;

    try {
      render(<App />);

      await user.click(screen.getByRole('button', { name: '改密码' }));
      await user.type(screen.getByLabelText('旧密码'), 'old-pass');
      await user.type(screen.getByLabelText('新密码'), 'new-pass');
      await user.type(screen.getByLabelText('确认新密码'), 'new-pass');
      await user.click(screen.getByRole('button', { name: '保存' }));

      expect(changePassword).toHaveBeenCalledWith({
        oldPassword: 'old-pass',
        newPassword: 'new-pass',
        confirmNewPassword: 'new-pass',
      });
    } finally {
      dashboardMock.value = previousDashboard;
    }
  });

  it('opens login from the top-right account area when signed out', async () => {
    const user = userEvent.setup();
    const previousDashboard = dashboardMock.value;
    const signIn = vi.fn().mockResolvedValue(undefined);
    dashboardMock.value = {
      ...previousDashboard,
      currentUser: null,
      errorMessage: null,
      signIn,
      token: null,
    } as unknown as typeof previousDashboard;

    try {
      render(<App />);

      expect(screen.queryByRole('dialog', { name: '账号登录' })).toBeNull();

      await user.click(screen.getByRole('button', { name: /登录/ }));

      const dialog = screen.getByRole('dialog', { name: '账号登录' });
      expect(dialog).not.toBeNull();

      await user.type(screen.getByLabelText('学号姓名'), '230511213黄炳睿');
      await user.type(screen.getByLabelText('密码'), 'secret');
      await user.click(within(dialog).getByRole('button', { name: '登录' }));

      expect(signIn).toHaveBeenCalledWith({
        password: 'secret',
        studentIdentity: '230511213黄炳睿',
      });
      await waitFor(() => {
        expect(screen.queryByRole('dialog', { name: '账号登录' })).toBeNull();
      });
    } finally {
      dashboardMock.value = previousDashboard;
    }
  });

  it('keeps full user deletion out of the global notice area', async () => {
    const user = userEvent.setup();
    const previousDashboard = dashboardMock.value;
    const deleteFullUserData = vi.fn().mockResolvedValue(deleteSummary);
    vi.stubGlobal('confirm', vi.fn().mockReturnValue(true));
    dashboardMock.value = {
      ...previousDashboard,
      deleteFullUserData,
      records: [
        {
          studentIdentity: '230511213黄炳睿',
          role: 'player',
          handle: 'tourist',
          handles: { CODEFORCES: 'tourist' },
          needCollect: true,
          collectionStates: {},
          handleStatus: 'bound',
          acceptedSummary: null,
          summaryStatus: 'not-requested',
          errorMessage: null,
          updatedAt: '2026-07-06T00:00:00Z',
        },
      ],
      users: [
        ...previousDashboard.users,
        {
          studentIdentity: '230511213黄炳睿',
          role: 'player',
          createdAt: '2026-07-06T00:00:00Z',
          updatedAt: '2026-07-06T00:00:00Z',
        },
      ],
    } as typeof previousDashboard;

    try {
      render(<App />);

      await user.click(screen.getByRole('tab', { name: '管理员操作' }));
      await user.click(screen.getByRole('tab', { name: /修改用户/ }));
      await user.click(screen.getByRole('button', { name: '编辑 230511213黄炳睿' }));
      await user.click(screen.getByRole('button', { name: '删除用户信息' }));

      await waitFor(() => {
        expect(deleteFullUserData).toHaveBeenCalledWith('230511213黄炳睿');
      });
      expect(screen.getByText('已删除 230511213黄炳睿，训练数据 14 行')).not.toBeNull();
      expect(screen.queryByText('正在彻底删除 230511213黄炳睿 的训练数据和 auth 账号。')).toBeNull();
      expect(screen.queryByText('已彻底删除 230511213黄炳睿，训练数据 14 行，auth 已删除。')).toBeNull();
    } finally {
      vi.unstubAllGlobals();
      dashboardMock.value = previousDashboard;
    }
  });
});
