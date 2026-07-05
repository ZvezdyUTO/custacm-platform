package com.custacm.platform.common.sqltask;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SqlScriptSplitterTest {
    @Test
    void keepsSemicolonsInsideQuotedTextAndComments() {
        String script = """
                -- comment with ;
                insert into demo(note) values ('a;b');
                insert into demo(note) values ("c;d");
                /* block ; comment */
                insert into demo(note) values ('escaped '' ; quote');
                """;

        assertThat(SqlScriptSplitter.split(script))
                .hasSize(3)
                .allSatisfy(statement -> assertThat(statement).contains("insert into demo"));
    }
}
