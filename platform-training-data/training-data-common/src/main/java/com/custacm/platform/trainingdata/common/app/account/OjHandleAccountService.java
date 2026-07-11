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

public class OjHandleAccountService implements TrainingUserDirectory {
    private final OjHandleAccountRepository repository;
    private final Clock clock;

    public OjHandleAccountService(OjHandleAccountRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public OjHandleAccount create(String username, Map<String, String> handles) {
		return create(username, handles, true);
	}

	public OjHandleAccount create(String username, Map<String, String> handles, boolean needCollect) {
        String normalizedUsername = requireText(
                username,
                "username",
                OjHandleAccountService::invalidRequest
        );
        Map<String, String> normalizedHandles = normalizeHandles(handles);
        if (repository.findByUsername(normalizedUsername).isPresent()) {
            throw new OjHandleAccountException(
                    OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_IDENTITY_EXISTS,
                    "username already has an OJ handle account"
            );
        }
        for (Map.Entry<String, String> entry : normalizedHandles.entrySet()) {
            if (repository.findByHandle(entry.getKey(), entry.getValue()).isPresent()) {
                throw new OjHandleAccountException(
                        OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_HANDLE_EXISTS,
                        entry.getKey() + " handle already belongs to a username"
                );
            }
        }
        Instant now = clock.instant();
        return repository.save(new OjHandleAccount(
                normalizedUsername,
                normalizedHandles,
                needCollect,
                now,
                now
        ));
    }

    public List<OjHandleAccount> listAll() {
        return repository.findAll();
    }

    public OjHandleAccount getByUsername(String username) {
        String normalizedUsername = requireText(
                username,
                "username",
                OjHandleAccountService::invalidRequest
        );
        return repository.findByUsername(normalizedUsername)
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

    public OjHandleAccount changeUsername(
            String oldUsername,
            String newUsername,
            Boolean needCollect
    ) {
        return changeUsername(oldUsername, newUsername, needCollect, null);
    }

    public OjHandleAccount changeUsername(
            String oldUsername,
            String newUsername,
            Boolean needCollect,
            Map<String, String> handles
    ) {
        String normalizedOldUsername = requireText(
                oldUsername,
                "oldUsername",
                OjHandleAccountService::invalidRequest
        );
        String normalizedNewUsername = requireText(
                newUsername,
                "newUsername",
                OjHandleAccountService::invalidRequest
        );
        OjHandleAccount existing = repository.findByUsername(normalizedOldUsername)
                .orElseThrow(() -> new OjHandleAccountException(
                        OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_NOT_FOUND,
                        "OJ handle account not found"
                ));
        boolean identityChanged = !normalizedOldUsername.equals(normalizedNewUsername);
        if (identityChanged && repository.findByUsername(normalizedNewUsername).isPresent()) {
            throw new OjHandleAccountException(
                    OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_IDENTITY_EXISTS,
                        "newUsername already has an OJ handle account"
            );
        }
        Map<String, String> updatedHandles = handles == null ? existing.handles() : mergeHandles(existing.handles(), handles);
        for (Map.Entry<String, String> entry : updatedHandles.entrySet()) {
            Optional<OjHandleAccount> conflictingAccount = repository.findByHandle(entry.getKey(), entry.getValue())
                    .filter(account -> !normalizedOldUsername.equals(account.username()));
            if (conflictingAccount.isPresent()) {
                throw new OjHandleAccountException(
                        OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_HANDLE_EXISTS,
                        entry.getKey() + " handle already belongs to a username"
                );
            }
        }
        boolean updatedNeedCollect = needCollect == null ? existing.needCollect() : needCollect;
        boolean handlesChanged = !updatedHandles.equals(existing.handles());
        if (!identityChanged && needCollect == null && !handlesChanged) {
            return existing;
        }
        return repository.updateUsernameAndNeedCollect(
                normalizedOldUsername,
                normalizedNewUsername,
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
                        account.username(),
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
