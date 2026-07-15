// Author: huangbingrui.awa
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { cwd } from 'node:process';
import { describe, expect, it } from 'vitest';

const stylesEntry = readFileSync(resolve(cwd(), 'src/styles.css'), 'utf8');
const redesignCss = readFileSync(resolve(cwd(), 'src/styles/training-redesign.css'), 'utf8');
const articleAdminCss = readFileSync(resolve(cwd(), 'src/styles/article-admin.css'), 'utf8');
const competitionAdminCss = readFileSync(resolve(cwd(), 'src/styles/competition-admin.css'), 'utf8');
const lightCss = readFileSync(resolve(cwd(), 'src/styles/light.css'), 'utf8');
const darkCss = readFileSync(resolve(cwd(), 'src/styles/dark.css'), 'utf8');
const viewSource = readFileSync(resolve(cwd(), 'src/views/TrainingView.vue'), 'utf8');
const loginSource = readFileSync(resolve(cwd(), 'src/components/LoginPanel.vue'), 'utf8');
const loginFooterSource = readFileSync(resolve(cwd(), 'src/components/LoginFooter.vue'), 'utf8');

describe('Training visual redesign contract', () => {
	it('loads generated tokens first and the theme override last', () => {
		expect(stylesEntry.indexOf('./styles/tokens.css')).toBeLessThan(stylesEntry.indexOf('./styles/foundation.css'));
		expect(stylesEntry).toContain('./styles/light.css');
		expect(stylesEntry).toContain('./styles/dark.css');
		expect(stylesEntry.indexOf('./styles/light.css')).toBeGreaterThan(stylesEntry.indexOf('./styles/training-redesign.css'));
		expect(stylesEntry.indexOf('./styles/light.css')).toBeLessThan(stylesEntry.indexOf('./styles/dark.css'));
		expect(lightCss).toContain('--color-canvas: #faf9f5');
		expect(lightCss).toContain('--color-canvas-alternate: #f0eee6');
		expect(lightCss).toContain('--color-surface: #f7f7f5');
		expect(lightCss).toContain('.article-admin-list');
		expect(stylesEntry.indexOf('./styles/dark.css')).toBeGreaterThan(stylesEntry.indexOf('./styles/training-redesign.css'));
		expect(darkCss).toContain('--color-canvas: #141413');
		expect(darkCss).toContain('--color-text: #faf9f5');
		expect(darkCss).toContain('--color-action: #d97757');
		expect(darkCss).toMatch(/html\.dark \.training-site \.oj-select-trigger \{[\s\S]*background: var\(--night-input\);/);
		expect(darkCss).toMatch(/html\.dark \.training-site \.oj-select-options \{[\s\S]*background: var\(--night-panel\);/);
		expect(darkCss).toMatch(/\.oj-select-options button\.is-selected \{[\s\S]*color: var\(--night-accent\);/);
		expect(darkCss).not.toContain('html.dark .training-site img');
	});

	it('keeps featured previews and competition headers aligned with both themes', () => {
		expect(articleAdminCss).toContain('--featured-preview-surface: #f7f7f5');
		expect(articleAdminCss).toContain('--featured-preview-media: #eeecea');
		expect(articleAdminCss).toContain('background: var(--featured-preview-surface);');
		expect(articleAdminCss).toContain('color: var(--featured-preview-text);');
		expect(darkCss).toContain('--featured-preview-surface: #1c1c1b');
		expect(darkCss).toContain('--featured-preview-media: #242422');
		expect(competitionAdminCss).toContain('background: var(--competition-navy-soft);');
		expect(darkCss).toMatch(/html\.dark \.competition-admin \{[\s\S]*--competition-navy-soft: var\(--night-panel-soft\);/);
		expect(darkCss).toMatch(/html\.dark \.competition-list-header \{[\s\S]*background: var\(--night-panel-soft\);/);
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
