package com.custacm.platform.trainingdata.codeforces.domain.criteria;

import java.time.LocalDateTime;

public record CodeforcesProblemFirstAcceptedHandleCriteria(
        String problemKey,
        LocalDateTime firstAcceptedFromUtcPlus8,
        LocalDateTime firstAcceptedToUtcPlus8
) {
}
