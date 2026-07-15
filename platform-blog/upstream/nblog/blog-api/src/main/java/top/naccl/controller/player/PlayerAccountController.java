package top.naccl.controller.player;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountException;
import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import top.naccl.model.dto.PasswordUpdate;
import top.naccl.model.dto.PlayerProfileUpdateRequest;
import top.naccl.model.dto.ProfileLinksReplaceRequest;
import top.naccl.model.vo.Result;
import top.naccl.service.UserService;
import top.naccl.service.PlayerAvatarService;
import top.naccl.service.PlayerProfileService;
import top.naccl.util.StringUtils;
import top.naccl.exception.BadRequestException;
import top.naccl.exception.PersistenceException;

import java.util.Map;

/**
 * @author huangbingrui.awa
 */
@RestController
@RequestMapping("/player/me")
public class PlayerAccountController {
	@Autowired
	private UserService userService;
	@Autowired
	private PlayerAvatarService playerAvatarService;
	@Autowired
	private PlayerProfileService playerProfileService;
	@Autowired
	private OjHandleAccountService ojHandleAccountService;

	@GetMapping
	public Result me(Authentication authentication) {
		return Result.ok("获取成功", playerProfileService.get(authentication.getName()));
	}

	@PatchMapping("/profile")
	public Result updateProfile(Authentication authentication, @RequestBody PlayerProfileUpdateRequest request) {
		return Result.ok("修改成功", playerProfileService.update(authentication.getName(), request));
	}

	@PutMapping("/profile-links")
	public Result replaceProfileLinks(Authentication authentication, @RequestBody ProfileLinksReplaceRequest request) {
		return Result.ok("保存成功", playerProfileService.replaceLinks(authentication.getName(), request));
	}

	@GetMapping("/oj-handles")
	public Result ojHandles(Authentication authentication) {
		try {
			return Result.ok("获取成功", ojHandleAccountService.getByUsername(authentication.getName()).handles());
		} catch (OjHandleAccountException exception) {
			if (exception.errorCode() == OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_NOT_FOUND) {
				return Result.ok("获取成功", Map.of());
			}
			throw exception;
		}
	}

	@PostMapping(value = "/avatar", consumes = "multipart/form-data")
	public Result updateAvatar(Authentication authentication, @RequestPart("file") MultipartFile file) {
		return Result.ok("头像已更新", playerAvatarService.updateAvatar(authentication.getName(), file));
	}

	@PatchMapping("/password")
	public Result updatePassword(Authentication authentication, @RequestBody PasswordUpdate update) {
		if (StringUtils.isEmpty(update.getOldPassword(), update.getNewPassword()) || update.getNewPassword().length() < 6) {
			throw new BadRequestException("新密码至少需要 6 个字符");
		}
		try {
			if (!userService.changePassword(authentication.getName(), update.getOldPassword(), update.getNewPassword())) {
				throw new PersistenceException("修改失败");
			}
			return Result.ok("修改成功");
		} catch (BadCredentialsException e) {
			throw new BadRequestException("旧密码错误", e);
		}
	}
}
