package com.custacm.platform.trainingdata.common.web.purge;

import com.custacm.platform.trainingdata.common.app.purge.OjStudentDataPurgeException;
import com.custacm.platform.trainingdata.common.app.purge.OjStudentDataPurgeService;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjStudentDataPurgeResult;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import com.custacm.platform.trainingdata.common.web.purge.response.OjStudentDataPurgeErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OjStudentDataPurgeControllerTest {
    private final OjStudentDataPurgeService service = mock(OjStudentDataPurgeService.class);
    private final OjStudentDataPurgeController controller = new OjStudentDataPurgeController(service);

    @Test
    void purgesStudentDataAndMapsCounts() {
        when(service.purgeStudentData("112487张三", OjNames.CODEFORCES)).thenReturn(new OjStudentDataPurgeResult(
                "112487张三",
                OjNames.CODEFORCES,
                "tourist",
                Map.of(OjNames.CODEFORCES, "tourist"),
                List.of(new OjStudentDataPurgeResult.OjDataPurgeResult(
                        OjNames.CODEFORCES,
                        "tourist",
                        2,
                        3,
                        4,
                        5
                )),
                0,
                2,
                3,
                4,
                5
        ));

        var response = controller.purgeStudentData("112487张三", OjNames.CODEFORCES);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().username()).isEqualTo("112487张三");
        assertThat(response.getBody().ojName()).isEqualTo(OjNames.CODEFORCES);
        assertThat(response.getBody().handle()).isEqualTo("tourist");
        assertThat(response.getBody().handles()).containsEntry(OjNames.CODEFORCES, "tourist");
        assertThat(response.getBody().ojResults()).hasSize(1);
        assertThat(response.getBody().handleAccountRows()).isZero();
        assertThat(response.getBody().odsSubmissionRows()).isEqualTo(2);
        assertThat(response.getBody().dwdSubmissionRows()).isEqualTo(3);
        assertThat(response.getBody().dwmFirstAcceptedRows()).isEqualTo(4);
        assertThat(response.getBody().dwsAcceptedSummaryRows()).isEqualTo(5);
        assertThat(response.getBody().totalDeletedRows()).isEqualTo(14);
        verify(service).purgeStudentData("112487张三", OjNames.CODEFORCES);
    }

    @Test
    void passesRequestedOjNameToService() {
        when(service.purgeStudentData("112487张三", OjNames.ATCODER)).thenReturn(new OjStudentDataPurgeResult(
                "112487张三",
                OjNames.ATCODER,
                "tourist_atcoder",
                Map.of(OjNames.ATCODER, "tourist_atcoder"),
                List.of(),
                0,
                0,
                0,
                0,
                0
        ));

        var response = controller.purgeStudentData("112487张三", OjNames.ATCODER);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().ojName()).isEqualTo(OjNames.ATCODER);
        assertThat(response.getBody().handle()).isEqualTo("tourist_atcoder");
        verify(service).purgeStudentData("112487张三", OjNames.ATCODER);
    }

    @Test
    void mapsInvalidRequestErrors() {
        OjStudentDataPurgeExceptionHandler handler = new OjStudentDataPurgeExceptionHandler();
        var response = handler.handlePurgeException(new OjStudentDataPurgeException(
                OjStudentDataPurgeException.ErrorCode.OJ_STUDENT_DATA_PURGE_INVALID_REQUEST,
                "username must not be blank"
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new OjStudentDataPurgeErrorResponse(
                "OJ_STUDENT_DATA_PURGE_INVALID_REQUEST",
                "username must not be blank"
        ));
    }
}
