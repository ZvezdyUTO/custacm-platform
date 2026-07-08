package com.custacm.platform.trainingdata.atcoder.infra;

import com.custacm.platform.trainingdata.common.domain.oj.repo.OjOdsDataPurgeRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public class JdbcAtcoderOdsDataPurgeRepository implements OjOdsDataPurgeRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcAtcoderOdsDataPurgeRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String ojName() {
        return OjNames.ATCODER;
    }

    @Override
    public int purgeAllByHandle(String handle) {
        String normalizedHandle = requireText(handle, "handle");
        return jdbcTemplate.update("""
                delete from ods_atcoder__submission
                where user_id = :handle
                """, new MapSqlParameterSource("handle", normalizedHandle));
    }

}
