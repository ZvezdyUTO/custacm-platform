package top.naccl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.naccl.config.properties.UploadProperties;
import top.naccl.exception.BadRequestException;
import top.naccl.model.vo.HomepageFeaturedImage;
import top.naccl.repository.HomepageFeaturedImageRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
@ExtendWith(MockitoExtension.class)
class HomepageFeaturedImageServiceTest {
    @Mock
    private HomepageFeaturedImageRepository repository;

    @TempDir
    Path uploadDirectory;

    private HomepageFeaturedImageService service;

    @BeforeEach
    void setUp() {
        UploadProperties properties = new UploadProperties();
        properties.setPath(uploadDirectory.toString() + "/");
        service = new HomepageFeaturedImageService(repository, properties);
    }

    @Test
    void uploadsCroppedJpegAndPersistsSameOriginUrl() throws Exception {
        when(repository.insert(
                startsWith("/api/image/homepage-featured-"),
                endsWith("-thumbnail.jpg")))
                .thenAnswer(invocation -> new HomepageFeaturedImage(
                        4L,
                        invocation.getArgument(0, String.class),
                        invocation.getArgument(1, String.class),
                        0));

        HomepageFeaturedImage created = service.upload(new MockMultipartFile(
                "file", "featured.jpg", "image/jpeg", jpeg(1200, 800)));

        assertEquals(4L, created.id());
        assertTrue(created.imageUrl().startsWith("/api/image/homepage-featured-"));
        assertTrue(created.thumbnailUrl().endsWith("-thumbnail.jpg"));
        try (Stream<Path> files = Files.list(uploadDirectory)) {
            assertEquals(2, files.count());
        }
    }

    @Test
    void enforcesCountAndCropDimensions() throws Exception {
        when(repository.count()).thenReturn(12);

        BadRequestException countError = assertThrows(
                BadRequestException.class,
                () -> service.upload(null));
        assertEquals("精选图片最多保留 12 张", countError.getMessage());

        when(repository.count()).thenReturn(0);
        BadRequestException dimensionError = assertThrows(
                BadRequestException.class,
                () -> service.upload(new MockMultipartFile(
                        "file", "featured.jpg", "image/jpeg", jpeg(1200, 799))));
        assertEquals("精选图片必须为 1200×800", dimensionError.getMessage());
    }

    @Test
    void reordersOnlyWhenEveryCurrentIdAppearsExactlyOnce() {
        List<HomepageFeaturedImage> current = List.of(
                new HomepageFeaturedImage(1L, "/one.jpg", "/one-thumb.jpg", 0),
                new HomepageFeaturedImage(2L, "/two.jpg", "/two-thumb.jpg", 1));
        when(repository.findAll()).thenReturn(current, List.of(current.get(1), current.get(0)));

        List<HomepageFeaturedImage> reordered = service.reorder(List.of(2L, 1L));

        verify(repository).replaceOrder(List.of(2L, 1L));
        assertEquals(List.of(2L, 1L), reordered.stream().map(HomepageFeaturedImage::id).toList());
        assertThrows(BadRequestException.class, () -> service.reorder(List.of(1L, 1L)));
    }

    @Test
    void deletesTheDatabaseRowBeforeRemovingTheManagedFile() throws Exception {
        String fileName = "homepage-featured-11111111-1111-1111-1111-111111111111.jpg";
        String thumbnailFileName = "homepage-featured-11111111-1111-1111-1111-111111111111-thumbnail.jpg";
        Path imageFile = uploadDirectory.resolve(fileName);
        Path thumbnailFile = uploadDirectory.resolve(thumbnailFileName);
        Files.write(imageFile, new byte[]{1, 2, 3});
        Files.write(thumbnailFile, new byte[]{4, 5, 6});
        HomepageFeaturedImage image = new HomepageFeaturedImage(
                1L,
                "/api/image/" + fileName,
                "/api/image/" + thumbnailFileName,
                0);
        when(repository.findById(1L)).thenReturn(Optional.of(image));
        when(repository.delete(1L)).thenReturn(1);
        when(repository.findAll()).thenReturn(List.of(), List.of());
        beginTransactionSynchronization();
        try {
            service.delete(1L);

            assertTrue(Files.exists(imageFile));
            completeTransaction(TransactionSynchronization.STATUS_COMMITTED);
            assertTrue(Files.notExists(imageFile));
            assertTrue(Files.notExists(thumbnailFile));
            verify(repository).replaceOrder(List.of());
        } finally {
            clearTransactionSynchronization();
        }
    }

    @Test
    void removesRolledBackUploadsAndOldOrphans() throws Exception {
        when(repository.insert(
                startsWith("/api/image/homepage-featured-"),
                endsWith("-thumbnail.jpg")))
                .thenAnswer(invocation -> new HomepageFeaturedImage(
                        4L,
                        invocation.getArgument(0, String.class),
                        invocation.getArgument(1, String.class),
                        0));
        beginTransactionSynchronization();
        try {
            service.upload(new MockMultipartFile(
                    "file", "featured.jpg", "image/jpeg", jpeg(1200, 800)));
            Path uploaded;
            try (Stream<Path> files = Files.list(uploadDirectory)) {
                uploaded = files.findFirst().orElseThrow();
            }
            completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);
            assertTrue(Files.notExists(uploaded));
            try (Stream<Path> files = Files.list(uploadDirectory)) {
                assertEquals(0, files.count());
            }
        } finally {
            clearTransactionSynchronization();
        }

        Path orphan = uploadDirectory.resolve(
                "homepage-featured-22222222-2222-2222-2222-222222222222.jpg");
        Files.write(orphan, new byte[]{1, 2, 3});
        Files.setLastModifiedTime(orphan, FileTime.from(Instant.now().minus(Duration.ofHours(2))));
        when(repository.findAll()).thenReturn(List.of());

        service.cleanupOrphanFiles();

        assertTrue(Files.notExists(orphan));
    }

    @Test
    void backfillsACompressedThumbnailForExistingFeaturedImages() throws Exception {
        String fileName = "homepage-featured-33333333-3333-3333-3333-333333333333.jpg";
        Path original = uploadDirectory.resolve(fileName);
        Files.write(original, jpeg(1200, 800));
        HomepageFeaturedImage legacy = new HomepageFeaturedImage(
                7L,
                "/api/image/" + fileName,
                null,
                0);
        when(repository.findAll()).thenReturn(List.of(legacy));

        HomepageFeaturedImage image = service.list().get(0);

        assertTrue(image.thumbnailUrl().endsWith("-thumbnail.jpg"));
        Path thumbnail = uploadDirectory.resolve(
                "homepage-featured-33333333-3333-3333-3333-333333333333-thumbnail.jpg");
        BufferedImage decoded = ImageIO.read(thumbnail.toFile());
        assertEquals(720, decoded.getWidth());
        assertEquals(480, decoded.getHeight());
        verify(repository).updateThumbnailUrl(7L, image.thumbnailUrl());
    }

    private static void beginTransactionSynchronization() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    private static void completeTransaction(int status) {
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        if (status == TransactionSynchronization.STATUS_COMMITTED) {
            synchronizations.forEach(TransactionSynchronization::afterCommit);
        }
        synchronizations.forEach(synchronization -> synchronization.afterCompletion(status));
    }

    private static void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    private static byte[] jpeg(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", output);
        return output.toByteArray();
    }
}
