package com.custacm.platform.auth.domain.repo;

import com.custacm.platform.auth.domain.model.UserAccount;
import com.custacm.platform.auth.domain.model.UserRole;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository {
    Optional<UserAccount> findByStudentIdentity(String studentIdentity);

    List<UserAccount> findAll();

    UserAccount save(UserAccount account);

    UserAccount update(UserAccount account);

    void deleteByStudentIdentity(String studentIdentity);

    long countByRole(UserRole role);
}
