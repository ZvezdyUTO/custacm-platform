package top.naccl.service.impl;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountException;
import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import com.custacm.platform.trainingdata.common.app.purge.OjStudentDataPurgeService;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.naccl.entity.User;
import top.naccl.exception.BadRequestException;
import top.naccl.exception.ForbiddenException;
import top.naccl.exception.NotFoundException;
import top.naccl.mapper.UserMapper;
import top.naccl.model.dto.AdminUserCreateRequest;
import top.naccl.model.dto.AdminUserPatchRequest;
import top.naccl.model.dto.OjHandlesUpdateRequest;
import top.naccl.model.dto.OjHandleReplaceRequest;
import top.naccl.model.vo.AdminUserMutationResponse;
import top.naccl.util.HashUtils;
import top.naccl.service.ImageAssetService;
import top.naccl.config.BootstrapAdminInitializer;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AdminUserService {
    private static final Set<String> ROLES = Set.of("ROLE_admin", "ROLE_player");
    private static final Pattern USERNAME = Pattern.compile("[\\p{L}\\p{N}._-]{1,128}");
    private static final char[] PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserMapper userMapper;
    private final OjHandleAccountService handleAccountService;
    private final OjStudentDataPurgeService purgeService;
    private final ImageAssetService imageAssetService;

    public AdminUserService(
            UserMapper userMapper,
            OjHandleAccountService handleAccountService,
            OjStudentDataPurgeService purgeService,
            ImageAssetService imageAssetService
    ) {
        this.userMapper = userMapper;
        this.handleAccountService = handleAccountService;
        this.purgeService = purgeService;
        this.imageAssetService = imageAssetService;
    }

    @Transactional
    public AdminUserMutationResponse create(AdminUserCreateRequest request) {
        if (request == null) {
            throw new BadRequestException("请求体不能为空");
        }
        String username = normalizeUsername(request.username());
        String role = normalizeRole(request.role());
        if (userMapper.findByUsername(username) != null) {
            throw new BadRequestException("用户名已存在");
        }
        String generatedPassword = request.password() == null || request.password().isBlank()
                ? randomPassword() : null;
        String rawPassword = generatedPassword == null ? request.password() : generatedPassword;
        validatePassword(rawPassword);

        User user = new User();
        user.setUsername(username);
        user.setPassword(HashUtils.getBC(rawPassword));
        user.setNickname(request.nickname() == null || request.nickname().isBlank()
				? username : request.nickname().trim());
        user.setEmail(trimToEmpty(request.email()));
        user.setAvatar("");
        user.setRole(role);
        if (userMapper.insert(user) != 1) {
            throw new IllegalStateException("创建用户失败");
        }
        Map<String, String> handles = request.handles() == null ? Map.of() : request.handles();
        boolean needCollect = request.needCollect() == null || request.needCollect();
        if (!handles.isEmpty()) {
            handleAccountService.create(username, handles, needCollect);
        }
        return response(user, handles, needCollect, generatedPassword, false);
    }

    @Transactional
    public List<AdminUserMutationResponse> batchCreate(List<AdminUserCreateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException("用户列表不能为空");
        }
        return requests.stream().map(this::create).toList();
    }

    public List<AdminUserMutationResponse> list() {
        return userMapper.findAll().stream().map(user -> responseWithCurrentHandles(user, null, false)).toList();
    }

    public AdminUserMutationResponse get(String username) {
        return responseWithCurrentHandles(requireUser(normalizeUsername(username)), null, false);
    }

    @Transactional
    public AdminUserMutationResponse patch(String username, AdminUserPatchRequest request) {
        if (request == null) {
            throw new BadRequestException("请求体不能为空");
        }
        String oldUsername = normalizeUsername(username);
        User user = requireUser(oldUsername);
        String newUsername = request.newUsername() == null ? oldUsername : normalizeUsername(request.newUsername());
        if (isRoot(oldUsername) && !oldUsername.equals(newUsername)) {
            throw new ForbiddenException("root 用户名不可修改");
        }
        if (!oldUsername.equals(newUsername) && userMapper.findByUsername(newUsername) != null) {
            throw new BadRequestException("用户名已存在");
        }
        String newRole = request.role() == null ? user.getRole() : normalizeRole(request.role());
        if (isRoot(oldUsername) && !"ROLE_admin".equals(newRole)) {
            throw new ForbiddenException("root 必须保持管理员角色");
        }
        if ("ROLE_admin".equals(user.getRole()) && !"ROLE_admin".equals(newRole)) {
            requireAnotherAdmin();
        }
        user.setUsername(newUsername);
        user.setNickname(request.nickname() == null ? user.getNickname() : trimToEmpty(request.nickname()));
        user.setEmail(request.email() == null ? user.getEmail() : trimToEmpty(request.email()));
        user.setRole(newRole);
        String generatedPassword = null;
        if (request.password() != null) {
			String rawPassword = request.password();
			if (rawPassword.isBlank()) {
				generatedPassword = randomPassword();
				rawPassword = generatedPassword;
			}
			validatePassword(rawPassword);
			user.setPassword(HashUtils.getBC(rawPassword));
        }
        if (userMapper.updateAdminFields(user, oldUsername) != 1) {
            throw new IllegalStateException("修改用户失败");
        }
        return responseWithCurrentHandles(user, generatedPassword, !oldUsername.equals(newUsername));
    }

    @Transactional
    public AdminUserMutationResponse updateHandles(String username, OjHandlesUpdateRequest request) {
        if (request == null || (request.needCollect() == null
                && (request.handles() == null || request.handles().isEmpty()))) {
            throw new BadRequestException("handles 和 needCollect 不能同时为空");
        }
        String normalizedUsername = normalizeUsername(username);
        User user = requireUser(normalizedUsername);
        if (isRoot(normalizedUsername)) {
            throw new ForbiddenException("root 不能绑定 OJ handle 或设置队员状态");
        }
        boolean needCollect = request.needCollect() == null || request.needCollect();
        Map<String, String> handles = request.handles() == null ? Map.of() : request.handles();
        OjHandleAccount account;
        try {
            account = handleAccountService.changeUsername(
                    normalizedUsername, normalizedUsername, needCollect, handles);
        } catch (OjHandleAccountException ex) {
            if (ex.errorCode() != OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_NOT_FOUND) {
                throw ex;
            }
            account = handleAccountService.create(normalizedUsername, handles, needCollect);
        }
        return response(user, account.handles(), account.needCollect(), null, false);
    }

    @Transactional
    public AdminUserMutationResponse replaceHandle(String username, OjHandleReplaceRequest request) {
        if (request == null) {
            throw new BadRequestException("请求体不能为空");
        }
        String normalizedUsername = normalizeUsername(username);
        User user = requireUser(normalizedUsername);
        if (isRoot(normalizedUsername)) {
            throw new ForbiddenException("root 不能更换 OJ handle");
        }
        if (request.newHandle() == null || request.newHandle().isBlank()) {
            throw new BadRequestException("newHandle 不能为空");
        }
        String normalizedNewHandle = request.newHandle().trim();
        OjHandleAccount existing = handleAccountService.getByUsername(normalizedUsername);
        String oldHandle = handleAccountService.getHandle(existing, request.ojName());
        if (oldHandle.equals(normalizedNewHandle)) {
            return response(user, existing.handles(), existing.needCollect(), null, false);
        }
        purgeService.purgeStudentData(normalizedUsername, request.ojName());
        OjHandleAccount replaced = handleAccountService.replaceHandleAfterPurge(
                normalizedUsername, request.ojName(), normalizedNewHandle);
        return response(user, replaced.handles(), replaced.needCollect(), null, false);
    }

    @Transactional
    public void delete(String username) {
        String normalizedUsername = normalizeUsername(username);
        User user = requireUser(normalizedUsername);
        if (isRoot(normalizedUsername)) {
            throw new ForbiddenException("root 用户不可删除");
        }
        if ("ROLE_admin".equals(user.getRole())) {
            requireAnotherAdmin();
        }
        OjHandleAccount account = findHandleAccount(normalizedUsername);
        if (account != null) {
            account.handles().keySet().forEach(ojName -> purgeService.purgeStudentData(normalizedUsername, ojName));
		}
		userMapper.anonymizeCommentsByUserId(user.getId());
		imageAssetService.prepareUserAssetDeletion(user.getId());
        if (userMapper.deleteByUsername(normalizedUsername) != 1) {
            throw new IllegalStateException("删除用户失败");
        }
    }

    private void requireAnotherAdmin() {
        if (userMapper.lockAdminIds().size() <= 1) {
            throw new ForbiddenException("系统必须至少保留一个管理员");
        }
    }

    private User requireUser(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        return user;
    }

    private AdminUserMutationResponse responseWithCurrentHandles(User user, String password, boolean relogin) {
        OjHandleAccount account = findHandleAccount(user.getUsername());
        return response(user, account == null ? Map.of() : account.handles(),
                account == null ? null : account.needCollect(), password, relogin);
    }

    private OjHandleAccount findHandleAccount(String username) {
        try {
            return handleAccountService.getByUsername(username);
        } catch (OjHandleAccountException ex) {
            if (ex.errorCode() == OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_NOT_FOUND) {
                return null;
            }
            throw ex;
        }
    }

    private static AdminUserMutationResponse response(
            User source, Map<String, String> handles, Boolean needCollect, String generatedPassword, boolean relogin
    ) {
        User safe = new User();
        safe.setId(source.getId());
        safe.setUsername(source.getUsername());
        safe.setNickname(source.getNickname());
        safe.setAvatar(source.getAvatar());
        safe.setEmail(source.getEmail());
        safe.setCreateTime(source.getCreateTime());
        safe.setUpdateTime(source.getUpdateTime());
        safe.setRole(source.getRole());
        return new AdminUserMutationResponse(safe, Map.copyOf(handles), needCollect, generatedPassword, relogin);
    }

    private static String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim();
        if (!USERNAME.matcher(normalized).matches()) {
            throw new BadRequestException("用户名需为 1 到 128 个中文、字母、数字、点、下划线或连字符");
        }
        return normalized;
    }

    private static String normalizeRole(String role) {
        if (!ROLES.contains(role)) {
            throw new BadRequestException("角色只能是 ROLE_admin 或 ROLE_player");
        }
        return role;
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 6 || password.length() > 128) {
            throw new BadRequestException("密码长度需为 6 到 128 个字符");
        }
    }

    private static String trimToEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static String randomPassword() {
        StringBuilder password = new StringBuilder(20);
        for (int i = 0; i < 20; i++) {
            password.append(PASSWORD_ALPHABET[RANDOM.nextInt(PASSWORD_ALPHABET.length)]);
        }
        return password.toString();
    }

    private static boolean isRoot(String username) {
        return BootstrapAdminInitializer.ROOT_USERNAME.equals(username);
    }
}
