package com.custacm.platform.trainingdata.common.app.purge;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountException;
import com.custacm.platform.trainingdata.common.app.account.TrainingUserDirectory;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjStudentDataPurgeResult;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjOdsDataPurgeRepository;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjWarehouseDataPurgeRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import org.springframework.transaction.support.TransactionOperations;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public class OjStudentDataPurgeService {
    private final Map<String, OjOdsDataPurgeRepository> odsDataPurgeRepositories;
    private final OjWarehouseDataPurgeRepository warehouseDataPurgeRepository;
    private final TrainingUserDirectory handleAccountService;
    private final TransactionOperations transactionOperations;

    public OjStudentDataPurgeService(
            Collection<OjOdsDataPurgeRepository> odsDataPurgeRepositories,
            OjWarehouseDataPurgeRepository warehouseDataPurgeRepository,
            TrainingUserDirectory handleAccountService,
            TransactionOperations transactionOperations
    ) {
        this.odsDataPurgeRepositories = indexOdsDataPurgeRepositories(odsDataPurgeRepositories);
        this.warehouseDataPurgeRepository = warehouseDataPurgeRepository;
        this.handleAccountService = handleAccountService;
        this.transactionOperations = transactionOperations;
    }

    public OjStudentDataPurgeResult purgeStudentData(String username, String ojName) {
        String normalizedUsername = requireText(
                username,
                "username",
                OjStudentDataPurgeService::invalidRequest
        );
        String normalizedOjName = normalizeRequiredOjName(ojName);
        return transactionOperations.execute(status -> purgeStudentDataInTransaction(
                normalizedUsername,
                normalizedOjName
        ));
    }

    private OjStudentDataPurgeResult purgeStudentDataInTransaction(
            String username,
            String requestedOjName
    ) {
        OjHandleAccount account = findAccount(username);
        if (account == null) {
            return OjStudentDataPurgeResult.aggregate(
                    username,
                    requestedOjName,
                    null,
                    Map.of(),
                    List.of()
            );
        }
        return purgeSingleRequestedOj(username, account, requestedOjName);
    }

    private OjStudentDataPurgeResult purgeSingleRequestedOj(
            String username,
            OjHandleAccount account,
            String ojName
    ) {
        String handle = account.handles().get(ojName);
        if (handle == null) {
            return OjStudentDataPurgeResult.aggregate(
                    username,
                    ojName,
                    null,
                    account.handles(),
                    List.of()
            );
        }
        return OjStudentDataPurgeResult.aggregate(
                username,
                ojName,
                handle,
                account.handles(),
                List.of(purgeSingleOj(ojName, handle))
        );
    }

    private OjStudentDataPurgeResult.OjDataPurgeResult purgeSingleOj(String ojName, String handle) {
        OjOdsDataPurgeRepository odsDataPurgeRepository = requireOdsDataPurgeRepository(ojName);
        OjWarehouseDataPurgeRepository.OjWarehouseDataPurgeCounts warehouseRows =
                warehouseDataPurgeRepository.purgeAllByHandle(ojName, handle);
        int odsRows = odsDataPurgeRepository.purgeAllByHandle(handle);
        return new OjStudentDataPurgeResult.OjDataPurgeResult(
                ojName,
                handle,
                odsRows,
                warehouseRows.dwdSubmissionRows(),
                warehouseRows.dwmFirstAcceptedRows(),
                warehouseRows.dwsAcceptedSummaryRows()
        );
    }

    private OjHandleAccount findAccount(String username) {
        try {
            return handleAccountService.getByUsername(username);
        } catch (OjHandleAccountException ex) {
            if (ex.errorCode() == OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_NOT_FOUND) {
                return null;
            }
            throw ex;
        }
    }

    private OjOdsDataPurgeRepository requireOdsDataPurgeRepository(String ojName) {
        OjOdsDataPurgeRepository repository = odsDataPurgeRepositories.get(ojName);
        if (repository == null) {
            throw new OjStudentDataPurgeException(
                    OjStudentDataPurgeException.ErrorCode.OJ_STUDENT_DATA_PURGE_INVALID_REQUEST,
                    ojName + " ODS purge is not implemented"
            );
        }
        return repository;
    }

    private static Map<String, OjOdsDataPurgeRepository> indexOdsDataPurgeRepositories(
            Collection<OjOdsDataPurgeRepository> repositories
    ) {
        Map<String, OjOdsDataPurgeRepository> indexed = new LinkedHashMap<>();
        for (OjOdsDataPurgeRepository repository : repositories) {
            String ojName = OjNames.normalize(repository.ojName());
            OjOdsDataPurgeRepository existing = indexed.putIfAbsent(ojName, repository);
            if (existing != null) {
                throw new IllegalArgumentException("duplicate ODS purge repository for " + ojName);
            }
        }
        return Map.copyOf(indexed);
    }

    private static String normalizeRequiredOjName(String ojName) {
        String requestedOjName = requireText(ojName, "ojName", OjStudentDataPurgeService::invalidRequest);
        try {
            return OjNames.normalize(requestedOjName);
        } catch (IllegalArgumentException ex) {
            throw invalidRequest(ex.getMessage());
        }
    }

    private static OjStudentDataPurgeException invalidRequest(String message) {
        return new OjStudentDataPurgeException(
                OjStudentDataPurgeException.ErrorCode.OJ_STUDENT_DATA_PURGE_INVALID_REQUEST,
                message
        );
    }
}
