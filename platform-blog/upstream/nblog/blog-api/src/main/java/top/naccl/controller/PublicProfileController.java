package top.naccl.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.model.vo.Result;
import top.naccl.service.PlayerProfileService;

/**
 * @author huangbingrui.awa
 */
@RestController
@RequestMapping("/profiles")
public class PublicProfileController {
	private final PlayerProfileService playerProfileService;

	public PublicProfileController(PlayerProfileService playerProfileService) {
		this.playerProfileService = playerProfileService;
	}

	@GetMapping("/{username}")
	public Result get(@PathVariable String username, Authentication authentication) {
		boolean authenticated = authentication != null
				&& !(authentication instanceof AnonymousAuthenticationToken);
		return Result.ok("获取成功", playerProfileService.getPublic(username, authenticated));
	}
}
