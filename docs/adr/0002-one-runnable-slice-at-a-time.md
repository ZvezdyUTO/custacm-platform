# ADR 0002: One Runnable Slice At A Time

## Status

Accepted.

## Decision

The project should evolve by adding one runnable slice at a time. Placeholder modules document future boundaries but should not gain broad implementation until their first runnable use case is selected.

## Consequences

- `platform-auth/auth-web` is currently the only runnable backend implementation.
- Placeholder modules stay out of the Maven reactor until implementation starts.
- Avoid adding shared abstractions to `platform-common` before repeated concrete needs exist.
- New slices must add local module docs, focused tests, and doc-sync-map entries in the same change.
