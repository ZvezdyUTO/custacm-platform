package top.naccl.util.comment;

import org.springframework.stereotype.Component;
import top.naccl.enums.CommentOpenStateEnum;
import top.naccl.service.BlogService;

/**
 * 文章评论状态校验。
 *
 * @author huangbingrui.awa
 */
@Component
public class CommentUtils {
	private static final int BLOG_PAGE = 0;

	private final BlogService blogService;

	public CommentUtils(BlogService blogService) {
		this.blogService = blogService;
	}

	public CommentOpenStateEnum judgeCommentState(Integer page, Long blogId) {
		if (page == null || page != BLOG_PAGE) {
			return CommentOpenStateEnum.NOT_FOUND;
		}
		Boolean commentEnabled = blogService.getCommentEnabledByBlogId(blogId);
		Boolean published = blogService.getPublishedByBlogId(blogId);
		if (!Boolean.TRUE.equals(published)) {
			return CommentOpenStateEnum.NOT_FOUND;
		}
		return Boolean.TRUE.equals(commentEnabled)
				? CommentOpenStateEnum.OPEN
				: CommentOpenStateEnum.CLOSE;
	}
}
