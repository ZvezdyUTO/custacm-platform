package top.naccl.controller.player;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.model.dto.Comment;
import top.naccl.model.vo.Result;
import top.naccl.service.PlayerCommentService;
import top.naccl.util.IpAddressUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @author huangbingrui.awa
 */
@RestController
@RequestMapping("/player")
public class PlayerCommentController {
	@Autowired private PlayerCommentService playerCommentService;

	@PostMapping("/comment")
	public Result comment(Authentication authentication, @RequestBody Comment comment, HttpServletRequest request) {
		boolean admin = authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_admin".equals(authority.getAuthority()));
		playerCommentService.create(authentication.getName(), admin, comment, IpAddressUtils.getIpAddress(request));
		return Result.ok("评论成功");
	}
}
