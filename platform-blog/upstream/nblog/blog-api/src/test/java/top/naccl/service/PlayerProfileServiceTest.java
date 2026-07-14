package top.naccl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.naccl.constant.RedisKeyConstants;
import top.naccl.entity.User;
import top.naccl.entity.UserProfileLink;
import top.naccl.exception.BadRequestException;
import top.naccl.mapper.UserMapper;
import top.naccl.mapper.UserProfileLinkMapper;
import top.naccl.model.dto.PlayerProfileUpdateRequest;
import top.naccl.model.dto.ProfileLinkInput;
import top.naccl.model.dto.ProfileLinksReplaceRequest;
import top.naccl.model.vo.CompetitionAchievement;
import top.naccl.model.vo.CompetitionResponse;
import top.naccl.model.vo.PlayerProfile;
import top.naccl.model.vo.PublicProfile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
@ExtendWith(MockitoExtension.class)
class PlayerProfileServiceTest {
	@Mock
	private UserMapper userMapper;
	@Mock
	private UserProfileLinkMapper linkMapper;
	@Mock
	private RedisService redisService;
	@Mock
	private ImageAssetService imageAssetService;
	@Mock
	private CompetitionService competitionService;

	private PlayerProfileService service;
	private User player;

	@BeforeEach
	void setUp() {
		service = new PlayerProfileService(userMapper, linkMapper, redisService, imageAssetService,
				competitionService);
		player = new User();
		player.setId(7L);
		player.setUsername("player1");
		player.setNickname("旧昵称");
		player.setEmail("player1@example.com");
		player.setSignature("");
		player.setAvatar("/avatar.png");
		player.setRole("ROLE_player");
	}

	@Test
	void returnsCurrentProfileWithOrderedLinks() {
		UserProfileLink link = link("GitHub", "https://github.com/example", 0);
		link.setId(12L);
		CompetitionAchievement achievement = achievement();
		when(userMapper.findByUsername("player1")).thenReturn(player);
		when(linkMapper.findByUserId(7L)).thenReturn(List.of(link));
		when(competitionService.achievements("player1")).thenReturn(List.of(achievement));

		PlayerProfile profile = service.get("player1");

		assertEquals("player1", profile.getUsername());
		assertEquals("player1@example.com", profile.getEmail());
		assertEquals("GitHub", profile.getLinks().getFirst().label());
		assertEquals(List.of(achievement), profile.getAchievements());
	}

	@Test
	void returnsPublicProfileWithoutAccountRole() {
		player.setSignature("保持好奇");
		UserProfileLink link = link("主页", "https://example.com", 0);
		CompetitionAchievement achievement = achievement();
		when(userMapper.findByUsername("player1")).thenReturn(player);
		when(linkMapper.findByUserId(7L)).thenReturn(List.of(link));
		when(competitionService.publicAchievements("player1")).thenReturn(List.of(achievement));

		PublicProfile profile = service.getPublic("player1");

		assertEquals("player1", profile.getUsername());
		assertEquals("player1@example.com", profile.getEmail());
		assertEquals("保持好奇", profile.getSignature());
		assertEquals("主页", profile.getLinks().getFirst().label());
		assertEquals(List.of(achievement), profile.getAchievements());
	}

	@Test
	void updatesNicknameAndSignatureAndReturnsFullProfile() {
		when(userMapper.findByUsername("player1")).thenReturn(player);
		when(userMapper.updateProfileByUsername("player1", "新昵称", "保持好奇")).thenReturn(1);
		when(linkMapper.findByUserId(7L)).thenReturn(List.of());

		PlayerProfile profile = service.update("player1", new PlayerProfileUpdateRequest(" 新昵称 ", " 保持好奇 "));

		assertEquals("新昵称", profile.getNickname());
		assertEquals("保持好奇", profile.getSignature());
		verify(redisService).deleteCacheByKey(RedisKeyConstants.HOME_BLOG_INFO_LIST);
	}

	@Test
	void clearsSignatureWithoutChangingNickname() {
		player.setSignature("旧签名");
		when(userMapper.findByUsername("player1")).thenReturn(player);
		when(userMapper.updateProfileByUsername("player1", "旧昵称", "")).thenReturn(1);
		when(linkMapper.findByUserId(7L)).thenReturn(List.of());

		PlayerProfile profile = service.update("player1", new PlayerProfileUpdateRequest(null, "  "));

		assertEquals("", profile.getSignature());
		verify(redisService, never()).deleteCacheByKey(RedisKeyConstants.HOME_BLOG_INFO_LIST);
	}

	@Test
	void replacesLinksInSubmittedOrder() {
		when(userMapper.findByUsername("player1")).thenReturn(player);
		when(linkMapper.insert(org.mockito.ArgumentMatchers.any())).thenReturn(1);
		when(linkMapper.findByUserId(7L)).thenReturn(List.of());

		service.replaceLinks("player1", new ProfileLinksReplaceRequest(List.of(
				new ProfileLinkInput(" GitHub ", " https://github.com/example "),
				new ProfileLinkInput("博客", "https://example.com")
		)));

		ArgumentCaptor<UserProfileLink> captor = ArgumentCaptor.forClass(UserProfileLink.class);
		verify(linkMapper).deleteByUserId(7L);
		verify(linkMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
		assertEquals(List.of(0, 1), captor.getAllValues().stream().map(UserProfileLink::getSortOrder).toList());
		assertEquals("GitHub", captor.getAllValues().getFirst().getLabel());
	}

	@Test
	void allowsEmptyListToClearLinks() {
		when(userMapper.findByUsername("player1")).thenReturn(player);
		when(linkMapper.findByUserId(7L)).thenReturn(List.of());

		service.replaceLinks("player1", new ProfileLinksReplaceRequest(List.of()));

		verify(linkMapper).deleteByUserId(7L);
		verify(linkMapper, never()).insert(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void rejectsUnsafeOrDuplicateLinksBeforeDeletingExistingData() {
		assertThrows(BadRequestException.class, () -> service.replaceLinks("player1",
				new ProfileLinksReplaceRequest(List.of(new ProfileLinkInput("脚本", "javascript:alert(1)")))));
		verify(linkMapper, never()).deleteByUserId(org.mockito.ArgumentMatchers.any());

		assertThrows(BadRequestException.class, () -> service.replaceLinks("player1",
				new ProfileLinksReplaceRequest(List.of(
						new ProfileLinkInput("A", "https://example.com"),
						new ProfileLinkInput("B", "https://example.com")
				))));
		verify(linkMapper, never()).deleteByUserId(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void rejectsMoreThanEightLinks() {
		List<ProfileLinkInput> links = java.util.stream.IntStream.range(0, 9)
				.mapToObj(index -> new ProfileLinkInput("链接" + index, "https://example.com/" + index))
				.toList();

		assertThrows(BadRequestException.class,
				() -> service.replaceLinks("player1", new ProfileLinksReplaceRequest(links)));
		verify(userMapper, never()).findByUsername(org.mockito.ArgumentMatchers.any());
	}

	private UserProfileLink link(String label, String url, int sortOrder) {
		UserProfileLink link = new UserProfileLink();
		link.setUserId(player.getId());
		link.setLabel(label);
		link.setUrl(url);
		link.setSortOrder(sortOrder);
		return link;
	}

	private static CompetitionAchievement achievement() {
		return new CompetitionAchievement(
				31L,
				"2025 年中国高校计算机大赛-团体程序设计天梯赛",
				2025,
				List.of(new CompetitionResponse.Type("GPLT", "团体程序设计天梯赛")),
				41L,
				"INDIVIDUAL",
				"个人",
				null,
				"NATIONAL",
				"国家级",
				1,
				"一等奖",
				3,
				120,
				"(3/120)",
				true
		);
	}
}
