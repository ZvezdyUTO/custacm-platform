package top.naccl.controller.admin;

import com.custacm.platform.trainingdata.codeforces.app.CodeforcesOdsSubmissionIngestService;
import com.custacm.platform.trainingdata.common.app.warehouse.OjWarehouseRefreshService;
import com.custacm.platform.trainingdata.common.collector.job.OjSubmissionCollectionJobService;
import com.custacm.platform.trainingdata.common.scheduler.OjScheduledSubmissionCollectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import top.naccl.handler.ControllerExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class TrainingDataAdminControllerTest {
    private static final String REFRESH_PATH = "/admin/training-data/codeforces/warehouse:refresh";

    @Mock
    private OjScheduledSubmissionCollectionService collectionService;
    @Mock
    private OjSubmissionCollectionJobService jobService;
    @Mock
    private CodeforcesOdsSubmissionIngestService codeforcesIngestService;
    @Mock
    private OjWarehouseRefreshService codeforcesRefreshService;
    @Mock
    private OjWarehouseRefreshService atcoderRefreshService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = mockMvc(codeforcesRefreshService);
    }

    @Test
    void nullBatchIdRefreshesLatestBatch() throws Exception {
        mockMvc.perform(post(REFRESH_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"batchId":null,"startFromTaskId":" example.dws.daily_summary "}
                                """))
                .andExpect(status().isOk());

        verify(codeforcesRefreshService).refreshLatest(" example.dws.daily_summary ");
        verify(codeforcesRefreshService, never()).refresh(any(), any());
    }

    @Test
    void blankBatchIdRefreshesLatestBatch() throws Exception {
        mockMvc.perform(post(REFRESH_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"batchId":"   ","startFromTaskId":null}
                                """))
                .andExpect(status().isOk());

        verify(codeforcesRefreshService).refreshLatest(null);
        verify(codeforcesRefreshService, never()).refresh(any(), any());
    }

    @Test
    void explicitBatchIdUsesStrictRefresh() throws Exception {
        mockMvc.perform(post(REFRESH_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"batchId":"batch-42","startFromTaskId":"example.dwm.problem_first_accepted"}
                                """))
                .andExpect(status().isOk());

        verify(codeforcesRefreshService).refresh("batch-42", "example.dwm.problem_first_accepted");
    }

    @Test
    void missingLatestBatchIsBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("没有可刷新的最新批次"))
                .when(codeforcesRefreshService).refreshLatest(null);

        mockMvc.perform(post(REFRESH_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"batchId":null,"startFromTaskId":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
    }

    @Test
    void missingExplicitBatchIsBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("批次不存在"))
                .when(codeforcesRefreshService).refresh("missing-batch", null);

        mockMvc.perform(post(REFRESH_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"batchId":"missing-batch","startFromTaskId":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
    }

    private MockMvc mockMvc(OjWarehouseRefreshService refreshService) {
        TrainingDataAdminController controller = new TrainingDataAdminController(
                collectionService,
                jobService,
                codeforcesIngestService,
                refreshService,
                atcoderRefreshService
        );
        return standaloneSetup(controller)
                .setControllerAdvice(new ControllerExceptionHandler())
                .build();
    }
}
