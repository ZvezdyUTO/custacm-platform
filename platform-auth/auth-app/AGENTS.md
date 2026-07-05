# auth-app Agent Notes

`auth-app` contains login and user-management use cases.

Key files:

- `AuthAccountService.java` - login, 5-second failed-login retry cooldown, current-user lookup, and own-password changes.
- `AdminUserService.java` - admin create/list/update/delete use cases; user update covers role changes, password resets, and generated passwords.
- `result/` - app-layer use-case return models such as login tokens and admin user operation results.
- `port/` - password hashing and JWT issuing ports.

Rules:

- Do not depend on Spring MVC, JDBC, or HTTP DTOs.
- Admins may manage other accounts, but must not downgrade, disable, or delete themselves.
- Players may only change their own password through the web layer.
- Keep service errors represented by stable `AuthErrorCode` values.
