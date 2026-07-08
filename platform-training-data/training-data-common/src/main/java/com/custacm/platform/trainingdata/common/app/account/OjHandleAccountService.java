package com.custacm.platform.trainingdata.common.app.account;

import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleCollectionState;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjHandleAccountRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public class OjHandleAccountService {
    private final OjHandleAccountRepository repository;
    private final Clock clock;

    public OjHandleAccountService(OjHandleAccountRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public OjHandleAccount create(String studentIdentity, Map<String, String> handles) {
        String normalizedStudentIdentity = requireText(
                studentIdentity,
                "studentIdentity",
                OjHandleAccountService::invalidRequest
        );
        Map<String, String> normalizedHandles = normalizeHandles(handles);
        if (repository.findByStudentIdentity(normalizedStudentIdentity).isPresent()) {
            throw new OjHandleAccountException(
                    OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_IDENTITY_EXISTS,
                    "studentIdentity already has an OJ handle account"
            );
        }
        for (Map.Entry<String, String> entry : normalizedHandles.entrySet()) {
            if (repository.findByHandle(entry.getKey(), entry.getValue()).isPresent()) {
                throw new OjHandleAccountException(
                        OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_HANDLE_EXISTS,
                        entry.getKey() + " handle already belongs to a studentIdentity"
                );
            }
        }
        Instant now = clock.instant();
        return repository.save(new OjHandleAccount(
                normalizedStudentIdentity,
                normalizedHandles,
                true,
                now,
                now
        ));
    }

    public List<OjHandleAccount> listAll() {
        return repository.findAll();
    }

    public OjHandleAccount getByStudentIdentity(String studentIdentity) {
        String normalizedStudentIdentity = requireText(
                studentIdentity,
                "studentIdentity",
                OjHandleAccountService::invalidRequest
        );
        return repository.findByStudentIdentity(normalizedStudentIdentity)
                .orElseThrow(OjHandleAccountService::notFound);
    }

    public OjHandleAccount getByHandle(String ojName, String handle) {
        String normalizedOjName = requireOjName(ojName);
        String normalizedHandle = requireText(handle, "handle", OjHandleAccountService::invalidRequest);
        return repository.findByHandle(normalizedOjName, normalizedHandle)
                .orElseThrow(OjHandleAccountService::notFound);
    }

    public String getHandle(OjHandleAccount account, String ojName) {
        String normalizedOjName = requireOjName(ojName);
        String handle = account.handles().get(normalizedOjName);
        if (handle == null) {
            throw notFound();
        }
        return handle;
    }

    public OjHandleAccount changeStudentIdentity(
            String oldStudentIdentity,
            String newStudentIdentity,
            Boolean needCollect
    ) {
        return changeStudentIdentity(oldStudentIdentity, newStudentIdentity, needCollect, null);
    }

    public OjHandleAccount changeStudentIdentity(
            String oldStudentIdentity,
            String newStudentIdentity,
            Boolean needCollect,
            Map<String, String> handles
    ) {
        String normalizedOldStudentIdentity = requireText(
                oldStudentIdentity,
                "oldStudentIdentity",
                OjHandleAccountService::invalidRequest
        );
        String normalizedNewStudentIdentity = requireText(
                newStudentIdentity,
                "newStudentIdentity",
                OjHandleAccountService::invalidRequest
        );
        OjHandleAccount existing = repository.findByStudentIdentity(normalizedOldStudentIdentity)
                .orElseThrow(() -> new OjHandleAccountException(
                        OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_NOT_FOUND,
                        "OJ handle account not found"
                ));
        boolean identityChanged = !normalizedOldStudentIdentity.equals(normalizedNewStudentIdentity);
        if (identityChanged && repository.findByStudentIdentity(normalizedNewStudentIdentity).isPresent()) {
            throw new OjHandleAccountException(
                    OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_IDENTITY_EXISTS,
                        "newStudentIdentity already has an OJ handle account"
            );
        }
        Map<String, String> updatedHandles = handles == null ? existing.handles() : mergeHandles(existing.handles(), handles);
        for (Map.Entry<String, String> entry : updatedHandles.entrySet()) {
            Optional<OjHandleAccount> conflictingAccount = repository.findByHandle(entry.getKey(), entry.getValue())
                    .filter(account -> !normalizedOldStudentIdentity.equals(account.studentIdentity()));
            if (conflictingAccount.isPresent()) {
                throw new OjHandleAccountException(
                        OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_HANDLE_EXISTS,
                        entry.getKey() + " handle already belongs to a studentIdentity"
                );
            }
        }
        boolean updatedNeedCollect = needCollect == null ? existing.needCollect() : needCollect;
        boolean handlesChanged = !updatedHandles.equals(existing.handles());
        if (!identityChanged && needCollect == null && !handlesChanged) {
            return existing;
        }
        return repository.updateStudentIdentityAndNeedCollect(
                normalizedOldStudentIdentity,
                normalizedNewStudentIdentity,
                updatedHandles,
                updatedNeedCollect,
                collectionStatesForUpdatedHandles(existing, updatedHandles),
                clock.instant()
        );
    }

    public Optional<OjHandleAccount> markCollectedByHandle(
            String ojName,
            String handle,
            boolean historyStartReached,
            Instant collectedAt
    ) {
        String normalizedOjName = requireOjName(ojName);
        String normalizedHandle = requireText(handle, "handle", OjHandleAccountService::invalidRequest);
        if (collectedAt == null) {
            throw new OjHandleAccountException(
                    OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_INVALID_REQUEST,
                    "collectedAt must not be null"
            );
        }
        return repository.findByHandle(normalizedOjName, normalizedHandle)
                .map(account -> repository.updateCollectionStates(
                        account.studentIdentity(),
                        markCollected(account.collectionStates(), normalizedOjName, historyStartReached, collectedAt),
                        clock.instant()
                ));
    }

    private static Map<String, OjHandleCollectionState> markCollected(
            Map<String, OjHandleCollectionState> collectionStates,
            String ojName,
            boolean historyStartReached,
            Instant collectedAt
    ) {
        Map<String, OjHandleCollectionState> updated = new LinkedHashMap<>(collectionStates);
        updated.put(
                ojName,
                updated.getOrDefault(ojName, OjHandleCollectionState.empty())
                        .markCollected(historyStartReached, collectedAt)
        );
        return updated;
    }

    private static Map<String, OjHandleCollectionState> collectionStatesForUpdatedHandles(
            OjHandleAccount existing,
            Map<String, String> updatedHandles
    ) {
        Map<String, OjHandleCollectionState> updatedStates = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : updatedHandles.entrySet()) {
            String ojName = entry.getKey();
            if (entry.getValue().equals(existing.handles().get(ojName))) {
                updatedStates.put(ojName, existing.collectionStates().getOrDefault(
                        ojName,
                        OjHandleCollectionState.empty()
                ));
            } else {
                updatedStates.put(ojName, OjHandleCollectionState.empty());
            }
        }
        return updatedStates;
    }

    private static String requireOjName(String ojName) {
        try {
            return OjNames.normalize(ojName);
        } catch (IllegalArgumentException ex) {
            throw new OjHandleAccountException(
                    OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_INVALID_REQUEST,
                    ex.getMessage()
            );
        }
    }

    private static Map<String, String> normalizeHandles(Map<String, String> handles) {
        try {
            return OjHandleAccount.normalizeHandles(handles);
        } catch (IllegalArgumentException ex) {
            throw new OjHandleAccountException(
                    OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_INVALID_REQUEST,
                    ex.getMessage()
            );
        }
    }

    private static Map<String, String> mergeHandles(
            Map<String, String> existingHandles,
            Map<String, String> updatedHandles
    ) {
        if (updatedHandles == null || updatedHandles.isEmpty()) {
            return normalizeHandles(existingHandles);
        }
        Map<String, String> mergedHandles = new LinkedHashMap<>(existingHandles);
        normalizeHandles(updatedHandles).forEach(mergedHandles::put);
        return normalizeHandles(mergedHandles);
    }

    private static OjHandleAccountException invalidRequest(String message) {
        return new OjHandleAccountException(
                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_INVALID_REQUEST,
                message
        );
    }

    private static OjHandleAccountException notFound() {
        return new OjHandleAccountException(
                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_NOT_FOUND,
                "OJ handle account not found"
        );
    }
}
