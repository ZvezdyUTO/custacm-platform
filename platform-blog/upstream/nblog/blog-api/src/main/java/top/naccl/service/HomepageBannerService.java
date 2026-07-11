package top.naccl.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import top.naccl.config.properties.UploadProperties;
import top.naccl.exception.BadRequestException;
import top.naccl.exception.NotFoundException;
import top.naccl.model.vo.HomepageBannerImage;
import top.naccl.repository.HomepageBannerRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 首页横幅图片的上传、排序与删除服务。
 *
 * @author huangbingrui.awa
 */
@Service
public class HomepageBannerService {
    static final int REQUIRED_WIDTH = 1920;
    static final int REQUIRED_HEIGHT = 1080;
    static final int MAX_IMAGE_COUNT = 2;
    static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private final HomepageBannerRepository repository;
    private final UploadProperties uploadProperties;

    public HomepageBannerService(HomepageBannerRepository repository, UploadProperties uploadProperties) {
        this.repository = repository;
        this.uploadProperties = uploadProperties;
    }

    public List<HomepageBannerImage> list() {
        return repository.findAll();
    }

    @Transactional
    public HomepageBannerImage upload(MultipartFile file) {
        if (repository.count() >= MAX_IMAGE_COUNT) {
            throw new BadRequestException("首页最多保留两张图片");
        }
        validateFile(file);
        String fileName = "homepage-banner-" + UUID.randomUUID() + ".jpg";
        Path target = Path.of(uploadProperties.getPath()).resolve(fileName).normalize();
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, file.getBytes(), StandardOpenOption.CREATE_NEW);
            return repository.insert("/api/image/" + fileName);
        } catch (IOException | RuntimeException exception) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
                // 原始异常包含稳定的请求错误信息，清理失败不覆盖它。
            }
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new BadRequestException("首页图片保存失败", exception);
        }
    }

    @Transactional
    public List<HomepageBannerImage> reorder(List<Long> ids) {
        List<HomepageBannerImage> current = repository.findAll();
        Set<Long> currentIds = current.stream().map(HomepageBannerImage::id).collect(java.util.stream.Collectors.toSet());
        Set<Long> requestedIds = ids == null ? Set.of() : new HashSet<>(ids);
        if (ids == null || ids.size() != current.size() || requestedIds.size() != ids.size() || !requestedIds.equals(currentIds)) {
            throw new BadRequestException("排序必须包含全部首页图片，且不能重复");
        }
        repository.replaceOrder(ids);
        return repository.findAll();
    }

    @Transactional
    public List<HomepageBannerImage> delete(long id) {
        if (repository.count() <= 1) {
            throw new BadRequestException("首页至少保留一张图片");
        }
        HomepageBannerImage image = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("首页图片不存在"));
        if (repository.delete(id) != 1) {
            throw new NotFoundException("首页图片不存在");
        }
        deleteLocalFile(image.imageUrl());
        List<Long> remainingIds = repository.findAll().stream().map(HomepageBannerImage::id).toList();
        repository.replaceOrder(remainingIds);
        return repository.findAll();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("请选择裁剪后的首页图片");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("首页图片不能超过 10MB");
        }
        if (!"image/jpeg".equalsIgnoreCase(file.getContentType())) {
            throw new BadRequestException("首页图片必须裁剪并导出为 JPEG");
        }
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null || image.getWidth() != REQUIRED_WIDTH || image.getHeight() != REQUIRED_HEIGHT) {
                throw new BadRequestException("首页图片必须为 1920×1080");
            }
        } catch (IOException exception) {
            throw new BadRequestException("无法读取首页图片", exception);
        }
    }

    private void deleteLocalFile(String imageUrl) {
        String prefix = "/api/image/";
        if (imageUrl == null || !imageUrl.startsWith(prefix)) {
            return;
        }
        String fileName = imageUrl.substring(prefix.length());
        if (!fileName.startsWith("homepage-banner-") || fileName.contains("/") || fileName.contains("\\")) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(uploadProperties.getPath()).resolve(fileName).normalize());
        } catch (IOException exception) {
            throw new BadRequestException("首页图片文件删除失败", exception);
        }
    }
}
