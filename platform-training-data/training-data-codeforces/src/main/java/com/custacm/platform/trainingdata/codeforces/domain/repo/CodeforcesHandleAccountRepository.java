package com.custacm.platform.trainingdata.codeforces.domain.repo;

import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesHandleAccount;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CodeforcesHandleAccountRepository {
    List<CodeforcesHandleAccount> findAll();

    Optional<CodeforcesHandleAccount> findByStudentIdentity(String studentIdentity);

    Optional<CodeforcesHandleAccount> findByHandle(String handle);

    CodeforcesHandleAccount save(CodeforcesHandleAccount account);

    CodeforcesHandleAccount updateStudentIdentityAndNeedCollect(
            String oldStudentIdentity,
            String newStudentIdentity,
            boolean needCollect,
            Instant updatedAt
    );
}
