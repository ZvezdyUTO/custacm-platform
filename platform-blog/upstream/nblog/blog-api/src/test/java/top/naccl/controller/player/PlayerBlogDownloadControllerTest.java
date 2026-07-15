package top.naccl.controller.player;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import top.naccl.entity.Blog;
import top.naccl.exception.BadRequestException;
import top.naccl.service.ArticleArchiveService;
import top.naccl.service.ArticleDownloadService;
import top.naccl.service.PlayerBlogService;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
@ExtendWith(MockitoExtension.class)
class PlayerBlogDownloadControllerTest {
	@Mock private ArticleDownloadService articleDownloadService;
	@Mock private ArticleArchiveService articleArchiveService;
	@Mock private PlayerBlogService playerBlogService;
	@InjectMocks private PlayerBlogController controller;

	@Test
	void downloadsAnArticleArchiveWithASafeFilenameForAPlayer() throws Exception {
		Blog blog = article(9L, "训练/A", "# 题解\n你好");
		TestingAuthenticationToken authentication = new TestingAuthenticationToken(
				"player1", null, "ROLE_player");
		when(articleDownloadService.download("player1", false, 9L)).thenReturn(blog);

		ResponseEntity<StreamingResponseBody> response = controller.download(authentication, 9L);

		assertEquals(200, response.getStatusCode().value());
		assertTrue(response.getHeaders().getContentType().isCompatibleWith(MediaType.parseMediaType("application/zip")));
		assertEquals("训练_A.zip", response.getHeaders().getContentDisposition().getFilename());
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		response.getBody().writeTo(output);
		verify(articleDownloadService).download("player1", false, 9L);
		verify(articleArchiveService).writeSingleArticle(blog, output);
	}

	@Test
	void capsAPathologicalUtf8TitleBeforeBuildingTheResponseHeader() {
		Blog blog = article(9L, "题".repeat(255), "content");
		TestingAuthenticationToken authentication = new TestingAuthenticationToken(
				"player1", null, "ROLE_player");
		when(articleDownloadService.download("player1", false, 9L)).thenReturn(blog);

		ResponseEntity<StreamingResponseBody> response = controller.download(authentication, 9L);

		assertEquals("题".repeat(80) + "-9.zip", response.getHeaders().getContentDisposition().getFilename());
		assertTrue(response.getHeaders().getFirst("Content-Disposition").length() < 4096);
	}

	@Test
	void passesTheAdministratorExemptionToTheService() {
		Blog blog = article(9L, "题解", "content");
		TestingAuthenticationToken authentication = new TestingAuthenticationToken(
				"root", null, "ROLE_admin");
		when(articleDownloadService.download("root", true, 9L)).thenReturn(blog);

		controller.download(authentication, 9L);

		verify(articleDownloadService).download("root", true, 9L);
	}

	@Test
	void movesAndRestoresOnlyForTheAuthenticatedPlayer() {
		TestingAuthenticationToken authentication = new TestingAuthenticationToken(
				"player1", null, "ROLE_player");

		controller.delete(authentication, 9L);
		controller.restore(authentication, 9L);

		verify(playerBlogService).delete("player1", 9L);
		verify(playerBlogService).restore("player1", 9L);
	}

	@Test
	void rejectsUnboundedActiveAndRecycleBinPagesBeforeServiceAccess() {
		TestingAuthenticationToken authentication = new TestingAuthenticationToken(
				"player1", null, "ROLE_player");

		assertThrows(BadRequestException.class,
				() -> controller.blogs(authentication, "", null, 1, 101));
		assertThrows(BadRequestException.class,
				() -> controller.recycleBin(authentication, "", null, 0, 10));
		verifyNoInteractions(playerBlogService);
	}

	private static Blog article(Long id, String title, String content) {
		Blog blog = new Blog();
		blog.setId(id);
		blog.setTitle(title);
		blog.setContent(content);
		return blog;
	}
}
