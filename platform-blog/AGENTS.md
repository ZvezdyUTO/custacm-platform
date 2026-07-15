# platform-blog Agent Notes

- `upstream/nblog/blog-api` is the only runnable backend. Keep `top.naccl` as its package root; training modules remain in-process libraries and must not add an HTTP runtime.
- Production exposes one Nginx `frontend` service. Blog owns `/` and the `/training/**` shell; Training is mounted internally at `/training-app/**`; browser APIs use `/api/**`. Keep the two Vue Routers separate and keep a single Blog navigation bar.
- Follow [authorization](../docs/authorization.md) and [API contracts](../docs/api.md). Public Blog requests must not receive a global JWT; protected adapters attach Bearer tokens explicitly.
- Passwords, JWTs, users, roles and OJ handles belong to Blog API. `username` is the JWT subject and training identity; stored roles are only `ROLE_admin` and `ROLE_player`.
- Article writes must enforce the authenticated author. Handle removal or replacement must purge that OJ's training data before the binding changes. The fixed `root` administrator cannot be deleted, renamed, demoted or used as a training member.
- Do not restore retired About/Friends/Moments, statistics, Quartz, notification, remote-upload or dynamic-banner paths without an explicit product decision.
- `blog-view/src/assets/css/tokens.css` is generated from the repository token source and must not be edited manually. Visual rules live in [the frontend design system](../docs/frontend-design-system.md).
- Submission schedules and AtCoder metadata bootstrap/schedules stay disabled unless operators explicitly enable them.
- Before backend log changes, read [logging.md](../docs/logging.md); never log credentials, tokens, Authorization headers, keys or full sensitive data.
- Verify Java changes from the repository root with `mvn clean test`; add `mvn clean package -DskipTests` only for packaging changes.
- Verify Vue Blog changes in `upstream/nblog/blog-view` with `npm install`, `npm test` and `npm run build`.
- When responsibilities, paths, build behavior or authorization change, update the nearest README and the relevant architecture, API or authorization document.
