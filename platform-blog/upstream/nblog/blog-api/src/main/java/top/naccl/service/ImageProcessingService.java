package top.naccl.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.naccl.entity.ImageAsset;
import top.naccl.exception.ImageAssetException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import static top.naccl.exception.ImageAssetException.ErrorCode.IMAGE_DIMENSIONS_INVALID;
import static top.naccl.exception.ImageAssetException.ErrorCode.IMAGE_FORMAT_UNSUPPORTED;
import static top.naccl.exception.ImageAssetException.ErrorCode.IMAGE_PROCESSING_FAILED;
import static top.naccl.exception.ImageAssetException.ErrorCode.IMAGE_TOO_LARGE;

/**
 * 对用户图片做真实格式校验、方向纠正、归一化和缩略图生成。
 *
 * @author huangbingrui.awa
 */
@Service
public class ImageProcessingService {
	static final long CONTENT_MAX_BYTES = 15L * 1024 * 1024;
	static final long COVER_MAX_BYTES = 10L * 1024 * 1024;
	static final long AVATAR_MAX_BYTES = 2L * 1024 * 1024;
	static final int MAX_EDGE = 8192;
	static final long MAX_PIXELS = 25_000_000L;

	public ProcessedImage process(MultipartFile file, ImageAsset.Purpose purpose) {
		validateFile(file, maxBytes(purpose));
		try {
			byte[] source = file.getBytes();
			ImageMetadata metadata = readMetadata(source);
			validateDimensions(metadata, purpose);
			BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(source));
			if (decoded == null) {
				throw new ImageAssetException(IMAGE_FORMAT_UNSUPPORTED, "仅支持 JPEG 或 PNG 图片");
			}
			String outputFormat = metadata.png() && decoded.getColorModel().hasAlpha() ? "png" : "jpg";
			int originalLimit = purpose == ImageAsset.Purpose.ARTICLE_CONTENT ? 2560
					: purpose == ImageAsset.Purpose.ARTICLE_COVER ? 1920 : 512;
			int thumbnailLimit = purpose == ImageAsset.Purpose.ARTICLE_CONTENT ? 960
					: purpose == ImageAsset.Purpose.ARTICLE_COVER ? 640 : 96;
			byte[] original = resize(source, metadata, originalLimit, outputFormat, 0.88);
			byte[] thumbnail = resize(source, metadata, thumbnailLimit, outputFormat, 0.80);
			BufferedImage normalized = ImageIO.read(new ByteArrayInputStream(original));
			return new ProcessedImage(original, thumbnail, outputFormat,
					normalized.getWidth(), normalized.getHeight(), "png".equals(outputFormat) ? "image/png" : "image/jpeg");
		} catch (ImageAssetException exception) {
			throw exception;
		} catch (IOException | RuntimeException exception) {
			throw new ImageAssetException(IMAGE_PROCESSING_FAILED, "图片处理失败", exception);
		}
	}

	private static void validateFile(MultipartFile file, long maxBytes) {
		if (file == null || file.isEmpty()) {
			throw new ImageAssetException(IMAGE_FORMAT_UNSUPPORTED, "请选择 JPEG 或 PNG 图片");
		}
		if (file.getSize() > maxBytes) {
			throw new ImageAssetException(IMAGE_TOO_LARGE, "图片文件过大");
		}
	}

	private static long maxBytes(ImageAsset.Purpose purpose) {
		return switch (purpose) {
			case ARTICLE_CONTENT -> CONTENT_MAX_BYTES;
			case ARTICLE_COVER -> COVER_MAX_BYTES;
			case AVATAR -> AVATAR_MAX_BYTES;
		};
	}

	private static ImageMetadata readMetadata(byte[] source) throws IOException {
		try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if (!readers.hasNext()) {
				throw new ImageAssetException(IMAGE_FORMAT_UNSUPPORTED, "仅支持 JPEG 或 PNG 图片");
			}
			ImageReader reader = readers.next();
			try {
				reader.setInput(input, true, true);
				String format = reader.getFormatName().toLowerCase(Locale.ROOT);
				if (!format.equals("jpeg") && !format.equals("jpg") && !format.equals("png")) {
					throw new ImageAssetException(IMAGE_FORMAT_UNSUPPORTED, "仅支持 JPEG 或 PNG 图片");
				}
				return new ImageMetadata(reader.getWidth(0), reader.getHeight(0), format.equals("png"));
			} finally {
				reader.dispose();
			}
		}
	}

	private static void validateDimensions(ImageMetadata metadata, ImageAsset.Purpose purpose) {
		if (metadata.width() <= 0 || metadata.height() <= 0 || metadata.width() > MAX_EDGE
				|| metadata.height() > MAX_EDGE || (long) metadata.width() * metadata.height() > MAX_PIXELS) {
			throw new ImageAssetException(IMAGE_DIMENSIONS_INVALID, "图片尺寸过大或无效");
		}
		if (purpose == ImageAsset.Purpose.ARTICLE_COVER
				&& (metadata.width() != 1920 || metadata.height() != 1080)) {
			throw new ImageAssetException(IMAGE_DIMENSIONS_INVALID, "文章首图必须为 1920×1080");
		}
		if (purpose == ImageAsset.Purpose.AVATAR
				&& (metadata.width() != 512 || metadata.height() != 512 || !metadata.png())) {
			throw new ImageAssetException(IMAGE_DIMENSIONS_INVALID, "头像必须为 512×512 PNG");
		}
	}

	private static byte[] resize(byte[] source, ImageMetadata metadata, int limit, String format, double quality)
			throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		Thumbnails.Builder<? extends java.io.InputStream> builder = Thumbnails.of(new ByteArrayInputStream(source))
				.useExifOrientation(true);
		if (metadata.width() <= limit && metadata.height() <= limit) {
			builder.scale(1.0);
		} else {
			builder.size(limit, limit).keepAspectRatio(true);
		}
		builder.outputFormat(format);
		if ("jpg".equals(format)) {
			builder.outputQuality(quality);
		}
		builder.toOutputStream(output);
		return output.toByteArray();
	}

	private record ImageMetadata(int width, int height, boolean png) {
	}

	public record ProcessedImage(byte[] original, byte[] thumbnail, String extension,
			int width, int height, String mimeType) {
	}
}
