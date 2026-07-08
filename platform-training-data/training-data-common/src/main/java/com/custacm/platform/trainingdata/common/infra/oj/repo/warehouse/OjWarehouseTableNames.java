package com.custacm.platform.trainingdata.common.infra.oj.repo.warehouse;

import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;

import java.util.Locale;

public final class OjWarehouseTableNames {
    private OjWarehouseTableNames() {
    }

    public static String dwdSubmission(String ojName) {
        return "dwd_" + tableSegment(ojName) + "__submission";
    }

    public static String dwmHandleProblemFirstAccepted(String ojName) {
        return "dwm_" + tableSegment(ojName) + "__handle_problem_first_accepted";
    }

    public static String dwsHandleDailyRatingAcceptedSummary(String ojName) {
        return "dws_" + tableSegment(ojName) + "__handle_daily_rating_accepted_summary";
    }

    private static String tableSegment(String ojName) {
        return OjNames.normalize(ojName).toLowerCase(Locale.ROOT);
    }
}
