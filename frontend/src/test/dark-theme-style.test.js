// Author: huangbingrui.awa
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { cwd } from 'node:process';
import { describe, expect, it } from 'vitest';

const darkCss = readFileSync(resolve(cwd(), 'src/styles/dark.css'), 'utf8');
const dashboardCss = readFileSync(resolve(cwd(), 'src/styles/dashboard.css'), 'utf8');

describe('training dark theme stylesheet contract', () => {
  it('covers collection empty states, expanded tables and partial-success jobs', () => {
    expect(darkCss).toContain('html.dark .collection-result-table-scroll');
    expect(darkCss).toContain('html.dark .batch-target-empty');
    expect(darkCss).toContain('html.dark .result-status-partial-success');
  });

  it('uses a readable dark foreground on solid copper action buttons', () => {
    expect(darkCss).toMatch(/\.collection-confirm-actions \.confirm-collection-button \{[^}]*color: #21150c;/s);
    expect(darkCss).toMatch(/\.article-backup-button \{[^}]*color: #21150c;/s);
  });

  it('dims training images gradually while preserving reduced-motion preferences', () => {
    expect(darkCss).toContain('html.dark .training-site img');
    expect(darkCss).toContain('filter: brightness(.84) saturate(.95)');
    expect(darkCss).toContain('transition: filter 260ms ease');
    expect(darkCss).toMatch(/prefers-reduced-motion: reduce[\s\S]*\.training-site img/);
  });

  it('covers the shared administrator confirmation dialog in dark mode', () => {
    expect(darkCss).toContain('.admin-confirm-backdrop');
    expect(darkCss).toContain('.admin-confirm-dialog');
    expect(darkCss).toContain('.admin-confirm-actions button');
  });

  it('keeps personal rating bars colorful with a thin dark-mode outline', () => {
    expect(dashboardCss).toMatch(/\.rating-bar-row i \{[^}]*background: var\(--rating-color, var\(--navy\)\);/s);
    expect(darkCss).toMatch(/html\.dark \.rating-bar-row i \{[^}]*background: var\(--rating-color\);[^}]*box-shadow: inset 0 0 0 1px/s);
  });
});
