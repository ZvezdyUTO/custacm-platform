package com.custacm.platform.trainingdata.atcoder.domain;

import java.util.List;

public interface AtcoderProblemPayloadParser {
    List<AtcoderOdsProblem> parseProblems(String problemPayload, AtcoderCollectBatch batch);
}
