# auth-infra Agent Notes

`auth-infra` contains persistence and security adapters for `platform-auth`.

Key files:

- `JdbcUserAccountRepository.java` - MySQL/JDBC implementation of the account repository.
- `BCryptPasswordHasher.java` - BCrypt password hashing adapter.
- `RsaJwtAccessTokenIssuer.java` - RSA JWT token issuer.
- `src/main/resources/db/migration/` - Flyway migrations for auth-owned tables.

Rules:

- Never store plaintext passwords.
- Do not commit JWT private keys, public keys used as local secrets, database passwords, or `.env` values.
- Keep migrations compatible with MySQL and the H2 MySQL-mode tests where practical.
