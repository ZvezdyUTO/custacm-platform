package top.naccl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import top.naccl.config.properties.UploadProperties;
import top.naccl.exception.BadRequestException;
import top.naccl.model.vo.HomepageBannerImage;
import top.naccl.repository.HomepageBannerRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
@ExtendWith(MockitoExtension.class)
class HomepageBannerServiceTest {
    @Mock
    private HomepageBannerRepository repository;

    @TempDir
    Path uploadDirectory;

    private HomepageBannerService service;

    @BeforeEach
    void setUp() {
        UploadProperties properties = new UploadProperties();
        properties.setPath(uploadDirectory.toString() + "/");
        service = new HomepageBannerService(repository, properties);
    }

    @Test
    void uploadsCroppedJpegAndPersistsSameOriginUrl() throws Exception {
        byte[] data = jpeg(1920, 1080);
        when(repository.insert(startsWith("/api/image/homepage-banner-")))
                .thenAnswer(invocation -> new HomepageBannerImage(
                        4L, invocation.getArgument(0, String.class), 3));

        HomepageBannerImage created = service.upload(new MockMultipartFile(
                "file", "banner.jpg", "image/jpeg", data));

        assertEquals(4L, created.id());
        assertTrue(created.imageUrl().startsWith("/api/image/homepage-banner-"));
        try (Stream<Path> files = Files.list(uploadDirectory)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void rejectsImageWithUnexpectedDimensions() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "banner.jpg", "image/jpeg", jpeg(1280, 720));

        BadRequestException error = assertThrows(BadRequestException.class, () -> service.upload(file));

        assertEquals("首页图片必须为 1920×1080", error.getMessage());
    }

    @Test
    void reordersOnlyWhenEveryIdAppearsExactlyOnce() {
        List<HomepageBannerImage> current = List.of(
                new HomepageBannerImage(1L, "/one.jpg", 0),
                new HomepageBannerImage(2L, "/two.jpg", 1));
        when(repository.findAll()).thenReturn(current, List.of(current.get(1), current.get(0)));

        List<HomepageBannerImage> reordered = service.reorder(List.of(2L, 1L));

        verify(repository).replaceOrder(List.of(2L, 1L));
        assertEquals(List.of(2L, 1L), reordered.stream().map(HomepageBannerImage::id).toList());
        assertThrows(BadRequestException.class, () -> service.reorder(List.of(1L, 1L)));
    }

    @Test
    void refusesToDeleteLastBanner() {
        when(repository.count()).thenReturn(1);

        BadRequestException error = assertThrows(BadRequestException.class, () -> service.delete(1L));

        assertEquals("首页至少保留一张图片", error.getMessage());
    }

    private byte[] jpeg(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", output);
        return output.toByteArray();
    }
}
