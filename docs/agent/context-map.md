# Context Map

This map is based on files read from the current repository. It intentionally avoids guessing future behavior.

| Path | Current responsibility | Evidence | Local docs |
| --- | --- | --- | --- |
| `AGENTS.md` | Hard rules for agents: identity, module boundaries, logging, git, and verification. | Root file content. | [../README.md](../README.md) |
| `.github/` | Pull request template, CODEOWNERS, and Maven verify workflow. | `.github/workflows/maven-verify.yml`, `.github/pull_request_template.md`, `.github/CODEOWNERS`. | [../../.github/AGENTS.md](../../.github/AGENTS.md) |
| `docs/` | Project documentation, architecture, API, logging, deployment notes, and agent documentation. | `docs/*.md`. | [../AGENTS.md](../AGENTS.md) |
| `deploy/` | Compose-based local/server deployment configuration and Keycloak realm import. | `deploy/docker-compose.yml`, `deploy/.env.example`, `deploy/keycloak/custacm-realm.json`. | [../../deploy/AGENTS.md](../../deploy/AGENTS.md) |
| `scripts/` | Deployment, module update, auto-update, log MCP install, and docs sync checks. | `scripts/*.sh`. | [../../scripts/AGENTS.md](../../scripts/AGENTS.md) |
| `platform-common/` | Shared Maven area; currently empty `common-core` and `common-web` jar modules. | `platform-common/pom.xml`, child POMs. | [../../platform-common/AGENTS.md](../../platform-common/AGENTS.md) |
| `platform-auth/` | Keycloak-backed auth module and the only runnable backend slice. | `platform-auth/pom.xml`, `auth-core`, `auth-interface`, `auth-web`. | [../../platform-auth/AGENTS.md](../../platform-auth/AGENTS.md) |
| `platform-auth/auth-core/` | JWT claim parsing, platform role extraction, current user extraction, Spring authority conversion. | Java files under `src/main/java/com/custacm/platform/auth/core`. | [../../platform-auth/auth-core/AGENTS.md](../../platform-auth/auth-core/AGENTS.md), [../../platform-auth/auth-core/TESTING.md](../../platform-auth/auth-core/TESTING.md) |
| `platform-auth/auth-interface/` | Cross-module DTO for current-user response. | `CurrentUserResponse.java`. | [../../platform-auth/auth-interface/AGENTS.md](../../platform-auth/auth-interface/AGENTS.md) |
| `platform-auth/auth-web/` | Spring Boot application, HTTP controllers, resource-server security config, runtime config. | Java files under `auth-web/src/main`, `application.yml`. | [../../platform-auth/auth-web/AGENTS.md](../../platform-auth/auth-web/AGENTS.md), [../../platform-auth/auth-web/TESTING.md](../../platform-auth/auth-web/TESTING.md) |
| `platform-training-data/` | Placeholder for future training data module. | `platform-training-data/README.md`. | [../../platform-training-data/AGENTS.md](../../platform-training-data/AGENTS.md) |
| `platform-blog/` | Placeholder for future blog/content module. | `platform-blog/README.md`. | [../../platform-blog/AGENTS.md](../../platform-blog/AGENTS.md) |
| `platform-editor/` | Placeholder for future editor integration. | `platform-editor/README.md`. | [../../platform-editor/AGENTS.md](../../platform-editor/AGENTS.md) |
| `platform-article-storage/` | Placeholder for future article storage module. | `platform-article-storage/README.md`. | [../../platform-article-storage/AGENTS.md](../../platform-article-storage/AGENTS.md) |
| `frontend/` | Placeholder for future frontend implementation. | `frontend/README.md`. | [../../frontend/AGENTS.md](../../frontend/AGENTS.md) |

## Known TODOs

- `platform-common/common-core` and `platform-common/common-web` have no source files yet.
- Placeholder modules should stay out of the Maven reactor until their first runnable slice is implemented.
- Unified error response and request tracing are future work documented in [../logging.md](../logging.md).
