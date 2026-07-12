# Integrated NBlog workspace

This directory retains the integrated NBlog backend and the deployed Vue 3 public Blog. In this repository:

- `blog-api` is the only runnable backend and is integrated into the root Maven reactor.
- Database initialization is Flyway-only; the old manual `nblog.sql` path is removed.
- Runtime configuration comes from `application.properties` environment placeholders and `deploy/.env`.
- Accounts are created by bootstrap configuration or `/admin/users`; there is no public registration.
- Roles are `ROLE_admin` and `ROLE_player`, and JWT `sub` is `username`.
- `blog-view` is built with Vue 3 and Vite and is deployed at `/` by the shared frontend image.

See [blog-api/README.md](blog-api/README.md), [blog-view/README.md](blog-view/README.md), [../../../docs/api.md](../../../docs/api.md), and [../../../deploy/README.md](../../../deploy/README.md).

The original NBlog project and license history remain in the source tree and Git history.
