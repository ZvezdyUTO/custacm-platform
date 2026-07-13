package top.naccl.service;

import org.junit.jupiter.api.Test;
import top.naccl.entity.Blog;
import top.naccl.exception.NotFoundException;
import top.naccl.mapper.BlogMapper;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
class ArticleDownloadServiceTest {
	private final BlogMapper blogMapper = mock(BlogMapper.class);
	private final ArticleDownloadRateLimiter rateLimiter = mock(ArticleDownloadRateLimiter.class);
	private final ArticleDownloadService service = new ArticleDownloadService(blogMapper, rateLimiter);

	@Test
	void limitsAPlayerAfterFindingThePublishedArticle() {
		Blog blog = new Blog();
		when(blogMapper.getPublishedBlogForDownload(9L)).thenReturn(blog);

		assertSame(blog, service.download("player1", false, 9L));

		verify(rateLimiter).acquire("player1");
	}

	@Test
	void letsAnAdministratorDownloadWithoutTouchingTheLimiter() {
		Blog blog = new Blog();
		when(blogMapper.getPublishedBlogForDownload(9L)).thenReturn(blog);

		assertSame(blog, service.download("root", true, 9L));

		verify(rateLimiter, never()).acquire("root");
	}

	@Test
	void doesNotConsumeAWindowForAnUnknownOrDraftArticle() {
		when(blogMapper.getPublishedBlogForDownload(9L)).thenReturn(null);

		assertThrows(NotFoundException.class, () -> service.download("player1", false, 9L));

		verifyNoInteractions(rateLimiter);
	}
}
