package top.naccl.model.vo;

import top.naccl.entity.User;

import java.time.Instant;
import java.util.Map;

public record AdminUserMutationResponse(
        User user,
        Map<String, String> handles,
        Boolean needCollect,
        Map<String, CollectionState> collectionStates,
        String generatedPassword,
        boolean reloginRequired
) {
    public record CollectionState(Instant lastCollectedAt) {
    }
}
