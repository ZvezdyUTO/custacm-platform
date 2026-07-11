package com.custacm.platform.trainingdata.common.web.account.request;

import java.util.Map;

public record ChangeOjHandleIdentityRequest(
        String oldUsername,
        String newUsername,
        Boolean needCollect,
        Map<String, String> handles
) {
}
