package top.naccl.exception;

/**
 * 表示请求与当前资源状态冲突。
 *
 * @author huangbingrui.awa
 */
public class ConflictException extends RuntimeException {
	public ConflictException(String message) {
		super(message);
	}

	public ConflictException(String message, Throwable cause) {
		super(message, cause);
	}
}
