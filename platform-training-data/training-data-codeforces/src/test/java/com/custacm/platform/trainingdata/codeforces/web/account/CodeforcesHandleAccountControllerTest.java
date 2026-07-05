package com.custacm.platform.trainingdata.codeforces.web.account;

import com.custacm.platform.trainingdata.codeforces.app.account.CodeforcesHandleAccountException;
import com.custacm.platform.trainingdata.codeforces.app.account.CodeforcesHandleAccountService;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesHandleAccount;
import com.custacm.platform.trainingdata.codeforces.web.account.CodeforcesHandleAccountController;
import com.custacm.platform.trainingdata.codeforces.web.account.request.ChangeCodeforcesHandleIdentityRequest;
import com.custacm.platform.trainingdata.codeforces.web.account.request.CreateCodeforcesHandleAccountRequest;
import com.custacm.platform.trainingdata.codeforces.web.account.response.CodeforcesHandleAccountErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CodeforcesHandleAccountControllerTest {
    private static final Instant NOW = Instant.parse("2026-07-05T00:00:00Z");

    private final CodeforcesHandleAccountService service = mock(CodeforcesHandleAccountService.class);
    private final CodeforcesHandleAccountController controller = new CodeforcesHandleAccountController(service);

    @Test
    void createsQueriesAndChangesIdentity() {
        when(service.create("112487张三", "tourist"))
                .thenReturn(account("112487张三", "tourist"));
        when(service.getByStudentIdentity("112487张三"))
                .thenReturn(account("112487张三", "tourist"));
        when(service.changeStudentIdentity("112487张三", "112488张三"))
                .thenReturn(account("112488张三", "tourist"));

        var created = controller.create(new CreateCodeforcesHandleAccountRequest(" 112487张三 ", " tourist "));
        var queried = controller.getByStudentIdentity(" 112487张三 ");
        var changed = controller.changeStudentIdentity(new ChangeCodeforcesHandleIdentityRequest(
                " 112487张三 ",
                " 112488张三 "
        ));

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().studentIdentity()).isEqualTo("112487张三");
        assertThat(created.getBody().handle()).isEqualTo("tourist");
        assertThat(queried.getBody()).isNotNull();
        assertThat(queried.getBody().handle()).isEqualTo("tourist");
        assertThat(changed.getBody()).isNotNull();
        assertThat(changed.getBody().studentIdentity()).isEqualTo("112488张三");
        assertThat(changed.getBody().handle()).isEqualTo("tourist");
        verify(service).create("112487张三", "tourist");
        verify(service).getByStudentIdentity("112487张三");
        verify(service).changeStudentIdentity("112487张三", "112488张三");
    }

    @Test
    void rejectsEmptyRequestBodiesAndBlankFields() {
        assertThatThrownBy(() -> controller.create(null))
                .isInstanceOfSatisfying(CodeforcesHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_INVALID_REQUEST
                        ));
        assertThatThrownBy(() -> controller.changeStudentIdentity(null))
                .isInstanceOfSatisfying(CodeforcesHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_INVALID_REQUEST
                        ));
        assertThatThrownBy(() -> controller.create(new CreateCodeforcesHandleAccountRequest(" ", "tourist")))
                .isInstanceOfSatisfying(CodeforcesHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_INVALID_REQUEST
                        ));
        assertThatThrownBy(() -> controller.create(new CreateCodeforcesHandleAccountRequest("112487张三", null)))
                .isInstanceOfSatisfying(CodeforcesHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_INVALID_REQUEST
                        ));
        assertThatThrownBy(() -> controller.getByStudentIdentity(""))
                .isInstanceOfSatisfying(CodeforcesHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_INVALID_REQUEST
                        ));
        assertThatThrownBy(() -> controller.changeStudentIdentity(new ChangeCodeforcesHandleIdentityRequest(
                "112487张三",
                " "
        )))
                .isInstanceOfSatisfying(CodeforcesHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_INVALID_REQUEST
                        ));
        verifyNoInteractions(service);
    }

    @Test
    void mapsServiceErrorsToHttpStatuses() {
        CodeforcesHandleAccountExceptionHandler handler = new CodeforcesHandleAccountExceptionHandler();
        Map<CodeforcesHandleAccountException.ErrorCode, HttpStatus> statuses = Map.of(
                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_INVALID_REQUEST, HttpStatus.BAD_REQUEST,
                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_NOT_FOUND, HttpStatus.NOT_FOUND,
                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_IDENTITY_EXISTS, HttpStatus.CONFLICT,
                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_HANDLE_EXISTS, HttpStatus.CONFLICT
        );

        statuses.forEach((code, status) -> {
            var response = handler.handleCodeforcesHandleAccountException(
                    new CodeforcesHandleAccountException(code, "message")
            );
            assertThat(response.getStatusCode()).isEqualTo(status);
            assertThat(response.getBody()).isEqualTo(new CodeforcesHandleAccountErrorResponse(
                    code.name(),
                    "message"
            ));
        });
    }

    private static CodeforcesHandleAccount account(String studentIdentity, String handle) {
        return new CodeforcesHandleAccount(studentIdentity, handle, NOW, NOW);
    }
}
