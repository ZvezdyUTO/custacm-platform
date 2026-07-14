package top.naccl.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.model.vo.Result;
import top.naccl.service.CompetitionService;

/**
 * 公开比赛记录查询。
 *
 * @author huangbingrui.awa
 */
@RestController
@RequestMapping("/competitions")
public class CompetitionController {
	private final CompetitionService competitionService;

	public CompetitionController(CompetitionService competitionService) {
		this.competitionService = competitionService;
	}

	@GetMapping
	public Result list(
			@RequestParam(required = false) Integer startYear,
			@RequestParam(required = false) Integer endYear,
			@RequestParam(required = false) String category,
			@RequestParam(defaultValue = "1") Integer pageNum,
			@RequestParam(defaultValue = "10") Integer pageSize
	) {
		return Result.ok("获取成功", competitionService.list(startYear, endYear, category, pageNum, pageSize));
	}

	@GetMapping("/{id}")
	public Result get(@PathVariable Long id) {
		return Result.ok("获取成功", competitionService.get(id));
	}
}
