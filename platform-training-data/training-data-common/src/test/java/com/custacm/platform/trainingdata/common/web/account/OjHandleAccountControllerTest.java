package com.custacm.platform.trainingdata.common.web.account;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountException;
import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import com.custacm.platform.trainingdata.common.web.account.request.ChangeOjHandleIdentityRequest;
import com.custacm.platform.trainingdata.common.web.account.request.CreateOjHandleAccountRequest;
import com.custacm.platform.trainingdata.common.web.account.response.OjHandleAccountErrorResponse;
import com.custacm.platform.trainingdata.common.web.account.response.OjHandleAccountResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OjHandleAccountControllerTest {
    private static final Instant NOW = Instant.parse("2026-07-05T00:00:00Z");

    private final OjHandleAccountService service = mock(OjHandleAccountService.class);
    private final OjHandleAccountController controller = new OjHandleAccountController(service);

    @Test
    void createsQueriesAndChangesIdentity() {
        Map<String, String> handles = Map.of(OjNames.CODEFORCES, "tourist", OjNames.ATCODER, "tourist_atcoder");
        when(service.create("112487张三", handles))
                .thenReturn(account("112487张三", handles, true));
        when(service.changeStudentIdentity("112487张三", "112488张三", false, handles))
                .thenReturn(account("112488张三", handles, false));
        when(service.listAll()).thenReturn(List.of(
                account("112487张三", handles, true),
                account("112488李四", Map.of(OjNames.CODEFORCES, "Benq"), true)
        ));

        var created = controller.create(new CreateOjHandleAccountRequest(
                " 112487张三 ",
                handles
        ));
        var changed = controller.changeStudentIdentity(new ChangeOjHandleIdentityRequest(
                " 112487张三 ",
                " 112488张三 ",
                false,
                handles
        ));

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().studentIdentity()).isEqualTo("112487张三");
        assertThat(created.getBody().handles()).containsEntry(OjNames.CODEFORCES, "tourist");
        assertThat(created.getBody().handles()).containsEntry(OjNames.ATCODER, "tourist_atcoder");
        assertThat(created.getBody().needCollect()).isTrue();
        assertThat(created.getBody().collectionStates()).containsKeys(OjNames.CODEFORCES, OjNames.ATCODER);
        assertThat(created.getBody().collectionStates().get(OjNames.CODEFORCES).historyStartReached()).isFalse();
        assertThat(created.getBody().collectionStates().get(OjNames.CODEFORCES).lastCollectedAt()).isNull();
        assertThat(changed.getBody()).isNotNull();
        assertThat(changed.getBody().studentIdentity()).isEqualTo("112488张三");
        assertThat(changed.getBody().handles()).containsEntry(OjNames.CODEFORCES, "tourist");
        assertThat(changed.getBody().handles()).containsEntry(OjNames.ATCODER, "tourist_atcoder");
        assertThat(changed.getBody().needCollect()).isFalse();
        assertThat(controller.listAll().getBody())
                .containsKeys("112487张三", "112488李四");
        assertThat(controller.listAll().getBody().get("112487张三").handles())
                .containsEntry(OjNames.CODEFORCES, "tourist")
                .containsEntry(OjNames.ATCODER, "tourist_atcoder");
        assertThat(controller.listAll().getBody().get("112487张三").collectionStates())
                .containsKeys(OjNames.CODEFORCES, OjNames.ATCODER);
        assertThat(controller.listAll().getBody().get("112488李四").handles())
                .containsEntry(OjNames.CODEFORCES, "Benq");
        verify(service).create("112487张三", handles);
        verify(service).changeStudentIdentity("112487张三", "112488张三", false, handles);
    }

    @Test
    void rejectsEmptyRequestBodiesAndBlankFields() {
        assertThatThrownBy(() -> controller.create(null))
                .isInstanceOfSatisfying(OjHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_INVALID_REQUEST
                        ));
        assertThatThrownBy(() -> controller.changeStudentIdentity(null))
                .isInstanceOfSatisfying(OjHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_INVALID_REQUEST
                        ));
        assertThatThrownBy(() -> controller.create(new CreateOjHandleAccountRequest(
                " ",
                Map.of(OjNames.CODEFORCES, "tourist")
        )))
                .isInstanceOfSatisfying(OjHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_INVALID_REQUEST
                        ));
        assertThatThrownBy(() -> controller.create(new CreateOjHandleAccountRequest("112487张三", null)))
                .isInstanceOfSatisfying(OjHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_INVALID_REQUEST
                        ));
        assertThatThrownBy(() -> controller.changeStudentIdentity(new ChangeOjHandleIdentityRequest(
                "112487张三",
                " ",
                null,
                null
        )))
                .isInstanceOfSatisfying(OjHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_INVALID_REQUEST
                        ));
        verifyNoInteractions(service);
    }

    @Test
    void mapsServiceErrorsToHttpStatuses() {
        OjHandleAccountExceptionHandler handler = new OjHandleAccountExceptionHandler();
        Map<OjHandleAccountException.ErrorCode, HttpStatus> statuses = Map.of(
                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_INVALID_REQUEST, HttpStatus.BAD_REQUEST,
                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_NOT_FOUND, HttpStatus.NOT_FOUND,
                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_IDENTITY_EXISTS, HttpStatus.CONFLICT,
                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_HANDLE_EXISTS, HttpStatus.CONFLICT
        );

        statuses.forEach((code, status) -> {
            var response = handler.handleOjHandleAccountException(
                    new OjHandleAccountException(code, "message")
            );
            assertThat(response.getStatusCode()).isEqualTo(status);
            assertThat(response.getBody()).isEqualTo(new OjHandleAccountErrorResponse(
                    code.name(),
                    "message"
            ));
        });
    }

    private static OjHandleAccount account(String studentIdentity, Map<String, String> handles, boolean needCollect) {
        return new OjHandleAccount(studentIdentity, handles, needCollect, NOW, NOW);
    }
}
