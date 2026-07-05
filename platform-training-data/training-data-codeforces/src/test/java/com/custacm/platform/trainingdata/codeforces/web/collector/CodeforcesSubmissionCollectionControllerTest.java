package com.custacm.platform.trainingdata.codeforces.web.collector;

import com.custacm.platform.trainingdata.codeforces.app.collector.result.CodeforcesSubmissionCollectionResult;
import com.custacm.platform.trainingdata.codeforces.app.collector.result.CodeforcesSubmissionCollectionStatus;
import com.custacm.platform.trainingdata.codeforces.app.collector.CodeforcesSubmissionCollectionService;
import com.custacm.platform.trainingdata.codeforces.web.collector.request.CodeforcesSubmissionCollectionRequest;
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

class CodeforcesSubmissionCollectionControllerTest {
    private static final Instant WINDOW_START = Instant.parse("2026-06-30T04:00:00Z");
    private static final Instant WINDOW_END = Instant.parse("2026-07-05T04:00:00Z");
    private static final String STUDENT_IDENTITY = "112487张三";
    private static final long LOOKBACK_HOURS = 120L;

    private final CodeforcesSubmissionCollectionService service = mock(CodeforcesSubmissionCollectionService.class);
    private final CodeforcesSubmissionCollectionController controller =
            new CodeforcesSubmissionCollectionController(service);

    @Test
    void collectsSubmissionsForRecentHours() throws Exception {
        when(service.collectRecentWindowForStudentIdentity(STUDENT_IDENTITY, Duration.ofHours(LOOKBACK_HOURS)))
                .thenReturn(result());

        var response = controller.collectSubmissions(new CodeforcesSubmissionCollectionRequest(
                STUDENT_IDENTITY,
                LOOKBACK_HOURS
        ));

        assertThat(response.status()).isEqualTo(CodeforcesSubmissionCollectionStatus.SUCCESS);
        assertThat(response.windowStartInclusive()).isEqualTo(WINDOW_START);
        assertThat(response.windowEndExclusive()).isEqualTo(WINDOW_END);
        assertThat(response.writtenRows()).isEqualTo(2);
        verify(service).collectRecentWindowForStudentIdentity(STUDENT_IDENTITY, Duration.ofHours(LOOKBACK_HOURS));
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
                new CodeforcesSubmissionCollectionRequest(STUDENT_IDENTITY, 0L)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("lookbackHours must be positive");
    }

    @Test
    void rejectsBlankStudentIdentity() {
        assertThatThrownBy(() -> controller.collectSubmissions(
                new CodeforcesSubmissionCollectionRequest(" ", LOOKBACK_HOURS)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("studentIdentity must not be blank");
    }

    @Test
    void mapsInvalidRequestToBadRequest() {
        CodeforcesSubmissionCollectionExceptionHandler handler =
                new CodeforcesSubmissionCollectionExceptionHandler();

        var response = handler.handleIllegalArgumentException(new IllegalArgumentException("bad lookbackHours"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(Map.of(
                "code", "CODEFORCES_SUBMISSION_COLLECTION_INVALID_REQUEST",
                "message", "bad lookbackHours"
        ));
    }

    private CodeforcesSubmissionCollectionResult result() {
        return new CodeforcesSubmissionCollectionResult(
                CodeforcesSubmissionCollectionStatus.SUCCESS,
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
}
