package top.naccl.controller.player;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.model.dto.Comment;
import top.naccl.model.vo.Result;
import top.naccl.model.vo.PageComment;
import top.naccl.model.vo.PageResult;
import top.naccl.service.BlogService;
import top.naccl.service.CommentService;
import top.naccl.service.PlayerCommentService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
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
	@Autowired private PlayerCommentService playerCommentService;
	@Autowired private CommentService commentService;
	@Autowired private BlogService blogService;

	@GetMapping("/comments")
	public Result comments(@RequestParam Long blogId,
	                       @RequestParam(defaultValue = "1") Integer pageNum,
	                       @RequestParam(defaultValue = "10") Integer pageSize) {
		if (!Boolean.TRUE.equals(blogService.getPublishedByBlogId(blogId))
				|| !Boolean.TRUE.equals(blogService.getInternalByBlogId(blogId))) {
			return Result.create(404, "该博客不存在");
		}
		if (!Boolean.TRUE.equals(blogService.getCommentEnabledByBlogId(blogId))) {
			return Result.create(403, "评论已关闭");
		}
		Integer allComment = commentService.countByPageAndIsPublished(0, blogId, null);
		Integer openComment = commentService.countByPageAndIsPublished(0, blogId, true);
		PageHelper.startPage(pageNum, pageSize);
		PageInfo<PageComment> pageInfo = new PageInfo<>(commentService.getPageCommentList(0, blogId, -1L));
		Map<String, Object> data = new HashMap<>(4);
		data.put("allComment", allComment);
		data.put("closeComment", allComment - openComment);
		data.put("comments", new PageResult<>(pageInfo.getPages(), pageInfo.getList()));
		return Result.ok("获取成功", data);
	}

	@PostMapping("/comment")
	public Result comment(Authentication authentication, @RequestBody Comment comment, HttpServletRequest request) {
		boolean admin = authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_admin".equals(authority.getAuthority()));
		playerCommentService.create(authentication.getName(), admin, comment, IpAddressUtils.getIpAddress(request));
		return Result.ok("评论成功");
	}
}
