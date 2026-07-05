# auth-domain Agent Notes

`auth-domain` contains local account domain types and repository contracts.

Key files:

- `UserAccount.java` - immutable local account record.
- `UserRole.java` - stored account roles: `admin`, `player`, and `disable`.
- `UserAccountRepository.java` - domain repository contract.

Rules:

- Unauthenticated access is represented outside the account domain by no current user role. `disable` is stored as an account role but cannot authenticate.
- Keep `studentIdentity` as one immutable business identity string.
- Do not add persistence, Spring web, or JDBC details here.
