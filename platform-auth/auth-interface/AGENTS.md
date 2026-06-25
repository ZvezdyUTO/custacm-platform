# auth-interface Agent Notes

`auth-interface` contains cross-module contracts for auth.

Current files:

- `CurrentUserResponse.java` - response DTO with `studentIdentity` and `role`.

Rules:

- Keep DTO field names aligned with [../../../docs/api.md](../../../docs/api.md).
- Business responses use a single `role` string, not a list.
- Do not add persistence or application services here.
- If this module gains more contracts, document each externally visible field in [../../../docs/api.md](../../../docs/api.md).
