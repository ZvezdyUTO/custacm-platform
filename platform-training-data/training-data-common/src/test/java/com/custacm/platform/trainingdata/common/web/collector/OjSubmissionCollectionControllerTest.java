package com.custacm.platform.trainingdata.common.web.collector;

import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionResult;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionStatus;
import com.custacm.platform.trainingdata.common.collector.job.OjSubmissionCollectionJobItem;
import com.custacm.platform.trainingdata.common.collector.job.OjSubmissionCollectionJobService;
import com.custacm.platform.trainingdata.common.collector.job.OjSubmissionCollectionJobSnapshot;
import com.custacm.platform.trainingdata.common.collector.job.OjSubmissionCollectionJobStatus;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import com.custacm.platform.trainingdata.common.web.collector.request.OjSubmissionCollectionJobStartRequest;
import com.custacm.platform.trainingdata.common.web.collector.request.OjSubmissionCollectionRequest;
import com.custacm.platform.trainingdata.common.scheduler.OjScheduledSubmissionCollectionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OjSubmissionCollectionControllerTest {
    private static final Instant WINDOW_START = Instant.parse("2026-06-30T04:00:00Z");
    private static final Instant WINDOW_END = Instant.parse("2026-07-05T04:00:00Z");
    private static final String STUDENT_IDENTITY = "112487张三";
    private static final long LOOKBACK_HOURS = 120L;

    private final OjScheduledSubmissionCollectionService service = mock(OjScheduledSubmissionCollectionService.class);
    private final OjSubmissionCollectionJobService jobService =
            mock(OjSubmissionCollectionJobService.class);
    private final OjSubmissionCollectionController controller =
            new OjSubmissionCollectionController(service, jobService);

    @Test
    void collectsSubmissionsForRecentHours() throws Exception {
        when(service.collectRecentWindowForUsername(null, STUDENT_IDENTITY, Duration.ofHours(LOOKBACK_HOURS)))
                .thenReturn(result());

        var response = controller.collectSubmissions(new OjSubmissionCollectionRequest(
                STUDENT_IDENTITY,
                LOOKBACK_HOURS,
                null
        ));

        assertThat(response.ojName()).isEqualTo(OjNames.CODEFORCES);
        assertThat(response.status()).isEqualTo(OjSubmissionCollectionStatus.SUCCESS);
        assertThat(response.windowStartInclusive()).isEqualTo(WINDOW_START);
        assertThat(response.windowEndExclusive()).isEqualTo(WINDOW_END);
        assertThat(response.writtenRows()).isEqualTo(2);
        verify(service).collectRecentWindowForUsername(null, STUDENT_IDENTITY, Duration.ofHours(LOOKBACK_HOURS));
    }

    @Test
    void collectsRequestedOjName() throws Exception {
        when(service.collectRecentWindowForUsername(
                OjNames.CODEFORCES,
                STUDENT_IDENTITY,
                Duration.ofHours(LOOKBACK_HOURS)
        )).thenReturn(result());

        controller.collectSubmissions(new OjSubmissionCollectionRequest(
                STUDENT_IDENTITY,
                LOOKBACK_HOURS,
                OjNames.CODEFORCES
        ));

        verify(service).collectRecentWindowForUsername(
                OjNames.CODEFORCES,
                STUDENT_IDENTITY,
                Duration.ofHours(LOOKBACK_HOURS)
        );
    }

    @Test
    void rejectsEmptyRequestBody() {
        assertThatThrownBy(() -> controller.collectSubmissions(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("request body must not be empty");
    }

    @Test
    void rejectsNonPositiveLookbackHours() {
        assertThatThrownBy(() -> controller.collectSubmissions(
                new OjSubmissionCollectionRequest(STUDENT_IDENTITY, 0L, null)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("lookbackHours must be positive");
    }

    @Test
    void rejectsBlankUsername() {
        assertThatThrownBy(() -> controller.collectSubmissions(
                new OjSubmissionCollectionRequest(" ", LOOKBACK_HOURS, null)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username must not be blank");
    }

    @Test
    void startsCollectionJob() {
        var snapshot = jobSnapshot(OjSubmissionCollectionJobStatus.RUNNING);
        when(jobService.startBatchCollection(
                List.of(STUDENT_IDENTITY),
                Duration.ofHours(LOOKBACK_HOURS),
                true,
                OjNames.CODEFORCES
        ))
                .thenReturn(snapshot);

        var response = controller.startCollectionJob(new OjSubmissionCollectionJobStartRequest(
                List.of(STUDENT_IDENTITY),
                LOOKBACK_HOURS,
                true,
                OjNames.CODEFORCES
        ));

        assertThat(response.jobId()).isEqualTo("job-1");
        assertThat(response.ojName()).isEqualTo(OjNames.CODEFORCES);
        assertThat(response.status()).isEqualTo(OjSubmissionCollectionJobStatus.RUNNING);
        assertThat(response.items()).singleElement().satisfies(item ->
                assertThat(item.username()).isEqualTo(STUDENT_IDENTITY));
        verify(jobService).startBatchCollection(
                List.of(STUDENT_IDENTITY),
                Duration.ofHours(LOOKBACK_HOURS),
                true,
                OjNames.CODEFORCES
        );
    }

    @Test
    void listsCollectionJobs() {
        when(jobService.listJobs()).thenReturn(List.of(jobSnapshot(OjSubmissionCollectionJobStatus.SUCCESS)));

        var response = controller.listCollectionJobs();

        assertThat(response).singleElement().satisfies(job -> {
            assertThat(job.jobId()).isEqualTo("job-1");
            assertThat(job.status()).isEqualTo(OjSubmissionCollectionJobStatus.SUCCESS);
        });
        verify(jobService).listJobs();
    }

    @Test
    void mapsInvalidRequestToBadRequest() {
        OjSubmissionCollectionExceptionHandler handler =
                new OjSubmissionCollectionExceptionHandler();

        var response = handler.handleIllegalArgumentException(new IllegalArgumentException("bad lookbackHours"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(Map.of(
                "code", "OJ_SUBMISSION_COLLECTION_INVALID_REQUEST",
                "message", "bad lookbackHours"
        ));
    }

    private OjSubmissionCollectionResult result() {
        return new OjSubmissionCollectionResult(
                OjNames.CODEFORCES,
                OjSubmissionCollectionStatus.SUCCESS,
                WINDOW_START,
                WINDOW_END,
                1,
                1,
                0,
                2,
                2,
                "collector-codeforces-1",
                "ods_codeforces__submission",
                2,
                Instant.parse("2026-07-05T04:00:00Z"),
                null,
                List.of()
        );
    }

    private OjSubmissionCollectionJobSnapshot jobSnapshot(OjSubmissionCollectionJobStatus status) {
        return new OjSubmissionCollectionJobSnapshot(
                "job-1",
                OjNames.CODEFORCES,
                status,
                1,
                status == OjSubmissionCollectionJobStatus.RUNNING ? 0 : 1,
                status == OjSubmissionCollectionJobStatus.SUCCESS ? 1 : 0,
                0,
                0,
                0,
                List.of(),
                WINDOW_START,
                status == OjSubmissionCollectionJobStatus.RUNNING ? null : WINDOW_END,
                "job",
                List.of(OjSubmissionCollectionJobItem.pending(STUDENT_IDENTITY))
        );
    }
}
