package top.naccl.exception;

/**
 * 文章下载限流状态暂时不可用。
 *
 * @author huangbingrui.awa
 */
public class ArticleDownloadRateLimitUnavailableException extends RuntimeException {
	public ArticleDownloadRateLimitUnavailableException(Throwable cause) {
		super("下载服务暂时不可用，请稍后重试", cause);
	}
}
