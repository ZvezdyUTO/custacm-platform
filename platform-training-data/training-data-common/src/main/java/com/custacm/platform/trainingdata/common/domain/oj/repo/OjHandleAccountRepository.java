package com.custacm.platform.trainingdata.common.domain.oj.repo;

import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleCollectionState;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OjHandleAccountRepository {
    List<OjHandleAccount> findAll();

    Optional<OjHandleAccount> findByStudentIdentity(String studentIdentity);

    Optional<OjHandleAccount> findByHandle(String ojName, String handle);

    OjHandleAccount save(OjHandleAccount account);

    OjHandleAccount updateStudentIdentityAndNeedCollect(
            String oldStudentIdentity,
            String newStudentIdentity,
            Map<String, String> handles,
            boolean needCollect,
            Map<String, OjHandleCollectionState> collectionStates,
            Instant updatedAt
    );

    OjHandleAccount updateCollectionStates(
            String studentIdentity,
            Map<String, OjHandleCollectionState> collectionStates,
            Instant updatedAt
    );
}
