package top.naccl.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import top.naccl.entity.ImageAsset;
import top.naccl.exception.ImageAssetException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author huangbingrui.awa
 */
class ImageProcessingServiceTest {
	private final ImageProcessingService service = new ImageProcessingService();

	@Test
	void normalizesLargeContentAndCreatesSmallerThumbnail() throws Exception {
		MockMultipartFile file = image("photo.jpg", "image/jpeg", 3000, 1000, BufferedImage.TYPE_INT_RGB, "jpg");
		var result = service.process(file, ImageAsset.Purpose.ARTICLE_CONTENT);

		BufferedImage original = ImageIO.read(new ByteArrayInputStream(result.original()));
		BufferedImage thumbnail = ImageIO.read(new ByteArrayInputStream(result.thumbnail()));
		assertEquals(2560, original.getWidth());
		assertEquals(960, thumbnail.getWidth());
		assertEquals("image/jpeg", result.mimeType());
		assertTrue(result.thumbnail().length < result.original().length);
	}

	@Test
	void preservesTransparentPng() throws Exception {
		MockMultipartFile file = image("diagram.png", "image/png", 1200, 600, BufferedImage.TYPE_INT_ARGB, "png");
		var result = service.process(file, ImageAsset.Purpose.ARTICLE_CONTENT);
		assertEquals("png", result.extension());
		assertEquals("image/png", result.mimeType());
		assertTrue(ImageIO.read(new ByteArrayInputStream(result.original())).getColorModel().hasAlpha());
		assertTrue(ImageIO.read(new ByteArrayInputStream(result.thumbnail())).getColorModel().hasAlpha());
	}

	@Test
	void preservesExifOrientationInBothVariantsAndReportedDimensions() throws Exception {
		byte[] jpeg = image("rotated.jpg", "image/jpeg", 1200, 600,
				BufferedImage.TYPE_INT_RGB, "jpg").getBytes();
		byte[] exif = {
				(byte) 0xff, (byte) 0xe1, 0, 34,
				'E', 'x', 'i', 'f', 0, 0,
				'I', 'I', 42, 0, 8, 0, 0, 0,
				1, 0, 0x12, 1, 3, 0, 1, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0
		};
		ByteArrayOutputStream source = new ByteArrayOutputStream();
		source.write(jpeg, 0, 2);
		source.write(exif);
		source.write(jpeg, 2, jpeg.length - 2);

		var result = service.process(new MockMultipartFile(
				"file", "rotated.jpg", "image/jpeg", source.toByteArray()), ImageAsset.Purpose.ARTICLE_CONTENT);

		BufferedImage original = ImageIO.read(new ByteArrayInputStream(result.original()));
		BufferedImage thumbnail = ImageIO.read(new ByteArrayInputStream(result.thumbnail()));
		assertEquals(600, original.getWidth());
		assertEquals(1200, original.getHeight());
		assertEquals(original.getWidth(), result.width());
		assertEquals(original.getHeight(), result.height());
		assertEquals(480, thumbnail.getWidth());
		assertEquals(960, thumbnail.getHeight());
	}

	@Test
	void rejectsCoverThatWasNotCropped() throws Exception {
		MockMultipartFile file = image("cover.jpg", "image/jpeg", 1600, 900, BufferedImage.TYPE_INT_RGB, "jpg");
		ImageAssetException error = assertThrows(ImageAssetException.class,
				() -> service.process(file, ImageAsset.Purpose.ARTICLE_COVER));
		assertEquals(ImageAssetException.ErrorCode.IMAGE_DIMENSIONS_INVALID, error.errorCode());
	}

	@Test
	void rejectsUnsupportedBytesEvenWithImageContentType() {
		ImageAssetException error = assertThrows(ImageAssetException.class, () -> service.process(
				new MockMultipartFile("file", "fake.png", "image/png", new byte[]{1, 2, 3}),
				ImageAsset.Purpose.ARTICLE_CONTENT));
		assertEquals(ImageAssetException.ErrorCode.IMAGE_FORMAT_UNSUPPORTED, error.errorCode());
	}

	private static MockMultipartFile image(String name, String mime, int width, int height, int type, String format)
			throws Exception {
		BufferedImage image = new BufferedImage(width, height, type);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(image, format, output);
		return new MockMultipartFile("file", name, mime, output.toByteArray());
	}
}
