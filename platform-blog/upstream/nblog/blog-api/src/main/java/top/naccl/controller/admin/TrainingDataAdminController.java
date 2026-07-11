package top.naccl.controller.admin;

import com.custacm.platform.common.sqltask.SqlTaskExecutionResult;
import com.custacm.platform.trainingdata.codeforces.app.CodeforcesOdsSubmissionIngestService;
import com.custacm.platform.trainingdata.common.app.warehouse.OjWarehouseRefreshService;
import com.custacm.platform.trainingdata.common.collector.job.OjSubmissionCollectionJobService;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import com.custacm.platform.trainingdata.common.scheduler.OjScheduledSubmissionCollectionService;
import com.custacm.platform.trainingdata.common.web.collector.request.OjSubmissionCollectionJobStartRequest;
import com.custacm.platform.trainingdata.common.web.collector.request.OjSubmissionCollectionRequest;
import com.custacm.platform.trainingdata.common.web.collector.response.OjSubmissionCollectionJobResponse;
import com.custacm.platform.trainingdata.common.web.collector.response.OjSubmissionCollectionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.exception.BadRequestException;
import top.naccl.model.dto.WarehouseRefreshRequest;
import top.naccl.model.vo.Result;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/admin/training-data")
public class TrainingDataAdminController {
    private final OjScheduledSubmissionCollectionService collectionService;
    private final OjSubmissionCollectionJobService jobService;
    private final CodeforcesOdsSubmissionIngestService codeforcesIngestService;
    private final Map<String, OjWarehouseRefreshService> refreshServices;

    public TrainingDataAdminController(
            OjScheduledSubmissionCollectionService collectionService,
            OjSubmissionCollectionJobService jobService,
            CodeforcesOdsSubmissionIngestService codeforcesIngestService,
            @Qualifier("codeforcesWarehouseRefreshService") OjWarehouseRefreshService codeforcesRefreshService,
            @Qualifier("atcoderWarehouseRefreshService") OjWarehouseRefreshService atcoderRefreshService
    ) {
        this.collectionService = collectionService;
        this.jobService = jobService;
        this.codeforcesIngestService = codeforcesIngestService;
        this.refreshServices = Map.of(
                OjNames.CODEFORCES, codeforcesRefreshService,
                OjNames.ATCODER, atcoderRefreshService
        );
    }

    @PostMapping("/submissions:collect")
    public Result collect(@RequestBody OjSubmissionCollectionRequest request) throws JsonProcessingException {
        if (request == null) {
            throw new BadRequestException("请求体不能为空");
        }
        return Result.ok("采集成功", OjSubmissionCollectionResponse.from(
                collectionService.collectRecentWindowForUsername(
                        request.optionalOjName(), request.requireUsername(), request.requireLookbackDuration())));
    }

    @PostMapping("/submission-collection-jobs")
    public Result startJob(@RequestBody OjSubmissionCollectionJobStartRequest request) {
        if (request == null) {
            throw new BadRequestException("请求体不能为空");
        }
        return Result.ok("任务已创建", OjSubmissionCollectionJobResponse.from(
                jobService.startBatchCollection(
                        request.requireUsernames(), request.requireLookbackDuration(),
                        request.refreshWarehouseOrDefault(), request.optionalOjName())));
    }

    @GetMapping("/submission-collection-jobs")
    public Result listJobs() {
        List<OjSubmissionCollectionJobResponse> jobs = jobService.listJobs().stream()
                .map(OjSubmissionCollectionJobResponse::from).toList();
        return Result.ok("获取成功", jobs);
    }

    @GetMapping("/submission-collection-jobs/{jobId}")
    public Result getJob(@PathVariable String jobId) {
        try {
            return Result.ok("获取成功", OjSubmissionCollectionJobResponse.from(jobService.getJob(jobId)));
        } catch (NoSuchElementException ex) {
            throw new top.naccl.exception.NotFoundException("采集任务不存在");
        }
    }

    @PostMapping("/ods/codeforces/submissions:batch-upsert")
    public Result batchUpsertCodeforces(@RequestBody JsonNode submissions) throws JsonProcessingException {
        return Result.ok("写入成功", codeforcesIngestService.upsertSubmissions(submissions));
    }

    @PostMapping("/{ojName}/warehouse:refresh")
    public Result refreshWarehouse(@PathVariable String ojName, @RequestBody WarehouseRefreshRequest request) {
        String normalizedOjName;
        try {
            normalizedOjName = OjNames.normalize(ojName);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }
        OjWarehouseRefreshService service = refreshServices.get(normalizedOjName);
        if (service == null || request == null) {
            throw new BadRequestException("不支持的 OJ 或请求体为空");
        }
        SqlTaskExecutionResult result;
        try {
            result = request.batchId() == null || request.batchId().isBlank()
                    ? service.refreshLatest(request.startFromTaskId())
                    : service.refresh(request.batchId(), request.startFromTaskId());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
        return Result.ok("刷新成功", result);
    }
}
