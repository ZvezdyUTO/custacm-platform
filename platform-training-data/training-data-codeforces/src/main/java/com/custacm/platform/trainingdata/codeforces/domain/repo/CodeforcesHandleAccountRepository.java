package com.custacm.platform.trainingdata.codeforces.domain.repo;

import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesHandleAccount;

import java.time.Instant;
import java.util.Optional;

public interface CodeforcesHandleAccountRepository {
    Optional<CodeforcesHandleAccount> findByStudentIdentity(String studentIdentity);

    Optional<CodeforcesHandleAccount> findByHandle(String handle);

    CodeforcesHandleAccount save(CodeforcesHandleAccount account);

    CodeforcesHandleAccount updateStudentIdentity(
            String oldStudentIdentity,
            String newStudentIdentity,
            Instant updatedAt
    );
}
