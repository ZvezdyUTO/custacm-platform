package top.naccl.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import top.naccl.exception.ArticleDownloadRateLimitException;
import top.naccl.exception.ArticleDownloadRateLimitUnavailableException;
import top.naccl.model.vo.Result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
class ArticleDownloadExceptionHandlerTest {
	private final ControllerExceptionHandler handler = new ControllerExceptionHandler();
	private final HttpServletRequest request = mock(HttpServletRequest.class);

	@Test
	void mapsRepeatedDownloadsTo429WithRetryAfter() {
		when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/player/blog/download"));

		ResponseEntity<Result> response = handler.articleDownloadRateLimitExceptionHandler(
				request, new ArticleDownloadRateLimitException(17));

		assertEquals(429, response.getStatusCode().value());
		assertEquals("17", response.getHeaders().getFirst("Retry-After"));
		assertEquals("ARTICLE_DOWNLOAD_RATE_LIMITED", response.getBody().getErrorCode());
	}

	@Test
	void mapsAnUnavailableLimiterTo503() {
		when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/player/blog/download"));

		ResponseEntity<Result> response = handler.articleDownloadRateLimitUnavailableExceptionHandler(
				request, new ArticleDownloadRateLimitUnavailableException(new IllegalStateException("redis")));

		assertEquals(503, response.getStatusCode().value());
		assertEquals("ARTICLE_DOWNLOAD_RATE_LIMIT_UNAVAILABLE", response.getBody().getErrorCode());
	}
}
