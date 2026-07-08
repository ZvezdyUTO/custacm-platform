# frontend Agent Notes

`frontend` is the first runnable frontend slice for the training-team management
platform. It implements a React/Vite workbench that reads real auth/training-data
HTTP APIs through same-origin dev/deploy proxies. Keep the default experience
focused on training-data query: single-user detail query, problem-level query,
and multi-user summary surfaces belong in the query workspace. The multi-user
page may load the existing public per-student accepted-summary endpoint for
students whose automatic collection is enabled and whose current OJ handle is
bound, but the frontend must not call or reintroduce a backend
automatic-summary endpoint. Query filters are applied automatically when OJ,
student, problem key, date range, or rating range changes, and page refresh
should load the default multi-user summary without requiring a query button; admin-only
mutation/update controls belong in the separate admin workspace after admin login. The top-right workspace switcher
chooses query/admin mode, while the left sidebar owns module/page navigation.
These workspace and page tabs are mirrored to browser history paths
(`/query/multiple`, `/query/single`, `/query/problem`, `/admin/user-create`, `/admin/user-edit`,
`/admin/collection`, `/admin/records`) so refresh and direct links preserve
the current page.
Inside the admin workspace, keep user creation, user modification, training-data
synchronization/collection, and operation records as separate pages instead of
mixing creation, query, import, and refresh actions in one surface. High-cost
actions such as recent-lookback collection and full user data deletion must
require explicit user confirmation in the UI.

## Scope

- Keep this module focused on the training query and admin workbench UI plus
  frontend-local API adapters. The query workspace can read the public auth
  user list, public full OJ handle account map, and personal/problem-level OJ
  detail data. Codeforces-compatible OJ collection is the only data-sync action
  currently represented as a backend job list; ODS upload, automatic-summary
  queries, and standalone warehouse refresh are not exposed in the UI.
- Do not change backend APIs or auth contracts from frontend code. Match the
  documented `auth-web` and `training-data-web` HTTP contracts.
- Full user deletion in the UI must compose backend-owned operations in order:
  clear training-data-owned OJ rows first, then call the auth admin account
  deletion endpoint.
- The user creation page owns text import and editable create rows, then calls
  the batch-create flow plus optional OJ handle binding.
- The user modification page should include an all-user list sorted by the
  student-number prefix of `studentIdentity` in descending order; accounts
  without a numeric prefix belong after student accounts. Existing-user edits
  should stay integrated with that list instead of living in a separate page
  section, including role/password changes, missing OJ handle completion,
  automatic-collection flag changes, OJ handle-account identity migration, full
  user deletion, and per-OJ history-start coverage status plus last collected
  time from `collectionStates`.
- Keep auth account `studentIdentity` as one immutable string wherever user identity appears.
  OJ handle-account identity migration is a training-data mapping operation and
  must not be presented as renaming the auth account.
- Use code-native controls for tables, filters, tabs, buttons, and states. Do
  not ship screenshots as UI.
- Do not store passwords in code. The login form posts credentials only to the
  local/proxied `/api/auth/login` endpoint and stores the returned access token
  in browser localStorage.

## Structure Rules

- `src/components/` owns reusable view components.
- `src/api/` owns frontend HTTP clients for platform services.
- `src/data/` owns static frontend navigation and local seed identity metadata,
  not business mock rows.
- `src/hooks/` owns reusable state or filtering helpers.
- `src/utils/` owns pure dashboard model builders derived from API responses.
- `src/test/` owns frontend unit tests.
- `src/App.tsx` should remain composition glue, not the place for all markup and
  HTTP details.
- `src/styles.css` is only the shared CSS entrypoint. Keep actual rules split
  under `src/styles/` by foundation, shell, dashboard, table, side panel, and
  responsive concerns.

## Verification

Use the scripts in `package.json` when changing frontend code:

```bash
pnpm lint
pnpm test
pnpm typecheck
pnpm build
```

For rendered UI changes, start the Vite dev server, ensure `auth-web` and
`training-data-web` are reachable, and inspect the page in a real browser across
desktop and mobile widths.

## Documentation

When materially changing frontend code, update this file, `README.md`,
`../docs/architecture.md`, and `../docs/agent/context-map.md` if responsibilities,
scripts, boundaries, or directory structure changed.
