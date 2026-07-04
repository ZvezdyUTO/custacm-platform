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

- `submissions_multi_user_1000.json`: large `result` array, ready to POST to the local ODS ingest endpoint.
- `submissions_multi_user_1000.metadata.json`: source URLs, row counts, handles, and time-range metadata.
- `submissions_tourist.json`: small legacy parser fixture.

Use the array fixture for local API testing:

```bash
curl -X POST "http://localhost:8082/api/training-data/ods/codeforces/submissions:batch-upsert" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  --data-binary @platform-training-data/training-data-codeforces/src/main/resources/fixtures/codeforces/submissions_multi_user_1000.json
```
