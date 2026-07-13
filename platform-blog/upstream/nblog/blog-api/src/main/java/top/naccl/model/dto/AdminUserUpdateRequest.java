package top.naccl.model.dto;

import java.util.Map;

// Author: huangbingrui.awa
public record AdminUserUpdateRequest(
        String newUsername,
        String nickname,
        String email,
        String role,
        String password,
        Map<String, String> handles,
        Boolean needCollect
) {
}
