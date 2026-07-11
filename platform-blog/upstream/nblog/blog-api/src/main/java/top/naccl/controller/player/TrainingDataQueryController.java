package top.naccl.controller.player;

import com.custacm.platform.trainingdata.common.app.query.OjAcceptedSummaryQueryService;
import com.custacm.platform.trainingdata.common.app.query.OjFirstAcceptedProblemQueryService;
import com.custacm.platform.trainingdata.common.app.query.OjSubmissionQueryService;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import com.custacm.platform.trainingdata.common.web.query.OjWarehouseQueryController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.model.vo.Result;
import top.naccl.service.TrainingUserQueryService;

@RestController
@RequestMapping("/player/training-data")
public class TrainingDataQueryController {
    private final OjWarehouseQueryController delegate;
    private final TrainingUserQueryService trainingUserQueryService;

    public TrainingDataQueryController(
            OjAcceptedSummaryQueryService acceptedSummaryQueryService,
            OjSubmissionQueryService submissionQueryService,
            OjFirstAcceptedProblemQueryService firstAcceptedProblemQueryService,
            TrainingUserQueryService trainingUserQueryService
    ) {
        this.delegate = new OjWarehouseQueryController(
                acceptedSummaryQueryService, submissionQueryService, firstAcceptedProblemQueryService);
        this.trainingUserQueryService = trainingUserQueryService;
    }

    @GetMapping("/users")
    public Result users() {
        return Result.ok("获取成功", trainingUserQueryService.listCollectableUsers());
    }

    @GetMapping("/accepted-summary")
    public Result acceptedSummary(
            @RequestParam(value = "ojName", defaultValue = OjNames.CODEFORCES) String ojName,
            @RequestParam String username,
            @RequestParam(required = false) String acceptedFromDateUtcPlus8,
            @RequestParam(required = false) String acceptedToDateUtcPlus8,
            @RequestParam(required = false) Integer minProblemRating,
            @RequestParam(required = false) Integer maxProblemRating
    ) {
        return Result.ok("获取成功", delegate.summarizeAcceptedProblems(
                ojName, username, acceptedFromDateUtcPlus8, acceptedToDateUtcPlus8,
                minProblemRating, maxProblemRating).getBody());
    }

    @GetMapping("/submissions/by-user")
    public Result submissionsByUser(
            @RequestParam(value = "ojName", defaultValue = OjNames.CODEFORCES) String ojName,
            @RequestParam String username,
            @RequestParam(required = false) String submittedFromUtcPlus8,
            @RequestParam(required = false) String submittedToUtcPlus8,
            @RequestParam(required = false) Integer minProblemRating,
            @RequestParam(required = false) Integer maxProblemRating,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.ok("获取成功", delegate.listStudentSubmissions(
                ojName, username, submittedFromUtcPlus8, submittedToUtcPlus8,
                minProblemRating, maxProblemRating, page, limit).getBody());
    }

    @GetMapping("/submissions/by-problem")
    public Result submissionsByProblem(
            @RequestParam(value = "ojName", defaultValue = OjNames.CODEFORCES) String ojName,
            @RequestParam String problemKey,
            @RequestParam(required = false) String submittedFromUtcPlus8,
            @RequestParam(required = false) String submittedToUtcPlus8,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.ok("获取成功", delegate.listProblemSubmissions(
                ojName, problemKey, submittedFromUtcPlus8, submittedToUtcPlus8, page, limit).getBody());
    }

    @GetMapping("/first-accepted/by-user")
    public Result firstAcceptedByUser(
            @RequestParam(value = "ojName", defaultValue = OjNames.CODEFORCES) String ojName,
            @RequestParam String username,
            @RequestParam(required = false) String firstAcceptedFromUtcPlus8,
            @RequestParam(required = false) String firstAcceptedToUtcPlus8,
            @RequestParam(required = false) Integer minProblemRating,
            @RequestParam(required = false) Integer maxProblemRating,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.ok("获取成功", delegate.summarizeStudentFirstAcceptedProblems(
                ojName, username, firstAcceptedFromUtcPlus8, firstAcceptedToUtcPlus8,
                minProblemRating, maxProblemRating, page, limit).getBody());
    }

    @GetMapping("/first-accepted/by-problem")
    public Result firstAcceptedByProblem(
            @RequestParam(value = "ojName", defaultValue = OjNames.CODEFORCES) String ojName,
            @RequestParam String problemKey,
            @RequestParam(required = false) String firstAcceptedFromUtcPlus8,
            @RequestParam(required = false) String firstAcceptedToUtcPlus8,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.ok("获取成功", delegate.summarizeProblemFirstAcceptedHandles(
                ojName, problemKey, firstAcceptedFromUtcPlus8, firstAcceptedToUtcPlus8, page, limit).getBody());
    }
}
