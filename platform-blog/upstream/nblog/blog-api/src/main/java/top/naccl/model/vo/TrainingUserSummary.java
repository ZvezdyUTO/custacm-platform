package top.naccl.model.vo;

import java.util.List;

public record TrainingUserSummary(String username, String nickname, List<String> ojNames) {
    public TrainingUserSummary {
        ojNames = List.copyOf(ojNames);
    }
}
