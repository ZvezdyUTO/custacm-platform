package com.custacm.platform.trainingdata.codeforces.app.query.result;

import java.util.List;

public record CodeforcesProblemSubmissionReport(
        String problemKey,
        List<CodeforcesSubmissionItem> submissions
) {
}
