# Architecture Decision Records

This directory records durable decisions that future agents should not rediscover or casually reverse.

| ADR | Decision |
| --- | --- |
| [0001-keycloak-owns-identity.md](0001-keycloak-owns-identity.md) | Superseded: Keycloak owns login and token issuance; platform code uses `studentIdentity`. |
| [0002-one-runnable-slice-at-a-time.md](0002-one-runnable-slice-at-a-time.md) | Build one runnable slice at a time; placeholders stay light. |
| [0003-agent-doc-governance.md](0003-agent-doc-governance.md) | Documentation is agent-facing and checked by path-to-doc sync rules. |
| [0004-platform-owns-auth.md](0004-platform-owns-auth.md) | Platform auth owns local accounts, BCrypt passwords, user management, and RSA JWT issuance. |
