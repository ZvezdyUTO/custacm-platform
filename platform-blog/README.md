# platform-blog

`platform-blog` keeps the upstream NBlog directory shape while providing the platform's only backend and its public Blog frontend.

Production has one external `frontend` Nginx service. It serves the Blog at `/`, keeps the Blog shell mounted for `/training/**`, embeds the Training build from `/training-app/**`, and proxies browser `/api/**` requests to Blog API after removing the `/api` prefix.

## Modules

| Path | Responsibility |
| --- | --- |
| `upstream/nblog/blog-api` | The only Spring Boot backend; owns Blog HTTP, identity, authorization, user/OJ-handle management and training-data HTTP adapters. |
| `upstream/nblog/blog-view` | Vue 3 public Blog and the outer Training shell. |
| `../frontend` | Vue 3 Training runtime and the shared Nginx image; documented in its own README. |

The two Vue applications keep separate routers but share `custacm.accessToken` and `custacm.user`. Public Blog calls do not globally attach JWTs; protected adapters attach Bearer tokens explicitly. Blog API is the only HTTP layer and composes `platform-training-data` in-process.

The integrated source retains the original NBlog origin and license history in the source tree and Git history. Existing upstream and third-party license files must remain intact.

## Directory Layout

```text
platform-blog/
  AGENTS.md
  README.md
  upstream/nblog/
    blog-api/
    blog-view/
```

## Key Entries

| Path | Responsibility |
| --- | --- |
| `upstream/nblog/blog-api/README.md` | Backend boundaries, key entrypoints and Java verification. |
| `upstream/nblog/blog-view/README.md` | Blog routes, frontend boundaries, key entrypoints and npm verification. |
| `upstream/nblog/blog-api/src/main/java/top/naccl/BlogApiApplication.java` | Spring Boot application entrypoint. |
| `upstream/nblog/blog-view/src/router/index.js` | Blog routes and Training-shell handoff. |

Architecture is defined in [docs/architecture.md](../docs/architecture.md), HTTP contracts in [docs/api.md](../docs/api.md), authorization in [docs/authorization.md](../docs/authorization.md), and visual rules in [docs/frontend-design-system.md](../docs/frontend-design-system.md).

## Verification

For Java changes, run from the repository root:

```bash
mvn clean test
```

For Vue Blog changes, run in `upstream/nblog/blog-view`:

```bash
npm install
npm test
npm run build
```
