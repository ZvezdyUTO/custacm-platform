package top.naccl.util;

import java.nio.charset.StandardCharsets;
import top.naccl.exception.BadRequestException;

/** Validates newly chosen passwords before BCrypt hashing. */
public final class PasswordPolicy {
    private PasswordPolicy() {
    }

    public static void validateNewPassword(String password) {
        if (password == null || password.length() < 6) {
            throw new BadRequestException("密码至少需要 6 个字符");
        }
        // BCrypt limits bytes, so a character count alone does not cover Chinese passwords.
        if (password.length() > 72 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadRequestException("密码的 UTF-8 编码不能超过 72 字节");
        }
    }
}
