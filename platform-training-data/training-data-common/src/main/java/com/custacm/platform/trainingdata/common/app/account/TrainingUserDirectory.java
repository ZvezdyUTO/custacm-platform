package com.custacm.platform.trainingdata.common.app.account;

import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TrainingUserDirectory {
    List<OjHandleAccount> listAll();

    OjHandleAccount getByUsername(String username);

    OjHandleAccount getByHandle(String ojName, String handle);

    String getHandle(OjHandleAccount account, String ojName);

    Optional<OjHandleAccount> markCollectedByHandle(
            String ojName, String handle, Instant collectedAt);
}
