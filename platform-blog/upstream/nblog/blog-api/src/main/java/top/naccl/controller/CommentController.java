package top.naccl.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.enums.CommentOpenStateEnum;
import top.naccl.model.vo.PageComment;
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
		if (Boolean.TRUE.equals(blogService.getInternalByBlogId(blogId))) {
			return Result.create(404, "该博客不存在");
		}
		CommentOpenStateEnum openState = commentUtils.judgeCommentState(BLOG_PAGE, blogId);
		if (openState == CommentOpenStateEnum.NOT_FOUND) {
			return Result.create(404, "该博客不存在");
		}
		if (openState == CommentOpenStateEnum.CLOSE) {
			return Result.create(403, "评论已关闭");
		}
		Integer allComment = commentService.countByPageAndIsPublished(BLOG_PAGE, blogId, null);
		Integer openComment = commentService.countByPageAndIsPublished(BLOG_PAGE, blogId, true);
		PageHelper.startPage(pageNum, pageSize);
		PageInfo<PageComment> pageInfo = new PageInfo<>(
				commentService.getPageCommentList(BLOG_PAGE, blogId, -1L));
		Map<String, Object> data = new HashMap<>(4);
		data.put("allComment", allComment);
		data.put("closeComment", allComment - openComment);
		data.put("comments", new PageResult<>(pageInfo.getPages(), pageInfo.getList()));
		return Result.ok("获取成功", data);
	}
}
