package top.naccl.controller;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import top.naccl.entity.User;
import top.naccl.exception.LoginBadCredentialsException;
import top.naccl.model.dto.LoginInfo;
import top.naccl.model.vo.Result;
import top.naccl.service.LoginAttemptLimiter;
import top.naccl.service.UserService;
import top.naccl.util.JwtUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 前台登录
 * @Author: Naccl
 * @Date: 2020-09-02
 */
@RestController
public class LoginController {
	private final UserService userService;
	private final LoginAttemptLimiter loginAttemptLimiter;
	private final JwtUtils jwtUtils;

	public LoginController(UserService userService, LoginAttemptLimiter loginAttemptLimiter, JwtUtils jwtUtils) {
		this.userService = userService;
		this.loginAttemptLimiter = loginAttemptLimiter;
		this.jwtUtils = jwtUtils;
	}

	/**
	 * 登录成功后，签发当前用户 Token。
	 *
	 * @param loginInfo
	 * @return
	 */
	@PostMapping("/login")
	public Result login(@RequestBody LoginInfo loginInfo) {
		String username = loginInfo.getUsername();
		loginAttemptLimiter.acquire(username);
		User user;
		try {
			user = userService.findUserByUsernameAndPassword(username, loginInfo.getPassword());
		} catch (UsernameNotFoundException e) {
			throw new LoginBadCredentialsException((int) LoginAttemptLimiter.COOLDOWN.toSeconds());
		}
		loginAttemptLimiter.release(username);
		user.setPassword(null);
		String jwt = jwtUtils.generateToken(user.getUsername(), user.getAuthorities());
		Map<String, Object> map = new HashMap<>(4);
		map.put("user", user);
		map.put("token", jwt);
		return Result.ok("登录成功", map);
	}
}
