package com.custacm.platform.trainingdata.common.domain.oj.repo;

import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleCollectionState;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OjHandleAccountRepository {
    List<OjHandleAccount> findAll();

    Optional<OjHandleAccount> findByUsername(String username);

    Optional<OjHandleAccount> findByHandle(String ojName, String handle);

    OjHandleAccount save(OjHandleAccount account);

    OjHandleAccount updateUsernameAndNeedCollect(
            String oldUsername,
            String newUsername,
            Map<String, String> handles,
            boolean needCollect,
            Map<String, OjHandleCollectionState> collectionStates,
            Instant updatedAt
    );

    OjHandleAccount updateCollectionStates(
            String username,
            Map<String, OjHandleCollectionState> collectionStates,
            Instant updatedAt
    );
}
