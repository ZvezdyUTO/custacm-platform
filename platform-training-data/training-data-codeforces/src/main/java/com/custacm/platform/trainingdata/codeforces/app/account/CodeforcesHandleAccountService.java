package com.custacm.platform.trainingdata.codeforces.app.account;

import com.custacm.platform.trainingdata.codeforces.app.account.CodeforcesHandleAccountException;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesHandleAccount;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesHandleAccountRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

public class CodeforcesHandleAccountService {
    private final CodeforcesHandleAccountRepository repository;
    private final Clock clock;

    public CodeforcesHandleAccountService(CodeforcesHandleAccountRepository repository) {
        this(repository, Clock.system(ZoneOffset.ofHours(8)));
    }

    public CodeforcesHandleAccountService(CodeforcesHandleAccountRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public CodeforcesHandleAccount create(String studentIdentity, String handle) {
        if (repository.findByStudentIdentity(studentIdentity).isPresent()) {
            throw new CodeforcesHandleAccountException(
                    CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_IDENTITY_EXISTS,
                    "studentIdentity already has a Codeforces handle"
            );
        }
        if (repository.findByHandle(handle).isPresent()) {
            throw new CodeforcesHandleAccountException(
                    CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_HANDLE_EXISTS,
                    "Codeforces handle already belongs to a studentIdentity"
            );
        }
        Instant now = clock.instant();
        return repository.save(new CodeforcesHandleAccount(studentIdentity, handle, true, now, now));
    }

    public List<CodeforcesHandleAccount> listAll() {
        return repository.findAll();
    }

    public CodeforcesHandleAccount getByStudentIdentity(String studentIdentity) {
        return repository.findByStudentIdentity(studentIdentity)
                .orElseThrow(() -> new CodeforcesHandleAccountException(
                        CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_NOT_FOUND,
                        "Codeforces handle account not found"
                ));
    }

    public CodeforcesHandleAccount getByHandle(String handle) {
        return repository.findByHandle(handle)
                .orElseThrow(() -> new CodeforcesHandleAccountException(
                        CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_NOT_FOUND,
                        "Codeforces handle account not found"
                ));
    }

    public CodeforcesHandleAccount changeStudentIdentity(
            String oldStudentIdentity,
            String newStudentIdentity,
            Boolean needCollect
    ) {
        CodeforcesHandleAccount existing = repository.findByStudentIdentity(oldStudentIdentity)
                .orElseThrow(() -> new CodeforcesHandleAccountException(
                        CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_NOT_FOUND,
                        "Codeforces handle account not found"
                ));
        boolean identityChanged = !oldStudentIdentity.equals(newStudentIdentity);
        if (identityChanged && repository.findByStudentIdentity(newStudentIdentity).isPresent()) {
            throw new CodeforcesHandleAccountException(
                    CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_IDENTITY_EXISTS,
                    "newStudentIdentity already has a Codeforces handle"
            );
        }
        boolean updatedNeedCollect = needCollect == null ? existing.needCollect() : needCollect;
        if (!identityChanged && needCollect == null) {
            return existing;
        }
        return repository.updateStudentIdentityAndNeedCollect(
                oldStudentIdentity,
                newStudentIdentity,
                updatedNeedCollect,
                clock.instant()
        );
    }
}
