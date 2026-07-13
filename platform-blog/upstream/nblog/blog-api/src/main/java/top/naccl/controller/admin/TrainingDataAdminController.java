package top.naccl.controller.admin;

import com.custacm.platform.trainingdata.common.collector.job.OjSubmissionCollectionJobService;
import com.custacm.platform.trainingdata.common.web.collector.request.OjSubmissionCollectionJobStartRequest;
import com.custacm.platform.trainingdata.common.web.collector.response.OjSubmissionCollectionJobResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.exception.BadRequestException;
import top.naccl.model.vo.Result;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/admin/training-data")
public class TrainingDataAdminController {
    private final OjSubmissionCollectionJobService jobService;

    public TrainingDataAdminController(OjSubmissionCollectionJobService jobService) {
        this.jobService = jobService;
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
}
