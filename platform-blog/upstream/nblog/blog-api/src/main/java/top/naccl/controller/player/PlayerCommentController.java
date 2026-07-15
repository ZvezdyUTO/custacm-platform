package top.naccl.controller.player;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.controller.support.PageRequestValidator;
import top.naccl.exception.ForbiddenException;
import top.naccl.exception.NotFoundException;
import top.naccl.model.dto.PlayerCommentCreateRequest;
import top.naccl.model.vo.PageCommentPage;
import top.naccl.model.vo.PageResult;
import top.naccl.model.vo.Result;
import top.naccl.service.BlogService;
import top.naccl.service.CommentService;
import top.naccl.service.PlayerCommentService;
import top.naccl.util.IpAddressUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author huangbingrui.awa
 */
@RestController
@RequestMapping("/player")
public class PlayerCommentController {
	private static final int BLOG_PAGE = 0;

	private final PlayerCommentService playerCommentService;
	private final CommentService commentService;
	private final BlogService blogService;

	public PlayerCommentController(PlayerCommentService playerCommentService,
	                               CommentService commentService,
	                               BlogService blogService) {
		this.playerCommentService = playerCommentService;
		this.commentService = commentService;
		this.blogService = blogService;
	}

	@GetMapping("/comments")
	public Result comments(@RequestParam Long blogId,
	                       @RequestParam(defaultValue = "1") Integer pageNum,
	                       @RequestParam(defaultValue = "10") Integer pageSize) {
		PageRequestValidator.validate(pageNum, pageSize);
		if (!Boolean.TRUE.equals(blogService.getPublishedByBlogId(blogId))
				|| !Boolean.TRUE.equals(blogService.getInternalByBlogId(blogId))) {
			throw new NotFoundException("该博客不存在");
		}
		if (!Boolean.TRUE.equals(blogService.getCommentEnabledByBlogId(blogId))) {
			throw new ForbiddenException("评论已关闭");
		}
		Integer allComment = commentService.countByPageAndIsPublished(BLOG_PAGE, blogId, null);
		Integer openComment = commentService.countByPageAndIsPublished(BLOG_PAGE, blogId, true);
		PageCommentPage commentPage = commentService.getPageComments(
				BLOG_PAGE, blogId, -1L, pageNum, pageSize);
		Map<String, Object> data = new HashMap<>(8);
		data.put("allComment", allComment);
		data.put("closeComment", allComment - openComment);
		data.put("comments", new PageResult<>(commentPage.totalPages(), commentPage.comments()));
		data.put("repliesTruncated", commentPage.repliesTruncated());
		return Result.ok("获取成功", data);
	}

	@PostMapping("/comment")
	public Result comment(Authentication authentication, @RequestBody PlayerCommentCreateRequest comment, HttpServletRequest request) {
		boolean admin = authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_admin".equals(authority.getAuthority()));
		playerCommentService.create(authentication.getName(), admin, comment, IpAddressUtils.getIpAddress(request));
		return Result.ok("评论成功");
	}
}
