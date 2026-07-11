package top.naccl.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.naccl.config.properties.UploadProperties;
import top.naccl.constant.RedisKeyConstants;
import top.naccl.entity.User;
import top.naccl.exception.BadRequestException;
import top.naccl.exception.NotFoundException;
import top.naccl.mapper.UserMapper;
import top.naccl.mapper.UserProfileLinkMapper;
import top.naccl.model.vo.PlayerProfile;
import top.naccl.model.vo.ProfileLinkResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * @author huangbingrui.awa
 */
@Service
public class PlayerAvatarService {
	static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;
	static final int AVATAR_SIZE = 512;

	private final UserMapper userMapper;
	private final UserProfileLinkMapper linkMapper;
	private final RedisService redisService;
	private final UploadProperties uploadProperties;

	public PlayerAvatarService(UserMapper userMapper, UserProfileLinkMapper linkMapper, RedisService redisService,
			UploadProperties uploadProperties) {
		this.userMapper = userMapper;
		this.linkMapper = linkMapper;
		this.redisService = redisService;
		this.uploadProperties = uploadProperties;
	}

	public PlayerProfile updateAvatar(String username, MultipartFile file) {
		validateFile(file);
		byte[] bytes = readBytes(file);
		validateImage(bytes);
		User user = userMapper.findByUsername(username);
		if (user == null) {
			throw new NotFoundException("用户不存在");
		}

		String fileName = "avatar-" + UUID.randomUUID() + ".png";
		Path uploadDirectory = Path.of(uploadProperties.getPath()).toAbsolutePath().normalize();
		Path avatarFile = uploadDirectory.resolve(fileName).normalize();
		if (!avatarFile.startsWith(uploadDirectory)) {
			throw new BadRequestException("头像保存路径无效");
		}

		try {
			Files.createDirectories(uploadDirectory);
			Files.write(avatarFile, bytes, StandardOpenOption.CREATE_NEW);
		} catch (IOException exception) {
			throw new BadRequestException("头像保存失败", exception);
		}

		String avatarUrl = "/api/image/" + fileName;
		if (userMapper.updateAvatarByUsername(username, avatarUrl) != 1) {
			deleteQuietly(avatarFile);
			throw new BadRequestException("头像更新失败");
		}
		redisService.deleteCacheByKey(RedisKeyConstants.HOME_BLOG_INFO_LIST);
		user.setAvatar(avatarUrl);
		return new PlayerProfile(user, linkMapper.findByUserId(user.getId()).stream()
				.map(ProfileLinkResponse::new)
				.toList());
	}

	private static void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("请选择裁剪后的头像");
		}
		if (file.getSize() > MAX_AVATAR_BYTES) {
			throw new BadRequestException("头像不能超过 2MB");
		}
		if (!"image/png".equalsIgnoreCase(file.getContentType())) {
			throw new BadRequestException("头像必须裁剪并导出为 PNG");
		}
	}

	private static byte[] readBytes(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (IOException exception) {
			throw new BadRequestException("无法读取头像", exception);
		}
	}

	private static void validateImage(byte[] bytes) {
		try {
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
			if (image == null || image.getWidth() != AVATAR_SIZE || image.getHeight() != AVATAR_SIZE) {
				throw new BadRequestException("头像必须为 512×512 的正方形图片");
			}
		} catch (IOException exception) {
			throw new BadRequestException("无法读取头像", exception);
		}
	}

	private static void deleteQuietly(Path path) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException ignored) {
			// The database update already failed; leave cleanup to operators rather than masking that result.
		}
	}
}
