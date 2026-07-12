# platform-training-data

Training-data is a library subsystem of Blog API; it has no Spring Boot entrypoint.

OJ handle 的普通更新只能首次绑定；已有 handle 更换由 Blog API 的高危用例编排，先调用本模块清理对应 OJ 的 ODS 与 DWD/DWM/DWS 数据，再重置绑定和采集状态。

| Module | Responsibility |
| --- | --- |
| `training-data-common` | `TrainingUserDirectory`, MVC-free query facade, shared identity migrations, job/scheduler/warehouse/purge logic, JDBC repositories |
| `training-data-codeforces` | Codeforces source collection, ODS persistence, OJ-specific migrations, warehouse SQL |
| `training-data-atcoder` | AtCoder source/metadata collection, ODS persistence, migrations, warehouse SQL |

Business identity is `username`. Blog API owns accounts, handle-management HTTP, and all Spring MVC controllers under `top.naccl`. Training code exposes transport-neutral application facades, reads username/handle mappings, writes collection state, and processes regenerative training data. Shared `oj_handle_account` migration history belongs to `training-data-common`; DataSource, Flyway, transactions, security, and HTTP runtime come from Blog API.
