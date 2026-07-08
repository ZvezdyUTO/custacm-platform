package com.custacm.platform.trainingdata.codeforces.domain;

import com.custacm.platform.trainingdata.common.domain.oj.repo.OjOdsDataPurgeRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;

public interface CodeforcesOdsDataPurgeRepository extends OjOdsDataPurgeRepository {
    @Override
    default String ojName() {
        return OjNames.CODEFORCES;
    }
}
