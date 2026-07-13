package top.naccl.exception;

/**
 * 登录冷却状态暂时无法由 Redis 判定。
 *
 * @author huangbingrui.awa
 */
public class LoginCooldownUnavailableException extends RuntimeException {
	public LoginCooldownUnavailableException(Throwable cause) {
		super("登录服务暂时不可用，请稍后重试", cause);
	}
}
