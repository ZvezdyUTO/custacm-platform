package top.naccl.model.dto;

public record AdminUserPatchRequest(
        String newUsername,
        String nickname,
        String email,
        String role,
        String password
) {
}
