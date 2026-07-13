package top.naccl.controller;

import org.junit.jupiter.api.Test;
import top.naccl.enums.CommentOpenStateEnum;
import top.naccl.model.vo.Result;
import top.naccl.service.BlogService;
import top.naccl.service.CommentService;
import top.naccl.util.comment.CommentUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
class CommentControllerTest {
	@Test
	void queriesOnlyArticleComments() {
		CommentService commentService = mock(CommentService.class);
		CommentUtils commentUtils = mock(CommentUtils.class);
		BlogService blogService = mock(BlogService.class);
		when(blogService.getInternalByBlogId(42L)).thenReturn(false);
		when(commentUtils.judgeCommentState(0, 42L)).thenReturn(CommentOpenStateEnum.OPEN);
		when(commentService.countByPageAndIsPublished(0, 42L, null)).thenReturn(2);
		when(commentService.countByPageAndIsPublished(0, 42L, true)).thenReturn(2);
		when(commentService.getPageCommentList(0, 42L, -1L)).thenReturn(List.of());
		CommentController controller = new CommentController(commentService, commentUtils, blogService);

		Result result = controller.comments(42L, 1, 10);

		assertEquals(200, result.getCode());
		verify(commentUtils).judgeCommentState(0, 42L);
		verify(commentService).getPageCommentList(0, 42L, -1L);
	}

	@Test
	void hidesInternalArticlesFromThePublicCommentEndpoint() {
		CommentService commentService = mock(CommentService.class);
		CommentUtils commentUtils = mock(CommentUtils.class);
		BlogService blogService = mock(BlogService.class);
		when(blogService.getInternalByBlogId(42L)).thenReturn(true);
		CommentController controller = new CommentController(commentService, commentUtils, blogService);

		Result result = controller.comments(42L, 1, 10);

		assertEquals(404, result.getCode());
	}
}
