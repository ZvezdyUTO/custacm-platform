package com.custacm.platform.trainingdata.common.web.account.response;

import java.util.Map;

public record OjHandleAccountResponse(
        String studentIdentity,
        Map<String, String> handles,
        boolean needCollect,
        Map<String, OjHandleCollectionStateResponse> collectionStates
) {
}
