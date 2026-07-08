package com.custacm.platform.trainingdata.atcoder.domain;

import java.util.List;

public interface AtcoderOdsProblemModelWriter {
    void upsertBatch(AtcoderCollectBatch batch, List<AtcoderOdsProblemModel> problemModels);
}
