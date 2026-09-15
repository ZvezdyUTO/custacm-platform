package top.naccl.service;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountException;
import com.custacm.platform.trainingdata.common.app.purge.OjStudentDataPurgeService;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleCollectionState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.naccl.entity.User;
import top.naccl.exception.BadRequestException;
import top.naccl.exception.ConflictException;
import top.naccl.exception.ForbiddenException;
import top.naccl.mapper.UserMapper;
import top.naccl.model.dto.AdminUserCreateRequest;
import top.naccl.model.dto.AdminUserUpdateRequest;
import top.naccl.service.impl.AdminUserService;
import top.naccl.util.HashUtils;

import java.util.List;
import java.util.Map;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {
    @Mock UserMapper userMapper;
    @Mock OjHandleAccountService handleAccountService;
    @Mock OjStudentDataPurgeService purgeService;
    @Mock ImageAssetService imageAssetService;
    @Mock TrainingDataMutationGuard trainingDataMutationGuard;

    @Test
    void createsPlayerWithGeneratedBcryptPasswordAndHandles() {
        AdminUserService service = service();
        AdminUserCreateRequest request = new AdminUserCreateRequest(
                "  张三-01  ", null, "张三", null, "ROLE_player",
                Map.of("codeforces", "tourist"), false
        );
        when(userMapper.findByUsername("张三-01")).thenReturn(null);
        when(userMapper.insert(argThat(user -> "张三-01".equals(user.getUsername())))).thenReturn(1);

        var response = service.create(request);

        assertEquals("张三-01", response.user().getUsername());
        assertEquals("", response.user().getAvatar());
        assertTrue(response.generatedPassword() != null && response.generatedPassword().length() >= 16);
        verify(userMapper).insert(argThat(user -> HashUtils.matchBC(response.generatedPassword(), user.getPassword())));
        verify(handleAccountService).create("张三-01", Map.of("CODEFORCES", "tourist"), false);
    }

    @Test
    void rejectsInvalidUsernameAndRole() {
        AdminUserService service = service();
        assertThrows(BadRequestException.class, () -> service.create(new AdminUserCreateRequest(
                "bad/name", "123456", null, null, "ROLE_player", Map.of(), true)));
        assertThrows(BadRequestException.class, () -> service.create(new AdminUserCreateRequest(
                "valid", "123456", null, null, "ROLE_guest", Map.of(), true)));
    }

    @Test
    void preventsDowngradingOrDeletingLastAdministrator() {
        AdminUserService service = service();
        User admin = user("admin", "ROLE_admin");
        when(userMapper.findByUsernameForUpdate("admin")).thenReturn(admin);
        when(userMapper.lockAdminIds()).thenReturn(List.of(1L));

        assertThrows(ForbiddenException.class, () -> service.update("admin", new AdminUserUpdateRequest(
                "admin", null, null, "ROLE_player", null, Map.of(), true)));
        assertThrows(ForbiddenException.class, () -> service.delete("admin"));
        var ordered = inOrder(trainingDataMutationGuard, userMapper);
        ordered.verify(trainingDataMutationGuard).acquireUntilTransactionCompletes();
        ordered.verify(userMapper).lockAdminIds();
        ordered.verify(userMapper).findByUsernameForUpdate("admin");
        ordered.verify(trainingDataMutationGuard).acquireUntilTransactionCompletes();
        ordered.verify(userMapper).lockAdminIds();
        ordered.verify(userMapper).findByUsernameForUpdate("admin");
    }

    @Test
    void rootCannotBeRenamedDowngradedDeletedOrGivenTrainingIdentity() {
        AdminUserService service = service();
        User root = user("root", "ROLE_admin");
        when(userMapper.findByUsernameForUpdate("root")).thenReturn(root);

        assertThrows(ForbiddenException.class, () -> service.update("root", new AdminUserUpdateRequest(
                "renamed-root", null, null, null, null, null, null)));
        assertThrows(ForbiddenException.class, () -> service.update("root", new AdminUserUpdateRequest(
                "root", null, null, "ROLE_player", null, null, null)));
        assertThrows(ForbiddenException.class, () -> service.update("root", new AdminUserUpdateRequest(
                "root", null, null, "ROLE_admin", null, Map.of("CODEFORCES", "root"), true)));
        assertThrows(ForbiddenException.class, () -> service.delete("root"));
    }

    @Test
    void rootProtectionUsesTheStoredIdentityForCaseInsensitiveUsernameLookups() {
        when(userMapper.findByUsernameForUpdate("ROOT")).thenReturn(user("root", "ROLE_admin"));

        AdminUserService service = service();
        assertThrows(ForbiddenException.class, () -> service.update("ROOT", new AdminUserUpdateRequest(
                "renamed-root", null, null, null, null, null, null)));
        assertThrows(ForbiddenException.class, () -> service.update("ROOT", new AdminUserUpdateRequest(
                null, null, null, "ROLE_player", null, null, null)));
        assertThrows(ForbiddenException.class, () -> service.update("ROOT", new AdminUserUpdateRequest(
                null, null, null, null, null, Map.of("CODEFORCES", "root"), true)));
        assertThrows(ForbiddenException.class, () -> service.delete("ROOT"));
        org.mockito.Mockito.verifyNoInteractions(handleAccountService, purgeService, imageAssetService);
    }

    @Test
    void rejectsMissingRoleAsBadRequest() {
        assertThrows(BadRequestException.class, () -> service().create(new AdminUserCreateRequest(
                "player", "123456", null, null, null, Map.of(), true)));
    }

    @Test
    void caseInsensitiveLookupDoesNotRenameAnOrdinaryAccount() {
        User player = user("Player", "ROLE_player");
        when(userMapper.findByUsernameForUpdate("PLAYER")).thenReturn(player);
        when(userMapper.updateAdminFields(player, "Player")).thenReturn(1);

        var response = service().update("PLAYER", new AdminUserUpdateRequest(
                null, "昵称", null, null, null, Map.of(), true));

        assertEquals("Player", response.user().getUsername());
        assertEquals(false, response.reloginRequired());
        verify(handleAccountService).create("Player", Map.of(), true);
    }

    @Test
    void rejectsNewPasswordsOverTheBcryptByteLimitBeforeWriting() {
        for (String password : List.of("a".repeat(73), "密".repeat(25))) {
            assertThrows(BadRequestException.class, () -> service().create(new AdminUserCreateRequest(
                    "player", password, null, null, "ROLE_player", Map.of(), true)));
        }
        org.mockito.Mockito.verify(userMapper, org.mockito.Mockito.never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void usernameChangeUsesParentForeignKeyCascadeAndRequiresRelogin() {
        AdminUserService service = service();
        User player = user("old-name", "ROLE_player");
        when(userMapper.findByUsernameForUpdate("old-name")).thenReturn(player);
        when(userMapper.findByUsername("new-name")).thenReturn(null);
        when(userMapper.updateAdminFields(argThat(user -> "new-name".equals(user.getUsername())),
                org.mockito.ArgumentMatchers.eq("old-name"))).thenReturn(1);

        var response = service.update("old-name", new AdminUserUpdateRequest(
                "new-name", null, null, null, null, Map.of(), true));

        assertTrue(response.reloginRequired());
        verify(userMapper).updateAdminFields(player, "old-name");
    }

	@Test
	void blankPatchPasswordGeneratesOneTimeResetPassword() {
		AdminUserService service = service();
		User player = user("player", "ROLE_player");
		when(userMapper.findByUsernameForUpdate("player")).thenReturn(player);
		when(userMapper.updateAdminFields(org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.eq("player"))).thenReturn(1);

        var response = service.update("player", new AdminUserUpdateRequest(
                "player", null, null, null, "", Map.of(), true));

		assertTrue(response.generatedPassword() != null && response.generatedPassword().length() >= 16);
		verify(userMapper).updateAdminFields(argThat(user ->
				HashUtils.matchBC(response.generatedPassword(), user.getPassword())),
				org.mockito.ArgumentMatchers.eq("player"));
	}

    @Test
    void persistsNeedCollectForUserWithoutOjHandlesInAtomicUpdate() {
        AdminUserService service = service();
        User player = user("player", "ROLE_player");
        Instant now = Instant.parse("2026-07-12T00:00:00Z");
        OjHandleAccount active = new OjHandleAccount("player", Map.of(), true, now, now);
        OjHandleAccount retired = new OjHandleAccount("player", Map.of(), false, now, now);
        when(userMapper.findByUsernameForUpdate("player")).thenReturn(player);
        when(userMapper.updateAdminFields(player, "player")).thenReturn(1);
        when(handleAccountService.getByUsername("player")).thenReturn(active);
        when(handleAccountService.replaceHandlesAfterPurge("player", Map.of(), false)).thenReturn(retired);

        var response = service.update("player", new AdminUserUpdateRequest(
                "player", null, null, null, null, Map.of(), false));

        assertEquals(false, response.needCollect());
        assertTrue(response.handles().isEmpty());
    }

    @Test
    void exposesPerOjCollectionProgressToAdministrators() {
        AdminUserService service = service();
        User player = user("player", "ROLE_player");
        Instant createdAt = Instant.parse("2026-07-01T00:00:00Z");
        Instant collectedAt = Instant.parse("2026-07-12T08:30:00Z");
        OjHandleAccount account = new OjHandleAccount(
                "player",
                Map.of("CODEFORCES", "tourist"),
                true,
                Map.of("CODEFORCES", new OjHandleCollectionState(collectedAt)),
                createdAt,
                collectedAt
        );
        when(handleAccountService.listAll()).thenReturn(List.of(account));
        when(userMapper.findAll()).thenReturn(List.of(player));

        var response = service.list().get(0);

        assertEquals(collectedAt, response.collectionStates().get("CODEFORCES").lastCollectedAt());
    }

    @Test
    void purgesChangedAndRemovedHandlesBeforeOneAtomicReplacement() {
        AdminUserService service = service();
        User player = user("player", "ROLE_player");
        Instant now = Instant.parse("2026-07-12T00:00:00Z");
        OjHandleAccount existing = new OjHandleAccount("player", Map.of(
                "CODEFORCES", "tourist", "ATCODER", "old-atcoder"), true, now, now);
        OjHandleAccount replaced = new OjHandleAccount(
                "player", Map.of("CODEFORCES", "Benq"), true, now, now);
        when(userMapper.findByUsernameForUpdate("player")).thenReturn(player);
        when(userMapper.updateAdminFields(player, "player")).thenReturn(1);
        when(handleAccountService.getByUsername("player")).thenReturn(existing);
        when(handleAccountService.replaceHandlesAfterPurge(
                "player", Map.of("CODEFORCES", "Benq"), true)).thenReturn(replaced);

        var response = service.update("player", new AdminUserUpdateRequest(
                "player", null, null, null, null, Map.of("CODEFORCES", "Benq"), true));

        assertEquals("Benq", response.handles().get("CODEFORCES"));
        var ordered = inOrder(purgeService, handleAccountService);
        ordered.verify(purgeService).purgeStudentData("player", "CODEFORCES");
        ordered.verify(purgeService).purgeStudentData("player", "ATCODER");
        ordered.verify(handleAccountService).replaceHandlesAfterPurge(
                "player", Map.of("CODEFORCES", "Benq"), true);
    }

	@Test
	void deletionSchedulesAllUnreferencedManagedAssetsForImmediateCleanup() {
		AdminUserService service = service();
		User player = user("player", "ROLE_player");
		when(userMapper.findByUsernameForUpdate("player")).thenReturn(player);
		when(userMapper.deleteByUsername("player")).thenReturn(1);

		service.delete("player");

		verify(imageAssetService).prepareUserAssetDeletion(1L);
	}

    @Test
    void activeCollectionRejectsUpdateAndDeletionBeforeAnyDatabaseAccess() {
        doThrow(new ConflictException("采集正在运行"))
                .when(trainingDataMutationGuard).acquireUntilTransactionCompletes();

        assertThrows(ConflictException.class, () -> service().update("player", new AdminUserUpdateRequest(
                null, null, null, null, null, Map.of(), true)));
        assertThrows(ConflictException.class, () -> service().delete("player"));

        verifyNoInteractions(userMapper, handleAccountService, purgeService, imageAssetService);
    }

    private AdminUserService service() {
        return new AdminUserService(userMapper, handleAccountService, purgeService, imageAssetService,
                trainingDataMutationGuard);
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
