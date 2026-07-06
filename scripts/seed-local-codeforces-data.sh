#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/deploy/.env"
AUTH_BASE_URL="${AUTH_BASE_URL:-http://localhost:8081}"
TRAINING_DATA_BASE_URL="${TRAINING_DATA_BASE_URL:-http://localhost:8082}"
FIXTURE_PATH="${FIXTURE_PATH:-${ROOT_DIR}/platform-training-data/training-data-codeforces/src/main/resources/fixtures/codeforces/submissions_multi_user_1000.json}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing ${ENV_FILE}. Copy deploy/.env.example to deploy/.env first." >&2
  exit 1
fi

if [[ ! -f "${FIXTURE_PATH}" ]]; then
  echo "Missing fixture: ${FIXTURE_PATH}" >&2
  exit 1
fi

if ! command -v node >/dev/null 2>&1; then
  echo "Missing required command: node" >&2
  exit 1
fi

AUTH_BASE_URL="${AUTH_BASE_URL}" \
TRAINING_DATA_BASE_URL="${TRAINING_DATA_BASE_URL}" \
ENV_FILE="${ENV_FILE}" \
FIXTURE_PATH="${FIXTURE_PATH}" \
node <<'NODE'
const fs = require('fs');

const envFile = process.env.ENV_FILE;
const fixturePath = process.env.FIXTURE_PATH;
const authBaseUrl = process.env.AUTH_BASE_URL;
const trainingDataBaseUrl = process.env.TRAINING_DATA_BASE_URL;

const env = Object.fromEntries(
  fs
    .readFileSync(envFile, 'utf8')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line && !line.startsWith('#'))
    .map((line) => {
      const index = line.indexOf('=');
      return [line.slice(0, index), line.slice(index + 1)];
    }),
);

const adminIdentity = env.AUTH_BOOTSTRAP_ADMIN_STUDENT_IDENTITY;
const adminPassword = env.AUTH_BOOTSTRAP_ADMIN_PASSWORD;

if (!adminIdentity || !adminPassword) {
  throw new Error('AUTH_BOOTSTRAP_ADMIN_STUDENT_IDENTITY and AUTH_BOOTSTRAP_ADMIN_PASSWORD are required in deploy/.env');
}

const sampleUsers = [
  { studentIdentity: '230511213黄炳睿', password: 'PlayerPass123!', role: 'player', handle: 'tourist' },
  { studentIdentity: '230511214李明', password: 'PlayerPass123!', role: 'player', handle: 'Benq' },
  { studentIdentity: '230511215王强', password: 'PlayerPass123!', role: 'admin', handle: 'ecnerwala' },
  { studentIdentity: '230511216赵敏', password: 'PlayerPass123!', role: 'player', handle: 'Um_nik' },
  { studentIdentity: '230511217陈晨', password: 'PlayerPass123!', role: 'disable', handle: 'jiangly' },
];

async function request(url, options = {}) {
  const response = await fetch(url, options);
  const text = await response.text();
  let body = null;
  try {
    body = text ? JSON.parse(text) : null;
  } catch {
    body = text;
  }
  return { status: response.status, ok: response.ok, body };
}

function jsonHeaders(token) {
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };
}

async function main() {
  const login = await request(`${authBaseUrl}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ studentIdentity: adminIdentity, password: adminPassword }),
  });
  if (!login.ok) {
    throw new Error(`admin login failed: HTTP ${login.status}`);
  }
  const token = login.body.accessToken;
  console.log(`Logged in admin: ${login.body.user.studentIdentity}`);

  const batchCreate = await request(`${authBaseUrl}/api/auth/admin/users:batch-create`, {
    method: 'POST',
    headers: jsonHeaders(token),
    body: JSON.stringify({
      users: sampleUsers.map(({ handle, ...user }) => user),
    }),
  });
  if (!batchCreate.ok) {
    throw new Error(`batch-create users failed: HTTP ${batchCreate.status}`);
  }
  const created = Array.isArray(batchCreate.body)
    ? batchCreate.body.filter((item) => item.success).length
    : 0;
  const existing = Array.isArray(batchCreate.body) ? batchCreate.body.length - created : 0;
  console.log(`Auth users ready: created=${created}, existing-or-skipped=${existing}`);

  let handleCreated = 0;
  let handleSkipped = 0;
  for (const user of sampleUsers) {
    const createHandle = await request(`${trainingDataBaseUrl}/api/training-data/admin/codeforces/handles`, {
      method: 'POST',
      headers: jsonHeaders(token),
      body: JSON.stringify({ studentIdentity: user.studentIdentity, handle: user.handle }),
    });
    if (createHandle.status === 201) {
      handleCreated += 1;
    } else if (createHandle.status === 409) {
      handleSkipped += 1;
    } else if (!createHandle.ok) {
      throw new Error(`create handle ${user.handle} failed: HTTP ${createHandle.status}`);
    }
  }
  console.log(`Codeforces handles ready: created=${handleCreated}, already-bound=${handleSkipped}`);

  const fixture = fs.readFileSync(fixturePath, 'utf8');
  const upsert = await request(`${trainingDataBaseUrl}/api/training-data/admin/ods/codeforces/submissions:batch-upsert`, {
    method: 'POST',
    headers: jsonHeaders(token),
    body: fixture,
  });
  if (!upsert.ok) {
    throw new Error(`ODS upsert failed: HTTP ${upsert.status}`);
  }
  console.log(`ODS upsert completed: rows=${upsert.body.writtenRows}, batchId=${upsert.body.batchId}`);

  const refresh = await request(`${trainingDataBaseUrl}/api/training-data/admin/codeforces/warehouse:refresh`, {
    method: 'POST',
    headers: jsonHeaders(token),
    body: JSON.stringify({ batchId: upsert.body.batchId }),
  });
  if (!refresh.ok) {
    throw new Error(`warehouse refresh failed: HTTP ${refresh.status}`);
  }
  console.log(`Warehouse refresh completed: status=${refresh.body.status}, tasks=${refresh.body.tasks?.length ?? 0}`);

  for (const user of sampleUsers) {
    const summary = await request(
      `${trainingDataBaseUrl}/api/training-data/codeforces/accepted-summary?studentIdentity=${encodeURIComponent(user.studentIdentity)}`,
    );
    if (summary.ok) {
      console.log(`${user.studentIdentity} -> ${summary.body.authorHandle}, accepted=${summary.body.totalAcceptedProblemCount}`);
    }
  }
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
NODE
