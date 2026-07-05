package com.custacm.platform.trainingdata.codeforces.web;

import com.custacm.platform.trainingdata.codeforces.app.CodeforcesHandleAccountException;
import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesAcceptedSummaryReport;
import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesHandleFirstAcceptedProblemReport;
import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesHandleSubmissionReport;
import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesProblemFirstAcceptedHandleReport;
import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesProblemSubmissionReport;
import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesSubmissionItem;
import com.custacm.platform.trainingdata.codeforces.app.service.CodeforcesAcceptedSummaryQueryService;
import com.custacm.platform.trainingdata.codeforces.app.service.CodeforcesFirstAcceptedProblemQueryService;
import com.custacm.platform.trainingdata.codeforces.app.service.CodeforcesSubmissionQueryService;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesProblemFirstAcceptedHandleCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesProblemSubmissionCriteria;
import com.custacm.platform.trainingdata.codeforces.web.controller.CodeforcesWarehouseQueryController;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CodeforcesWarehouseQueryControllerTest {
    private final CodeforcesAcceptedSummaryQueryService acceptedSummaryQueryService =
            mock(CodeforcesAcceptedSummaryQueryService.class);
    private final CodeforcesSubmissionQueryService submissionQueryService = mock(CodeforcesSubmissionQueryService.class);
    private final CodeforcesFirstAcceptedProblemQueryService firstAcceptedProblemQueryService =
            mock(CodeforcesFirstAcceptedProblemQueryService.class);
    private final CodeforcesWarehouseQueryController controller = new CodeforcesWarehouseQueryController(
            acceptedSummaryQueryService,
            submissionQueryService,
            firstAcceptedProblemQueryService
    );

    @Test
    void summarizesAcceptedProblems() {
        LocalDate from = LocalDate.parse("2026-07-01");
        LocalDate to = LocalDate.parse("2026-07-03");
        when(acceptedSummaryQueryService.summarizeStudentAcceptedProblems(
                "112487张三",
                from,
                to,
                800,
                1600
        )).thenReturn(new CodeforcesAcceptedSummaryReport(
                "112487张三",
                "tourist",
                3,
                List.of(
                        new CodeforcesAcceptedSummaryReport.CodeforcesRatingAcceptedCount("800", 2),
                        new CodeforcesAcceptedSummaryReport.CodeforcesRatingAcceptedCount("1600", 1)
                )
        ));

        var response = controller.summarizeAcceptedProblems(
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
        verify(acceptedSummaryQueryService).summarizeStudentAcceptedProblems("112487张三", from, to, 800, 1600);
        verifyNoInteractions(submissionQueryService, firstAcceptedProblemQueryService);
    }

    @Test
    void listsStudentAndProblemSubmissions() {
        LocalDateTime from = LocalDateTime.parse("2026-07-01T00:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-07-01T23:59:59");
        CodeforcesSubmissionItem item = submissionItem(1L, "112487张三", "tourist");
        when(submissionQueryService.listStudentSubmissions("112487张三", from, to, 800, 1600))
                .thenReturn(new CodeforcesHandleSubmissionReport("112487张三", "tourist", List.of(item)));
        CodeforcesProblemSubmissionCriteria problemQuery =
                new CodeforcesProblemSubmissionCriteria("1000:A", from, to);
        when(submissionQueryService.listProblemSubmissions(problemQuery))
                .thenReturn(new CodeforcesProblemSubmissionReport("1000:A", List.of(item)));

        var studentResponse = controller.listStudentSubmissions(
                "112487张三",
                "2026-07-01T00:00:00",
                "2026-07-01T23:59:59",
                800,
                1600
        );
        var problemResponse = controller.listProblemSubmissions(
                " 1000:A ",
                "2026-07-01T00:00:00",
                "2026-07-01T23:59:59"
        );

        assertThat(studentResponse.getBody()).isNotNull();
        assertThat(studentResponse.getBody().studentIdentity()).isEqualTo("112487张三");
        assertThat(studentResponse.getBody().submissions()).hasSize(1);
        assertThat(studentResponse.getBody().submissions().getFirst().codeforcesSubmissionId()).isEqualTo(1L);
        assertThat(problemResponse.getBody()).isNotNull();
        assertThat(problemResponse.getBody().problemKey()).isEqualTo("1000:A");
        assertThat(problemResponse.getBody().submissions()).hasSize(1);
        verify(submissionQueryService).listStudentSubmissions("112487张三", from, to, 800, 1600);
        verify(submissionQueryService).listProblemSubmissions(problemQuery);
    }

    @Test
    void summarizesStudentAndProblemFirstAcceptedProblems() {
        LocalDateTime from = LocalDateTime.parse("2026-07-02T00:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-07-02T23:59:59");
        var problemItem = new CodeforcesHandleFirstAcceptedProblemReport.CodeforcesFirstAcceptedProblemItem(
                "1000:A",
                1000L,
                "A",
                "A problem",
                "PROGRAMMING",
                BigDecimal.ZERO,
                800,
                "[]",
                1L,
                LocalDateTime.parse("2026-07-02T10:00:00"),
                LocalDate.parse("2026-07-02"),
                "Java 21"
        );
        when(firstAcceptedProblemQueryService.summarizeStudentFirstAcceptedProblems(
                "112487张三",
                from,
                to,
                800,
                1600
        )).thenReturn(new CodeforcesHandleFirstAcceptedProblemReport(
                "112487张三",
                "tourist",
                1,
                List.of(problemItem)
        ));
        CodeforcesProblemFirstAcceptedHandleCriteria problemQuery =
                new CodeforcesProblemFirstAcceptedHandleCriteria("1000:A", from, to);
        when(firstAcceptedProblemQueryService.summarizeProblemFirstAcceptedHandles(problemQuery))
                .thenReturn(new CodeforcesProblemFirstAcceptedHandleReport(
                        "1000:A",
                        1,
                        List.of(new CodeforcesProblemFirstAcceptedHandleReport.CodeforcesFirstAcceptedHandle(
                                "112487张三",
                                "tourist",
                                LocalDateTime.parse("2026-07-02T10:00:00")
                        ))
                ));

        var studentResponse = controller.summarizeStudentFirstAcceptedProblems(
                "112487张三",
                "2026-07-02T00:00:00",
                "2026-07-02T23:59:59",
                800,
                1600
        );
        var problemResponse = controller.summarizeProblemFirstAcceptedHandles(
                "1000:A",
                "2026-07-02T00:00:00",
                "2026-07-02T23:59:59"
        );

        assertThat(studentResponse.getBody()).isNotNull();
        assertThat(studentResponse.getBody().studentIdentity()).isEqualTo("112487张三");
        assertThat(studentResponse.getBody().totalAcceptedProblemCount()).isEqualTo(1);
        assertThat(studentResponse.getBody().problems()).hasSize(1);
        assertThat(problemResponse.getBody()).isNotNull();
        assertThat(problemResponse.getBody().acceptedHandleCount()).isEqualTo(1);
        assertThat(problemResponse.getBody().acceptedHandles().getFirst().studentIdentity()).isEqualTo("112487张三");
        verify(firstAcceptedProblemQueryService)
                .summarizeStudentFirstAcceptedProblems("112487张三", from, to, 800, 1600);
        verify(firstAcceptedProblemQueryService).summarizeProblemFirstAcceptedHandles(problemQuery);
    }

    @Test
    void rejectsBlankRequiredFieldsAndInvalidDateTimes() {
        assertThatThrownBy(() -> controller.listStudentSubmissions(" ", null, null, null, null))
                .isInstanceOfSatisfying(CodeforcesHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_INVALID_REQUEST
                        ));
        assertThatThrownBy(() -> controller.listProblemSubmissions(" ", null, null))
                .isInstanceOfSatisfying(CodeforcesHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_INVALID_REQUEST
                        ));
        assertThatThrownBy(() -> controller.summarizeAcceptedProblems(
                "112487张三",
                "not-a-date",
                null,
                null,
                null
        )).isInstanceOfSatisfying(CodeforcesHandleAccountException.class, ex ->
                assertThat(ex.errorCode()).isEqualTo(
                        CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_INVALID_REQUEST
                ));
        assertThatThrownBy(() -> controller.summarizeStudentFirstAcceptedProblems(
                "112487张三",
                "not-a-date-time",
                null,
                null,
                null
        )).isInstanceOfSatisfying(CodeforcesHandleAccountException.class, ex ->
                assertThat(ex.errorCode()).isEqualTo(
                        CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_INVALID_REQUEST
                ));
        verifyNoInteractions(acceptedSummaryQueryService, submissionQueryService, firstAcceptedProblemQueryService);
    }

    private static CodeforcesSubmissionItem submissionItem(
            long codeforcesSubmissionId,
            String studentIdentity,
            String authorHandle
    ) {
        return new CodeforcesSubmissionItem(
                codeforcesSubmissionId,
                studentIdentity,
                authorHandle,
                1000L,
                LocalDateTime.parse("2026-07-01T12:00:00"),
                LocalDate.parse("2026-07-01"),
                0,
                "1000:A",
                1000L,
                "A",
                "A problem",
                "PROGRAMMING",
                BigDecimal.ZERO,
                800,
                "[]",
                "PRACTICE",
                "Java 21",
                "OK",
                true,
                "TESTS",
                1,
                100,
                1024L
        );
    }
}
