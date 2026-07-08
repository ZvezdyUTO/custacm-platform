package com.custacm.platform.trainingdata.common.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextsTest {
    @Test
    void requireTextReturnsTrimmedText() {
        assertThat(Texts.requireText("  handle  ", "handle")).isEqualTo("handle");
    }

    @Test
    void requireTextRejectsNullAndBlankWithDefaultException() {
        assertThatThrownBy(() -> Texts.requireText(null, "studentIdentity"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("studentIdentity must not be blank");

        assertThatThrownBy(() -> Texts.requireText(" ", "studentIdentity"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("studentIdentity must not be blank");
    }

    @Test
    void requireTextRejectsNullWithCustomException() {
        assertThatThrownBy(() -> Texts.requireText(null, "ojName", TestRequestException::new))
                .isInstanceOf(TestRequestException.class)
                .hasMessage("ojName must not be blank");
    }

    private static final class TestRequestException extends RuntimeException {
        private TestRequestException(String message) {
            super(message);
        }
    }
}
