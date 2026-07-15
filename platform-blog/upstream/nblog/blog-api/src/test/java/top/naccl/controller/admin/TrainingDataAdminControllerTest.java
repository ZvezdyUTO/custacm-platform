package top.naccl.controller.admin;

import com.custacm.platform.trainingdata.common.collector.job.OjSubmissionCollectionJobService;
import com.custacm.platform.trainingdata.common.collector.job.OjSubmissionCollectionJobSnapshot;
import com.custacm.platform.trainingdata.common.collector.job.OjSubmissionCollectionJobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import top.naccl.handler.ControllerExceptionHandler;

import java.time.Duration;
import java.time.Instant;
import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * @author huangbingrui.awa
 */
@ExtendWith(MockitoExtension.class)
class TrainingDataAdminControllerTest {
    @Mock
    private OjSubmissionCollectionJobService jobService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new TrainingDataAdminController(jobService))
                .setControllerAdvice(new ControllerExceptionHandler())
                .build();
    }

    @Test
    void startsTheRetainedBatchCollectionJob() throws Exception {
        when(jobService.startBatchCollection(
                List.of("alice"), Duration.ofHours(24), true, "CODEFORCES"))
                .thenReturn(job("job-1"));

        mockMvc.perform(post("/admin/training-data/submission-collection-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernames":["alice"],
                                  "lookbackHours":24,
                                  "refreshWarehouse":true,
                                  "ojName":"CODEFORCES"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobId").value("job-1"));

        verify(jobService).startBatchCollection(
                List.of("alice"), Duration.ofHours(24), true, "CODEFORCES");
    }

    @Test
    void invalidJobRequestIsRejectedBeforeCallingTheJobService() throws Exception {
        mockMvc.perform(post("/admin/training-data/submission-collection-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernames":[" "],
                                  "lookbackHours":0,
                                  "ojName":"unsupported"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));

        verifyNoInteractions(jobService);
    }

    @Test
    void listsRetainedCollectionJobs() throws Exception {
        when(jobService.listJobs()).thenReturn(List.of(job("job-1")));

        mockMvc.perform(get("/admin/training-data/submission-collection-jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].jobId").value("job-1"));
    }

    @Test
    void missingCollectionJobReturnsNotFound() throws Exception {
        when(jobService.getJob("missing")).thenThrow(new NoSuchElementException("missing"));

        mockMvc.perform(get("/admin/training-data/submission-collection-jobs/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void blankCollectionJobIdIsRejectedAtTheHttpBoundary() throws Exception {
        mockMvc.perform(get(URI.create("/admin/training-data/submission-collection-jobs/%20")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));

        verifyNoInteractions(jobService);
    }

    private static OjSubmissionCollectionJobSnapshot job(String jobId) {
        return new OjSubmissionCollectionJobSnapshot(
                jobId,
                "CODEFORCES",
                OjSubmissionCollectionJobStatus.RUNNING,
                1,
                0,
                0,
                0,
                0,
                0,
                List.of(),
                Instant.EPOCH,
                null,
                "采集任务已创建",
                List.of()
        );
    }
}
