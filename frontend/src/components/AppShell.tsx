import {
  KeyRound,
  LogIn,
  LogOut,
  Save,
  ShieldCheck,
} from 'lucide-react';
import type { FormEvent, ReactNode } from 'react';
import { useState } from 'react';
import type { ChangeCurrentPasswordRequest, CurrentUser } from '../types';

interface CurrentPageMeta {
  detail: string;
  eyebrow?: string;
  title: string;
}

interface AppShellProps {
  children: ReactNode;
  currentUser?: CurrentUser | null;
  currentPage: CurrentPageMeta;
  onChangePassword?: (request: ChangeCurrentPasswordRequest) => Promise<void>;
  onSignIn?: () => void;
  onSignOut?: () => void;
  sidebarContent: ReactNode;
  workspaceSwitcher?: ReactNode;
}

export function AppShell({
  children,
  currentPage,
  currentUser,
  onChangePassword,
  onSignIn,
  onSignOut,
  sidebarContent,
  workspaceSwitcher,
}: AppShellProps) {
  return (
    <div className="app-shell">
      <aside className="context-sidebar" aria-label="当前工作台范围">
        <div className="sidebar-brand" aria-label="custacm wiki">
          <img className="brand-logo" src="/custacm-acm-logo.png" alt="" aria-hidden="true" />
          <span>
            <strong>custacm wiki</strong>
            <small>训练数据管理面板</small>
          </span>
        </div>
        <div className="current-page-card" aria-current="page">
          <span>{currentPage.eyebrow ?? '当前页面'}</span>
          <strong>{currentPage.title}</strong>
          <small>{currentPage.detail}</small>
        </div>
        {sidebarContent}
      </aside>

      <div className="workspace">
        <header className="topbar">
          <div className="topbar-title">
            <span className="topbar-icon">
              <ShieldCheck size={14} aria-hidden="true" />
            </span>
            <span>custacm wiki</span>
          </div>
          {workspaceSwitcher ? <div className="topbar-workspace-switcher">{workspaceSwitcher}</div> : null}
          <div className="topbar-actions" aria-label="全局操作">
            {currentUser ? (
              <div className="account-summary">
                <span className="avatar">{avatarOf(currentUser.studentIdentity)}</span>
                <span>
                  <strong>{currentUser.studentIdentity}</strong>
                  <small>{roleLabel(currentUser.role)}</small>
                </span>
              </div>
            ) : (
              <button className="account-summary account-button" type="button" onClick={onSignIn}>
                <span className="avatar" aria-hidden="true">
                  <LogIn size={15} />
                </span>
                <span>
                  <strong>登录</strong>
                  <small>访客</small>
                </span>
              </button>
            )}
            {currentUser && onChangePassword ? (
              <PasswordChangeMenu onChangePassword={onChangePassword} />
            ) : null}
            {currentUser && onSignOut ? (
              <button className="secondary-button compact" type="button" onClick={onSignOut}>
                <LogOut size={15} aria-hidden="true" />
                退出
              </button>
            ) : null}
          </div>
        </header>
        {children}
      </div>
    </div>
  );
}

interface PasswordChangeMenuProps {
  onChangePassword: (request: ChangeCurrentPasswordRequest) => Promise<void>;
}

function PasswordChangeMenu({ onChangePassword }: PasswordChangeMenuProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmNewPassword, setConfirmNewPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    if (!newPassword || newPassword !== confirmNewPassword) {
      setError('两次输入的新密码必须一致。');
      return;
    }
    setIsSubmitting(true);
    try {
      await onChangePassword({ oldPassword, newPassword, confirmNewPassword });
      setOldPassword('');
      setNewPassword('');
      setConfirmNewPassword('');
      setIsOpen(false);
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : '修改密码失败。');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="password-menu">
      <button
        aria-expanded={isOpen}
        className="secondary-button compact"
        onClick={() => setIsOpen((current) => !current)}
        type="button"
      >
        <KeyRound size={15} aria-hidden="true" />
        改密码
      </button>
      {isOpen ? (
        <form className="password-change-form" onSubmit={handleSubmit}>
          <label>
            旧密码
            <input
              autoComplete="current-password"
              required
              value={oldPassword}
              onChange={(event) => setOldPassword(event.target.value)}
              type="password"
            />
          </label>
          <label>
            新密码
            <input
              autoComplete="new-password"
              required
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              type="password"
            />
          </label>
          <label>
            确认新密码
            <input
              autoComplete="new-password"
              required
              value={confirmNewPassword}
              onChange={(event) => setConfirmNewPassword(event.target.value)}
              type="password"
            />
          </label>
          {error ? <p role="alert">{error}</p> : null}
          <button className="primary-button compact" disabled={isSubmitting} type="submit">
            <Save size={14} aria-hidden="true" />
            保存
          </button>
        </form>
      ) : null}
    </div>
  );
}

function roleLabel(role: CurrentUser['role']) {
  return role === 'admin' ? '管理员' : '队员';
}

function avatarOf(studentIdentity: string) {
  return Array.from(studentIdentity.trim()).at(-1) ?? 'U';
}
