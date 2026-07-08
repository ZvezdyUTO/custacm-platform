package com.custacm.platform.trainingdata.common.domain.oj.repo;

public interface OjOdsDataPurgeRepository {
    String ojName();

    int purgeAllByHandle(String handle);
}
