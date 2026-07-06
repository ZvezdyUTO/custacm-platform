package com.custacm.platform.trainingdata.codeforces.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodeforcesHandleAccountTest {
    private static final Instant NOW = Instant.parse("2026-07-05T00:00:00Z");

    @Test
    void keepsStudentIdentityHandleAndTimestamps() {
        CodeforcesHandleAccount account = new CodeforcesHandleAccount(
                "112487张三",
                "tourist",
                NOW,
                NOW
        );

        assertThat(account.studentIdentity()).isEqualTo("112487张三");
        assertThat(account.handle()).isEqualTo("tourist");
        assertThat(account.needCollect()).isTrue();
        assertThat(account.createdAt()).isEqualTo(NOW);
        assertThat(account.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void keepsExplicitNeedCollectFlag() {
        CodeforcesHandleAccount account = new CodeforcesHandleAccount(
                "112487张三",
                "tourist",
                false,
                NOW,
                NOW
        );

        assertThat(account.needCollect()).isFalse();
    }

    @Test
    void rejectsBlankRequiredFields() {
        assertThatThrownBy(() -> new CodeforcesHandleAccount(null, "tourist", NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("studentIdentity");
        assertThatThrownBy(() -> new CodeforcesHandleAccount("112487张三", " ", NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("handle");
        assertThatThrownBy(() -> new CodeforcesHandleAccount("112487张三", "tourist", null, NOW))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("createdAt");
    }
}
