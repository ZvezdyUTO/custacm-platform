package com.custacm.platform.auth.infra.jdbc;

import com.custacm.platform.auth.domain.model.UserAccount;
import com.custacm.platform.auth.domain.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcUserAccountRepositoryTest {
    private JdbcUserAccountRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:auth_" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        dataSource.setDriverClassName("org.h2.Driver");
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V020__create_auth_user_account.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V021__fold_auth_user_status_into_role.sql"));
        }
        repository = new JdbcUserAccountRepository(new NamedParameterJdbcTemplate(dataSource));
    }

    @Test
    void savesUpdatesListsAndDeletesUsers() {
        Instant now = Instant.parse("2026-07-04T12:00:00Z");
        UserAccount account = new UserAccount(
                "230511213黄炳睿",
                "hash",
                UserRole.PLAYER,
                now,
                now
        );

        repository.save(account);
        repository.update(account.withRole(UserRole.DISABLE, now.plusSeconds(60)));

        assertThat(repository.findByStudentIdentity("230511213黄炳睿"))
                .get()
                .satisfies(saved -> {
                    assertThat(saved.role()).isEqualTo(UserRole.DISABLE);
                    assertThat(saved.role().canAuthenticate()).isFalse();
                });
        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.countByRole(UserRole.DISABLE)).isEqualTo(1);

        repository.deleteByStudentIdentity("230511213黄炳睿");

        assertThat(repository.findByStudentIdentity("230511213黄炳睿")).isEmpty();
    }
}
