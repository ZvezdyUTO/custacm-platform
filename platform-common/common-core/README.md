# common-core

`common-core` contains reusable non-business backend utilities for the platform.

Current responsibility:

- SQL task DAG execution core: read a YAML manifest on every run, build an adjacency-list graph, validate that the graph is a DAG, and execute SQL task nodes in topological order.

## Directory Layout

```text
common-core/
  src/main/java/com/custacm/platform/common/sqltask/
  src/test/java/com/custacm/platform/common/sqltask/
  src/test/resources/sqltask/
```

## Dependency And Layer Rules

- Must not contain business entities or module-specific table/domain concepts.
- May contain shared infrastructure helpers that are reusable by multiple backend modules.
- SQL task execution depends only on Spring core resources, Spring JDBC/transactions, SnakeYAML, and SLF4J.
- Callers own their business manifest, SQL files, HTTP controllers, request validation, and security tier.

## File Responsibilities

- `pom.xml` - declares common-core dependencies for Spring resources, JDBC/transactions, YAML parsing, SLF4J, and tests.
- `SqlTaskRunner.java` - public executor that reads the manifest on each invocation, validates the graph, executes nodes sequentially, applies one transaction per node, and logs run/node lifecycle events.
- `YamlSqlTaskManifestLoader.java` - loads and validates YAML manifest structure into task definitions.
- `SqlTaskGraph.java` - builds the adjacency-list graph, validates unique/missing/self dependencies, checks DAG shape, creates topological execution plans, and supports `startFromTaskId` resume plans with a dedicated invalid-start-node error code.
- `SqlScriptSplitter.java` - splits multi-statement SQL scripts while preserving semicolons inside quoted text and comments.
- `SqlTaskDefinition.java` - immutable task definition with id, description, SQL resource location, dependencies, and timeout.
- `SqlTaskExecutionRequest.java` - execution input: manifest location, named SQL parameters, and optional `startFromTaskId`.
- `SqlTaskExecutionResult.java` - run-level result: run id, status, manifest, resume node, failed node, timing, and node results.
- `SqlTaskNodeResult.java` - node-level result for success, failure, or skipped downstream nodes.
- `SqlTaskRunStatus.java` - run status enum.
- `SqlTaskNodeStatus.java` - node status enum.
- `SqlTaskException.java` - runtime exception carrying a stable SQL task error code.
- `SqlTaskErrorCode.java` - stable error codes used by logs and HTTP adapters.
- `SqlTaskRunnerTest.java` - verifies DAG execution, resume execution, invalid graph rejection, fail-fast behavior, skipped downstream nodes, and failed-node transaction rollback.
- `SqlScriptSplitterTest.java` - verifies SQL statement splitting around quotes and comments.
- `src/test/resources/sqltask/*.yml` - test manifests for valid, cyclic, missing-dependency, and failing graphs.
- `src/test/resources/sqltask/*.sql` - test SQL scripts used by the common-core executor tests.
