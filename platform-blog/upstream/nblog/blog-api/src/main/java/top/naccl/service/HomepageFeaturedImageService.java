package top.naccl.service;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import top.naccl.config.properties.UploadProperties;
import top.naccl.exception.BadRequestException;
import top.naccl.exception.NotFoundException;
import top.naccl.model.vo.HomepageFeaturedImage;
import top.naccl.repository.HomepageFeaturedImageRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 首页精选图片的上传、排序、删除与孤儿文件清理服务。
 *
 * @author huangbingrui.awa
 */
@Service
public class HomepageFeaturedImageService {
    private static final Logger log = LoggerFactory.getLogger(HomepageFeaturedImageService.class);
    private static final String FILE_DELETE_FAILED_ERROR_CODE = "HOMEPAGE_FEATURED_IMAGE_FILE_DELETE_FAILED";
    private static final String ORPHAN_SCAN_FAILED_ERROR_CODE = "HOMEPAGE_FEATURED_IMAGE_ORPHAN_SCAN_FAILED";
    private static final String THUMBNAIL_BACKFILL_FAILED_ERROR_CODE = "HOMEPAGE_FEATURED_IMAGE_THUMBNAIL_BACKFILL_FAILED";
    private static final Duration ORPHAN_RETENTION = Duration.ofHours(1);
    static final int REQUIRED_WIDTH = 1200;
    static final int REQUIRED_HEIGHT = 800;
    static final int THUMBNAIL_WIDTH = 720;
    static final int THUMBNAIL_HEIGHT = 480;
    public static final int MAX_IMAGE_COUNT = 12;
    static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private final HomepageFeaturedImageRepository repository;
    private final UploadProperties uploadProperties;

    public HomepageFeaturedImageService(
            HomepageFeaturedImageRepository repository,
            UploadProperties uploadProperties
    ) {
        this.repository = repository;
        this.uploadProperties = uploadProperties;
    }

    public List<HomepageFeaturedImage> list() {
        return repository.findAll().stream().map(this::ensureThumbnail).toList();
    }

    @Transactional
    public HomepageFeaturedImage upload(MultipartFile file) {
        if (repository.count() >= MAX_IMAGE_COUNT) {
            throw new BadRequestException("精选图片最多保留 12 张");
        }
        validateFile(file);
        String fileStem = "homepage-featured-" + UUID.randomUUID();
        String originalFileName = fileStem + ".jpg";
        String thumbnailFileName = fileStem + "-thumbnail.jpg";
        Path originalTarget = Path.of(uploadProperties.getPath()).resolve(originalFileName).normalize();
        Path thumbnailTarget = Path.of(uploadProperties.getPath()).resolve(thumbnailFileName).normalize();
        try {
            byte[] original = file.getBytes();
            byte[] thumbnail = createThumbnail(original);
            Files.createDirectories(originalTarget.getParent());
            Files.write(originalTarget, original, StandardOpenOption.CREATE_NEW);
            Files.write(thumbnailTarget, thumbnail, StandardOpenOption.CREATE_NEW);
            registerRollbackCleanup(originalTarget, thumbnailTarget);
            return repository.insert(
                    "/api/image/" + originalFileName,
                    "/api/image/" + thumbnailFileName);
        } catch (IOException | RuntimeException exception) {
            deletePathQuietly(originalTarget);
            deletePathQuietly(thumbnailTarget);
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new BadRequestException("精选图片保存失败", exception);
        }
    }

    @Transactional
    public List<HomepageFeaturedImage> reorder(List<Long> ids) {
        List<HomepageFeaturedImage> current = repository.findAll();
        Set<Long> currentIds = current.stream()
                .map(HomepageFeaturedImage::id)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> requestedIds = ids == null ? Set.of() : new HashSet<>(ids);
        if (ids == null
                || ids.size() != current.size()
                || requestedIds.size() != ids.size()
                || !requestedIds.equals(currentIds)) {
            throw new BadRequestException("排序必须包含全部精选图片，且不能重复");
        }
        repository.replaceOrder(ids);
        return list();
    }

    @Transactional
    public List<HomepageFeaturedImage> delete(long id) {
        HomepageFeaturedImage image = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("精选图片不存在"));
        if (repository.delete(id) != 1) {
            throw new NotFoundException("精选图片不存在");
        }
        runAfterCommit(() -> {
            deleteLocalFileQuietly(image.imageUrl());
            deleteLocalFileQuietly(image.thumbnailUrl());
        });
        List<Long> remainingIds = repository.findAll().stream()
                .map(HomepageFeaturedImage::id)
                .toList();
        repository.replaceOrder(remainingIds);
        return list();
    }

    public void cleanupOrphanFiles() {
        Path root = Path.of(uploadProperties.getPath()).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            return;
        }
        Set<String> referencedFiles = repository.findAll().stream()
                .flatMap(image -> java.util.stream.Stream.of(image.imageUrl(), image.thumbnailUrl()))
                .filter(url -> url != null && url.startsWith("/api/image/"))
                .map(url -> url.substring("/api/image/".length()))
                .collect(java.util.stream.Collectors.toSet());
        Instant cutoff = Instant.now().minus(ORPHAN_RETENTION);
        try (var files = Files.list(root)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> isManagedFileName(path.getFileName().toString()))
                    .filter(path -> !referencedFiles.contains(path.getFileName().toString()))
                    .filter(path -> lastModifiedBefore(path, cutoff))
                    .forEach(HomepageFeaturedImageService::deletePathQuietly);
        } catch (IOException exception) {
            log.error("Homepage featured image orphan scan failed, errorCode={}",
                    ORPHAN_SCAN_FAILED_ERROR_CODE, exception);
        }
    }

    private HomepageFeaturedImage ensureThumbnail(HomepageFeaturedImage image) {
        if (image.thumbnailUrl() != null && !image.thumbnailUrl().isBlank()) {
            return image;
        }
        String originalFileName = managedFileName(image.imageUrl());
        if (originalFileName == null) {
            return new HomepageFeaturedImage(image.id(), image.imageUrl(), image.imageUrl(), image.sortOrder());
        }
        String thumbnailFileName = originalFileName.replaceFirst("\\.jpg$", "-thumbnail.jpg");
        Path root = Path.of(uploadProperties.getPath()).toAbsolutePath().normalize();
        Path original = root.resolve(originalFileName).normalize();
        Path thumbnail = root.resolve(thumbnailFileName).normalize();
        if (!Files.isRegularFile(original)) {
            return new HomepageFeaturedImage(image.id(), image.imageUrl(), image.imageUrl(), image.sortOrder());
        }
        try {
            if (!Files.isRegularFile(thumbnail)) {
                Files.write(thumbnail, createThumbnail(Files.readAllBytes(original)), StandardOpenOption.CREATE_NEW);
            }
            String thumbnailUrl = "/api/image/" + thumbnailFileName;
            repository.updateThumbnailUrl(image.id(), thumbnailUrl);
            return new HomepageFeaturedImage(image.id(), image.imageUrl(), thumbnailUrl, image.sortOrder());
        } catch (FileAlreadyExistsException ignored) {
            String thumbnailUrl = "/api/image/" + thumbnailFileName;
            repository.updateThumbnailUrl(image.id(), thumbnailUrl);
            return new HomepageFeaturedImage(image.id(), image.imageUrl(), thumbnailUrl, image.sortOrder());
        } catch (IOException | RuntimeException exception) {
            log.warn("Homepage featured image thumbnail backfill failed, errorCode={}, imageId={}",
                    THUMBNAIL_BACKFILL_FAILED_ERROR_CODE, image.id(), exception);
            return new HomepageFeaturedImage(image.id(), image.imageUrl(), image.imageUrl(), image.sortOrder());
        }
    }

    private static byte[] createThumbnail(byte[] original) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(original))
                .size(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
                .keepAspectRatio(false)
                .outputFormat("jpg")
                .outputQuality(.72)
                .toOutputStream(output);
        return output.toByteArray();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("请选择裁剪后的精选图片");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("精选图片不能超过 10MB");
        }
        if (!"image/jpeg".equalsIgnoreCase(file.getContentType())) {
            throw new BadRequestException("精选图片必须裁剪并导出为 JPEG");
        }
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null || image.getWidth() != REQUIRED_WIDTH || image.getHeight() != REQUIRED_HEIGHT) {
                throw new BadRequestException("精选图片必须为 1200×800");
            }
        } catch (IOException exception) {
            throw new BadRequestException("无法读取精选图片", exception);
        }
    }

    private void deleteLocalFileQuietly(String imageUrl) {
        String fileName = managedFileName(imageUrl);
        if (fileName == null) return;
        if (isManagedFileName(fileName)) {
            deletePathQuietly(Path.of(uploadProperties.getPath()).resolve(fileName).normalize());
        }
    }

    private static String managedFileName(String imageUrl) {
        String prefix = "/api/image/";
        if (imageUrl == null || !imageUrl.startsWith(prefix)) return null;
        String fileName = imageUrl.substring(prefix.length());
        return isManagedFileName(fileName) ? fileName : null;
    }

    private static void registerRollbackCleanup(Path... files) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    for (Path file : files) deletePathQuietly(file);
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
            public void afterCommit() {
                action.run();
            }
        });
    }

    private static boolean isManagedFileName(String fileName) {
        return fileName != null
                && fileName.matches("homepage-featured-[0-9a-fA-F-]{36}(?:-thumbnail)?\\.jpg");
    }

    private static boolean lastModifiedBefore(Path path, Instant cutoff) {
        try {
            return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
        } catch (IOException exception) {
            log.error("Homepage featured image timestamp read failed, errorCode={}, fileName={}",
                    ORPHAN_SCAN_FAILED_ERROR_CODE, path.getFileName(), exception);
            return false;
        }
    }

    private static void deletePathQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.error("Homepage featured image file deletion failed, errorCode={}, fileName={}",
                    FILE_DELETE_FAILED_ERROR_CODE, path.getFileName(), exception);
        }
    }
}
