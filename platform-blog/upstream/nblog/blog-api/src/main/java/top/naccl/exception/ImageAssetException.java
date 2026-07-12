package top.naccl.exception;

/**
 * @author huangbingrui.awa
 */
public class ImageAssetException extends RuntimeException {
	public enum ErrorCode {
		IMAGE_TOO_LARGE,
		IMAGE_FORMAT_UNSUPPORTED,
		IMAGE_DIMENSIONS_INVALID,
		IMAGE_NOT_OWNED,
		IMAGE_PROCESSING_FAILED
	}

	private final ErrorCode errorCode;

	public ImageAssetException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public ImageAssetException(ErrorCode errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	public ErrorCode errorCode() {
		return errorCode;
	}
}
