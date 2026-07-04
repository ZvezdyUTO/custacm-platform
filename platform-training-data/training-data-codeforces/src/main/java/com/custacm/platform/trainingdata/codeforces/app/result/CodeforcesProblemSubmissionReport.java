package com.custacm.platform.trainingdata.codeforces.app.result;

import java.util.List;

public record CodeforcesProblemSubmissionReport(
        String problemKey,
        List<CodeforcesSubmissionItem> submissions
) {
}
