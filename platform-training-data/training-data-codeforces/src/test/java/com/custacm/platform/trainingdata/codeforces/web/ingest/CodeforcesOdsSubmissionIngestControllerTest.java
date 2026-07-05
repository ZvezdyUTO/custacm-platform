package com.custacm.platform.trainingdata.codeforces.web.ingest;

import com.custacm.platform.trainingdata.codeforces.app.ingest.CodeforcesOdsSubmissionIngestService;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesOdsSubmissionWriter;
import com.custacm.platform.trainingdata.codeforces.infra.parser.JacksonCodeforcesSubmissionParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CodeforcesOdsSubmissionIngestControllerTest {
    private static final Instant FIXED_NOW = Instant.parse("2026-06-27T00:00:00Z");
    private static final String MULTI_USER_CODEFORCES_ARRAY_FIXTURE =
            "fixtures/codeforces/submissions_multi_user_1000.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CodeforcesOdsSubmissionWriter writer = mock(CodeforcesOdsSubmissionWriter.class);
    private final CodeforcesOdsSubmissionIngestService ingestService = new CodeforcesOdsSubmissionIngestService(
            new JacksonCodeforcesSubmissionParser(objectMapper),
            writer,
            objectMapper,
            Clock.fixed(FIXED_NOW, ZoneOffset.ofHours(8))
    );
    private final CodeforcesOdsSubmissionIngestController controller = new CodeforcesOdsSubmissionIngestController(
            ingestService
    );

    @Test
    void upsertsCodeforcesSubmissionArray() throws Exception {
        String fixture = new ClassPathResource(MULTI_USER_CODEFORCES_ARRAY_FIXTURE)
                .getContentAsString(StandardCharsets.UTF_8);

        var response = controller.upsertSubmissions(objectMapper.readTree(fixture));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().tableName()).isEqualTo("ods_codeforces__submission");
        assertThat(response.getBody().writtenRows()).isEqualTo(1000);
        assertThat(response.getBody().batchId()).startsWith("external-codeforces-");
        assertThat(response.getBody().fetchedAt()).isEqualTo(FIXED_NOW);
        verify(writer).upsertBatch(any(), any());
    }

    @Test
    void rejectsNonArrayRequestBody() throws Exception {
        assertThatThrownBy(() -> controller.upsertSubmissions(objectMapper.readTree("{}")))
                .hasMessageContaining("400 BAD_REQUEST")
                .hasMessageContaining("JSON array");
    }
}
