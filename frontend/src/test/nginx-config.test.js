import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync, rmSync, copyFileSync } from 'node:fs';
import { join } from 'node:path';
import process from 'node:process';
import { afterEach, describe, expect, it } from 'vitest';

const nginxConfigs = [
  ['HTTP', 'nginx.conf'],
  ['HTTPS', 'nginx-https.conf'],
];

const testRoot = '.tmp-nginx-config-test';

function readConfig(configPath) {
  return readFileSync(configPath, 'utf8');
}

function validRefererTokens(config) {
  const match = config.match(/valid_referers\s+([^;]+);/);

  expect(match).not.toBeNull();
  return match?.[1].trim().split(/\s+/) ?? [];
}

function imagePublicOrigin(config) {
  const match = config.match(/sub_filter\s+"\/api\/image\/"\s+"([^"]+)\/api\/image\/";/);

  expect(match).not.toBeNull();
  return match?.[1] ?? '';
}

function renderConfig(env = {}) {
  const caseDir = join(testRoot, String(Date.now()), Math.random().toString(36).slice(2));
  mkdirSync(caseDir, { recursive: true });
  copyFileSync('nginx.conf', join(caseDir, 'nginx-http.conf'));
  copyFileSync('nginx-https.conf', join(caseDir, 'nginx-https.conf'));

  const outputPath = join(caseDir, 'default.conf');
  execFileSync('bash', ['docker-entrypoint.d/10-select-nginx-config.sh'], {
    cwd: process.cwd(),
    env: {
      ...process.env,
      CUSTACM_NGINX_CONFIG_DIR: caseDir,
      CUSTACM_NGINX_OUTPUT_CONFIG: outputPath,
      ...env,
    },
    stdio: ['ignore', 'pipe', 'pipe'],
  });

  return readConfig(outputPath);
}

afterEach(() => {
  if (existsSync(testRoot)) {
    rmSync(testRoot, { recursive: true, force: true });
  }
});

describe('Nginx hosted image referer policy', () => {
  it('normalizes the Docker entrypoint script for Linux containers', () => {
    const dockerfile = readConfig('Dockerfile');

    expect(dockerfile).toContain("sed -i 's/\\r$//'");
  });

  it('runs a real nginx config test while building the frontend image', () => {
    const dockerfile = readConfig('Dockerfile');

    expect(dockerfile).toContain('COPY frontend/test-nginx-config.sh /tmp/test-nginx-config.sh');
    expect(dockerfile).toContain('&& /tmp/test-nginx-config.sh');
  });

  it.each(nginxConfigs)('%s config renders trusted referers from the startup environment', (_label, configPath) => {
    const tokens = validRefererTokens(readConfig(configPath));

    expect(tokens).toEqual(['__CUSTACM_IMAGE_TRUSTED_REFERERS__']);
  });

  it.each(nginxConfigs)('%s config never whitelists omitted or stripped referers in the template', (_label, configPath) => {
    const tokens = validRefererTokens(readConfig(configPath));

    expect(tokens).not.toEqual(expect.arrayContaining(['none', 'blocked']));
  });

  it.each(nginxConfigs)('%s config never exposes forbidden image responses to shared caches', (_label, configPath) => {
    const config = readConfig(configPath);

    expect(config).toContain('error_page 418 = @image_referer_forbidden;');
    expect(config).toMatch(/if \(\$invalid_referer\) \{\s+return 418;\s+\}/);
    expect(config).toMatch(/location @image_referer_forbidden \{[\s\S]*Cache-Control "no-store, max-age=0" always;[\s\S]*return 403;/);
  });

  it('renders the production referer by default', () => {
    const config = renderConfig();
    const tokens = validRefererTokens(config);

    expect(tokens).toEqual(['custacm.top', 'www.custacm.top']);
    expect(imagePublicOrigin(config)).toBe('https://www.custacm.top');
  });

  it('renders a validated canonical HTTPS image origin', () => {
    const config = renderConfig({
      FRONTEND_IMAGE_PUBLIC_ORIGIN: 'https://images.custacm.top',
    });

    expect(imagePublicOrigin(config)).toBe('https://images.custacm.top');
    expect(config).toContain('proxy_set_header Accept-Encoding "";');
    expect(config).toContain('sub_filter_types application/json;');
  });

  it('adds local development referers when the environment enables them', () => {
    const tokens = validRefererTokens(
      renderConfig({
        FRONTEND_IMAGE_REFERER_HOSTS: 'custacm.top,preview.custacm.top',
        FRONTEND_ALLOW_LOCAL_REFERERS: 'true',
      }),
    );

    expect(tokens).toEqual(['custacm.top', 'preview.custacm.top', 'localhost', '127.0.0.1']);
  });

  it('accepts Nginx-supported leading and trailing hostname wildcards', () => {
    const tokens = validRefererTokens(
      renderConfig({
        FRONTEND_IMAGE_REFERER_HOSTS: 'custacm.top,*.custacm.top,custacm.*',
      }),
    );

    expect(tokens).toEqual(['custacm.top', '*.custacm.top', 'custacm.*']);
  });

  it.each([
    'none',
    'blocked',
    '*',
    '**',
    'foo*bar',
    '*.',
    '**.example.com',
    'example.**',
    '*.*',
    '*.example.*',
    'foo..example.com',
    '-foo.example.com',
    'foo-.example.com',
    '_foo.example.com',
    'custacm.top;include',
  ])('rejects invalid referer token %s', (invalidReferer) => {
    expect(() => {
      renderConfig({ FRONTEND_IMAGE_REFERER_HOSTS: `custacm.top ${invalidReferer}` });
    }).toThrow();
  });

  it.each([
    'http://www.custacm.top',
    'https://www.custacm.top/path',
    'https://*.custacm.top',
    'https://www.custacm.top;include',
  ])('rejects invalid canonical image origin %s', (invalidOrigin) => {
    expect(() => {
      renderConfig({FRONTEND_IMAGE_PUBLIC_ORIGIN: invalidOrigin});
    }).toThrow();
  });
});
