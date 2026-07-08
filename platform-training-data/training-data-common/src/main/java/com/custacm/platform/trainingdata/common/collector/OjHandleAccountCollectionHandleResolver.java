package com.custacm.platform.trainingdata.common.collector;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class OjHandleAccountCollectionHandleResolver implements OjCollectionHandleResolver {
    private final OjHandleAccountService handleAccountService;

    public OjHandleAccountCollectionHandleResolver(OjHandleAccountService handleAccountService) {
        this.handleAccountService = handleAccountService;
    }

    @Override
    public String getHandleByStudentIdentity(String ojName, String studentIdentity) {
        String normalizedOjName = OjNames.normalize(ojName);
        return handleAccountService.getHandle(
                handleAccountService.getByStudentIdentity(studentIdentity),
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
    public void markHandleCollected(
            String ojName,
            String handle,
            boolean historyStartReached,
            Instant collectedAt
    ) {
        handleAccountService.markCollectedByHandle(ojName, handle, historyStartReached, collectedAt);
    }
}
