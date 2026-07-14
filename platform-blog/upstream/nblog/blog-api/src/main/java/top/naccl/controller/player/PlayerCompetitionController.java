package top.naccl.controller.player;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.model.dto.CompetitionAchievementOrderRequest;
import top.naccl.model.dto.CompetitionAchievementVisibilityRequest;
import top.naccl.model.vo.Result;
import top.naccl.service.CompetitionService;

/**
 * 参赛队员自行管理本人公开文章和个人名片奖项展示状态。
 *
 * @author huangbingrui.awa
 */
@RestController
@RequestMapping("/player/competitions")
public class PlayerCompetitionController {
	private final CompetitionService competitionService;

	public PlayerCompetitionController(CompetitionService competitionService) {
		this.competitionService = competitionService;
	}

	@PostMapping("/{competitionId}/articles/{blogId}")
	public Result bindArticle(Authentication authentication, @PathVariable Long competitionId,
			@PathVariable Long blogId) {
		competitionService.bindArticle(authentication.getName(), competitionId, blogId);
		return Result.ok("文章绑定成功");
	}

	@DeleteMapping("/{competitionId}/articles/{blogId}")
	public Result unbindArticle(Authentication authentication, @PathVariable Long competitionId,
			@PathVariable Long blogId) {
		competitionService.unbindArticle(authentication.getName(), competitionId, blogId);
		return Result.ok("文章解绑成功");
	}

	@PutMapping("/{competitionId}/awards/{awardId}/profile-visibility")
	public Result updateAchievementVisibility(Authentication authentication,
			@PathVariable Long competitionId, @PathVariable Long awardId,
			@RequestBody CompetitionAchievementVisibilityRequest request) {
		competitionService.updateAchievementVisibility(
				authentication.getName(), competitionId, awardId, request);
		return Result.ok("奖项展示状态更新成功");
	}

	@PutMapping("/achievement-order")
	public Result updateAchievementOrder(Authentication authentication,
			@RequestBody CompetitionAchievementOrderRequest request) {
		competitionService.updateAchievementOrder(authentication.getName(), request);
		return Result.ok("奖项展示顺序更新成功");
	}
}
