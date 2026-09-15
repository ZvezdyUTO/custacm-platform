package top.naccl.util;

import org.junit.jupiter.api.Test;
import top.naccl.exception.BadRequestException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyTest {
    @Test
    void acceptsPasswordsAtTheBcryptByteLimit() {
        for (String password : new String[]{"123456", "a".repeat(72), "密".repeat(24), "🔒".repeat(18)}) {
            assertDoesNotThrow(() -> PasswordPolicy.validateNewPassword(password));
            assertTrue(HashUtils.matchBC(password, HashUtils.getBC(password)));
        }
    }

    @Test
    void rejectsShortOrOversizedNewPasswords() {
        for (String password : new String[]{null, "", "12345", "a".repeat(73), "密".repeat(25), "🔒".repeat(19)}) {
            assertThrows(BadRequestException.class, () -> PasswordPolicy.validateNewPassword(password));
        }
    }
}
