package top.naccl.controller.player;

import org.junit.jupiter.api.Test;
import top.naccl.exception.BadRequestException;
import top.naccl.exception.ForbiddenException;
import top.naccl.exception.NotFoundException;
import top.naccl.model.vo.Result;
import top.naccl.model.vo.PageCommentPage;
import top.naccl.service.BlogService;
import top.naccl.service.CommentService;
import top.naccl.service.PlayerCommentService;

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
class PlayerCommentControllerTest {
	@Test
	void queriesCommentsForPublishedInternalArticles() {
		PlayerCommentService playerCommentService = mock(PlayerCommentService.class);
		CommentService commentService = mock(CommentService.class);
		BlogService blogService = mock(BlogService.class);
		when(blogService.getPublishedByBlogId(42L)).thenReturn(true);
		when(blogService.getInternalByBlogId(42L)).thenReturn(true);
		when(blogService.getCommentEnabledByBlogId(42L)).thenReturn(true);
		when(commentService.countByPageAndIsPublished(0, 42L, null)).thenReturn(2);
		when(commentService.countByPageAndIsPublished(0, 42L, true)).thenReturn(2);
		when(commentService.getPageComments(0, 42L, -1L, 1, 10))
				.thenReturn(new PageCommentPage(0, List.of(), false));
		PlayerCommentController controller = new PlayerCommentController(
				playerCommentService, commentService, blogService);

		Result result = controller.comments(42L, 1, 10);

		assertEquals(200, result.getCode());
		verify(commentService).getPageComments(0, 42L, -1L, 1, 10);
	}

	@Test
	void validatesPageBoundsBeforeAccessingTheArticle() {
		PlayerCommentService playerCommentService = mock(PlayerCommentService.class);
		CommentService commentService = mock(CommentService.class);
		BlogService blogService = mock(BlogService.class);
		PlayerCommentController controller = new PlayerCommentController(
				playerCommentService, commentService, blogService);

		assertThrows(BadRequestException.class, () -> controller.comments(42L, 0, 10));
		assertThrows(BadRequestException.class, () -> controller.comments(42L, 1, 101));
		verifyNoInteractions(blogService, commentService, playerCommentService);
	}

	@Test
	void hidesArticlesOutsideThePlayerCommentScope() {
		PlayerCommentService playerCommentService = mock(PlayerCommentService.class);
		CommentService commentService = mock(CommentService.class);
		BlogService blogService = mock(BlogService.class);
		when(blogService.getPublishedByBlogId(42L)).thenReturn(true);
		when(blogService.getInternalByBlogId(42L)).thenReturn(false);
		PlayerCommentController controller = new PlayerCommentController(
				playerCommentService, commentService, blogService);

		assertThrows(NotFoundException.class, () -> controller.comments(42L, 1, 10));
		verifyNoInteractions(commentService, playerCommentService);
	}

	@Test
	void rejectsClosedCommentsWithAForbiddenException() {
		PlayerCommentService playerCommentService = mock(PlayerCommentService.class);
		CommentService commentService = mock(CommentService.class);
		BlogService blogService = mock(BlogService.class);
		when(blogService.getPublishedByBlogId(42L)).thenReturn(true);
		when(blogService.getInternalByBlogId(42L)).thenReturn(true);
		when(blogService.getCommentEnabledByBlogId(42L)).thenReturn(false);
		PlayerCommentController controller = new PlayerCommentController(
				playerCommentService, commentService, blogService);

		assertThrows(ForbiddenException.class, () -> controller.comments(42L, 1, 10));
		verifyNoInteractions(commentService, playerCommentService);
	}
}
