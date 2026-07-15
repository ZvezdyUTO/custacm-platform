# common-core

`common-core` contains reusable, non-business backend infrastructure. Its current responsibility is the SQL task DAG executor: load a YAML manifest, validate the graph, build a topological plan and execute SQL nodes with one transaction per node.

## Directory Layout

```text
src/main/java/com/custacm/platform/common/sqltask/
src/test/java/com/custacm/platform/common/sqltask/
src/test/resources/sqltask/
```

## Dependency And Layer Rules

- Must not contain business entities, module-specific schema concepts, HTTP controllers or authorization rules.
- Depends only on Spring resources/JDBC/transactions, SnakeYAML and SLF4J in production code.
- Callers own business manifests, SQL files, parameters, transport validation and security tiers.
- Each task node executes in its own transaction; graph validation and fail-fast semantics stay inside the executor.

## Key Entries

| Path | Responsibility |
| --- | --- |
| `SqlTaskRunner.java` | Public manifest execution entrypoint and node transaction orchestration. |
| `YamlSqlTaskManifestLoader.java` | YAML loading and manifest-structure validation. |
| `SqlTaskManifest.java` and `SqlTaskDefinition.java` | Immutable manifest and task definitions. |
| `SqlTaskGraph.java` | Dependency validation, DAG checks and execution-plan construction. |
| `SqlScriptSplitter.java` | SQL statement splitting with quote/comment awareness. |
| `SqlTaskExecutionRequest.java` | Manifest, parameters and optional resume-node input. |
| `SqlTaskExecutionResult.java`, `SqlTaskNodeResult.java` and status enums | Run and node outcomes. |
| `SqlTaskException.java` and `SqlTaskErrorCode.java` | Stable executor failure contract. |
| `src/test/` | DAG, transaction, resume, failure and SQL-splitting tests/fixtures. |

This table lists stable concepts rather than every supporting type.

## Verification

Run from the repository root:

```bash
mvn clean test
```
