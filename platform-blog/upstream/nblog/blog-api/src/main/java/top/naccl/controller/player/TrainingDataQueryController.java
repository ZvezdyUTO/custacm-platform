package top.naccl.controller.player;

import com.custacm.platform.trainingdata.common.app.query.OjWarehouseQueryFacade;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.model.vo.Result;
import top.naccl.service.TrainingUserQueryService;

@RestController
@RequestMapping("/player/training-data")
public class TrainingDataQueryController {
    private final OjWarehouseQueryFacade queryFacade;
    private final TrainingUserQueryService trainingUserQueryService;

    public TrainingDataQueryController(
            OjWarehouseQueryFacade queryFacade,
            TrainingUserQueryService trainingUserQueryService
    ) {
        this.queryFacade = queryFacade;
        this.trainingUserQueryService = trainingUserQueryService;
    }

    @GetMapping("/users")
    public Result users(@RequestParam(value = "includeRetired", defaultValue = "false") boolean includeRetired) {
        return Result.ok("获取成功", trainingUserQueryService.listUsers(includeRetired));
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
        return Result.ok("获取成功", queryFacade.summarizeAcceptedProblems(
                ojName, username, acceptedFromDateUtcPlus8, acceptedToDateUtcPlus8,
                minProblemRating, maxProblemRating));
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
        return Result.ok("获取成功", queryFacade.listStudentSubmissions(
                ojName, username, submittedFromUtcPlus8, submittedToUtcPlus8,
                minProblemRating, maxProblemRating, page, limit));
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
        return Result.ok("获取成功", queryFacade.listProblemSubmissions(
                ojName, problemKey, submittedFromUtcPlus8, submittedToUtcPlus8, page, limit));
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
        return Result.ok("获取成功", queryFacade.summarizeStudentFirstAcceptedProblems(
                ojName, username, firstAcceptedFromUtcPlus8, firstAcceptedToUtcPlus8,
                minProblemRating, maxProblemRating, page, limit));
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
        return Result.ok("获取成功", queryFacade.summarizeProblemFirstAcceptedHandles(
                ojName, problemKey, firstAcceptedFromUtcPlus8, firstAcceptedToUtcPlus8, page, limit));
    }
}
