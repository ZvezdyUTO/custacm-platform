package top.naccl.service;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import com.custacm.platform.trainingdata.common.app.purge.OjStudentDataPurgeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.naccl.entity.User;
import top.naccl.exception.BadRequestException;
import top.naccl.exception.ForbiddenException;
import top.naccl.mapper.UserMapper;
import top.naccl.model.dto.AdminUserCreateRequest;
import top.naccl.model.dto.AdminUserPatchRequest;
import top.naccl.service.impl.AdminUserService;
import top.naccl.util.HashUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {
    @Mock UserMapper userMapper;
    @Mock OjHandleAccountService handleAccountService;
    @Mock OjStudentDataPurgeService purgeService;

    @Test
    void createsPlayerWithGeneratedBcryptPasswordAndHandles() {
        AdminUserService service = service();
        AdminUserCreateRequest request = new AdminUserCreateRequest(
                "  张三-01  ", null, "张三", null, null, "ROLE_player",
                Map.of("codeforces", "tourist"), false
        );
        when(userMapper.findByUsername("张三-01")).thenReturn(null);
        when(userMapper.insert(argThat(user -> "张三-01".equals(user.getUsername())))).thenReturn(1);

        var response = service.create(request);

        assertEquals("张三-01", response.user().getUsername());
        assertTrue(response.generatedPassword() != null && response.generatedPassword().length() >= 16);
        verify(userMapper).insert(argThat(user -> HashUtils.matchBC(response.generatedPassword(), user.getPassword())));
        verify(handleAccountService).create("张三-01", Map.of("codeforces", "tourist"), false);
    }

    @Test
    void rejectsInvalidUsernameAndRole() {
        AdminUserService service = service();
        assertThrows(BadRequestException.class, () -> service.create(new AdminUserCreateRequest(
                "bad/name", "123456", null, null, null, "ROLE_player", Map.of(), true)));
        assertThrows(BadRequestException.class, () -> service.create(new AdminUserCreateRequest(
                "valid", "123456", null, null, null, "ROLE_guest", Map.of(), true)));
    }

    @Test
    void preventsDowngradingOrDeletingLastAdministrator() {
        AdminUserService service = service();
        User admin = user("admin", "ROLE_admin");
        when(userMapper.findByUsername("admin")).thenReturn(admin);
        when(userMapper.lockAdminIds()).thenReturn(List.of(1L));

        assertThrows(ForbiddenException.class, () -> service.patch("admin", new AdminUserPatchRequest(
                null, null, null, null, "ROLE_player", null)));
        assertThrows(ForbiddenException.class, () -> service.delete("admin"));
    }

    @Test
    void usernameChangeUsesParentForeignKeyCascadeAndRequiresRelogin() {
        AdminUserService service = service();
        User player = user("old-name", "ROLE_player");
        when(userMapper.findByUsername("old-name")).thenReturn(player);
        when(userMapper.findByUsername("new-name")).thenReturn(null);
        when(userMapper.updateAdminFields(argThat(user -> "new-name".equals(user.getUsername())),
                org.mockito.ArgumentMatchers.eq("old-name"))).thenReturn(1);

        var response = service.patch("old-name", new AdminUserPatchRequest(
                "new-name", null, null, null, null, null));

        assertTrue(response.reloginRequired());
        verify(userMapper).updateAdminFields(player, "old-name");
    }

	@Test
	void blankPatchPasswordGeneratesOneTimeResetPassword() {
		AdminUserService service = service();
		User player = user("player", "ROLE_player");
		when(userMapper.findByUsername("player")).thenReturn(player);
		when(userMapper.updateAdminFields(org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.eq("player"))).thenReturn(1);

		var response = service.patch("player", new AdminUserPatchRequest(
				null, null, null, null, null, ""));

		assertTrue(response.generatedPassword() != null && response.generatedPassword().length() >= 16);
		verify(userMapper).updateAdminFields(argThat(user ->
				HashUtils.matchBC(response.generatedPassword(), user.getPassword())),
				org.mockito.ArgumentMatchers.eq("player"));
	}

    private AdminUserService service() {
        return new AdminUserService(userMapper, handleAccountService, purgeService);
    }

    private User user(String username, String role) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPassword(HashUtils.getBC("password"));
        user.setRole(role);
        return user;
    }
}
