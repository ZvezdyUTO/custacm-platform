package top.naccl.controller.player;

import com.custacm.platform.trainingdata.common.app.query.OjWarehouseQueryFacade;
import com.custacm.platform.trainingdata.common.app.query.result.OjAcceptedSummaryReport;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import org.junit.jupiter.api.Test;
import top.naccl.service.TrainingUserQueryService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
class TrainingDataBatchQueryControllerTest {
    @Test
    void delegatesMultiUserSummaryToOneBatchUseCase() {
        OjWarehouseQueryFacade facade = mock(OjWarehouseQueryFacade.class);
        var expected = List.of(new OjAcceptedSummaryReport("alice", "tourist", 0, List.of()));
        when(facade.summarizeAcceptedProblems(
                OjNames.CODEFORCES, true, "2026-07-01", "2026-07-07", null, 2400))
                .thenReturn(expected);
        TrainingDataQueryController controller = new TrainingDataQueryController(
                facade, mock(TrainingUserQueryService.class));

        var result = controller.acceptedSummaries(
                OjNames.CODEFORCES, true, "2026-07-01", "2026-07-07", null, 2400);

        assertThat(result.getData()).isEqualTo(expected);
        verify(facade).summarizeAcceptedProblems(
                OjNames.CODEFORCES, true, "2026-07-01", "2026-07-07", null, 2400);
    }
}
