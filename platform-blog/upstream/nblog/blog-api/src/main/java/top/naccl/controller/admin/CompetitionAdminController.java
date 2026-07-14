package top.naccl.controller.admin;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.model.dto.CompetitionAwardCreateRequest;
import top.naccl.model.dto.CompetitionCreateRequest;
import top.naccl.model.dto.CompetitionParticipantsCreateRequest;
import top.naccl.model.vo.Result;
import top.naccl.service.CompetitionService;

/**
 * 比赛、参赛队员和奖项的管理员添加/删除入口。
 *
 * @author huangbingrui.awa
 */
@RestController
@RequestMapping("/admin/competitions")
public class CompetitionAdminController {
	private final CompetitionService competitionService;

	public CompetitionAdminController(CompetitionService competitionService) {
		this.competitionService = competitionService;
	}

	@PostMapping
	public Result create(@RequestBody CompetitionCreateRequest request) {
		return Result.ok("创建成功", competitionService.create(request));
	}

	@GetMapping("/recycle-bin")
	public Result recycleBin(
			@RequestParam(required = false) Integer startYear,
			@RequestParam(required = false) Integer endYear,
			@RequestParam(required = false) String category,
			@RequestParam(defaultValue = "1") Integer pageNum,
			@RequestParam(defaultValue = "10") Integer pageSize
	) {
		return Result.ok("获取成功",
				competitionService.listRecycleBin(startYear, endYear, category, pageNum, pageSize));
	}

	@DeleteMapping("/{id}")
	public Result delete(@PathVariable Long id) {
		competitionService.moveToRecycleBin(id);
		return Result.ok("已移入回收站");
	}

	@PutMapping("/{id}/restore")
	public Result restore(@PathVariable Long id) {
		return Result.ok("恢复成功", competitionService.restore(id));
	}

	@PostMapping("/{id}/participants")
	public Result addParticipants(@PathVariable Long id,
			@RequestBody CompetitionParticipantsCreateRequest request) {
		return Result.ok("参赛队员添加成功", competitionService.addParticipants(id, request));
	}

	@DeleteMapping("/{id}/participants/{participantId}")
	public Result deleteParticipant(@PathVariable Long id, @PathVariable Long participantId) {
		competitionService.deleteParticipant(id, participantId);
		return Result.ok("参赛队员删除成功");
	}

	@PostMapping("/{id}/awards")
	public Result addAward(@PathVariable Long id, @RequestBody CompetitionAwardCreateRequest request) {
		return Result.ok("奖项添加成功", competitionService.addAward(id, request));
	}

	@DeleteMapping("/{id}/awards/{awardId}")
	public Result deleteAward(@PathVariable Long id, @PathVariable Long awardId) {
		competitionService.deleteAward(id, awardId);
		return Result.ok("奖项删除成功");
	}
}
