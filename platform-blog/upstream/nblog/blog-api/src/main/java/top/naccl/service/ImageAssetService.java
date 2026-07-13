package top.naccl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import top.naccl.config.properties.UploadProperties;
import top.naccl.entity.ImageAsset;
import top.naccl.entity.User;
import top.naccl.exception.ImageAssetException;
import top.naccl.exception.NotFoundException;
import top.naccl.mapper.ImageAssetMapper;
import top.naccl.mapper.UserMapper;
import top.naccl.model.vo.ImageAssetResponse;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static top.naccl.exception.ImageAssetException.ErrorCode.IMAGE_NOT_OWNED;
import static top.naccl.exception.ImageAssetException.ErrorCode.IMAGE_PROCESSING_FAILED;

/**
 * 图片资产的上传、文章绑定和零垃圾回收入口。
 *
 * @author huangbingrui.awa
 */
@Service
public class ImageAssetService {
	private static final Logger log = LoggerFactory.getLogger(ImageAssetService.class);
	private static final Pattern MANAGED_IMAGE = Pattern.compile(
			"/api/image/assets/([0-9a-fA-F-]{36})/(?:original|thumbnail)\\.(?:jpg|png)");
	private static final Duration TEMP_RETENTION = Duration.ofHours(24);

	private final ImageAssetMapper assetMapper;
	private final UserMapper userMapper;
	private final ImageProcessingService processingService;
	private final UploadProperties uploadProperties;

	public ImageAssetService(ImageAssetMapper assetMapper, UserMapper userMapper,
			ImageProcessingService processingService, UploadProperties uploadProperties) {
		this.assetMapper = assetMapper;
		this.userMapper = userMapper;
		this.processingService = processingService;
		this.uploadProperties = uploadProperties;
	}

	@Transactional
	public ImageAssetResponse upload(String username, MultipartFile file, ImageAsset.Purpose purpose) {
		if (purpose == ImageAsset.Purpose.AVATAR) {
			throw new ImageAssetException(IMAGE_NOT_OWNED, "头像请使用本人头像接口上传");
		}
		User user = requireUser(username);
		return new ImageAssetResponse(store(user.getId(), file, purpose));
	}

	@Transactional
	public ImageAsset storeAvatar(Long userId, MultipartFile file) {
		return store(userId, file, ImageAsset.Purpose.AVATAR);
	}

	private ImageAsset store(Long userId, MultipartFile file, ImageAsset.Purpose purpose) {
		ImageProcessingService.ProcessedImage processed = processingService.process(file, purpose);
		String publicId = UUID.randomUUID().toString();
		Path assetsRoot = assetsRoot();
		Path temporaryDirectory = assetsRoot.resolve(".tmp-" + publicId).normalize();
		Path finalDirectory = assetsRoot.resolve(publicId).normalize();
		ensureWithinAssets(temporaryDirectory);
		ensureWithinAssets(finalDirectory);
		String originalName = "original." + processed.extension();
		String thumbnailName = "thumbnail." + processed.extension();
		try {
			Files.createDirectories(assetsRoot);
			Files.createDirectory(temporaryDirectory);
			Files.write(temporaryDirectory.resolve(originalName), processed.original(), StandardOpenOption.CREATE_NEW);
			Files.write(temporaryDirectory.resolve(thumbnailName), processed.thumbnail(), StandardOpenOption.CREATE_NEW);
			moveDirectory(temporaryDirectory, finalDirectory);
		} catch (IOException exception) {
			deleteDirectoryQuietly(temporaryDirectory);
			deleteDirectoryQuietly(finalDirectory);
			throw new ImageAssetException(IMAGE_PROCESSING_FAILED, "图片保存失败", exception);
		}

		ImageAsset asset = new ImageAsset();
		asset.setPublicId(publicId);
		asset.setOwnerUserId(userId);
		asset.setPurpose(purpose.name());
		asset.setOriginalPath("assets/" + publicId + "/" + originalName);
		asset.setThumbnailPath("assets/" + publicId + "/" + thumbnailName);
		asset.setOriginalUrl("/api/image/" + asset.getOriginalPath());
		asset.setThumbnailUrl("/api/image/" + asset.getThumbnailPath());
		asset.setMimeType(processed.mimeType());
		asset.setWidth(processed.width());
		asset.setHeight(processed.height());
		asset.setOriginalBytes((long) processed.original().length);
		asset.setThumbnailBytes((long) processed.thumbnail().length);
		asset.setStatus(ImageAsset.Status.TEMP.name());
		try {
			if (assetMapper.insert(asset) != 1) {
				throw new ImageAssetException(IMAGE_PROCESSING_FAILED, "图片元数据保存失败");
			}
		} catch (RuntimeException exception) {
			deleteDirectoryQuietly(finalDirectory);
			throw exception;
		}
		registerRollbackCleanup(finalDirectory);
		return asset;
	}

	public PreparedBlogAssets prepareBlogAssets(Long ownerUserId, Long blogId, Long coverAssetId, String content) {
		ImageAsset cover = coverAssetId == null ? null
				: validateBindableAsset(ownerUserId, blogId, assetMapper.findByIdWithReference(coverAssetId),
						ImageAsset.Purpose.ARTICLE_COVER);
		Set<String> referencedPublicIds = new LinkedHashSet<>();
		Matcher matcher = MANAGED_IMAGE.matcher(content == null ? "" : content);
		while (matcher.find()) {
			referencedPublicIds.add(matcher.group(1));
		}
		Map<String, ImageAsset> assetsByPublicId = referencedPublicIds.isEmpty() ? Map.of()
				: assetMapper.findByPublicIdsWithReference(List.copyOf(referencedPublicIds)).stream()
				.collect(java.util.stream.Collectors.toMap(ImageAsset::getPublicId, asset -> asset));
		Map<Long, ImageAsset> contentAssets = new LinkedHashMap<>();
		for (String publicId : referencedPublicIds) {
			ImageAsset asset = assetsByPublicId.get(publicId);
			if (asset == null) {
				throw new ImageAssetException(IMAGE_NOT_OWNED, "正文引用的托管图片不存在");
			}
			ImageAsset validated = validateBindableAsset(ownerUserId, blogId, asset,
					ImageAsset.Purpose.ARTICLE_CONTENT);
			contentAssets.put(validated.getId(), validated);
		}
		List<ImageAsset> previous = blogId == null ? List.of() : assetMapper.findByBlogId(blogId);
		return new PreparedBlogAssets(cover, List.copyOf(contentAssets.values()), previous);
	}

	@Transactional
	public void bindBlogAssets(Long blogId, PreparedBlogAssets prepared) {
		assetMapper.deleteReferencesByBlogId(blogId);
		Set<Long> retained = new HashSet<>();
		if (prepared.cover() != null) {
			bind(blogId, prepared.cover(), "COVER");
			retained.add(prepared.cover().getId());
		}
		for (ImageAsset asset : prepared.content()) {
			bind(blogId, asset, "CONTENT");
			retained.add(asset.getId());
		}
		List<ImageAsset> removed = prepared.previous().stream()
				.filter(asset -> !retained.contains(asset.getId()))
				.toList();
		markDeletingAfterCommit(removed);
	}

	@Transactional
	public void prepareBlogDeletion(Long blogId) {
		markDeletingAfterCommit(assetMapper.findByBlogId(blogId));
	}

	@Transactional
	public void replaceAvatar(ImageAsset current, ImageAsset replacement) {
		assetMapper.updateStatus(replacement.getId(), ImageAsset.Status.ACTIVE.name());
		if (current != null) {
			markDeletingAfterCommit(List.of(current));
		}
	}

	@Transactional
	public void prepareUserAssetDeletion(Long ownerUserId) {
		if (ownerUserId == null) {
			return;
		}
		markDeletingAfterCommit(assetMapper.findUnreferencedByOwnerUserId(ownerUserId));
	}

	@Transactional
	public void deleteUnbound(String username, Long assetId) {
		ImageAsset asset = assetMapper.findById(assetId);
		if (asset == null) {
			return;
		}
		User user = requireUser(username);
		if (!user.getId().equals(asset.getOwnerUserId())) {
			throw new ImageAssetException(IMAGE_NOT_OWNED, "图片不属于当前用户");
		}
		if (assetMapper.findReferencedBlogId(assetId) != null || ImageAsset.Status.ACTIVE.name().equals(asset.getStatus())) {
			throw new ImageAssetException(IMAGE_NOT_OWNED, "已绑定图片不能单独删除");
		}
		markDeletingAfterCommit(List.of(asset));
	}

	public ImageAsset findById(Long id) {
		return id == null ? null : assetMapper.findById(id);
	}

	public ImageAssetResponse response(Long id) {
		ImageAsset asset = findById(id);
		return asset == null ? null : new ImageAssetResponse(asset);
	}

	public void cleanupStaleAssets() {
		Date cutoff = Date.from(Instant.now().minus(TEMP_RETENTION));
		for (ImageAsset asset : assetMapper.findCleanupCandidates(cutoff)) {
			deleteFilesAndRecord(asset);
		}
		cleanupOrphanDirectories(cutoff.toInstant());
	}

	private ImageAsset validateBindableAsset(Long ownerUserId, Long blogId, ImageAsset asset,
			ImageAsset.Purpose expectedPurpose) {
		if (asset == null || !ownerUserId.equals(asset.getOwnerUserId())
				|| !expectedPurpose.name().equals(asset.getPurpose())
				|| ImageAsset.Status.DELETING.name().equals(asset.getStatus())) {
			throw new ImageAssetException(IMAGE_NOT_OWNED, "图片不存在、用途不正确或不属于当前用户");
		}
		Long referencedBlogId = asset.getBlogId();
		if (referencedBlogId != null && !referencedBlogId.equals(blogId)) {
			throw new ImageAssetException(IMAGE_NOT_OWNED, "托管图片不能跨文章复用");
		}
		return asset;
	}

	private void bind(Long blogId, ImageAsset asset, String role) {
		assetMapper.insertReference(blogId, asset.getId(), role);
		assetMapper.updateStatus(asset.getId(), ImageAsset.Status.ACTIVE.name());
	}

	private void markDeletingAfterCommit(List<ImageAsset> assets) {
		if (assets == null || assets.isEmpty()) {
			return;
		}
		List<ImageAsset> unique = new ArrayList<>(assets.stream()
				.collect(java.util.stream.Collectors.toMap(ImageAsset::getId, asset -> asset, (left, right) -> left,
						LinkedHashMap::new)).values());
		for (ImageAsset asset : unique) {
			assetMapper.updateStatus(asset.getId(), ImageAsset.Status.DELETING.name());
		}
		runAfterCommit(() -> unique.forEach(this::deleteFilesAndRecord));
	}

	private void deleteFilesAndRecord(ImageAsset asset) {
		try {
			Path path = uploadRoot().resolve(asset.getOriginalPath()).normalize();
			ensureWithinAssets(path);
			deleteDirectory(path.getParent());
			assetMapper.deleteById(asset.getId());
		} catch (Exception exception) {
			log.error("Image asset cleanup failed, errorCode=IMAGE_ASSET_DELETE_FAILED assetId={}",
					asset.getId(), exception);
		}
	}

	private void cleanupOrphanDirectories(Instant cutoff) {
		Path root = assetsRoot();
		if (!Files.isDirectory(root)) {
			return;
		}
		Set<String> known = new HashSet<>(assetMapper.findAllPublicIds());
		try (var directories = Files.list(root)) {
			directories.filter(Files::isDirectory)
					.filter(path -> !known.contains(path.getFileName().toString()))
					.filter(path -> olderThan(path, cutoff))
					.forEach(ImageAssetService::deleteDirectoryQuietly);
		} catch (IOException exception) {
			log.error("Image orphan scan failed, errorCode=IMAGE_ASSET_ORPHAN_SCAN_FAILED", exception);
		}
	}

	private User requireUser(String username) {
		User user = userMapper.findByUsername(username);
		if (user == null) {
			throw new NotFoundException("用户不存在");
		}
		return user;
	}

	private Path uploadRoot() {
		return Path.of(uploadProperties.getPath()).toAbsolutePath().normalize();
	}

	private Path assetsRoot() {
		return uploadRoot().resolve("assets").normalize();
	}

	private void ensureWithinAssets(Path path) {
		if (!path.toAbsolutePath().normalize().startsWith(assetsRoot())) {
			throw new ImageAssetException(IMAGE_PROCESSING_FAILED, "图片保存路径无效");
		}
	}

	private static void moveDirectory(Path source, Path target) throws IOException {
		try {
			Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException exception) {
			Files.move(source, target);
		}
	}

	private static void registerRollbackCleanup(Path directory) {
		if (!TransactionSynchronizationManager.isActualTransactionActive()) {
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status != STATUS_COMMITTED) {
					deleteDirectoryQuietly(directory);
				}
			}
		});
	}

	private static void runAfterCommit(Runnable action) {
		if (!TransactionSynchronizationManager.isActualTransactionActive()) {
			action.run();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status == STATUS_COMMITTED) {
					action.run();
				}
			}
		});
	}

	private static boolean olderThan(Path path, Instant cutoff) {
		try {
			return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
		} catch (IOException exception) {
			return false;
		}
	}

	private static void deleteDirectoryQuietly(Path directory) {
		try {
			deleteDirectory(directory);
		} catch (IOException ignored) {
			// A tracked asset is retried by cleanup; an untracked temp path is safe to leave for the orphan scan.
		}
	}

	private static void deleteDirectory(Path directory) throws IOException {
		if (directory == null || !Files.exists(directory)) {
			return;
		}
		try (var paths = Files.walk(directory)) {
			for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
				Files.deleteIfExists(path);
			}
		}
	}

	public record PreparedBlogAssets(ImageAsset cover, List<ImageAsset> content, List<ImageAsset> previous) {
		public String coverThumbnailUrl(String fallback) {
			return cover == null ? fallback : cover.getThumbnailUrl();
		}
	}
}
