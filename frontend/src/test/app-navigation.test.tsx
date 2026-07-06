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
    trainingQuery: {
      acceptedFromDateUtcPlus8: '',
      acceptedToDateUtcPlus8: '',
      minProblemRating: '',
      maxProblemRating: '',
    },
    submissionPage: 1,
    submissionLimit: 50,
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
    selectedIdentity: null,
    submissions: null,
    firstAccepted: null,
    lastBatch: null,
    collectionJob: null,
    collectionJobSummary: null,
    collectionJobs: [],
    operations: [],
    errorMessage: null,
    signIn: vi.fn(),
    signOut: vi.fn(),
    refreshDashboard: vi.fn().mockResolvedValue(undefined),
    applyTrainingQuery: vi.fn().mockResolvedValue(undefined),
    changeSubmissionPage: vi.fn().mockResolvedValue(undefined),
    chooseIdentity: vi.fn().mockResolvedValue(undefined),
    batchCollectSubmissions: vi.fn(),
    batchImportStudents: vi.fn(),
    deleteFullUserData: vi.fn(),
    importOdsFile: vi.fn().mockResolvedValue(undefined),
    refreshWarehouse: vi.fn().mockResolvedValue(undefined),
    updateStudentInfo: vi.fn(),
  },
}));

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
    expect(window.location.pathname).toBe('/admin/users');
    expect(screen.getByRole('tab', { name: /用户信息/ })).not.toBeNull();
    expect(screen.getByRole('tab', { name: /数据维护/ })).not.toBeNull();
    expect(screen.getByRole('tab', { name: /操作记录/ })).not.toBeNull();

    await user.click(collectionTab);

    expect(window.location.pathname).toBe('/admin/collection');
    expect(collectionTab.getAttribute('aria-selected')).toBe('true');
    expect(screen.getByRole('heading', { name: '训练数据采集' })).not.toBeNull();
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
});
