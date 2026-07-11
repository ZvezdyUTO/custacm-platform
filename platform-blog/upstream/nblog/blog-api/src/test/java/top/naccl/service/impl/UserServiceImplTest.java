package top.naccl.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import top.naccl.entity.User;
import top.naccl.mapper.UserMapper;
import top.naccl.service.RedisService;
import top.naccl.constant.RedisKeyConstants;
import top.naccl.util.HashUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
	@Mock
	private UserMapper userMapper;
	@Mock
	private RedisService redisService;

	@InjectMocks
	private UserServiceImpl userService;

	private User player;

	@BeforeEach
	void setUp() {
		player = new User();
		player.setUsername("player1");
		player.setNickname("旧昵称");
		player.setPassword(HashUtils.getBC("old-password"));
		player.setRole("ROLE_player");
	}

	@Test
	void updatesNicknameWithoutRequiringUniqueness() {
		when(userMapper.findByUsername("player1")).thenReturn(player);
		when(userMapper.updateNicknameByUsername("player1", "共享昵称")).thenReturn(1);

		assertTrue(userService.updateNickname("player1", "共享昵称"));
		verify(userMapper).updateNicknameByUsername("player1", "共享昵称");
		verify(redisService).deleteCacheByKey(RedisKeyConstants.HOME_BLOG_INFO_LIST);
	}

	@Test
	void rejectsPasswordChangeWhenOldPasswordIsWrong() {
		when(userMapper.findByUsername("player1")).thenReturn(player);

		assertThrows(BadCredentialsException.class,
				() -> userService.changePassword("player1", "wrong-password", "new-password"));
	}

	@Test
	void storesNewPasswordAsBcrypt() {
		when(userMapper.findByUsername("player1")).thenReturn(player);
		when(userMapper.updatePasswordByUsername(org.mockito.ArgumentMatchers.eq("player1"), org.mockito.ArgumentMatchers.anyString())).thenReturn(1);

		assertTrue(userService.changePassword("player1", "old-password", "new-password"));
		verify(userMapper).updatePasswordByUsername(org.mockito.ArgumentMatchers.eq("player1"),
				argThat(encoded -> HashUtils.matchBC("new-password", encoded)));
	}
}
