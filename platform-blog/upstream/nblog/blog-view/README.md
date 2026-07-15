# blog-view

`blog-view` is the Vue 3 + Vite public Blog. It serves the Blog at `/` and owns the outer `/training/**` shell so the same Blog navigation remains mounted while the separate Training application runs at `/training-app/**`.

The public page scope is homepage, article catalogue and detail, category, tag, competition archive, profile, writing and comments. Retired About, Friends and Moments routes are not part of the application.

## Routes

```text
/home
/articles
/blog/:id
/write/:id?
/category/:name
/tag/:name
/competitions
/competitions/:id
/profile
/training/**
```

`/` redirects to `/home`; `/login` hands off to `/training/login`.

## Dependency And Boundary Rules

- Blog and Training are separate Vue builds and separate routers. Do not copy Training components into this module or add a second navigation bar.
- Axios uses `/api/`. Public requests must not receive a global `Authorization` header; protected adapters attach the shared Bearer token explicitly.
- The shared session keys are only `custacm.accessToken` and `custacm.user`; server authorization remains authoritative.
- `frontend-design-tokens/tokens.css` is the shared token source. `src/assets/css/tokens.css` is generated and must not be edited manually.
- Theme selection is manual, stored in `custacm.theme`, and synchronized to the same-origin Training frame; Blog applies the saved value before Vue mounts and uses `src/assets/css/night.css` for the night palette.
- Keep visual rules in [the frontend design system](../../../../docs/frontend-design-system.md), not in this README.
- Sanitize all rendered `v-html` content through `src/util/sanitizeHtml.js`.
- Article images and avatars use Blog API managed assets. Do not restore GitHub, Upyun or other remote-upload paths.
- API behavior is defined in [docs/api.md](../../../../docs/api.md); route access and ownership are defined in [docs/authorization.md](../../../../docs/authorization.md).

## Directory Layout

```text
public/          static images, local emoji assets and license/source notes
src/api/         public and protected API adapters
src/assets/      generated tokens and Blog styles
src/auth/        shared browser session handling
src/components/  navigation, article, comment and profile components
src/plugins/     Axios, editor and emoji integrations
src/router/      Blog routes and Training-shell routing
src/store/       Blog and comment state
src/util/        shared sanitizing and UI helpers
src/utils/       feature-specific pure helpers
src/views/       page-level views and Training host
src/test/        API, session, route and interaction tests
```

## Key Entries

| Path | Responsibility |
| --- | --- |
| `src/main.js` | Vue, Router, Vuex, Element Plus, icons and global style registration. |
| `src/router/index.js` | Blog routes, authentication guard and Training handoff. |
| `src/auth/session.js` | Shared token and user-summary lifecycle. |
| `src/plugins/axios.js` | Same-origin Blog client and response handling; no global JWT. |
| `src/views/Index.vue` | Public Blog shell and page composition. |
| `src/views/training/TrainingHost.vue` | Same-origin Training runtime host. |
| `src/views/blog/`, `category/`, `tag/` and `article/` | Article reading, catalogue and editing pages. |
| `src/views/competition/` and `profile/` | Competition archive and user profile pages. |
| `src/components/index/` | Site navigation, homepage content and footer. |
| `src/components/article/`, `comment/` and `profile/` | Article, comment and profile UI. |
| `src/assets/css/tokens.css` | Generated shared-token copy; never edit directly. |
| `src/assets/css/blog-redesign.css` | Blog-specific semantic style mapping. |
| `public/emoji/noto/` | Local Noto Emoji assets and required license notes. |
| `public/img/home-logos/` | Homepage logos and source notes. |
| `src/test/` | Frontend contract and regression tests. |
| `vite.config.js` | Development proxy, build and Vitest configuration. |

This table lists stable navigation points rather than every component or test. Inspect current files with `rg --files`.

## Verification

Run in this directory:

```bash
npm install
npm test
npm run build
```
