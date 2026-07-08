package com.custacm.platform.trainingdata.common.web.account.response;

import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleCollectionState;

import java.time.Instant;

public record OjHandleCollectionStateResponse(
        boolean historyStartReached,
        Instant lastCollectedAt
) {
    public static OjHandleCollectionStateResponse from(OjHandleCollectionState state) {
        return new OjHandleCollectionStateResponse(state.historyStartReached(), state.lastCollectedAt());
    }
}
