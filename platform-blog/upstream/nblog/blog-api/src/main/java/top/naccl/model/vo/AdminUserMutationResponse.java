package top.naccl.model.vo;

import top.naccl.entity.User;

import java.util.Map;

public record AdminUserMutationResponse(
        User user,
        Map<String, String> handles,
        Boolean needCollect,
        String generatedPassword,
        boolean reloginRequired
) {
}
