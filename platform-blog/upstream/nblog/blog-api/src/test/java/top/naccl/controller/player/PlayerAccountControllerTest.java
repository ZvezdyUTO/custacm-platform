package top.naccl.controller.player;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountException;
import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import top.naccl.model.dto.PlayerProfileUpdateRequest;
import top.naccl.model.dto.ProfileLinksReplaceRequest;
import top.naccl.model.vo.PlayerProfile;
import top.naccl.model.vo.Result;
import top.naccl.service.PlayerAvatarService;
import top.naccl.service.PlayerProfileService;
import top.naccl.service.UserService;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
@ExtendWith(MockitoExtension.class)
class PlayerAccountControllerTest {
	@Mock
	private UserService userService;
	@Mock
	private PlayerAvatarService playerAvatarService;
	@Mock
	private OjHandleAccountService ojHandleAccountService;
	@Mock
	private PlayerProfileService playerProfileService;
	@Mock
	private Authentication authentication;
	@InjectMocks
	private PlayerAccountController controller;

	@Test
	void returnsOnlyCurrentUsersOjHandles() {
		when(authentication.getName()).thenReturn("player1");
		when(ojHandleAccountService.getByUsername("player1")).thenReturn(new OjHandleAccount(
				"player1",
				Map.of("CODEFORCES", "tourist", "ATCODER", "chokudai"),
				true,
				Instant.EPOCH,
				Instant.EPOCH
		));

		Result result = controller.ojHandles(authentication);

		assertEquals(200, result.getCode());
		assertEquals(Map.of("CODEFORCES", "tourist", "ATCODER", "chokudai"), result.getData());
	}

	@Test
	void returnsEmptyHandlesWhenCurrentUserHasNoOjAccount() {
		when(authentication.getName()).thenReturn("player1");
		when(ojHandleAccountService.getByUsername("player1")).thenThrow(new OjHandleAccountException(
				OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_NOT_FOUND,
				"not found"
		));

		Result result = controller.ojHandles(authentication);

		assertEquals(200, result.getCode());
		assertEquals(Map.of(), result.getData());
	}

	@Test
	void updatesOnlyAuthenticatedUsersProfile() {
		when(authentication.getName()).thenReturn("player1");
		PlayerProfileUpdateRequest request = new PlayerProfileUpdateRequest("新昵称", "新签名");
		PlayerProfile profile = org.mockito.Mockito.mock(PlayerProfile.class);
		when(playerProfileService.update("player1", request)).thenReturn(profile);

		Result result = controller.updateProfile(authentication, request);

		assertEquals(200, result.getCode());
		assertEquals(profile, result.getData());
	}

	@Test
	void replacesOnlyAuthenticatedUsersLinks() {
		when(authentication.getName()).thenReturn("player1");
		ProfileLinksReplaceRequest request = new ProfileLinksReplaceRequest(java.util.List.of());
		PlayerProfile profile = org.mockito.Mockito.mock(PlayerProfile.class);
		when(playerProfileService.replaceLinks("player1", request)).thenReturn(profile);

		Result result = controller.replaceProfileLinks(authentication, request);

		assertEquals(200, result.getCode());
		assertEquals(profile, result.getData());
	}
}
