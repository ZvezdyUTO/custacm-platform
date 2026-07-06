# frontend Agent Notes

`frontend` is the first runnable frontend slice for the training-team management
platform. It implements a React/Vite workbench that reads real auth/training-data
HTTP APIs through same-origin dev/deploy proxies. Keep the default experience
focused on training-data query: single-user detail query and automatic-collection
multi-user summary belong in the query workspace; admin-only mutation/update
controls belong in the separate admin workspace after admin login. The top-right workspace switcher
chooses query/admin mode, while the left sidebar owns module/page navigation.
These workspace and page tabs are mirrored to browser history paths
(`/query/multiple`, `/query/single`, `/admin/users`, `/admin/collection`,
`/admin/maintenance`, `/admin/records`) so refresh and direct links preserve
the current page.
Inside the admin workspace, keep user information management, training-data
synchronization/collection, data maintenance, and operation records as separate
pages instead of mixing creation, query, import, and refresh actions in one
surface. High-cost actions such as recent-lookback collection and full user data
deletion must require explicit user confirmation in the UI.

## Scope

- Keep this module focused on the training query and admin workbench UI plus
  frontend-local API adapters. The query workspace can read the public auth
  user list, public single Codeforces handle lookup, personal Codeforces detail data, and the
  automatic-collection users' summary list. Codeforces collection is the only
  data-sync action currently represented as a backend job list; ODS upload and
  manual warehouse refresh remain immediate admin requests.
- Do not change backend APIs or auth contracts from frontend code. Match the
  documented `auth-web` and `training-data-web` HTTP contracts.
- Full user deletion in the UI must compose backend-owned operations in order:
  clear training-data-owned Codeforces rows first, then call the auth admin
  account deletion endpoint.
- The user information page should include an all-user list sorted by the
  student-number prefix of `studentIdentity` in descending order; accounts
  without a numeric prefix belong after student accounts. Existing-user edits
  should stay integrated with that list instead of living in a separate page
  section.
- Keep `studentIdentity` as one immutable string wherever user identity appears.
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
