package com.custacm.platform.trainingdata.common.app.query;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountException;
import com.custacm.platform.trainingdata.common.app.query.result.OjAcceptedSummaryReport;
import com.custacm.platform.trainingdata.common.app.query.result.OjHandleFirstAcceptedProblemReport;
import com.custacm.platform.trainingdata.common.app.query.result.OjHandleSubmissionReport;
import com.custacm.platform.trainingdata.common.app.query.result.OjProblemFirstAcceptedHandleReport;
import com.custacm.platform.trainingdata.common.app.query.result.OjProblemSubmissionReport;
import com.custacm.platform.trainingdata.common.app.query.result.OjSubmissionItem;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjProblemFirstAcceptedHandleCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjProblemSubmissionCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OjWarehouseQueryFacadeTest {
    private final OjAcceptedSummaryQueryService acceptedSummaryQueryService =
            mock(OjAcceptedSummaryQueryService.class);
    private final OjSubmissionQueryService submissionQueryService = mock(OjSubmissionQueryService.class);
    private final OjFirstAcceptedProblemQueryService firstAcceptedProblemQueryService =
            mock(OjFirstAcceptedProblemQueryService.class);
    private final OjWarehouseQueryFacade facade = new OjWarehouseQueryFacade(
            acceptedSummaryQueryService,
            submissionQueryService,
            firstAcceptedProblemQueryService
    );

    @Test
    void summarizesAcceptedProblemsAfterNormalizingInputs() {
        LocalDate from = LocalDate.parse("2026-07-01");
        LocalDate to = LocalDate.parse("2026-07-03");
        OjAcceptedSummaryReport report = new OjAcceptedSummaryReport(
                "112487张三", "tourist", 3,
                List.of(new OjAcceptedSummaryReport.OjRatingAcceptedCount("800", 3))
        );
        when(acceptedSummaryQueryService.summarizeStudentAcceptedProblems(
                OjNames.CODEFORCES, "112487张三", from, to, 800, 1600
        )).thenReturn(report);

        assertThat(facade.summarizeAcceptedProblems(
                " CODEFORCES ", " 112487张三 ", " 2026-07-01 ", "2026-07-03", 800, 1600
        )).isSameAs(report);
        verify(acceptedSummaryQueryService)
                .summarizeStudentAcceptedProblems(OjNames.CODEFORCES, "112487张三", from, to, 800, 1600);
        verifyNoInteractions(submissionQueryService, firstAcceptedProblemQueryService);
    }

    @Test
    void listsStudentAndProblemSubmissionsWithRequestedPagination() {
        LocalDateTime from = LocalDateTime.parse("2026-07-01T00:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-07-01T23:59:59");
        OjHandleSubmissionReport studentReport = new OjHandleSubmissionReport(
                "112487张三", "tourist", 2, 50, 51, 2, false, List.of(submissionItem())
        );
        OjProblemSubmissionReport problemReport = new OjProblemSubmissionReport(
                "1000:A", 2, 50, 51, 2, false, List.of(submissionItem())
        );
        when(submissionQueryService.listStudentSubmissions(
                OjNames.CODEFORCES, "112487张三", from, to, 800, 1600, 2, 50
        )).thenReturn(studentReport);
        OjProblemSubmissionCriteria criteria = new OjProblemSubmissionCriteria(
                OjNames.CODEFORCES, "1000:A", from, to, 50, 50
        );
        when(submissionQueryService.listProblemSubmissions(criteria)).thenReturn(problemReport);

        assertThat(facade.listStudentSubmissions(
                OjNames.CODEFORCES, "112487张三",
                "2026-07-01T00:00:00", "2026-07-01T23:59:59",
                800, 1600, 2, 50
        )).isSameAs(studentReport);
        assertThat(facade.listProblemSubmissions(
                OjNames.CODEFORCES, " 1000:A ",
                "2026-07-01T00:00:00", "2026-07-01T23:59:59", 2, 50
        )).isSameAs(problemReport);
        verify(submissionQueryService)
                .listStudentSubmissions(OjNames.CODEFORCES, "112487张三", from, to, 800, 1600, 2, 50);
        verify(submissionQueryService).listProblemSubmissions(criteria);
    }

    @Test
    void appliesDefaultPagination() {
        OjHandleSubmissionReport studentReport = new OjHandleSubmissionReport(
                "112487张三", "tourist", 1, 15, 0, 0, false, List.of()
        );
        OjProblemSubmissionReport problemReport = new OjProblemSubmissionReport(
                "1000:A", 1, 15, 0, 0, false, List.of()
        );
        when(submissionQueryService.listStudentSubmissions(
                OjNames.CODEFORCES, "112487张三", null, null, null, null, 1, 15
        )).thenReturn(studentReport);
        OjProblemSubmissionCriteria criteria = new OjProblemSubmissionCriteria(
                OjNames.CODEFORCES, "1000:A", null, null, 15, 0
        );
        when(submissionQueryService.listProblemSubmissions(criteria)).thenReturn(problemReport);

        assertThat(facade.listStudentSubmissions(
                OjNames.CODEFORCES, "112487张三", null, " ", null, null, null, null
        )).isSameAs(studentReport);
        assertThat(facade.listProblemSubmissions(
                OjNames.CODEFORCES, "1000:A", null, " ", null, null
        )).isSameAs(problemReport);
    }

    @Test
    void summarizesFirstAcceptedProblemsForStudentAndProblem() {
        LocalDateTime from = LocalDateTime.parse("2026-07-02T00:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-07-02T23:59:59");
        OjHandleFirstAcceptedProblemReport studentReport = new OjHandleFirstAcceptedProblemReport(
                "112487张三", "tourist", 1, 2, 50, 51, 2, false, List.of()
        );
        OjProblemFirstAcceptedHandleReport problemReport = new OjProblemFirstAcceptedHandleReport(
                "1000:A", 1, 2, 50, 51, 2, false, List.of()
        );
        when(firstAcceptedProblemQueryService.summarizeStudentFirstAcceptedProblems(
                OjNames.CODEFORCES, "112487张三", from, to, 800, 1600, 2, 50
        )).thenReturn(studentReport);
        OjProblemFirstAcceptedHandleCriteria criteria = new OjProblemFirstAcceptedHandleCriteria(
                OjNames.CODEFORCES, "1000:A", from, to, 50, 50
        );
        when(firstAcceptedProblemQueryService.summarizeProblemFirstAcceptedHandles(criteria))
                .thenReturn(problemReport);

        assertThat(facade.summarizeStudentFirstAcceptedProblems(
                OjNames.CODEFORCES, "112487张三",
                "2026-07-02T00:00:00", "2026-07-02T23:59:59",
                800, 1600, 2, 50
        )).isSameAs(studentReport);
        assertThat(facade.summarizeProblemFirstAcceptedHandles(
                OjNames.CODEFORCES, "1000:A",
                "2026-07-02T00:00:00", "2026-07-02T23:59:59", 2, 50
        )).isSameAs(problemReport);
        verify(firstAcceptedProblemQueryService)
                .summarizeStudentFirstAcceptedProblems(OjNames.CODEFORCES, "112487张三", from, to, 800, 1600, 2, 50);
        verify(firstAcceptedProblemQueryService).summarizeProblemFirstAcceptedHandles(criteria);
    }

    @Test
    void rejectsBlankRequiredFieldsAndInvalidDatesBeforeCallingServices() {
        assertInvalidRequest(() -> facade.listStudentSubmissions(
                OjNames.CODEFORCES, " ", null, null, null, null, null, null
        ));
        assertInvalidRequest(() -> facade.listProblemSubmissions(
                OjNames.CODEFORCES, " ", null, null, null, null
        ));
        assertInvalidRequest(() -> facade.summarizeAcceptedProblems(
                OjNames.CODEFORCES, "112487张三", "not-a-date", null, null, null
        ));
        assertInvalidRequest(() -> facade.summarizeStudentFirstAcceptedProblems(
                OjNames.CODEFORCES, "112487张三", "not-a-date-time", null, null, null, null, null
        ));
        assertInvalidRequest(() -> facade.summarizeProblemFirstAcceptedHandles(
                OjNames.CODEFORCES, "1000:A", null, "not-a-date-time", null, null
        ));
        verifyNoInteractions(acceptedSummaryQueryService, submissionQueryService, firstAcceptedProblemQueryService);
    }

    @Test
    void rejectsInvalidPaginationBeforeCallingServices() {
        assertInvalidRequest(() -> facade.listStudentSubmissions(
                OjNames.CODEFORCES, "112487张三", null, null, null, null, 0, 100
        ));
        assertInvalidRequest(() -> facade.listProblemSubmissions(
                OjNames.CODEFORCES, "1000:A", null, null, 1, 0
        ));
        assertInvalidRequest(() -> facade.listProblemSubmissions(
                OjNames.CODEFORCES, "1000:A", null, null, 1, 2001
        ));
        assertInvalidRequest(() -> facade.listProblemSubmissions(
                " ", "1000:A", null, null, 1, 100
        ));
        assertInvalidRequest(() -> facade.summarizeStudentFirstAcceptedProblems(
                OjNames.CODEFORCES, "112487张三", null, null, null, null, 0, 100
        ));
        assertInvalidRequest(() -> facade.summarizeProblemFirstAcceptedHandles(
                OjNames.CODEFORCES, "1000:A", null, null, 1, 2001
        ));
        verifyNoInteractions(acceptedSummaryQueryService, submissionQueryService, firstAcceptedProblemQueryService);
    }

    private static void assertInvalidRequest(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(OjHandleAccountException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(
                                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_INVALID_REQUEST
                        ));
    }

    private static OjSubmissionItem submissionItem() {
        return new OjSubmissionItem(
                "1", "112487张三", "tourist",
                LocalDateTime.parse("2026-07-01T12:00:00"), LocalDate.parse("2026-07-01"),
                "1000:A", "A", "A problem", "800", "Java 21", "OK", true, 100,
                "https://codeforces.com/contest/1000/submission/1"
        );
    }
}
