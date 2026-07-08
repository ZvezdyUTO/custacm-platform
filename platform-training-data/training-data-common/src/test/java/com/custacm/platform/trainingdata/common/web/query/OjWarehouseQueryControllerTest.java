package com.custacm.platform.trainingdata.common.web.query;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountException;
import com.custacm.platform.trainingdata.common.app.query.result.OjAcceptedSummaryReport;
import com.custacm.platform.trainingdata.common.app.query.result.OjHandleFirstAcceptedProblemReport;
import com.custacm.platform.trainingdata.common.app.query.result.OjHandleSubmissionReport;
import com.custacm.platform.trainingdata.common.app.query.result.OjProblemFirstAcceptedHandleReport;
import com.custacm.platform.trainingdata.common.app.query.result.OjProblemSubmissionReport;
import com.custacm.platform.trainingdata.common.app.query.result.OjSubmissionItem;
import com.custacm.platform.trainingdata.common.app.query.OjAcceptedSummaryQueryService;
import com.custacm.platform.trainingdata.common.app.query.OjFirstAcceptedProblemQueryService;
import com.custacm.platform.trainingdata.common.app.query.OjSubmissionQueryService;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjProblemFirstAcceptedHandleCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjProblemSubmissionCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OjWarehouseQueryControllerTest {
    private final OjAcceptedSummaryQueryService acceptedSummaryQueryService =
            mock(OjAcceptedSummaryQueryService.class);
    private final OjSubmissionQueryService submissionQueryService = mock(OjSubmissionQueryService.class);
    private final OjFirstAcceptedProblemQueryService firstAcceptedProblemQueryService =
            mock(OjFirstAcceptedProblemQueryService.class);
    private final OjWarehouseQueryController controller = new OjWarehouseQueryController(
            acceptedSummaryQueryService,
            submissionQueryService,
            firstAcceptedProblemQueryService
    );

    @Test
    void summarizesAcceptedProblems() {
        LocalDate from = LocalDate.parse("2026-07-01");
        LocalDate to = LocalDate.parse("2026-07-03");
        when(acceptedSummaryQueryService.summarizeStudentAcceptedProblems(
                OjNames.CODEFORCES,
                "112487张三",
                from,
                to,
                800,
                1600
        )).thenReturn(new OjAcceptedSummaryReport(
                "112487张三",
                "tourist",
                3,
                List.of(
                        new OjAcceptedSummaryReport.OjRatingAcceptedCount("800", 2),
                        new OjAcceptedSummaryReport.OjRatingAcceptedCount("1600", 1)
                )
        ));

        var response = controller.summarizeAcceptedProblems(
                " CODEFORCES ",
                " 112487张三 ",
                " 2026-07-01 ",
                "2026-07-03",
                800,
                1600
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().studentIdentity()).isEqualTo("112487张三");
        assertThat(response.getBody().authorHandle()).isEqualTo("tourist");
        assertThat(response.getBody().totalAcceptedProblemCount()).isEqualTo(3);
        assertThat(response.getBody().ratingCounts())
                .extracting("problemRating", "acceptedProblemCount")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("800", 2),
                        org.assertj.core.groups.Tuple.tuple("1600", 1)
                );
        verify(acceptedSummaryQueryService)
                .summarizeStudentAcceptedProblems(OjNames.CODEFORCES, "112487张三", from, to, 800, 1600);
        verifyNoInteractions(submissionQueryService, firstAcceptedProblemQueryService);
    }

    @Test
    void doesNotExposeAutoCollectAcceptedSummaryList() {
        List<String> getMappings = Arrays.stream(OjWarehouseQueryController.class.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getAnnotationsByType(GetMapping.class)))
                .flatMap(mapping -> Arrays.stream(mapping.value()))
                .toList();

        assertThat(getMappings).doesNotContain("/accepted-summary/auto-collect-users");
    }

    @Test
    void listsStudentAndProblemSubmissions() {
        LocalDateTime from = LocalDateTime.parse("2026-07-01T00:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-07-01T23:59:59");
        OjSubmissionItem item = submissionItem(1L, "112487张三", "tourist");
        when(submissionQueryService.listStudentSubmissions(
                OjNames.CODEFORCES,
                "112487张三",
                from,
                to,
                800,
                1600,
                2,
                50
        ))
                .thenReturn(new OjHandleSubmissionReport(
                        "112487张三",
                        "tourist",
                        2,
                        50,
                        51,
                        2,
                        false,
                        List.of(item)
                ));
        OjProblemSubmissionCriteria problemQuery =
                new OjProblemSubmissionCriteria("1000:A", from, to, 50, 50);
        when(submissionQueryService.listProblemSubmissions(problemQuery))
                .thenReturn(new OjProblemSubmissionReport(
                        "1000:A",
                        2,
                        50,
                        51,
                        2,
                        false,
                        List.of(item)
                ));

        var studentResponse = controller.listStudentSubmissions(
                OjNames.CODEFORCES,
                "112487张三",
                "2026-07-01T00:00:00",
                "2026-07-01T23:59:59",
                800,
                1600,
                2,
                50
        );
        var problemResponse = controller.listProblemSubmissions(
                OjNames.CODEFORCES,
                " 1000:A ",
                "2026-07-01T00:00:00",
                "2026-07-01T23:59:59",
                2,
                50
        );

        assertThat(studentResponse.getBody()).isNotNull();
        assertThat(studentResponse.getBody().studentIdentity()).isEqualTo("112487张三");
        assertThat(studentResponse.getBody().page()).isEqualTo(2);
        assertThat(studentResponse.getBody().limit()).isEqualTo(50);
        assertThat(studentResponse.getBody().total()).isEqualTo(51);
        assertThat(studentResponse.getBody().totalPages()).isEqualTo(2);
        assertThat(studentResponse.getBody().hasMore()).isFalse();
        assertThat(studentResponse.getBody().submissions()).hasSize(1);
        assertThat(studentResponse.getBody().submissions().getFirst().submissionId()).isEqualTo("1");
        assertThat(problemResponse.getBody()).isNotNull();
        assertThat(problemResponse.getBody().problemKey()).isEqualTo("1000:A");
        assertThat(problemResponse.getBody().page()).isEqualTo(2);
        assertThat(problemResponse.getBody().limit()).isEqualTo(50);
        assertThat(problemResponse.getBody().total()).isEqualTo(51);
        assertThat(problemResponse.getBody().totalPages()).isEqualTo(2);
        assertThat(problemResponse.getBody().hasMore()).isFalse();
        assertThat(problemResponse.getBody().submissions()).hasSize(1);
        verify(submissionQueryService)
                .listStudentSubmissions(OjNames.CODEFORCES, "112487张三", from, to, 800, 1600, 2, 50);
        verify(submissionQueryService).listProblemSubmissions(problemQuery);
    }

    @Test
    void listsSubmissionsWithDefaultPagination() {
        OjSubmissionItem item = submissionItem(1L, "112487张三", "tourist");
        when(submissionQueryService.listStudentSubmissions(
                OjNames.CODEFORCES,
                "112487张三",
                null,
                null,
                null,
                null,
                1,
                15
        ))
                .thenReturn(new OjHandleSubmissionReport(
                        "112487张三",
                        "tourist",
                        1,
                        15,
                        1,
                        1,
                        false,
                        List.of(item)
                ));
        OjProblemSubmissionCriteria problemQuery =
                new OjProblemSubmissionCriteria("1000:A", null, null, 15, 0);
        when(submissionQueryService.listProblemSubmissions(problemQuery))
                .thenReturn(new OjProblemSubmissionReport(
                        "1000:A",
                        1,
                        15,
                        1,
                        1,
                        false,
                        List.of(item)
                ));

        var studentResponse = controller.listStudentSubmissions(
                OjNames.CODEFORCES,
                "112487张三",
                null,
                null,
                null,
                null,
                null,
                null
        );
        var problemResponse = controller.listProblemSubmissions(OjNames.CODEFORCES, "1000:A", null, null, null, null);

        assertThat(studentResponse.getBody()).isNotNull();
        assertThat(studentResponse.getBody().page()).isEqualTo(1);
        assertThat(studentResponse.getBody().limit()).isEqualTo(15);
        assertThat(problemResponse.getBody()).isNotNull();
        assertThat(problemResponse.getBody().page()).isEqualTo(1);
        assertThat(problemResponse.getBody().limit()).isEqualTo(15);
        verify(submissionQueryService)
                .listStudentSubmissions(OjNames.CODEFORCES, "112487张三", null, null, null, null, 1, 15);
        verify(submissionQueryService).listProblemSubmissions(problemQuery);
    }

    @Test
    void summarizesStudentAndProblemFirstAcceptedProblems() {
        LocalDateTime from = LocalDateTime.parse("2026-07-02T00:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-07-02T23:59:59");
        var problemItem = new OjHandleFirstAcceptedProblemReport.OjFirstAcceptedProblemItem(
                "1000:A",
                "A",
                "A problem",
                "800",
                "1",
                LocalDateTime.parse("2026-07-02T10:00:00"),
                LocalDate.parse("2026-07-02"),
                "Java 21",
                "https://codeforces.com/contest/1000/submission/1"
        );
        when(firstAcceptedProblemQueryService.summarizeStudentFirstAcceptedProblems(
                OjNames.CODEFORCES,
                "112487张三",
                from,
                to,
                800,
                1600,
                2,
                50
        )).thenReturn(new OjHandleFirstAcceptedProblemReport(
                "112487张三",
                "tourist",
                1,
                2,
                50,
                51,
                2,
                false,
                List.of(problemItem)
        ));
        OjProblemFirstAcceptedHandleCriteria problemQuery =
                new OjProblemFirstAcceptedHandleCriteria("1000:A", from, to, 50, 50);
        when(firstAcceptedProblemQueryService.summarizeProblemFirstAcceptedHandles(problemQuery))
                .thenReturn(new OjProblemFirstAcceptedHandleReport(
                        "1000:A",
                        1,
                        2,
                        50,
                        51,
                        2,
                        false,
                        List.of(new OjProblemFirstAcceptedHandleReport.OjFirstAcceptedHandle(
                                "112487张三",
                                "tourist",
                                LocalDateTime.parse("2026-07-02T10:00:00")
                        ))
                ));

        var studentResponse = controller.summarizeStudentFirstAcceptedProblems(
                OjNames.CODEFORCES,
                "112487张三",
                "2026-07-02T00:00:00",
                "2026-07-02T23:59:59",
                800,
                1600,
                2,
                50
        );
        var problemResponse = controller.summarizeProblemFirstAcceptedHandles(
                OjNames.CODEFORCES,
                "1000:A",
                "2026-07-02T00:00:00",
                "2026-07-02T23:59:59",
                2,
                50
        );

        assertThat(studentResponse.getBody()).isNotNull();
        assertThat(studentResponse.getBody().studentIdentity()).isEqualTo("112487张三");
        assertThat(studentResponse.getBody().totalAcceptedProblemCount()).isEqualTo(1);
        assertThat(studentResponse.getBody().page()).isEqualTo(2);
        assertThat(studentResponse.getBody().limit()).isEqualTo(50);
        assertThat(studentResponse.getBody().total()).isEqualTo(51);
        assertThat(studentResponse.getBody().totalPages()).isEqualTo(2);
        assertThat(studentResponse.getBody().hasMore()).isFalse();
        assertThat(studentResponse.getBody().problems()).hasSize(1);
        assertThat(problemResponse.getBody()).isNotNull();
        assertThat(problemResponse.getBody().acceptedHandleCount()).isEqualTo(1);
        assertThat(problemResponse.getBody().page()).isEqualTo(2);
        assertThat(problemResponse.getBody().limit()).isEqualTo(50);
        assertThat(problemResponse.getBody().total()).isEqualTo(51);
        assertThat(problemResponse.getBody().totalPages()).isEqualTo(2);
        assertThat(problemResponse.getBody().hasMore()).isFalse();
        assertThat(problemResponse.getBody().acceptedHandles().getFirst().studentIdentity()).isEqualTo("112487张三");
        verify(firstAcceptedProblemQueryService)
                .summarizeStudentFirstAcceptedProblems(OjNames.CODEFORCES, "112487张三", from, to, 800, 1600, 2, 50);
        verify(firstAcceptedProblemQueryService).summarizeProblemFirstAcceptedHandles(problemQuery);
    }

    @Test
    void rejectsBlankRequiredFieldsAndInvalidDateTimes() {
        assertThatThrownBy(() -> controller.listStudentSubmissions(
                OjNames.CODEFORCES,
                " ",
                null,
                null,
                null,
                null,
                null,
                null
        ))
                .isInstanceOfSatisfying(OjHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_INVALID_REQUEST
                        ));
        assertThatThrownBy(() -> controller.listProblemSubmissions(OjNames.CODEFORCES, " ", null, null, null, null))
                .isInstanceOfSatisfying(OjHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_INVALID_REQUEST
                        ));
        assertThatThrownBy(() -> controller.summarizeAcceptedProblems(
                OjNames.CODEFORCES,
                "112487张三",
                "not-a-date",
                null,
                null,
                null
        )).isInstanceOfSatisfying(OjHandleAccountException.class, ex ->
                assertThat(ex.errorCode()).isEqualTo(
                        OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_INVALID_REQUEST
                ));
        assertThatThrownBy(() -> controller.summarizeStudentFirstAcceptedProblems(
                OjNames.CODEFORCES,
                "112487张三",
                "not-a-date-time",
                null,
                null,
                null,
                null,
                null
        )).isInstanceOfSatisfying(OjHandleAccountException.class, ex ->
                assertThat(ex.errorCode()).isEqualTo(
                        OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_INVALID_REQUEST
                ));
        verifyNoInteractions(acceptedSummaryQueryService, submissionQueryService, firstAcceptedProblemQueryService);
    }

    @Test
    void rejectsInvalidSubmissionPaginationParameters() {
        assertInvalidRequest(() -> controller.listStudentSubmissions(
                OjNames.CODEFORCES,
                "112487张三",
                null,
                null,
                null,
                null,
                0,
                100
        ));
        assertInvalidRequest(() -> controller.listProblemSubmissions(OjNames.CODEFORCES, "1000:A", null, null, 1, 0));
        assertInvalidRequest(() -> controller.listProblemSubmissions(OjNames.CODEFORCES, "1000:A", null, null, 1, 2001));
        assertInvalidRequest(() -> controller.listProblemSubmissions(" ", "1000:A", null, null, 1, 100));
        assertInvalidRequest(() -> controller.summarizeStudentFirstAcceptedProblems(
                OjNames.CODEFORCES,
                "112487张三",
                null,
                null,
                null,
                null,
                0,
                100
        ));
        assertInvalidRequest(() -> controller.summarizeProblemFirstAcceptedHandles(
                OjNames.CODEFORCES,
                "1000:A",
                null,
                null,
                1,
                2001
        ));
        verifyNoInteractions(acceptedSummaryQueryService, submissionQueryService, firstAcceptedProblemQueryService);
    }

    private static void assertInvalidRequest(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(OjHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_INVALID_REQUEST
                        ));
    }

    private static OjSubmissionItem submissionItem(
            long codeforcesSubmissionId,
            String studentIdentity,
            String authorHandle
    ) {
        return new OjSubmissionItem(
                String.valueOf(codeforcesSubmissionId),
                studentIdentity,
                authorHandle,
                LocalDateTime.parse("2026-07-01T12:00:00"),
                LocalDate.parse("2026-07-01"),
                "1000:A",
                "A",
                "A problem",
                "800",
                "Java 21",
                "OK",
                true,
                100,
                "https://codeforces.com/contest/1000/submission/" + codeforcesSubmissionId
        );
    }
}
