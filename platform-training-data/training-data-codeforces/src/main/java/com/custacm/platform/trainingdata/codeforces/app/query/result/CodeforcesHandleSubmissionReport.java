package com.custacm.platform.trainingdata.codeforces.app.query.result;

import java.util.List;

public record CodeforcesHandleSubmissionReport(
        String studentIdentity,
        String authorHandle,
        List<CodeforcesSubmissionItem> submissions
) {
}
