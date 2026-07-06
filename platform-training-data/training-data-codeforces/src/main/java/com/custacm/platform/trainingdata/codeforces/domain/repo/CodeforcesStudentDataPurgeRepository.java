package com.custacm.platform.trainingdata.codeforces.domain.repo;

import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesStudentDataPurgeResult;

public interface CodeforcesStudentDataPurgeRepository {
    CodeforcesStudentDataPurgeResult purgeByStudentIdentity(String studentIdentity);
}
