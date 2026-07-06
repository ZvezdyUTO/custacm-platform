package com.custacm.platform.trainingdata.codeforces.web.account.request;

public record ChangeCodeforcesHandleIdentityRequest(
        String oldStudentIdentity,
        String newStudentIdentity,
        Boolean needCollect
) {
}
