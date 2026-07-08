package com.custacm.platform.trainingdata.common.web.account.request;

import java.util.Map;

public record ChangeOjHandleIdentityRequest(
        String oldStudentIdentity,
        String newStudentIdentity,
        Boolean needCollect,
        Map<String, String> handles
) {
}
