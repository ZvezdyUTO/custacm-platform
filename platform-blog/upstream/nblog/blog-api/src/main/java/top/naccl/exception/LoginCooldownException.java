package top.naccl.exception;

/**
 * 同一用户名仍处于登录失败冷却窗口。
 *
 * @author huangbingrui.awa
 */
public class LoginCooldownException extends RuntimeException {
	private final int retryAfterSeconds;

	public LoginCooldownException(int retryAfterSeconds) {
		super("登录冷却中，请 " + retryAfterSeconds + " 秒后再试");
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public int retryAfterSeconds() {
		return retryAfterSeconds;
	}
}
