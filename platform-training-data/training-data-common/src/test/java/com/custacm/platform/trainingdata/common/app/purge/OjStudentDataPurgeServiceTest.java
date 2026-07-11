package com.custacm.platform.trainingdata.common.app.purge;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountException;
import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjStudentDataPurgeResult;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjOdsDataPurgeRepository;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjWarehouseDataPurgeRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OjStudentDataPurgeServiceTest {
    private final OjOdsDataPurgeRepository codeforcesOdsDataPurgeRepository = mock(OjOdsDataPurgeRepository.class);
    private final OjOdsDataPurgeRepository atcoderOdsDataPurgeRepository = mock(OjOdsDataPurgeRepository.class);
    private final OjWarehouseDataPurgeRepository warehouseDataPurgeRepository =
            mock(OjWarehouseDataPurgeRepository.class);
    private final OjHandleAccountService handleAccountService = mock(OjHandleAccountService.class);
    private final OjStudentDataPurgeService service =
            new OjStudentDataPurgeService(
                    odsDataPurgeRepositories(),
                    warehouseDataPurgeRepository,
                    handleAccountService,
                    immediateTransactionOperations()
            );

    @BeforeEach
    void clearConstructorInteractions() {
        clearInvocations(codeforcesOdsDataPurgeRepository, atcoderOdsDataPurgeRepository);
    }

    @Test
    void purgesOnlyRequestedOj() {
        OjHandleAccount account = account(
                "112487张三",
                Map.of(OjNames.CODEFORCES, "tourist", OjNames.ATCODER, "tourist_atcoder")
        );
        when(handleAccountService.getByUsername("112487张三")).thenReturn(account);
        when(warehouseDataPurgeRepository.purgeAllByHandle(OjNames.ATCODER, "tourist_atcoder"))
                .thenReturn(new OjWarehouseDataPurgeRepository.OjWarehouseDataPurgeCounts(
                        7,
                        8,
                        9
                ));
        when(atcoderOdsDataPurgeRepository.purgeAllByHandle("tourist_atcoder")).thenReturn(6);

        OjStudentDataPurgeResult result = service.purgeStudentData(" 112487张三 ", " atcoder ");

        assertThat(result.username()).isEqualTo("112487张三");
        assertThat(result.ojName()).isEqualTo(OjNames.ATCODER);
        assertThat(result.handle()).isEqualTo("tourist_atcoder");
        assertThat(result.handles()).containsEntry(OjNames.CODEFORCES, "tourist")
                .containsEntry(OjNames.ATCODER, "tourist_atcoder");
        assertThat(result.odsSubmissionRows()).isEqualTo(6);
        assertThat(result.dwdSubmissionRows()).isEqualTo(7);
        assertThat(result.dwmFirstAcceptedRows()).isEqualTo(8);
        assertThat(result.dwsAcceptedSummaryRows()).isEqualTo(9);
        assertThat(result.totalDeletedRows()).isEqualTo(30);
        assertThat(result.ojResults()).hasSize(1);
        verify(handleAccountService).getByUsername("112487张三");
        verify(warehouseDataPurgeRepository).purgeAllByHandle(OjNames.ATCODER, "tourist_atcoder");
        verify(atcoderOdsDataPurgeRepository).purgeAllByHandle("tourist_atcoder");
    }

    @Test
    void purgesOnlyRequestedOjWhenOjNameIsProvided() {
        OjHandleAccount account = account(
                "112487张三",
                Map.of(OjNames.CODEFORCES, "tourist", OjNames.ATCODER, "tourist_atcoder")
        );
        when(handleAccountService.getByUsername("112487张三")).thenReturn(account);
        when(warehouseDataPurgeRepository.purgeAllByHandle(OjNames.ATCODER, "tourist_atcoder"))
                .thenReturn(new OjWarehouseDataPurgeRepository.OjWarehouseDataPurgeCounts(
                        7,
                        8,
                        9
                ));
        when(atcoderOdsDataPurgeRepository.purgeAllByHandle("tourist_atcoder")).thenReturn(6);

        OjStudentDataPurgeResult result = service.purgeStudentData("112487张三", " atcoder ");

        assertThat(result.ojName()).isEqualTo(OjNames.ATCODER);
        assertThat(result.handle()).isEqualTo("tourist_atcoder");
        assertThat(result.odsSubmissionRows()).isEqualTo(6);
        assertThat(result.dwdSubmissionRows()).isEqualTo(7);
        assertThat(result.dwmFirstAcceptedRows()).isEqualTo(8);
        assertThat(result.dwsAcceptedSummaryRows()).isEqualTo(9);
        assertThat(result.ojResults()).hasSize(1);
        verify(warehouseDataPurgeRepository).purgeAllByHandle(OjNames.ATCODER, "tourist_atcoder");
        verify(atcoderOdsDataPurgeRepository).purgeAllByHandle("tourist_atcoder");
    }

    @Test
    void returnsZeroCountsWhenIdentityHasNoHandleAccount() {
        when(handleAccountService.getByUsername("missing")).thenThrow(new OjHandleAccountException(
                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_NOT_FOUND,
                "OJ handle account not found"
        ));

        OjStudentDataPurgeResult result = service.purgeStudentData("missing", OjNames.CODEFORCES);

        assertThat(result).isEqualTo(OjStudentDataPurgeResult.aggregate(
                "missing",
                OjNames.CODEFORCES,
                null,
                Map.of(),
                List.of()
        ));
        verifyNoInteractions(codeforcesOdsDataPurgeRepository, atcoderOdsDataPurgeRepository, warehouseDataPurgeRepository);
    }

    @Test
    void returnsZeroCountsWhenRequestedOjIsNotBound() {
        OjHandleAccount account = account("112487张三", Map.of(OjNames.CODEFORCES, "tourist"));
        when(handleAccountService.getByUsername("112487张三")).thenReturn(account);

        OjStudentDataPurgeResult result = service.purgeStudentData("112487张三", OjNames.ATCODER);

        assertThat(result.ojName()).isEqualTo(OjNames.ATCODER);
        assertThat(result.handle()).isNull();
        assertThat(result.totalDeletedRows()).isZero();
        assertThat(result.ojResults()).isEmpty();
        verifyNoInteractions(codeforcesOdsDataPurgeRepository, atcoderOdsDataPurgeRepository, warehouseDataPurgeRepository);
    }

    @Test
    void rejectsBlankIdentity() {
        assertThatThrownBy(() -> service.purgeStudentData(" ", OjNames.CODEFORCES))
                .isInstanceOfSatisfying(OjStudentDataPurgeException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                OjStudentDataPurgeException.ErrorCode
                                        .OJ_STUDENT_DATA_PURGE_INVALID_REQUEST
                        ));
    }

    @Test
    void rejectsBlankOjName() {
        assertThatThrownBy(() -> service.purgeStudentData("112487张三", " "))
                .isInstanceOfSatisfying(OjStudentDataPurgeException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                OjStudentDataPurgeException.ErrorCode
                                        .OJ_STUDENT_DATA_PURGE_INVALID_REQUEST
                        ));
    }

    @Test
    void rejectsUnsupportedOjName() {
        assertThatThrownBy(() -> service.purgeStudentData("112487张三", "vjudge"))
                .isInstanceOfSatisfying(OjStudentDataPurgeException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                OjStudentDataPurgeException.ErrorCode
                                        .OJ_STUDENT_DATA_PURGE_INVALID_REQUEST
                        ));
    }

    private List<OjOdsDataPurgeRepository> odsDataPurgeRepositories() {
        when(codeforcesOdsDataPurgeRepository.ojName()).thenReturn(OjNames.CODEFORCES);
        when(atcoderOdsDataPurgeRepository.ojName()).thenReturn(OjNames.ATCODER);
        return List.of(codeforcesOdsDataPurgeRepository, atcoderOdsDataPurgeRepository);
    }

    private static TransactionOperations immediateTransactionOperations() {
        return new TransactionOperations() {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(mock(TransactionStatus.class));
            }
        };
    }

    private static OjHandleAccount account(String username, Map<String, String> handles) {
        Instant now = Instant.parse("2026-07-07T00:00:00Z");
        return new OjHandleAccount(
                username,
                handles,
                true,
                now,
                now
        );
    }
}
