package top.naccl.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountException;
import com.custacm.platform.trainingdata.common.app.purge.OjStudentDataPurgeException;

import jakarta.servlet.http.HttpServletRequest;
import top.naccl.exception.ArticleDownloadRateLimitException;
import top.naccl.exception.ArticleDownloadRateLimitUnavailableException;
import top.naccl.exception.BadRequestException;
import top.naccl.exception.ConflictException;
import top.naccl.exception.ForbiddenException;
import top.naccl.exception.ImageAssetException;
import top.naccl.exception.LoginBadCredentialsException;
import top.naccl.exception.LoginCooldownException;
import top.naccl.exception.LoginCooldownUnavailableException;
import top.naccl.exception.NotFoundException;
import top.naccl.exception.PersistenceException;
import top.naccl.model.vo.Result;

/**
 * @Description: 对Controller层全局异常处理
 * @RestControllerAdvice 捕获异常后，返回json数据类型
 * @Author: Naccl
 * @Date: 2020-08-14
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ControllerExceptionHandler {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * 捕获自定义的404异常
	 *
	 * @param request 请求
	 * @param e       自定义抛出的异常信息
	 * @return
	 */
	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<Result> notFoundExceptionHandler(HttpServletRequest request, NotFoundException e) {
		logger.warn("errorCode=RESOURCE_NOT_FOUND requestUrl={} message={}", request.getRequestURL(), e.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(Result.error(404, "RESOURCE_NOT_FOUND", e.getMessage()));
	}

	/**
	 * 捕获自定义的持久化异常
	 *
	 * @param request 请求
	 * @param e       自定义抛出的异常信息
	 * @return
	 */
	@ExceptionHandler(PersistenceException.class)
	public ResponseEntity<Result> persistenceExceptionHandler(HttpServletRequest request, PersistenceException e) {
		logger.error("errorCode=PERSISTENCE_ERROR requestUrl={}", request.getRequestURL(), e);
		return ResponseEntity.internalServerError().body(Result.error(500, "PERSISTENCE_ERROR", e.getMessage()));
	}

	/**
	 * 捕获自定义的登录失败异常
	 *
	 * @param request 请求
	 * @param e       自定义抛出的异常信息
	 * @return
	 */
	@ExceptionHandler(UsernameNotFoundException.class)
	public ResponseEntity<Result> usernameNotFoundExceptionHandler(HttpServletRequest request, UsernameNotFoundException e) {
		logger.warn("errorCode=AUTH_BAD_CREDENTIALS requestUrl={}", request.getRequestURL());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(Result.error(401, "AUTH_BAD_CREDENTIALS", "用户名或密码错误！"));
	}

	@ExceptionHandler(LoginBadCredentialsException.class)
	public ResponseEntity<Result> loginBadCredentialsExceptionHandler(
			HttpServletRequest request, LoginBadCredentialsException e) {
		logger.warn("errorCode=AUTH_BAD_CREDENTIALS requestUrl={} retryAfterSeconds={}",
				request.getRequestURL(), e.retryAfterSeconds());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.header(HttpHeaders.RETRY_AFTER, Integer.toString(e.retryAfterSeconds()))
				.body(Result.error(401, "AUTH_BAD_CREDENTIALS", e.getMessage()));
	}

	@ExceptionHandler(LoginCooldownException.class)
	public ResponseEntity<Result> loginCooldownExceptionHandler(
			HttpServletRequest request, LoginCooldownException e) {
		logger.warn("errorCode=AUTH_LOGIN_COOLDOWN requestUrl={} retryAfterSeconds={}",
				request.getRequestURL(), e.retryAfterSeconds());
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.header(HttpHeaders.RETRY_AFTER, Integer.toString(e.retryAfterSeconds()))
				.body(Result.error(429, "AUTH_LOGIN_COOLDOWN", e.getMessage()));
	}

	@ExceptionHandler(LoginCooldownUnavailableException.class)
	public ResponseEntity<Result> loginCooldownUnavailableExceptionHandler(
			HttpServletRequest request, LoginCooldownUnavailableException e) {
		logger.error("errorCode=AUTH_LOGIN_COOLDOWN_UNAVAILABLE requestUrl={}", request.getRequestURL(), e);
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(Result.error(503, "AUTH_LOGIN_COOLDOWN_UNAVAILABLE", e.getMessage()));
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<Result> badRequestExceptionHandler(HttpServletRequest request, BadRequestException e) {
		logger.warn("errorCode=BAD_REQUEST requestUrl={} message={}", request.getRequestURL(), e.getMessage());
		return ResponseEntity.badRequest().body(Result.error(400, "BAD_REQUEST", e.getMessage()));
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<Result> conflictExceptionHandler(HttpServletRequest request, ConflictException e) {
		logger.warn("errorCode=RESOURCE_CONFLICT requestUrl={} message={}", request.getRequestURL(), e.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(Result.error(409, "RESOURCE_CONFLICT", e.getMessage()));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Result> unreadableRequestBodyHandler(HttpServletRequest request) {
		logger.warn("errorCode=REQUEST_BODY_INVALID requestUrl={}", request.getRequestURL());
		return ResponseEntity.badRequest()
				.body(Result.error(400, "REQUEST_BODY_INVALID", "请求体格式错误"));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Result> requestValidationHandler(HttpServletRequest request) {
		logger.warn("errorCode=REQUEST_VALIDATION_FAILED requestUrl={}", request.getRequestURL());
		return ResponseEntity.badRequest()
				.body(Result.error(400, "REQUEST_VALIDATION_FAILED", "请求参数校验失败"));
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<Result> methodValidationHandler(
			HttpServletRequest request, HandlerMethodValidationException e) {
		if (e.isForReturnValue()) {
			logger.error("errorCode=INTERNAL_ERROR requestUrl={}", request.getRequestURL(), e);
			return ResponseEntity.internalServerError()
					.body(Result.error(500, "INTERNAL_ERROR", "异常错误"));
		}
		logger.warn("errorCode=REQUEST_VALIDATION_FAILED requestUrl={}", request.getRequestURL());
		return ResponseEntity.badRequest()
				.body(Result.error(400, "REQUEST_VALIDATION_FAILED", "请求参数校验失败"));
	}

	@ExceptionHandler({BindException.class, MethodArgumentTypeMismatchException.class})
	public ResponseEntity<Result> requestBindingHandler(HttpServletRequest request) {
		logger.warn("errorCode=REQUEST_PARAMETER_INVALID requestUrl={}", request.getRequestURL());
		return ResponseEntity.badRequest()
				.body(Result.error(400, "REQUEST_PARAMETER_INVALID", "请求参数格式错误"));
	}

	@ExceptionHandler(ServletRequestBindingException.class)
	public ResponseEntity<Result> servletRequestBindingHandler(
			HttpServletRequest request, ServletRequestBindingException e) {
		if (e.getStatusCode().is5xxServerError()) {
			logger.error("errorCode=INTERNAL_ERROR requestUrl={}", request.getRequestURL(), e);
			return ResponseEntity.status(e.getStatusCode())
					.body(Result.error(e.getStatusCode().value(), "INTERNAL_ERROR", "异常错误"));
		}
		logger.warn("errorCode=REQUEST_PARAMETER_INVALID requestUrl={}", request.getRequestURL());
		return ResponseEntity.status(e.getStatusCode())
				.body(Result.error(e.getStatusCode().value(),
						"REQUEST_PARAMETER_INVALID", "请求参数格式错误"));
	}

	@ExceptionHandler(ImageAssetException.class)
	public ResponseEntity<Result> imageAssetExceptionHandler(HttpServletRequest request, ImageAssetException e) {
		logger.warn("errorCode={} requestUrl={} message={}", e.errorCode(), request.getRequestURL(), e.getMessage());
		return ResponseEntity.badRequest().body(Result.error(400, e.errorCode().name(), e.getMessage()));
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<Result> forbiddenExceptionHandler(HttpServletRequest request, ForbiddenException e) {
		logger.warn("errorCode=AUTH_FORBIDDEN requestUrl={} message={}", request.getRequestURL(), e.getMessage());
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(Result.error(403, "AUTH_FORBIDDEN", e.getMessage()));
	}

	@ExceptionHandler(ArticleDownloadRateLimitException.class)
	public ResponseEntity<Result> articleDownloadRateLimitExceptionHandler(
			HttpServletRequest request, ArticleDownloadRateLimitException e) {
		logger.warn("errorCode=ARTICLE_DOWNLOAD_RATE_LIMITED requestUrl={} retryAfterSeconds={}",
				request.getRequestURL(), e.retryAfterSeconds());
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.header(HttpHeaders.RETRY_AFTER, Integer.toString(e.retryAfterSeconds()))
				.body(Result.error(429, "ARTICLE_DOWNLOAD_RATE_LIMITED", e.getMessage()));
	}

	@ExceptionHandler(ArticleDownloadRateLimitUnavailableException.class)
	public ResponseEntity<Result> articleDownloadRateLimitUnavailableExceptionHandler(
			HttpServletRequest request, ArticleDownloadRateLimitUnavailableException e) {
		logger.error("errorCode=ARTICLE_DOWNLOAD_RATE_LIMIT_UNAVAILABLE requestUrl={}", request.getRequestURL(), e);
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(Result.error(503, "ARTICLE_DOWNLOAD_RATE_LIMIT_UNAVAILABLE", e.getMessage()));
	}

	@ExceptionHandler(OjHandleAccountException.class)
	public ResponseEntity<Result> handleAccountExceptionHandler(HttpServletRequest request, OjHandleAccountException e) {
		logger.warn("errorCode={} requestUrl={} message={}", e.errorCode(), request.getRequestURL(), e.getMessage());
		HttpStatus status = e.errorCode() == OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_NOT_FOUND
				? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
		return ResponseEntity.status(status).body(Result.error(status.value(), e.errorCode().name(), e.getMessage()));
	}

	@ExceptionHandler(OjStudentDataPurgeException.class)
	public ResponseEntity<Result> purgeExceptionHandler(HttpServletRequest request, OjStudentDataPurgeException e) {
		logger.error("errorCode={} requestUrl={}", e.errorCode(), request.getRequestURL(), e);
		return ResponseEntity.badRequest().body(Result.error(400, e.errorCode().name(), e.getMessage()));
	}

	/**
	 * 捕获其它异常
	 *
	 * @param request 请求
	 * @param e       异常信息
	 * @return
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Result> exceptionHandler(HttpServletRequest request, Exception e) {
		if (e instanceof ErrorResponse errorResponse) {
			int status = errorResponse.getStatusCode().value();
			if (errorResponse.getStatusCode().is4xxClientError()) {
				logger.warn("errorCode=REQUEST_ERROR requestUrl={} status={}", request.getRequestURL(), status);
				return ResponseEntity.status(errorResponse.getStatusCode())
						.headers(errorResponse.getHeaders())
						.body(Result.error(status, "REQUEST_ERROR", "请求无法处理"));
			}
			if (errorResponse.getStatusCode().is5xxServerError()) {
				logger.error("errorCode=INTERNAL_ERROR requestUrl={} status={}",
						request.getRequestURL(), status, e);
				return ResponseEntity.status(errorResponse.getStatusCode())
						.headers(errorResponse.getHeaders())
						.body(Result.error(status, "INTERNAL_ERROR", "异常错误"));
			}
		}
		logger.error("errorCode=INTERNAL_ERROR requestUrl={}", request.getRequestURL(), e);
		return ResponseEntity.internalServerError().body(Result.error(500, "INTERNAL_ERROR", "异常错误"));
	}
}
