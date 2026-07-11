# platform-training-data

Training-data is a library subsystem of Blog API; it has no Spring Boot entrypoint.

| Module | Responsibility |
| --- | --- |
| `training-data-common` | `TrainingUserDirectory`, OJ-neutral query/job/scheduler/warehouse/purge logic, JDBC repositories |
| `training-data-codeforces` | Codeforces source collection, ODS persistence, migrations, warehouse SQL |
| `training-data-atcoder` | AtCoder source/metadata collection, ODS persistence, migrations, warehouse SQL |

Business identity is `username`. Blog API owns accounts and handle-management HTTP. Training code only reads username/handle mappings, writes collection state, and processes regenerative training data. DataSource, Flyway, transactions, security, and HTTP runtime come from Blog API.
