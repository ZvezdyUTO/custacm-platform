package top.naccl.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import top.naccl.entity.ImageAsset;
import top.naccl.entity.User;
import top.naccl.exception.NotFoundException;
import top.naccl.mapper.UserMapper;
import top.naccl.mapper.UserProfileLinkMapper;
import top.naccl.model.vo.PlayerProfile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
@ExtendWith(MockitoExtension.class)
class PlayerAvatarServiceTest {
	@Mock private UserMapper userMapper;
	@Mock private UserProfileLinkMapper linkMapper;
	@Mock private RedisService redisService;
	@Mock private ImageAssetService imageAssetService;

	@Test
	void switchesToManagedThumbnailAndSchedulesPreviousAssetCleanup() {
		User user = user();
		user.setAvatarAssetId(3L);
		ImageAsset previous = asset(3L, "/old-thumb.png", "/old.png");
		ImageAsset replacement = asset(8L, "/api/image/assets/new/thumbnail.png", "/api/image/assets/new/original.png");
		MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1});
		when(userMapper.findByUsername("player1")).thenReturn(user);
		when(imageAssetService.findById(3L)).thenReturn(previous);
		when(imageAssetService.storeAvatar(1L, file)).thenReturn(replacement);
		when(userMapper.updateAvatarByUsername("player1", replacement.getThumbnailUrl(), 8L)).thenReturn(1);
		when(linkMapper.findByUserId(1L)).thenReturn(java.util.List.of());

		PlayerProfile profile = service().updateAvatar("player1", file);

		assertEquals(replacement.getThumbnailUrl(), profile.getAvatar());
		assertEquals(replacement.getOriginalUrl(), profile.getAvatarOriginalUrl());
		verify(imageAssetService).replaceAvatar(previous, replacement);
	}

	@Test
	void rejectsMissingCurrentUserBeforeWritingFiles() {
		when(userMapper.findByUsername("missing")).thenReturn(null);
		assertThrows(NotFoundException.class, () -> service().updateAvatar("missing",
				new MockMultipartFile("file", new byte[]{1})));
	}

	private PlayerAvatarService service() {
		return new PlayerAvatarService(userMapper, linkMapper, redisService, imageAssetService);
	}

	private static User user() {
		User user = new User();
		user.setId(1L);
		user.setUsername("player1");
		user.setNickname("队员一");
		user.setRole("ROLE_player");
		return user;
	}

	private static ImageAsset asset(long id, String thumbnail, String original) {
		ImageAsset asset = new ImageAsset();
		asset.setId(id);
		asset.setThumbnailUrl(thumbnail);
		asset.setOriginalUrl(original);
		return asset;
	}
}
