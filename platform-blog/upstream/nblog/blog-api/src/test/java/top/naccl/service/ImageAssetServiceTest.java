package top.naccl.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import top.naccl.config.properties.UploadProperties;
import top.naccl.entity.ImageAsset;
import top.naccl.entity.User;
import top.naccl.exception.ImageAssetException;
import top.naccl.mapper.ImageAssetMapper;
import top.naccl.mapper.UserMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
@ExtendWith(MockitoExtension.class)
class ImageAssetServiceTest {
	@Mock private ImageAssetMapper assetMapper;
	@Mock private UserMapper userMapper;
	@Mock private ImageProcessingService processingService;
	@TempDir Path uploadDirectory;

	@Test
	void uploadWritesBothVariantsAndPersistsTemporaryAsset() {
		when(userMapper.findByUsername("player1")).thenReturn(user());
		when(processingService.process(any(), any())).thenReturn(
				new ImageProcessingService.ProcessedImage(new byte[]{1, 2}, new byte[]{3}, "jpg", 1200, 800, "image/jpeg"));
		doAnswer(invocation -> { ((ImageAsset) invocation.getArgument(0)).setId(9L); return 1; })
				.when(assetMapper).insert(any(ImageAsset.class));

		var response = service().upload("player1", new MockMultipartFile("file", new byte[]{1}),
				ImageAsset.Purpose.ARTICLE_CONTENT);

		assertEquals(9L, response.id());
		assertTrue(Files.exists(uploadDirectory.resolve("assets").resolve(response.publicId()).resolve("original.jpg")));
		assertTrue(Files.exists(uploadDirectory.resolve("assets").resolve(response.publicId()).resolve("thumbnail.jpg")));
	}

	@Test
	void deletingUnboundTemporaryAssetRemovesDirectoryAndRecord() throws Exception {
		ImageAsset asset = asset(4L, "abc", ImageAsset.Purpose.ARTICLE_CONTENT, ImageAsset.Status.TEMP);
		Path directory = uploadDirectory.resolve("assets/abc");
		Files.createDirectories(directory);
		Files.write(directory.resolve("original.jpg"), new byte[]{1});
		when(assetMapper.findById(4L)).thenReturn(asset);
		when(userMapper.findByUsername("player1")).thenReturn(user());
		when(assetMapper.findReferencedBlogId(4L)).thenReturn(null);

		service().deleteUnbound("player1", 4L);

		assertFalse(Files.exists(directory));
		verify(assetMapper).deleteById(4L);
	}

	@Test
	void rejectsManagedImageAlreadyBoundToAnotherArticle() {
		ImageAsset asset = asset(4L, "123e4567-e89b-12d3-a456-426614174000",
				ImageAsset.Purpose.ARTICLE_CONTENT, ImageAsset.Status.ACTIVE);
		when(assetMapper.findByPublicId(asset.getPublicId())).thenReturn(asset);
		when(assetMapper.findById(4L)).thenReturn(asset);
		when(assetMapper.findReferencedBlogId(4L)).thenReturn(77L);

		assertThrows(ImageAssetException.class, () -> service().prepareBlogAssets(1L, 88L, null,
				"![x](/api/image/assets/123e4567-e89b-12d3-a456-426614174000/thumbnail.jpg)"));
	}

	@Test
	void cleanupRemovesStaleTemporaryDirectoryLeftBeforeDatabaseInsert() throws Exception {
		Path temporaryDirectory = uploadDirectory.resolve("assets/.tmp-crashed-upload");
		Files.createDirectories(temporaryDirectory);
		Files.write(temporaryDirectory.resolve("original.jpg"), new byte[]{1});
		Files.setLastModifiedTime(temporaryDirectory, FileTime.from(Instant.now().minusSeconds(25 * 60 * 60)));
		when(assetMapper.findCleanupCandidates(any())).thenReturn(List.of());
		when(assetMapper.findAllPublicIds()).thenReturn(List.of());

		service().cleanupStaleAssets();

		assertFalse(Files.exists(temporaryDirectory));
	}

	@Test
	void userDeletionImmediatelyRemovesOnlyAssetsWithoutPreservedArticleReferences() {
		ImageAsset temporary = asset(4L, "temp", ImageAsset.Purpose.ARTICLE_CONTENT, ImageAsset.Status.TEMP);
		ImageAsset retained = asset(5L, "retained", ImageAsset.Purpose.ARTICLE_CONTENT, ImageAsset.Status.ACTIVE);
		when(assetMapper.findByOwnerUserId(1L)).thenReturn(List.of(temporary, retained));
		when(assetMapper.findReferencedBlogId(4L)).thenReturn(null);
		when(assetMapper.findReferencedBlogId(5L)).thenReturn(88L);

		service().prepareUserAssetDeletion(1L);

		verify(assetMapper).updateStatus(4L, ImageAsset.Status.DELETING.name());
		verify(assetMapper).deleteById(4L);
		verify(assetMapper, never()).updateStatus(5L, ImageAsset.Status.DELETING.name());
		verify(assetMapper, never()).deleteById(5L);
	}

	private ImageAssetService service() {
		UploadProperties properties = new UploadProperties();
		properties.setPath(uploadDirectory.toString());
		return new ImageAssetService(assetMapper, userMapper, processingService, properties);
	}

	private static User user() {
		User user = new User();
		user.setId(1L);
		user.setUsername("player1");
		return user;
	}

	private static ImageAsset asset(long id, String publicId, ImageAsset.Purpose purpose, ImageAsset.Status status) {
		ImageAsset asset = new ImageAsset();
		asset.setId(id);
		asset.setPublicId(publicId);
		asset.setOwnerUserId(1L);
		asset.setPurpose(purpose.name());
		asset.setStatus(status.name());
		asset.setOriginalPath("assets/" + publicId + "/original.jpg");
		asset.setThumbnailPath("assets/" + publicId + "/thumbnail.jpg");
		return asset;
	}
}
