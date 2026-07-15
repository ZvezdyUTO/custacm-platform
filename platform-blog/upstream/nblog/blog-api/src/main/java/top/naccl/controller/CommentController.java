package top.naccl.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.controller.support.PageRequestValidator;
import top.naccl.enums.CommentOpenStateEnum;
import top.naccl.exception.ForbiddenException;
import top.naccl.exception.NotFoundException;
import top.naccl.model.vo.PageCommentPage;
import top.naccl.model.vo.PageResult;
import top.naccl.model.vo.Result;
import top.naccl.service.BlogService;
import top.naccl.service.CommentService;
import top.naccl.util.comment.CommentUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 公开文章评论查询。
 *
 * @author huangbingrui.awa
 */
@RestController
public class CommentController {
	private static final int BLOG_PAGE = 0;

	private final CommentService commentService;
	private final CommentUtils commentUtils;
	private final BlogService blogService;

	public CommentController(CommentService commentService, CommentUtils commentUtils, BlogService blogService) {
		this.commentService = commentService;
		this.commentUtils = commentUtils;
		this.blogService = blogService;
	}

	@GetMapping("/comments")
	public Result comments(@RequestParam Long blogId,
	                       @RequestParam(defaultValue = "1") Integer pageNum,
	                       @RequestParam(defaultValue = "10") Integer pageSize) {
		PageRequestValidator.validate(pageNum, pageSize);
		if (Boolean.TRUE.equals(blogService.getInternalByBlogId(blogId))) {
			throw new NotFoundException("该博客不存在");
		}
		CommentOpenStateEnum openState = commentUtils.judgeCommentState(BLOG_PAGE, blogId);
		if (openState == CommentOpenStateEnum.NOT_FOUND) {
			throw new NotFoundException("该博客不存在");
		}
		if (openState == CommentOpenStateEnum.CLOSE) {
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
}
