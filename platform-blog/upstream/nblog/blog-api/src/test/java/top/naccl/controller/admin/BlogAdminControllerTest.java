package top.naccl.controller.admin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import top.naccl.service.ArticleArchiveService;
import top.naccl.service.BlogService;
import top.naccl.service.ArticleRecycleBinService;
import top.naccl.service.CategoryService;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.verify;

/**
 * @author huangbingrui.awa
 */
@ExtendWith(MockitoExtension.class)
class BlogAdminControllerTest {
	@Mock private BlogService blogService;
	@Mock private CategoryService categoryService;
	@Mock private ArticleRecycleBinService recycleBinService;
	@Mock private ArticleArchiveService articleArchiveService;
	@InjectMocks private BlogAdminController controller;

	@Test
	void movesDeletedArticleToTheSevenDayRecycleBin() {
		controller.delete(42L);

		verify(recycleBinService).moveToRecycleBin(42L);
	}

	@Test
	void restoresArticleThroughTheRecycleBinService() {
		controller.restore(42L);

		verify(recycleBinService).restore(42L);
	}

	@Test
	void streamsACompleteArticleBackupWithAShortAsciiFilename() throws Exception {
		ResponseEntity<StreamingResponseBody> response = controller.backup();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		response.getBody().writeTo(output);

		assertTrue(response.getHeaders().getContentType().isCompatibleWith(MediaType.parseMediaType("application/zip")));
		assertTrue(response.getHeaders().getContentDisposition().getFilename()
				.matches("custacm-article-backup-\\d{8}-\\d{6}\\.zip"));
		verify(articleArchiveService).writeAllArticlesBackup(output);
	}
}
