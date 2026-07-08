# AtCoder Warehouse Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Populate AtCoder `dwd`/`dwm`/`dws` warehouse tables from Kenkoooo ODS data, and route collection-job warehouse refresh through the existing common job and SQL-task infrastructure.

**Architecture:** Keep OJ-specific cleaning logic in `training-data-atcoder` as SQL resources, manifest, refresh service, interval repository, and tests. Reuse the existing common SQL task runner, collection job service, scheduled OJ collection config, same-layer warehouse tables, public query repositories/controllers, and student-data purge path. Add only a small common refresh-handler dispatch contract so collection jobs can refresh Codeforces or AtCoder without hard-coding OJ-specific services in the job service.

**Tech Stack:** Java 17, Spring Boot, Spring JDBC, Flyway, H2 in MySQL mode for tests, existing `common-core` SQL task DAG runner, Maven/JUnit/AssertJ/Mockito.

## Global Constraints

- Do not commit or push unless the project owner explicitly asks. A "push" instruction implies commit and push permission.
- MR titles and descriptions must be in Chinese.
- Follow `platform-training-data/AGENTS.md`: keep OJ-specific source/ODS/SQL in the OJ module; keep common HTTP/query/purge/scheduler paths common.
- Do not add standalone warehouse-refresh HTTP endpoints, ADS tables, persistent pipeline run state, or a cross-OJ pipeline scheduler in this slice.
- AtCoder public reads must use existing `ojName=ATCODER` query paths backed by `dwd_atcoder__submission`, `dwm_atcoder__handle_problem_first_accepted`, and `dws_atcoder__handle_daily_rating_accepted_summary`.
- Kenkoooo problem list has no rating/difficulty field in the currently collected `resources/problems.json`; AtCoder DWD `difficulty` should stay `null`, and DWS should aggregate those rows under `UNRATED`.
- Use set-based SQL transformations. Do not transform warehouse rows one by one in Java.
- After Java changes, run `mvn clean verify` and `./scripts/check-test-policy.sh`.

---

## Research Findings

- AtCoder ODS tables already exist in `training-data-atcoder`:
  - `ods_atcoder__submission`
  - `ods_atcoder__problem`
- AtCoder warehouse tables already exist in `training-data-common/src/main/resources/db/migration/V020__create_atcoder_warehouse_tables.sql` and match the common query contract:
  - `dwd_atcoder__submission`
  - `dwm_atcoder__handle_problem_first_accepted`
  - `dws_atcoder__handle_daily_rating_accepted_summary`
- Public query APIs already select same-layer tables by `ojName` through:
  - `JdbcOjSubmissionRepository`
  - `JdbcOjFirstAcceptedProblemRepository`
  - `JdbcOjAcceptedSummaryRepository`
  - `OjWarehouseQueryController`
- Student cleanup already deletes AtCoder ODS through `JdbcAtcoderOdsDataPurgeRepository` and common DWD/DWM/DWS through `JdbcOjWarehouseDataPurgeRepository`.
- Recent submission collection already supports `ojName=ATCODER` through `OjSubmissionCollectionDispatcher` and `AtcoderSubmissionCollectionService`.
- Batch collection job refresh currently has a Codeforces-only lambda in `CodeforcesTrainingDataConfig`; when `collectionResult.ojName()` is `ATCODER`, it returns a failed refresh result.
- `training-data-atcoder` does not currently depend on `common-core`, but AtCoder warehouse refresh needs `SqlTaskRunner`, so the AtCoder module needs a direct `common-core` dependency.
- Existing Spring schedule config `platform.training-data.collector.schedules` is reusable for AtCoder ODS collection by adding a disabled-by-default schedule entry with `oj-name: ATCODER`. That scheduler currently performs collection only; warehouse refresh remains a collection-job `refreshWarehouse=true` callback in this slice.

## File Structure

### Common Refresh Dispatch

- Create `platform-training-data/training-data-common/src/main/java/com/custacm/platform/trainingdata/common/collector/job/OjWarehouseRefreshHandler.java`
  - Common contract implemented by each OJ that can refresh DWD/DWM/DWS for a collected ODS batch.
- Create `platform-training-data/training-data-common/src/main/java/com/custacm/platform/trainingdata/common/collector/job/OjWarehouseRefreshDispatcher.java`
  - Dispatches `OjSubmissionCollectionResult` to the matching `OjWarehouseRefreshHandler` by normalized OJ name.
- Create `platform-training-data/training-data-common/src/test/java/com/custacm/platform/trainingdata/common/collector/job/OjWarehouseRefreshDispatcherTest.java`
  - Tests dispatch, unsupported-OJ failure, duplicate handler rejection, and blank handler OJ rejection.

### Codeforces Wiring Cleanup

- Create `platform-training-data/training-data-codeforces/src/main/java/com/custacm/platform/trainingdata/codeforces/app/CodeforcesWarehouseRefreshHandler.java`
  - Wraps `CodeforcesWarehouseRefreshService` behind the common refresh-handler contract.
- Modify `platform-training-data/training-data-codeforces/src/main/java/com/custacm/platform/trainingdata/codeforces/config/CodeforcesTrainingDataConfig.java`
  - Register the Codeforces handler.
  - Replace the current hard-coded refresh lambda with `new OjWarehouseRefreshDispatcher(refreshHandlers)`.
- Create `platform-training-data/training-data-codeforces/src/test/java/com/custacm/platform/trainingdata/codeforces/app/CodeforcesWarehouseRefreshHandlerTest.java`
  - Verifies `SqlTaskRunStatus.SUCCESS` maps to `SUCCESS`, `FAILED` maps to `FAILED`, and blank batch behavior is delegated to the service.

### AtCoder Warehouse Refresh

- Modify `platform-training-data/training-data-atcoder/pom.xml`
  - Add direct dependency on `com.custacm.platform:common-core`.
- Create `platform-training-data/training-data-atcoder/src/main/java/com/custacm/platform/trainingdata/atcoder/domain/AtcoderWarehouseRefreshInterval.java`
  - Date interval value object.
- Create `platform-training-data/training-data-atcoder/src/main/java/com/custacm/platform/trainingdata/atcoder/domain/AtcoderWarehouseRefreshIntervalRepository.java`
  - Repository port for deriving refresh dates from an AtCoder ODS batch plus existing DWM facts affected by that batch.
- Create `platform-training-data/training-data-atcoder/src/main/java/com/custacm/platform/trainingdata/atcoder/infra/JdbcAtcoderWarehouseRefreshIntervalRepository.java`
  - JDBC implementation.
- Create `platform-training-data/training-data-atcoder/src/main/java/com/custacm/platform/trainingdata/atcoder/app/AtcoderWarehouseRefreshService.java`
  - Builds `SqlTaskExecutionRequest` for `classpath:sql/tasks/atcoder-warehouse-refresh.yml`.
- Create `platform-training-data/training-data-atcoder/src/main/java/com/custacm/platform/trainingdata/atcoder/app/AtcoderWarehouseRefreshHandler.java`
  - Wraps AtCoder refresh service behind the common refresh-handler contract.
- Modify `platform-training-data/training-data-atcoder/src/main/java/com/custacm/platform/trainingdata/atcoder/config/AtcoderTrainingDataConfig.java`
  - Register interval repository, refresh service, and refresh handler.

### AtCoder SQL Resources

- Create `platform-training-data/training-data-atcoder/src/main/resources/sql/dwd/upsert_dwd_atcoder__submission.sql`
- Create `platform-training-data/training-data-atcoder/src/main/resources/sql/dwm/upsert_dwm_atcoder__handle_problem_first_accepted.sql`
- Create `platform-training-data/training-data-atcoder/src/main/resources/sql/dws/upsert_dws_atcoder__handle_daily_rating_accepted_summary.sql`
- Create `platform-training-data/training-data-atcoder/src/main/resources/sql/tasks/atcoder-warehouse-refresh.yml`

### AtCoder Tests

- Create `platform-training-data/training-data-atcoder/src/test/java/com/custacm/platform/trainingdata/atcoder/app/AtcoderWarehouseRefreshServiceTest.java`
- Create `platform-training-data/training-data-atcoder/src/test/java/com/custacm/platform/trainingdata/atcoder/app/AtcoderWarehouseRefreshHandlerTest.java`
- Create `platform-training-data/training-data-atcoder/src/test/java/com/custacm/platform/trainingdata/atcoder/infra/JdbcAtcoderWarehouseRefreshIntervalRepositoryTest.java`
- Create `platform-training-data/training-data-atcoder/src/test/java/com/custacm/platform/trainingdata/atcoder/infra/AtcoderWarehouseSqlTaskTest.java`

### Web/Config/Docs

- Modify `platform-training-data/training-data-web/src/main/resources/application.yml`
  - Add a disabled-by-default AtCoder recent-submission schedule entry under `platform.training-data.collector.schedules`.
- Modify `platform-training-data/training-data-web/src/test/java/com/custacm/platform/trainingdata/web/AtcoderCollectionHttpIntegrationTest.java`
  - Add a batch-job test for `ojName=ATCODER` with `refreshWarehouse=true`.
- Update module docs:
  - `platform-training-data/README.md`
  - `platform-training-data/AGENTS.md`
  - `platform-training-data/docs/ods-submission.md`
  - `platform-training-data/docs/atcoder-collection.md`
  - `platform-training-data/training-data-atcoder/README.md`
  - `platform-training-data/training-data-common/README.md`
  - `platform-training-data/training-data-codeforces/README.md`
  - `platform-training-data/training-data-web/README.md`
  - `docs/api.md`
  - `docs/architecture.md`
  - `docs/agent/context-map.md`

---

### Task 1: Add Common Warehouse Refresh Dispatch

**Files:**
- Create: `platform-training-data/training-data-common/src/main/java/com/custacm/platform/trainingdata/common/collector/job/OjWarehouseRefreshHandler.java`
- Create: `platform-training-data/training-data-common/src/main/java/com/custacm/platform/trainingdata/common/collector/job/OjWarehouseRefreshDispatcher.java`
- Create: `platform-training-data/training-data-common/src/test/java/com/custacm/platform/trainingdata/common/collector/job/OjWarehouseRefreshDispatcherTest.java`

**Interfaces:**
- Consumes: `OjSubmissionCollectionResult`, `OjSubmissionCollectionJobRefreshResult`, `OjNames.normalize`.
- Produces: a common `OjWarehouseRefreshHandler` list that OJ modules implement and the collection job service can call.

- [ ] **Step 1: Write failing dispatcher tests**

Test cases:

```java
class OjWarehouseRefreshDispatcherTest {
    @Test
    void dispatchesRefreshToMatchingOjHandler() {
        OjWarehouseRefreshDispatcher dispatcher = new OjWarehouseRefreshDispatcher(List.of(
                new FakeHandler("ATCODER", OjSubmissionCollectionJobRefreshResult.notRequested())
        ));

        OjSubmissionCollectionJobRefreshResult result = dispatcher.refresh(collectionResult("ATCODER", "batch-1"));

        assertThat(result.status()).isEqualTo(OjSubmissionCollectionJobRefreshStatus.NOT_REQUESTED);
    }

    @Test
    void returnsFailedRefreshForUnsupportedOj() {
        OjWarehouseRefreshDispatcher dispatcher = new OjWarehouseRefreshDispatcher(List.of());

        OjSubmissionCollectionJobRefreshResult result = dispatcher.refresh(collectionResult("ATCODER", "batch-1"));

        assertThat(result.status()).isEqualTo(OjSubmissionCollectionJobRefreshStatus.FAILED);
        assertThat(result.message()).isEqualTo("ATCODER warehouse refresh is not implemented");
    }

    @Test
    void rejectsDuplicateHandlers() {
        assertThatThrownBy(() -> new OjWarehouseRefreshDispatcher(List.of(
                new FakeHandler("ATCODER", OjSubmissionCollectionJobRefreshResult.notRequested()),
                new FakeHandler("atcoder", OjSubmissionCollectionJobRefreshResult.notRequested())
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("duplicate OJ warehouse refresh handler: ATCODER");
    }
}
```

- [ ] **Step 2: Run the targeted failing test**

Run:

```bash
mvn -pl platform-training-data/training-data-common -Dtest=OjWarehouseRefreshDispatcherTest test
```

Expected: compilation fails because `OjWarehouseRefreshHandler` and `OjWarehouseRefreshDispatcher` do not exist.

- [ ] **Step 3: Add the common interface**

Implementation shape:

```java
package com.custacm.platform.trainingdata.common.collector.job;

public interface OjWarehouseRefreshHandler {
    String ojName();

    OjSubmissionCollectionJobRefreshResult refresh(String batchId);
}
```

- [ ] **Step 4: Add the dispatcher**

Implementation shape:

```java
package com.custacm.platform.trainingdata.common.collector.job;

import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionResult;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class OjWarehouseRefreshDispatcher implements OjSubmissionCollectionJobService.RefreshHandler {
    private final Map<String, OjWarehouseRefreshHandler> handlersByOjName;

    public OjWarehouseRefreshDispatcher(List<OjWarehouseRefreshHandler> handlers) {
        this.handlersByOjName = handlersByOjName(handlers);
    }

    @Override
    public OjSubmissionCollectionJobRefreshResult refresh(OjSubmissionCollectionResult result) {
        String ojName = OjNames.normalize(result.ojName());
        OjWarehouseRefreshHandler handler = handlersByOjName.get(ojName);
        if (handler == null) {
            return OjSubmissionCollectionJobRefreshResult.failed(ojName + " warehouse refresh is not implemented");
        }
        return handler.refresh(result.batchId());
    }

    private static Map<String, OjWarehouseRefreshHandler> handlersByOjName(List<OjWarehouseRefreshHandler> handlers) {
        Map<String, OjWarehouseRefreshHandler> indexed = new LinkedHashMap<>();
        for (OjWarehouseRefreshHandler handler : handlers == null ? List.<OjWarehouseRefreshHandler>of() : handlers) {
            OjWarehouseRefreshHandler nonNullHandler = Objects.requireNonNull(handler, "handler must not be null");
            String ojName = OjNames.normalize(nonNullHandler.ojName());
            if (indexed.putIfAbsent(ojName, nonNullHandler) != null) {
                throw new IllegalArgumentException("duplicate OJ warehouse refresh handler: " + ojName);
            }
        }
        return Map.copyOf(indexed);
    }
}
```

- [ ] **Step 5: Verify**

Run:

```bash
mvn -pl platform-training-data/training-data-common -Dtest=OjWarehouseRefreshDispatcherTest test
```

Expected: test passes.

---

### Task 2: Move Codeforces Refresh Routing Behind the Common Handler

**Files:**
- Create: `platform-training-data/training-data-codeforces/src/main/java/com/custacm/platform/trainingdata/codeforces/app/CodeforcesWarehouseRefreshHandler.java`
- Create: `platform-training-data/training-data-codeforces/src/test/java/com/custacm/platform/trainingdata/codeforces/app/CodeforcesWarehouseRefreshHandlerTest.java`
- Modify: `platform-training-data/training-data-codeforces/src/main/java/com/custacm/platform/trainingdata/codeforces/config/CodeforcesTrainingDataConfig.java`

**Interfaces:**
- Consumes: `CodeforcesWarehouseRefreshService.refresh(String batchId, String startFromTaskId)`.
- Produces: `OjWarehouseRefreshHandler` bean for `CODEFORCES`.

- [ ] **Step 1: Write the handler test**

Expected behavior:

```java
@Test
void mapsSuccessfulSqlTaskRunToSuccessfulJobRefresh() {
    CodeforcesWarehouseRefreshService service = mock(CodeforcesWarehouseRefreshService.class);
    when(service.refresh("batch-1", null)).thenReturn(sqlResult(SqlTaskRunStatus.SUCCESS));

    CodeforcesWarehouseRefreshHandler handler = new CodeforcesWarehouseRefreshHandler(service);

    OjSubmissionCollectionJobRefreshResult result = handler.refresh("batch-1");

    assertThat(handler.ojName()).isEqualTo(OjNames.CODEFORCES);
    assertThat(result.status()).isEqualTo(OjSubmissionCollectionJobRefreshStatus.SUCCESS);
    assertThat(result.message()).isEqualTo("SUCCESS");
}
```

- [ ] **Step 2: Add the handler**

Implementation shape:

```java
public class CodeforcesWarehouseRefreshHandler implements OjWarehouseRefreshHandler {
    private final CodeforcesWarehouseRefreshService refreshService;

    public CodeforcesWarehouseRefreshHandler(CodeforcesWarehouseRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    @Override
    public String ojName() {
        return OjNames.CODEFORCES;
    }

    @Override
    public OjSubmissionCollectionJobRefreshResult refresh(String batchId) {
        SqlTaskExecutionResult result = refreshService.refresh(batchId, null);
        return new OjSubmissionCollectionJobRefreshResult(
                result.status() == SqlTaskRunStatus.SUCCESS
                        ? OjSubmissionCollectionJobRefreshStatus.SUCCESS
                        : OjSubmissionCollectionJobRefreshStatus.FAILED,
                result.status().name()
        );
    }
}
```

- [ ] **Step 3: Replace the Codeforces-only lambda in config**

Change `codeforcesSubmissionCollectionJobService` to accept `List<OjWarehouseRefreshHandler> refreshHandlers` and pass:

```java
new OjWarehouseRefreshDispatcher(refreshHandlers)
```

instead of the inline `if (!OjNames.CODEFORCES.equals(...))` lambda.

- [ ] **Step 4: Register the Codeforces handler bean**

Add:

```java
@Bean
OjWarehouseRefreshHandler codeforcesWarehouseRefreshHandler(
        CodeforcesWarehouseRefreshService warehouseRefreshService
) {
    return new CodeforcesWarehouseRefreshHandler(warehouseRefreshService);
}
```

- [ ] **Step 5: Verify Codeforces tests still pass**

Run:

```bash
mvn -pl platform-training-data/training-data-codeforces -Dtest=CodeforcesWarehouseRefreshHandlerTest,CodeforcesWarehouseRefreshServiceTest test
```

Expected: both tests pass.

---

### Task 3: Add AtCoder Refresh Interval Repository

**Files:**
- Create: `platform-training-data/training-data-atcoder/src/main/java/com/custacm/platform/trainingdata/atcoder/domain/AtcoderWarehouseRefreshInterval.java`
- Create: `platform-training-data/training-data-atcoder/src/main/java/com/custacm/platform/trainingdata/atcoder/domain/AtcoderWarehouseRefreshIntervalRepository.java`
- Create: `platform-training-data/training-data-atcoder/src/main/java/com/custacm/platform/trainingdata/atcoder/infra/JdbcAtcoderWarehouseRefreshIntervalRepository.java`
- Create: `platform-training-data/training-data-atcoder/src/test/java/com/custacm/platform/trainingdata/atcoder/infra/JdbcAtcoderWarehouseRefreshIntervalRepositoryTest.java`

**Interfaces:**
- Consumes: `ods_atcoder__submission`, existing `dwm_atcoder__handle_problem_first_accepted`.
- Produces: `Optional<AtcoderWarehouseRefreshInterval> findBatchDateInterval(String batchId)`.

- [ ] **Step 1: Write repository tests**

Test cases:

```java
@Test
void returnsBaseBatchSubmittedDateInterval() {
    insertOds("batch-1", 1L, "tourist", "abc100_a", "2026-07-01T23:30:00Z", "AC");
    insertOds("batch-1", 2L, "tourist", "abc100_b", "2026-07-02T00:30:00Z", "WA");

    Optional<AtcoderWarehouseRefreshInterval> interval = repository.findBatchDateInterval("batch-1");

    assertThat(interval).contains(new AtcoderWarehouseRefreshInterval(
            LocalDate.parse("2026-07-02"),
            LocalDate.parse("2026-07-02")
    ));
}

@Test
void includesExistingFirstAcceptedDateForTouchedAcceptedProblems() {
    insertExistingDwm("tourist", "abc100_a", "2026-07-10");
    insertOds("batch-1", 1L, "tourist", "abc100_a", "2026-07-01T00:00:00Z", "AC");

    Optional<AtcoderWarehouseRefreshInterval> interval = repository.findBatchDateInterval("batch-1");

    assertThat(interval).contains(new AtcoderWarehouseRefreshInterval(
            LocalDate.parse("2026-07-01"),
            LocalDate.parse("2026-07-10")
    ));
}
```

- [ ] **Step 2: Add value object and port**

Implementation shape:

```java
public record AtcoderWarehouseRefreshInterval(
        LocalDate fromDateUtcPlus8,
        LocalDate toDateUtcPlus8
) {
    public AtcoderWarehouseRefreshInterval {
        Objects.requireNonNull(fromDateUtcPlus8, "fromDateUtcPlus8 must not be null");
        Objects.requireNonNull(toDateUtcPlus8, "toDateUtcPlus8 must not be null");
        if (toDateUtcPlus8.isBefore(fromDateUtcPlus8)) {
            throw new IllegalArgumentException("toDateUtcPlus8 must not be before fromDateUtcPlus8");
        }
    }
}

public interface AtcoderWarehouseRefreshIntervalRepository {
    Optional<AtcoderWarehouseRefreshInterval> findBatchDateInterval(String batchId);
}
```

- [ ] **Step 3: Implement JDBC interval query**

Use a union of:

- UTC+8 submitted dates from the ODS batch;
- old DWM first-accepted dates for accepted `handle + problem_id` pairs touched by the batch.

SQL shape:

```sql
select min(refresh_date) as from_date, max(refresh_date) as to_date
from (
    select cast(timestampadd(
        HOUR,
        8,
        timestampadd(SECOND, epoch_second, timestamp '1970-01-01 00:00:00')
    ) as date) as refresh_date
    from ods_atcoder__submission
    where batch_id = :batchId

    union all

    select existing.first_accepted_date_utc_plus8 as refresh_date
    from dwm_atcoder__handle_problem_first_accepted existing
    join (
        select distinct user_id, problem_id
        from ods_atcoder__submission
        where batch_id = :batchId
          and result = 'AC'
          and problem_id is not null
          and trim(problem_id) <> ''
    ) touched
      on existing.handle = touched.user_id
     and existing.problem_key = touched.problem_id
) refresh_dates
```

- [ ] **Step 4: Verify**

Run:

```bash
mvn -pl platform-training-data/training-data-atcoder -Dtest=JdbcAtcoderWarehouseRefreshIntervalRepositoryTest test
```

Expected: test passes.

---

### Task 4: Add AtCoder SQL Task Resources

**Files:**
- Create: `platform-training-data/training-data-atcoder/src/main/resources/sql/dwd/upsert_dwd_atcoder__submission.sql`
- Create: `platform-training-data/training-data-atcoder/src/main/resources/sql/dwm/upsert_dwm_atcoder__handle_problem_first_accepted.sql`
- Create: `platform-training-data/training-data-atcoder/src/main/resources/sql/dws/upsert_dws_atcoder__handle_daily_rating_accepted_summary.sql`
- Create: `platform-training-data/training-data-atcoder/src/main/resources/sql/tasks/atcoder-warehouse-refresh.yml`
- Create: `platform-training-data/training-data-atcoder/src/test/java/com/custacm/platform/trainingdata/atcoder/infra/AtcoderWarehouseSqlTaskTest.java`

**Interfaces:**
- Consumes: SQL parameters `batchId`, `refreshFromDateUtcPlus8`, `refreshToDateUtcPlus8`.
- Produces: rows in existing AtCoder warehouse tables.

- [ ] **Step 1: Write the H2 SQL chain test**

Test fixture:

- Insert `ods_atcoder__problem` rows for `abc100_a` and `abc100_b`.
- Insert ODS submissions:
  - `tourist`, `abc100_a`, `AC`, earlier timestamp.
  - `tourist`, `abc100_a`, `WA`, later timestamp.
  - `tourist`, `abc100_b`, `AC`.
  - `other`, `abc100_a`, `AC`.
- Run the SQL task runner against `classpath:sql/tasks/atcoder-warehouse-refresh.yml`.
- Assert:
  - DWD row count equals ODS submission count in interval.
  - DWD `problem_key` equals AtCoder `problem_id`, for example `abc100_a`.
  - DWD `source_url` equals `https://atcoder.jp/contests/abc100/submissions/{submissionId}`.
  - DWM has one row per `handle + problem_key`.
  - DWM tie-break uses earliest `submitted_at_utc_plus8`, then smallest `submission_id`.
  - DWS contains `UNRATED` summary counts because current Kenkoooo problem list does not provide difficulty.
  - Running the task twice leaves row counts stable.

- [ ] **Step 2: Add DWD SQL**

Core SQL:

```sql
delete from dwd_atcoder__submission
where submitted_date_utc_plus8 between :refreshFromDateUtcPlus8 and :refreshToDateUtcPlus8;

insert into dwd_atcoder__submission (
    ods_submission_id,
    submission_id,
    handle,
    submitted_at_utc_plus8,
    submitted_date_utc_plus8,
    problem_key,
    problem_index,
    problem_name,
    difficulty,
    language,
    verdict,
    is_accepted,
    time_consumed_millis,
    source_url,
    ods_batch_id,
    ods_fetched_at,
    ods_payload_hash
)
select
    ods.id,
    concat('', ods.atcoder_submission_id),
    ods.user_id,
    timestampadd(HOUR, 8, timestampadd(SECOND, ods.epoch_second, timestamp '1970-01-01 00:00:00')),
    cast(timestampadd(HOUR, 8, timestampadd(SECOND, ods.epoch_second, timestamp '1970-01-01 00:00:00')) as date),
    ods.problem_id,
    problem.problem_index,
    coalesce(problem.title, problem.problem_name, ods.problem_id),
    null,
    ods.language,
    ods.result,
    case when ods.result = 'AC' then 1 else 0 end,
    ods.execution_time_millis,
    case
        when ods.contest_id is null or trim(ods.contest_id) = '' then null
        else concat('https://atcoder.jp/contests/', ods.contest_id, '/submissions/', ods.atcoder_submission_id)
    end,
    ods.batch_id,
    ods.fetched_at,
    ods.payload_hash
from ods_atcoder__submission ods
left join ods_atcoder__problem problem
       on problem.problem_id = ods.problem_id
where cast(timestampadd(HOUR, 8, timestampadd(SECOND, ods.epoch_second, timestamp '1970-01-01 00:00:00')) as date)
      between :refreshFromDateUtcPlus8 and :refreshToDateUtcPlus8
on duplicate key update
    ods_submission_id = values(ods_submission_id),
    handle = values(handle),
    submitted_at_utc_plus8 = values(submitted_at_utc_plus8),
    submitted_date_utc_plus8 = values(submitted_date_utc_plus8),
    problem_key = values(problem_key),
    problem_index = values(problem_index),
    problem_name = values(problem_name),
    difficulty = values(difficulty),
    language = values(language),
    verdict = values(verdict),
    is_accepted = values(is_accepted),
    time_consumed_millis = values(time_consumed_millis),
    source_url = values(source_url),
    ods_batch_id = values(ods_batch_id),
    ods_fetched_at = values(ods_fetched_at),
    ods_payload_hash = values(ods_payload_hash),
    updated_at = current_timestamp(6);
```

- [ ] **Step 3: Add DWM SQL**

Core SQL:

```sql
delete from dwm_atcoder__handle_problem_first_accepted
where first_accepted_date_utc_plus8 between :refreshFromDateUtcPlus8 and :refreshToDateUtcPlus8;

insert into dwm_atcoder__handle_problem_first_accepted (
    handle,
    problem_key,
    problem_index,
    problem_name,
    difficulty,
    first_accepted_submission_id,
    first_accepted_at_utc_plus8,
    first_accepted_date_utc_plus8,
    first_accepted_language,
    first_accepted_source_url
)
select
    ranked.handle,
    ranked.problem_key,
    ranked.problem_index,
    ranked.problem_name,
    ranked.difficulty,
    ranked.submission_id,
    ranked.submitted_at_utc_plus8,
    ranked.submitted_date_utc_plus8,
    ranked.language,
    ranked.source_url
from (
    select
        dwd.*,
        row_number() over (
            partition by dwd.handle, dwd.problem_key
            order by dwd.submitted_at_utc_plus8, length(dwd.submission_id), dwd.submission_id
        ) as accepted_rank
    from dwd_atcoder__submission dwd
    where dwd.is_accepted = 1
      and dwd.problem_key is not null
      and trim(dwd.problem_key) <> ''
      and dwd.submitted_at_utc_plus8 is not null
      and dwd.submitted_date_utc_plus8 is not null
) ranked
where ranked.accepted_rank = 1
  and ranked.submitted_date_utc_plus8 between :refreshFromDateUtcPlus8 and :refreshToDateUtcPlus8
on duplicate key update
    problem_index = values(problem_index),
    problem_name = values(problem_name),
    difficulty = values(difficulty),
    first_accepted_submission_id = values(first_accepted_submission_id),
    first_accepted_at_utc_plus8 = values(first_accepted_at_utc_plus8),
    first_accepted_date_utc_plus8 = values(first_accepted_date_utc_plus8),
    first_accepted_language = values(first_accepted_language),
    first_accepted_source_url = values(first_accepted_source_url),
    updated_at = current_timestamp(6);
```

- [ ] **Step 4: Add DWS SQL**

Core SQL:

```sql
delete from dws_atcoder__handle_daily_rating_accepted_summary
where accepted_date_utc_plus8 between :refreshFromDateUtcPlus8 and :refreshToDateUtcPlus8;

insert into dws_atcoder__handle_daily_rating_accepted_summary (
    handle,
    accepted_date_utc_plus8,
    difficulty,
    accepted_problem_count
)
select
    first_accepted.handle,
    first_accepted.first_accepted_date_utc_plus8,
    coalesce(first_accepted.difficulty, 'UNRATED') as difficulty,
    count(*) as accepted_problem_count
from dwm_atcoder__handle_problem_first_accepted first_accepted
where first_accepted.first_accepted_date_utc_plus8 between :refreshFromDateUtcPlus8 and :refreshToDateUtcPlus8
group by
    first_accepted.handle,
    first_accepted.first_accepted_date_utc_plus8,
    coalesce(first_accepted.difficulty, 'UNRATED')
on duplicate key update
    accepted_problem_count = values(accepted_problem_count),
    updated_at = current_timestamp(6);
```

- [ ] **Step 5: Add SQL task manifest**

Manifest:

```yaml
tasks:
  - id: atcoder.dwd.submission
    description: Refresh AtCoder DWD submissions for the ODS batch date interval.
    sql: classpath:sql/dwd/upsert_dwd_atcoder__submission.sql
    timeoutSeconds: 60
  - id: atcoder.dwm.handle_problem_first_accepted
    description: Refresh AtCoder first-accepted handle/problem facts from DWD.
    sql: classpath:sql/dwm/upsert_dwm_atcoder__handle_problem_first_accepted.sql
    dependsOn:
      - atcoder.dwd.submission
    timeoutSeconds: 60
  - id: atcoder.dws.handle_daily_rating_accepted_summary
    description: Refresh AtCoder handle/date/difficulty accepted summary from DWM.
    sql: classpath:sql/dws/upsert_dws_atcoder__handle_daily_rating_accepted_summary.sql
    dependsOn:
      - atcoder.dwm.handle_problem_first_accepted
    timeoutSeconds: 60
```

- [ ] **Step 6: Verify**

Run:

```bash
mvn -pl platform-training-data/training-data-atcoder -Dtest=AtcoderWarehouseSqlTaskTest test
```

Expected: SQL chain passes and is idempotent.

---

### Task 5: Add AtCoder Refresh Service and Handler

**Files:**
- Modify: `platform-training-data/training-data-atcoder/pom.xml`
- Create: `platform-training-data/training-data-atcoder/src/main/java/com/custacm/platform/trainingdata/atcoder/app/AtcoderWarehouseRefreshService.java`
- Create: `platform-training-data/training-data-atcoder/src/main/java/com/custacm/platform/trainingdata/atcoder/app/AtcoderWarehouseRefreshHandler.java`
- Modify: `platform-training-data/training-data-atcoder/src/main/java/com/custacm/platform/trainingdata/atcoder/config/AtcoderTrainingDataConfig.java`
- Create: `platform-training-data/training-data-atcoder/src/test/java/com/custacm/platform/trainingdata/atcoder/app/AtcoderWarehouseRefreshServiceTest.java`
- Create: `platform-training-data/training-data-atcoder/src/test/java/com/custacm/platform/trainingdata/atcoder/app/AtcoderWarehouseRefreshHandlerTest.java`

**Interfaces:**
- Consumes: `SqlTaskRunner`, `AtcoderWarehouseRefreshIntervalRepository`.
- Produces: `AtcoderWarehouseRefreshService.refresh(String batchId, String startFromTaskId)` and common refresh handler bean for `ATCODER`.

- [ ] **Step 1: Add `common-core` dependency**

Add to `training-data-atcoder/pom.xml`:

```xml
<dependency>
    <groupId>com.custacm.platform</groupId>
    <artifactId>common-core</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 2: Write refresh service tests**

Expected assertions:

```java
@Test
void runsAtcoderManifestWithBatchIdDateParametersAndResumeNode() {
    when(intervalRepository.findBatchDateInterval("batch-1"))
            .thenReturn(Optional.of(new AtcoderWarehouseRefreshInterval(
                    LocalDate.parse("2026-07-01"),
                    LocalDate.parse("2026-07-03")
            )));
    when(runner.execute(any())).thenReturn(successResult());

    service.refresh(" batch-1 ", " atcoder.dwm.handle_problem_first_accepted ");

    verify(runner).execute(captor.capture());
    SqlTaskExecutionRequest request = captor.getValue();
    assertThat(request.manifestLocation()).isEqualTo("classpath:sql/tasks/atcoder-warehouse-refresh.yml");
    assertThat(request.parameters().get("batchId")).isEqualTo("batch-1");
    assertThat(request.parameters().get("refreshFromDateUtcPlus8")).isEqualTo(Date.valueOf("2026-07-01"));
    assertThat(request.parameters().get("refreshToDateUtcPlus8")).isEqualTo(Date.valueOf("2026-07-03"));
    assertThat(request.startFromTaskId()).isEqualTo("atcoder.dwm.handle_problem_first_accepted");
}
```

- [ ] **Step 3: Implement service**

Implementation shape mirrors Codeforces with AtCoder manifest and error message:

```java
public class AtcoderWarehouseRefreshService {
    private static final String MANIFEST_LOCATION = "classpath:sql/tasks/atcoder-warehouse-refresh.yml";

    public SqlTaskExecutionResult refresh(String batchId, String startFromTaskId) {
        String normalizedBatchId = requireText(batchId, "batchId");
        String normalizedStartFromTaskId = normalizeOptionalText(startFromTaskId);
        AtcoderWarehouseRefreshInterval interval = intervalRepository.findBatchDateInterval(normalizedBatchId)
                .orElseThrow(() -> new IllegalArgumentException("batchId has no AtCoder submissions"));
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("batchId", normalizedBatchId);
        parameters.put("refreshFromDateUtcPlus8", Date.valueOf(interval.fromDateUtcPlus8()));
        parameters.put("refreshToDateUtcPlus8", Date.valueOf(interval.toDateUtcPlus8()));
        return sqlTaskRunner.execute(new SqlTaskExecutionRequest(
                MANIFEST_LOCATION,
                parameters,
                normalizedStartFromTaskId
        ));
    }
}
```

- [ ] **Step 4: Implement handler**

Implementation shape:

```java
public class AtcoderWarehouseRefreshHandler implements OjWarehouseRefreshHandler {
    private final AtcoderWarehouseRefreshService refreshService;

    @Override
    public String ojName() {
        return OjNames.ATCODER;
    }

    @Override
    public OjSubmissionCollectionJobRefreshResult refresh(String batchId) {
        SqlTaskExecutionResult result = refreshService.refresh(batchId, null);
        return new OjSubmissionCollectionJobRefreshResult(
                result.status() == SqlTaskRunStatus.SUCCESS
                        ? OjSubmissionCollectionJobRefreshStatus.SUCCESS
                        : OjSubmissionCollectionJobRefreshStatus.FAILED,
                result.status().name()
        );
    }
}
```

- [ ] **Step 5: Wire Spring beans**

Add to `AtcoderTrainingDataConfig`:

```java
@Bean
AtcoderWarehouseRefreshIntervalRepository atcoderWarehouseRefreshIntervalRepository(
        NamedParameterJdbcTemplate jdbcTemplate
) {
    return new JdbcAtcoderWarehouseRefreshIntervalRepository(jdbcTemplate);
}

@Bean
AtcoderWarehouseRefreshService atcoderWarehouseRefreshService(
        SqlTaskRunner sqlTaskRunner,
        AtcoderWarehouseRefreshIntervalRepository intervalRepository
) {
    return new AtcoderWarehouseRefreshService(sqlTaskRunner, intervalRepository);
}

@Bean
OjWarehouseRefreshHandler atcoderWarehouseRefreshHandler(
        AtcoderWarehouseRefreshService refreshService
) {
    return new AtcoderWarehouseRefreshHandler(refreshService);
}
```

- [ ] **Step 6: Verify**

Run:

```bash
mvn -pl platform-training-data/training-data-atcoder -Dtest=AtcoderWarehouseRefreshServiceTest,AtcoderWarehouseRefreshHandlerTest test
```

Expected: tests pass.

---

### Task 6: Add AtCoder Schedule Config and Web Job Verification

**Files:**
- Modify: `platform-training-data/training-data-web/src/main/resources/application.yml`
- Modify: `platform-training-data/training-data-web/src/test/java/com/custacm/platform/trainingdata/web/AtcoderCollectionHttpIntegrationTest.java`

**Interfaces:**
- Consumes: existing common `POST /api/training-data/admin/codeforces/submissions:collect-batch-jobs`.
- Produces: AtCoder jobs with `refreshWarehouse=true` should report successful refresh and populate DWD/DWM/DWS.

- [ ] **Step 1: Add disabled AtCoder recent-submission schedule entry**

Append a second schedule under `platform.training-data.collector.schedules`:

```yaml
- name: atcoder-daily-recent-submissions
  oj-name: ATCODER
  enabled: false
  cron: "0 15 12 * * *"
  zone: Asia/Shanghai
  lookback: 120h
```

This reuses `OjCollectorSchedulingConfig`. It collects AtCoder ODS only, matching the current common scheduler behavior.

- [ ] **Step 2: Add a web integration test for batch job refresh**

Test flow:

1. Insert `oj_handle_account` with `{"ATCODER":"tourist"}`.
2. Insert or collect one `ods_atcoder__problem` row for `abc121_c`.
3. Mock `RestClientAtcoderSourceClient.fetchUserSubmissions("tourist", anyLong())` to return one recent AC submission then an empty page if needed.
4. Start:

```http
POST /api/training-data/admin/codeforces/submissions:collect-batch-jobs
{
  "studentIdentities": ["112487张三"],
  "lookbackHours": 120,
  "refreshWarehouse": true,
  "ojName": "ATCODER"
}
```

5. Poll `/api/training-data/admin/codeforces/submissions/collect-batch-jobs` until the returned job is not `RUNNING`.
6. Assert:
   - job `ojName` is `ATCODER`;
   - item `refreshStatus` is `SUCCESS`;
   - `ods_atcoder__submission` has one row;
   - `dwd_atcoder__submission` has one row;
   - `dwm_atcoder__handle_problem_first_accepted` has one row;
   - `dws_atcoder__handle_daily_rating_accepted_summary` has one `UNRATED` row.

- [ ] **Step 3: Verify web integration**

Run:

```bash
mvn -pl platform-training-data/training-data-web -Dtest=AtcoderCollectionHttpIntegrationTest test
```

Expected: all AtCoder web integration tests pass.

---

### Task 7: Update Documentation

**Files:**
- Modify: `platform-training-data/README.md`
- Modify: `platform-training-data/AGENTS.md`
- Modify: `platform-training-data/docs/ods-submission.md`
- Modify: `platform-training-data/docs/atcoder-collection.md`
- Modify: `platform-training-data/training-data-atcoder/README.md`
- Modify: `platform-training-data/training-data-common/README.md`
- Modify: `platform-training-data/training-data-codeforces/README.md`
- Modify: `platform-training-data/training-data-web/README.md`
- Modify: `docs/api.md`
- Modify: `docs/architecture.md`
- Modify: `docs/agent/context-map.md`

**Interfaces:**
- Consumes: actual implementation from Tasks 1-6.
- Produces: docs aligned with code and config.

- [ ] **Step 1: Update AtCoder collection and warehouse docs**

Required facts to document:

- AtCoder warehouse refresh now exists and is collection-job-triggered.
- SQL task order is DWD -> DWM -> DWS.
- DWD derives `problem_key` from Kenkoooo `problem_id`.
- DWD left joins `ods_atcoder__problem` for `problem_index` and display name.
- DWD stores `difficulty = null` because current problem list has no difficulty field.
- DWS groups `null` difficulty as `UNRATED`.
- Public read APIs remain the existing Codeforces-compatible paths with `ojName=ATCODER`.
- No standalone warehouse refresh HTTP endpoint was added.

- [ ] **Step 2: Update module README file responsibility tables**

Add exact entries for:

- AtCoder refresh service/handler.
- AtCoder interval record/repository.
- AtCoder SQL resources and manifest.
- Common refresh handler/dispatcher.
- Codeforces refresh handler and config routing.
- Web disabled AtCoder ODS schedule entry.

- [ ] **Step 3: Update API docs**

Update batch job section:

- `refreshWarehouse=true` now supports `CODEFORCES` and `ATCODER`.
- AtCoder refresh writes same-layer DWD/DWM/DWS rows.
- AtCoder difficulty/rating summary is `UNRATED` until a difficulty source is added.

- [ ] **Step 4: Update context map**

Change AtCoder known state from "no cleaning SQL" to "has ODS-to-DWD/DWM/DWS SQL refresh through collection-job callback".

- [ ] **Step 5: Verify doc sync locally if refs are available**

Run:

```bash
./scripts/check-doc-sync.sh origin/main WORKTREE
```

Expected: passes when local `origin/main` is available. If `origin/main` is unavailable, record the reason in the final implementation summary.

---

### Task 8: Full Verification

**Files:**
- No source changes beyond Tasks 1-7.

**Interfaces:**
- Consumes: full Maven reactor.
- Produces: final confidence that Java, SQL, docs, and tests are consistent.

- [ ] **Step 1: Run targeted module tests**

Run:

```bash
mvn -pl platform-training-data/training-data-common,platform-training-data/training-data-codeforces,platform-training-data/training-data-atcoder,platform-training-data/training-data-web test
```

Expected: all targeted modules pass.

- [ ] **Step 2: Run full verification**

Run:

```bash
mvn clean verify
./scripts/check-test-policy.sh
```

Expected:

- Maven reactor succeeds.
- Test policy passes.
- AtCoder module has tests and reports.
- No live Kenkoooo calls happen in default tests.

- [ ] **Step 3: Manual local smoke path**

Use mocked tests for default verification. For a manual run with real data after deployment, expected flow is:

```text
1. POST /api/training-data/admin/atcoder/problems:collect
2. POST /api/training-data/admin/codeforces/submissions:collect-batch-jobs
   body: ojName=ATCODER, refreshWarehouse=true, studentIdentities=[...]
3. GET /api/training-data/admin/codeforces/submissions/collect-batch-jobs
4. GET /api/training-data/codeforces/submissions/by-student?ojName=ATCODER&studentIdentity=...
5. GET /api/training-data/codeforces/first-accepted/by-student?ojName=ATCODER&studentIdentity=...
6. GET /api/training-data/codeforces/accepted-summary?ojName=ATCODER&studentIdentity=...
```

Expected:

- job refresh status is `SUCCESS`;
- DWD returns submission details;
- DWM returns first accepted problems;
- DWS accepted summary counts are under `UNRATED`.

## Self-Review

- Spec coverage: DWD, DWM, DWS cleaning SQL are in Task 4; task manifest and insertion into job refresh are Tasks 1, 2, 5, and 6; scheduler config is Task 6; common query/table/purge paths are reused by design.
- Boundary check: AtCoder-specific SQL, interval logic, service, handler, and tests stay in `training-data-atcoder`; only generic refresh dispatch goes to `training-data-common`.
- Risk check: AtCoder difficulty is unavailable from current ODS sources, so rating filters will exclude AtCoder rows unless no rating bounds are supplied. This is documented as current behavior, not guessed implementation.
- Verification check: targeted tests, full Maven verify, test policy, and doc sync are included.
