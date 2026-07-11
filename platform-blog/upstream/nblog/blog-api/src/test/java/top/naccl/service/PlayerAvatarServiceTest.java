package top.naccl.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import top.naccl.config.properties.UploadProperties;
import top.naccl.entity.User;
import top.naccl.exception.BadRequestException;
import top.naccl.mapper.UserMapper;
import top.naccl.mapper.UserProfileLinkMapper;
import top.naccl.model.vo.PlayerProfile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
class PlayerAvatarServiceTest {
	@Mock
	private UserMapper userMapper;
	@Mock
	private UserProfileLinkMapper linkMapper;
	@Mock
	private RedisService redisService;
	@TempDir
	Path uploadDirectory;

	@Test
	void storesSquarePngAndUpdatesCurrentUser() throws Exception {
		User user = new User();
		user.setUsername("player1");
		user.setId(1L);
		user.setNickname("队员一");
		user.setRole("ROLE_player");
		when(userMapper.findByUsername("player1")).thenReturn(user);
		when(userMapper.updateAvatarByUsername(org.mockito.ArgumentMatchers.eq("player1"), startsWith("/api/image/avatar-")))
				.thenReturn(1);
		when(linkMapper.findByUserId(1L)).thenReturn(java.util.List.of());

		PlayerProfile profile = service().updateAvatar("player1", pngFile(512, 512));

		assertTrue(profile.getAvatar().matches("/api/image/avatar-[0-9a-f-]+\\.png"));
		try (var files = Files.list(uploadDirectory)) {
			assertEquals(1, files.count());
		}
		verify(redisService).deleteCacheByKey(top.naccl.constant.RedisKeyConstants.HOME_BLOG_INFO_LIST);
	}

	@Test
	void rejectsImageThatWasNotCroppedToRequiredSquare() throws Exception {
		assertThrows(BadRequestException.class, () -> service().updateAvatar("player1", pngFile(512, 400)));
	}

	@Test
	void rejectsNonPngFile() {
		MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[]{1, 2, 3});
		assertThrows(BadRequestException.class, () -> service().updateAvatar("player1", file));
	}

	private PlayerAvatarService service() {
		UploadProperties properties = new UploadProperties();
		properties.setPath(uploadDirectory.toString());
		return new PlayerAvatarService(userMapper, linkMapper, redisService, properties);
	}

	private static MockMultipartFile pngFile(int width, int height) throws Exception {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(image, "png", output);
		return new MockMultipartFile("file", "avatar.png", "image/png", output.toByteArray());
	}
}
