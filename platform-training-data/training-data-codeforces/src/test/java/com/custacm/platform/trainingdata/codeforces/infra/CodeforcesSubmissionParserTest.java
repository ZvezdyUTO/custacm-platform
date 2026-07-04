package com.custacm.platform.trainingdata.codeforces.infra;

import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesCollectBatch;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CodeforcesSubmissionParserTest {
    private static final String MULTI_USER_CODEFORCES_ARRAY_FIXTURE =
            "fixtures/codeforces/submissions_multi_user_1000.json";

    @Test
    void parsesCodeforcesFixtureIntoOdsRecords() throws Exception {
        String fixture = new ClassPathResource("fixtures/codeforces/submissions_tourist.json")
                .getContentAsString(StandardCharsets.UTF_8);
        CodeforcesCollectBatch batch = new CodeforcesCollectBatch("batch-1", Instant.parse("2026-06-27T00:00:00Z"));

        var records = new CodeforcesSubmissionParser(new ObjectMapper()).parse(fixture, batch);

        assertThat(records).hasSize(2);
        assertThat(records.get(0).codeforcesSubmissionId()).isEqualTo(379398914L);
        assertThat(records.get(0).authorHandle()).isEqualTo("tourist");
        assertThat(records.get(0).contestId()).isEqualTo(2237L);
        assertThat(records.get(0).creationTimeSeconds()).isEqualTo(1781798091L);
        assertThat(records.get(0).relativeTimeSeconds()).isEqualTo(4791);
        assertThat(records.get(0).problemName()).isEqualTo("Send GCDs");
        assertThat(records.get(0).testset()).isEqualTo("TESTS");
        assertThat(records.get(0).passedTestCount()).isEqualTo(10);
        assertThat(records.get(0).rawPayload()).contains("\"id\":379398914");
        assertThat(records.get(0).payloadHash()).hasSize(64);
        assertThat(records.get(1).verdict()).isEqualTo("WRONG_ANSWER");
    }

    @Test
    void parsesRawCodeforcesSubmissionArray() throws Exception {
        String fixture = new ClassPathResource("fixtures/codeforces/submissions_tourist.json")
                .getContentAsString(StandardCharsets.UTF_8);
        String array = new ObjectMapper().readTree(fixture).path("result").toString();
        CodeforcesCollectBatch batch = new CodeforcesCollectBatch("batch-1", Instant.parse("2026-06-27T00:00:00Z"));

        var records = new CodeforcesSubmissionParser(new ObjectMapper()).parse(array, batch);

        assertThat(records).hasSize(2);
        assertThat(records.get(0).codeforcesSubmissionId()).isEqualTo(379398914L);
        assertThat(records.get(0).authorHandle()).isEqualTo("tourist");
    }

    @Test
    void parsesLargeMultiUserCodeforcesArrayFixtureForHttpIngest() throws Exception {
        String fixture = new ClassPathResource(MULTI_USER_CODEFORCES_ARRAY_FIXTURE)
                .getContentAsString(StandardCharsets.UTF_8);
        CodeforcesCollectBatch batch = new CodeforcesCollectBatch("batch-1", Instant.parse("2026-07-03T00:00:00Z"));

        var records = new CodeforcesSubmissionParser(new ObjectMapper()).parse(fixture, batch);

        assertThat(records).hasSize(1000);
        assertThat(records.getFirst().codeforcesSubmissionId()).isEqualTo(380351477L);
        assertThat(records.getFirst().authorHandle()).isEqualTo("tourist");
        assertThat(records.getFirst().problemName()).isEqualTo("Hunting the Beast");
        assertThat(records.getLast().codeforcesSubmissionId()).isEqualTo(298818342L);
        assertThat(records.getLast().authorHandle()).isEqualTo("jiangly");
        assertThat(records.getFirst().payloadHash()).hasSize(64);
    }
}
