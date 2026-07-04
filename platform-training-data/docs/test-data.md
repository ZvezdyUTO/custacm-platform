# Local Test Data

This document is the stable entrypoint for training-data fixtures used by parser, API, and warehouse SQL tests. Default tests must read these local files instead of crawling online judges.

## Codeforces Submissions

Primary fixture:

```text
platform-training-data/training-data-codeforces/src/main/resources/fixtures/codeforces/submissions_multi_user_1000.json
```

Metadata:

```text
platform-training-data/training-data-codeforces/src/main/resources/fixtures/codeforces/submissions_multi_user_1000.metadata.json
```

Shape:

- JSON array of raw Codeforces `Submission` objects.
- Ready to use as the request body for `POST /api/training-data/ods/codeforces/submissions:batch-upsert`.
- Captured once from the public Codeforces `user.status` API on `2026-07-03`.
- 1000 unique submissions.
- Requested handles: `tourist`, `Benq`, `ecnerwala`, `Um_nik`, `jiangly`.
- Payload handles include team members observed in Codeforces responses: see metadata.
- Submission time range: `2022-10-15T14:36:48Z` through `2026-07-03T02:41:35Z`.

Source requests:

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

Local API replay:

```bash
curl -X POST "http://localhost:8082/api/training-data/ods/codeforces/submissions:batch-upsert" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  --data-binary @platform-training-data/training-data-codeforces/src/main/resources/fixtures/codeforces/submissions_multi_user_1000.json
```

Rules:

- Do not refresh this fixture during normal tests.
- Add new fixtures with stable filenames and sidecar metadata.
- Keep large fixture files under the owning OJ module's `src/main/resources/fixtures/<oj>/` directory so Maven tests can load them through the classpath.
