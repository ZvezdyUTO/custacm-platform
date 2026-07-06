package com.custacm.platform.trainingdata.codeforces.web.purge;

import com.custacm.platform.trainingdata.codeforces.app.purge.CodeforcesStudentDataPurgeException;
import com.custacm.platform.trainingdata.codeforces.app.purge.CodeforcesStudentDataPurgeService;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesStudentDataPurgeResult;
import com.custacm.platform.trainingdata.codeforces.web.purge.response.CodeforcesStudentDataPurgeErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CodeforcesStudentDataPurgeControllerTest {
    private final CodeforcesStudentDataPurgeService service = mock(CodeforcesStudentDataPurgeService.class);
    private final CodeforcesStudentDataPurgeController controller = new CodeforcesStudentDataPurgeController(service);

    @Test
    void purgesStudentDataAndMapsCounts() {
        when(service.purgeStudentData("112487张三")).thenReturn(new CodeforcesStudentDataPurgeResult(
                "112487张三",
                "tourist",
                1,
                2,
                3,
                4,
                5
        ));

        var response = controller.purgeStudentData("112487张三");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().studentIdentity()).isEqualTo("112487张三");
        assertThat(response.getBody().handle()).isEqualTo("tourist");
        assertThat(response.getBody().handleAccountRows()).isEqualTo(1);
        assertThat(response.getBody().odsSubmissionRows()).isEqualTo(2);
        assertThat(response.getBody().dwdSubmissionRows()).isEqualTo(3);
        assertThat(response.getBody().dwmFirstAcceptedRows()).isEqualTo(4);
        assertThat(response.getBody().dwsAcceptedSummaryRows()).isEqualTo(5);
        assertThat(response.getBody().totalDeletedRows()).isEqualTo(15);
        verify(service).purgeStudentData("112487张三");
    }

    @Test
    void mapsInvalidRequestErrors() {
        CodeforcesStudentDataPurgeExceptionHandler handler = new CodeforcesStudentDataPurgeExceptionHandler();
        var response = handler.handlePurgeException(new CodeforcesStudentDataPurgeException(
                CodeforcesStudentDataPurgeException.ErrorCode.CODEFORCES_STUDENT_DATA_PURGE_INVALID_REQUEST,
                "studentIdentity must not be blank"
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new CodeforcesStudentDataPurgeErrorResponse(
                "CODEFORCES_STUDENT_DATA_PURGE_INVALID_REQUEST",
                "studentIdentity must not be blank"
        ));
    }
}
