// Author: huangbingrui.awa
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { cwd } from 'node:process';
import { describe, expect, it } from 'vitest';

const stylesEntry = readFileSync(resolve(cwd(), 'src/styles.css'), 'utf8');
const redesignCss = readFileSync(resolve(cwd(), 'src/styles/training-redesign.css'), 'utf8');
const viewSource = readFileSync(resolve(cwd(), 'src/views/TrainingView.vue'), 'utf8');
const loginSource = readFileSync(resolve(cwd(), 'src/components/LoginPanel.vue'), 'utf8');
const loginFooterSource = readFileSync(resolve(cwd(), 'src/components/LoginFooter.vue'), 'utf8');

describe('Training visual redesign contract', () => {
  it('loads generated tokens first without a switchable dark override', () => {
    expect(stylesEntry.indexOf('./styles/tokens.css')).toBeLessThan(stylesEntry.indexOf('./styles/foundation.css'));
    expect(stylesEntry).not.toContain('./styles/dark.css');
  });

  it('limits glass styling to approved floating surfaces', () => {
    expect(redesignCss).toContain('.blog-topbar');
    expect(redesignCss).toContain('.top-nav-dropdown');
    expect(redesignCss).toContain('.admin-confirm-dialog');
    expect(redesignCss).toContain('backdrop-filter: var(--glass-filter)');
    expect(redesignCss).not.toMatch(/\.table-panel[^}]*backdrop-filter/s);
    expect(redesignCss).not.toMatch(/\.side-section[^}]*backdrop-filter/s);
  });

  it('uses shared motion tokens for the route transition', () => {
    expect(viewSource).toContain('<Transition name="route-fade" mode="out-in">');
    expect(redesignCss).toMatch(/\.route-fade-enter-active,[\s\S]*var\(--duration-medium\)[\s\S]*var\(--ease-standard\)/);
  });

  it('uses the centered account-style login without changing the navigation shell', () => {
    expect(loginSource).toContain('class="login-brand-mark"');
    expect(loginSource).toContain('custacm-training-logo.jpg');
    expect(loginSource).toContain('class="login-field"');
    expect(loginSource).toContain('class="login-submit-row"');
    expect(loginSource).toContain('<LoginFooter />');
    expect(loginFooterSource).toContain('Welcome back to custacm-platform');
    expect(loginFooterSource).toContain("label: 'Codeforces'");
    expect(loginFooterSource).toContain("label: 'QOJ'");
    expect(loginFooterSource).toContain('border-radius: 18px 18px 0 0');
    expect(redesignCss).toMatch(/\.training-site\.is-login-page \.login-card[\s\S]*background: transparent;[\s\S]*text-align: center;/);
    expect(redesignCss).toMatch(/\.training-site\.is-login-page \.login-field input[\s\S]*min-height: 46px;/);
    expect(redesignCss).toMatch(/\.training-site\.is-login-page \.login-submit[\s\S]*border-radius: 50%;/);
    expect(redesignCss).toContain('min-height: calc(100vh + 220px)');
    expect(redesignCss).toContain('html.login-scrollbar-hidden::-webkit-scrollbar');
    expect(redesignCss).toMatch(/\.training-site\.is-login-page \{[\s\S]*height: 100vh;[\s\S]*overflow-y: auto;[\s\S]*overscroll-behavior-y: contain;/);
    expect(redesignCss).toContain('.training-site.is-login-page::-webkit-scrollbar');
    expect(redesignCss).toContain('scrollbar-width: none');
    expect(redesignCss).toMatch(/\.login-brand-mark[\s\S]*width: 184px;[\s\S]*height: 184px;/);
    expect(redesignCss).toMatch(/\.login-brand-mark img[\s\S]*object-fit: cover;/);
    expect(redesignCss).toContain('--login-mark-accent: color-mix');
    expect(redesignCss).toMatch(/\.login-brand-mark \{[\s\S]*box-shadow: 0 10px 32px color-mix\(in srgb, var\(--color-text\) 8%, transparent\);/);
    expect(redesignCss).not.toMatch(/\.login-brand-mark \{[\s\S]*0 0 96px 28px/);
    expect(redesignCss).toMatch(/\.training-site\.is-login-page \.login-submit[\s\S]*background: #c9962e;/);
    expect(redesignCss).toMatch(/\.login-submit:hover:not\(:disabled\)[\s\S]*background: #a87a1f;/);
  });
});
