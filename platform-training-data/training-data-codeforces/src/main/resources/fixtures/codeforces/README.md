# Codeforces Fixtures

These files are local test data captured once from the public Codeforces API. Keep default tests and local chain checks fixture-backed; do not refresh them during normal verification.

Large local fixture:

```text
submissions_multi_user_1000.json
```

It contains 1000 unique submissions captured on `2026-07-03` from these public Codeforces API requests:

```text
https://codeforces.com/api/user.status?handle=tourist&from=1&count=100
https://codeforces.com/api/user.status?handle=tourist&from=1001&count=100
https://codeforces.com/api/user.status?handle=Benq&from=1&count=100
https://codeforces.com/api/user.status?handle=Benq&from=1001&count=100
https://codeforces.com/api/user.status?handle=ecnerwala&from=1&count=100
https://codeforces.com/api/user.status?handle=ecnerwala&from=1001&count=100
https://codeforces.com/api/user.status?handle=Um_nik&from=1&count=100
https://codeforces.com/api/user.status?handle=Um_nik&from=1001&count=100
https://codeforces.com/api/user.status?handle=jiangly&from=1&count=100
https://codeforces.com/api/user.status?handle=jiangly&from=1001&count=100
```

Files:

- `submissions_multi_user_1000.json`: large `result` array used by parser, repository, and warehouse SQL tests.
- `submissions_multi_user_1000.metadata.json`: source URLs, row counts, handles, and time-range metadata.
- `submissions_tourist.json`: small legacy parser fixture.

Use this fixture through local tests or seed scripts; manual ODS import is no longer exposed as an HTTP endpoint.
