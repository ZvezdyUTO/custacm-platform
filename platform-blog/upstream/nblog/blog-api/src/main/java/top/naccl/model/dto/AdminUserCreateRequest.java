package top.naccl.model.dto;

import java.util.Map;

public record AdminUserCreateRequest(
        String username,
        String password,
        String nickname,
        String email,
        String avatar,
        String role,
        Map<String, String> handles,
        Boolean needCollect
) {
}
