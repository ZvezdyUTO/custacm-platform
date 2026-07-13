package com.custacm.platform.trainingdata.common.collector;

import com.custacm.platform.trainingdata.common.app.account.TrainingUserDirectory;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleCollectionState;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class OjHandleAccountCollectionHandleResolver implements OjCollectionHandleResolver {
    private final TrainingUserDirectory handleAccountService;

    public OjHandleAccountCollectionHandleResolver(TrainingUserDirectory handleAccountService) {
        this.handleAccountService = handleAccountService;
    }

    @Override
    public String getHandleByUsername(String ojName, String username) {
        String normalizedOjName = OjNames.normalize(ojName);
        return handleAccountService.getHandle(
                handleAccountService.getByUsername(username),
                normalizedOjName
        );
    }

    @Override
    public List<String> listHandlesForCollection(String ojName) {
        String normalizedOjName = OjNames.normalize(ojName);
        return handleAccountService.listAll().stream()
                .filter(OjHandleAccount::needCollect)
                .map(account -> account.handles().get(normalizedOjName))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public Instant getLastCollectedAt(String ojName, String handle) {
        String normalizedOjName = OjNames.normalize(ojName);
        return handleAccountService.getByHandle(normalizedOjName, handle)
                .collectionStates()
                .getOrDefault(normalizedOjName, OjHandleCollectionState.empty())
                .lastCollectedAt();
    }

    @Override
    public void markHandleCollected(
            String ojName,
            String handle,
            Instant collectedAt
    ) {
        handleAccountService.markCollectedByHandle(ojName, handle, collectedAt);
    }
}
