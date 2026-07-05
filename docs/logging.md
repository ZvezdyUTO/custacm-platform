# Logging Guide

This document is mandatory reading for agents before changing backend code that logs anything.

## Runtime Shape

The project uses Spring Boot's default SLF4J and Logback stack. Do not build a custom logging system and do not add a heavy log platform for the current phase.

Backend logs are written to local files:

```text
logs/combined.log  all backend logs
logs/error.log     ERROR level logs only
```

On the server these files live under:

```text
/opt/custacm-platform/logs/
```

AI log lookup uses the read-only `local-logs-mcp-server` over SSH. The server must not expose a public log query port.

Every runnable Spring Boot web service in this repository must ship a `logback-spring.xml` that writes to the same file contract unless a future shared logging starter replaces the duplicated resource. Placeholder modules do not need logging files until they become runnable services.

## Required Logging Style

Use one logger per class:

```java
private static final Logger log = LoggerFactory.getLogger(CurrentClass.class);
```

Use structured key-value text in the message so agents can search stable tokens:

```java
log.info("User profile loaded, studentIdentityHash={}", studentIdentityHash);
log.warn("Request rejected, errorCode={}, reason={}", errorCode, reason);
log.error("Failed to load current user, errorCode={}", errorCode, ex);
```

Rules:

- `error` logs must include a stable `errorCode`.
- If an exception exists, pass the `Throwable` as the final argument. Do not only log `ex.getMessage()`.
- After request tracing is added, every request log must carry `traceId` through MDC; business code must not generate trace IDs manually.
- User-facing copy is not a stable query key. Prefer `errorCode`, `traceId`, route path, HTTP status, and log level.
- Schedulers and background workers must log unexpected task failures at the boundary that also records task state. The same stable `errorCode` should appear in the log and in the task or job status table.

## Log Levels

- `info`: important successful lifecycle or business events, such as startup, current user loaded, or an important state transition.
- `warn`: expected rejection or degraded behavior, such as invalid input, forbidden access, conflict, or missing optional data.
- `error`: unexpected failure that needs investigation. Include `errorCode` and the exception object.
- `debug`: local troubleshooting details only. Do not depend on production `debug` logs.

## Sensitive Data

Never log these values:

- password, reset code, login session, token, cookie, or `Authorization` header
- JWT private key, signing key material, database password, or `.env` value
- full personal information or raw request/response bodies that may contain personal data

When user correlation is needed, prefer a hash such as `studentIdentityHash` unless the raw `studentIdentity` is explicitly required for an operator-facing audit trail.

## Future Error Response Contract

When the unified exception slice is implemented, errors should return:

```json
{
  "code": "AUTH_TOKEN_INVALID",
  "message": "登录状态无效",
  "traceId": "..."
}
```

The same `code` must appear in logs as `errorCode`, and `traceId` must be written into MDC so it appears in `combined.log` and `error.log`.

## Agent Checklist

Before adding or changing logs:

1. Use SLF4J parameter placeholders, not string concatenation.
2. Add `errorCode` to all error logs.
3. Pass `Throwable` as the final argument for exception logs.
4. Keep sensitive data out of logs.
5. Make the log searchable by `errorCode`, `traceId`, route path, or a stable domain key.
6. For a new runnable `*-web` service, add `logback-spring.xml` and verify it uses `LOG_DIR`, `combined.log`, `error.log`, and the shared pattern with `traceId` and `errorCode`.
