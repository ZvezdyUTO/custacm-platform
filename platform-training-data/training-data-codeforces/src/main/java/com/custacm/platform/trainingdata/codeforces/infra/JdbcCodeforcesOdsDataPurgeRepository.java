package com.custacm.platform.trainingdata.codeforces.infra;

import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesOdsDataPurgeRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public class JdbcCodeforcesOdsDataPurgeRepository implements CodeforcesOdsDataPurgeRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcCodeforcesOdsDataPurgeRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int purgeAllByHandle(String handle) {
        String normalizedHandle = requireText(handle, "handle");
        return jdbcTemplate.update("""
                delete from ods_codeforces__submission
                where author_handle = :handle
                """, new MapSqlParameterSource("handle", normalizedHandle));
    }

}
