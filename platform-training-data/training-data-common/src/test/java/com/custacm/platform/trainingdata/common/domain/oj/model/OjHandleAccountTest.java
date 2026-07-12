package com.custacm.platform.trainingdata.common.domain.oj.model;

import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OjHandleAccountTest {
    private static final Instant NOW = Instant.parse("2026-07-05T00:00:00Z");

    @Test
    void keepsUsernameHandleAndTimestamps() {
        OjHandleAccount account = new OjHandleAccount(
                "112487张三",
                Map.of(OjNames.CODEFORCES, "tourist"),
                true,
                NOW,
                NOW
        );

        assertThat(account.username()).isEqualTo("112487张三");
        assertThat(account.handles()).containsExactlyEntriesOf(Map.of(OjNames.CODEFORCES, "tourist"));
        assertThat(account.needCollect()).isTrue();
        assertThat(account.collectionStates()).containsKey(OjNames.CODEFORCES);
        assertThat(account.collectionStates().get(OjNames.CODEFORCES).historyStartReached()).isFalse();
        assertThat(account.collectionStates().get(OjNames.CODEFORCES).lastCollectedAt()).isNull();
        assertThat(account.createdAt()).isEqualTo(NOW);
        assertThat(account.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void keepsOptionalAtcoderHandleAndExplicitNeedCollectFlag() {
        OjHandleAccount account = new OjHandleAccount(
                "112487张三",
                Map.of(OjNames.CODEFORCES, "tourist", OjNames.ATCODER, "  tourist_atcoder "),
                false,
                NOW,
                NOW
        );

        assertThat(account.handles()).containsEntry(OjNames.CODEFORCES, "tourist");
        assertThat(account.handles()).containsEntry(OjNames.ATCODER, "tourist_atcoder");
        assertThat(account.needCollect()).isFalse();
    }

    @Test
    void keepsCollectionEligibilityBeforeAnOjHandleIsBound() {
        OjHandleAccount account = new OjHandleAccount("112487张三", Map.of(), false, NOW, NOW);

        assertThat(account.handles()).isEmpty();
        assertThat(account.collectionStates()).isEmpty();
        assertThat(account.needCollect()).isFalse();
    }

    @Test
    void normalizesOjNamesToUppercaseConstants() {
        OjHandleAccount account = new OjHandleAccount(
                "112487张三",
                Map.of("codeforces", " tourist ", "atcoder", " tourist_atcoder "),
                true,
                Map.of(
                        "codeforces",
                        new OjHandleCollectionState(true, Instant.parse("2026-07-04T00:00:00Z"))
                ),
                NOW,
                NOW
        );

        assertThat(account.handles()).containsEntry(OjNames.CODEFORCES, "tourist");
        assertThat(account.handles()).containsEntry(OjNames.ATCODER, "tourist_atcoder");
        assertThat(account.collectionStates().get(OjNames.CODEFORCES).historyStartReached()).isTrue();
        assertThat(account.collectionStates().get(OjNames.ATCODER).historyStartReached()).isFalse();
    }

    @Test
    void rejectsBlankRequiredFields() {
        assertThatThrownBy(() -> new OjHandleAccount(null, Map.of(OjNames.CODEFORCES, "tourist"), true, NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username");
        assertThatThrownBy(() -> new OjHandleAccount("112487张三", Map.of(OjNames.CODEFORCES, " "), true, NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("handles");
        assertThatThrownBy(() -> new OjHandleAccount("112487张三", Map.of("unknown", "tourist"), true, NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported oj name");
        assertThatThrownBy(() -> new OjHandleAccount(
                "112487张三",
                Map.of(OjNames.CODEFORCES, "tourist"),
                true,
                null,
                NOW
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("createdAt");
    }
}
