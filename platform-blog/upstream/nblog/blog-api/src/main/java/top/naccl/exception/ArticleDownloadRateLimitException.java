package top.naccl.exception;

/**
 * 普通用户在文章下载冷却时间内重复请求。
 *
 * @author huangbingrui.awa
 */
public class ArticleDownloadRateLimitException extends RuntimeException {
	private final int retryAfterSeconds;

	public ArticleDownloadRateLimitException(int retryAfterSeconds) {
		super("下载过于频繁，请 " + retryAfterSeconds + " 秒后再试");
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public int retryAfterSeconds() {
		return retryAfterSeconds;
	}
}
