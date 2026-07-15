package top.naccl.controller;

import org.junit.jupiter.api.Test;
import top.naccl.enums.CommentOpenStateEnum;
import top.naccl.exception.BadRequestException;
import top.naccl.exception.ForbiddenException;
import top.naccl.exception.NotFoundException;
import top.naccl.model.vo.Result;
import top.naccl.model.vo.PageCommentPage;
import top.naccl.service.BlogService;
import top.naccl.service.CommentService;
import top.naccl.util.comment.CommentUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
		when(commentService.getPageComments(0, 42L, -1L, 1, 10))
				.thenReturn(new PageCommentPage(0, List.of(), false));
		CommentController controller = new CommentController(commentService, commentUtils, blogService);

		Result result = controller.comments(42L, 1, 10);

		assertEquals(200, result.getCode());
		verify(commentUtils).judgeCommentState(0, 42L);
		verify(commentService).getPageComments(0, 42L, -1L, 1, 10);
	}

	@Test
	void hidesInternalArticlesFromThePublicCommentEndpoint() {
		CommentService commentService = mock(CommentService.class);
		CommentUtils commentUtils = mock(CommentUtils.class);
		BlogService blogService = mock(BlogService.class);
		when(blogService.getInternalByBlogId(42L)).thenReturn(true);
		CommentController controller = new CommentController(commentService, commentUtils, blogService);

		assertThrows(NotFoundException.class, () -> controller.comments(42L, 1, 10));
		verifyNoInteractions(commentUtils, commentService);
	}

	@Test
	void rejectsClosedCommentsWithAForbiddenException() {
		CommentService commentService = mock(CommentService.class);
		CommentUtils commentUtils = mock(CommentUtils.class);
		BlogService blogService = mock(BlogService.class);
		when(blogService.getInternalByBlogId(42L)).thenReturn(false);
		when(commentUtils.judgeCommentState(0, 42L)).thenReturn(CommentOpenStateEnum.CLOSE);
		CommentController controller = new CommentController(commentService, commentUtils, blogService);

		assertThrows(ForbiddenException.class, () -> controller.comments(42L, 1, 10));
		verifyNoInteractions(commentService);
	}

	@Test
	void validatesPageBoundsBeforeAccessingTheArticle() {
		CommentService commentService = mock(CommentService.class);
		CommentUtils commentUtils = mock(CommentUtils.class);
		BlogService blogService = mock(BlogService.class);
		CommentController controller = new CommentController(commentService, commentUtils, blogService);

		assertThrows(BadRequestException.class, () -> controller.comments(42L, 0, 10));
		assertThrows(BadRequestException.class, () -> controller.comments(42L, 1, 101));
		verifyNoInteractions(blogService, commentUtils, commentService);
	}
}
