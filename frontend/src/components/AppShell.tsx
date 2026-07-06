import {
  LogIn,
  LogOut,
  ShieldCheck,
} from 'lucide-react';
import type { ReactNode } from 'react';
import type { CurrentUser } from '../types';

interface CurrentPageMeta {
  detail: string;
  eyebrow?: string;
  title: string;
}

interface AppShellProps {
  children: ReactNode;
  currentUser?: CurrentUser | null;
  currentPage: CurrentPageMeta;
  onSignIn?: () => void;
  onSignOut?: () => void;
  sidebarContent: ReactNode;
  workspaceSwitcher?: ReactNode;
}

export function AppShell({
  children,
  currentPage,
  currentUser,
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

function roleLabel(role: CurrentUser['role']) {
  return role === 'admin' ? '管理员' : '选手';
}

function avatarOf(studentIdentity: string) {
  return Array.from(studentIdentity.trim()).at(-1) ?? 'U';
}
