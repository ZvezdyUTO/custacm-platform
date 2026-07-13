package top.naccl.exception;

/**
 * 用户名或密码错误，并告知客户端本次失败触发的冷却时长。
 *
 * @author huangbingrui.awa
 */
public class LoginBadCredentialsException extends RuntimeException {
	private final int retryAfterSeconds;

	public LoginBadCredentialsException(int retryAfterSeconds) {
		super("用户名或密码错误！");
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public int retryAfterSeconds() {
		return retryAfterSeconds;
	}
}
