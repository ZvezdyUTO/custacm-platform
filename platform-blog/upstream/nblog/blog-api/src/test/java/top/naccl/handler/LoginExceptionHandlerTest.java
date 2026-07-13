package top.naccl.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import top.naccl.exception.LoginBadCredentialsException;
import top.naccl.exception.LoginCooldownException;
import top.naccl.exception.LoginCooldownUnavailableException;
import top.naccl.model.vo.Result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
class LoginExceptionHandlerTest {
	private final ControllerExceptionHandler handler = new ControllerExceptionHandler();
	private final HttpServletRequest request = mock(HttpServletRequest.class);

	@Test
	void returnsTheInitialFiveSecondCooldownWithBadCredentials() {
		when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/login"));

		ResponseEntity<Result> response = handler.loginBadCredentialsExceptionHandler(
				request, new LoginBadCredentialsException(5));

		assertEquals(401, response.getStatusCode().value());
		assertEquals("5", response.getHeaders().getFirst("Retry-After"));
		assertEquals("AUTH_BAD_CREDENTIALS", response.getBody().getErrorCode());
	}

	@Test
	void returns429WhileTheCooldownIsActive() {
		when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/login"));

		ResponseEntity<Result> response = handler.loginCooldownExceptionHandler(
				request, new LoginCooldownException(4));

		assertEquals(429, response.getStatusCode().value());
		assertEquals("4", response.getHeaders().getFirst("Retry-After"));
		assertEquals("AUTH_LOGIN_COOLDOWN", response.getBody().getErrorCode());
	}

	@Test
	void returns503WhenTheCooldownStoreIsUnavailable() {
		when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/login"));

		ResponseEntity<Result> response = handler.loginCooldownUnavailableExceptionHandler(
				request, new LoginCooldownUnavailableException(new IllegalStateException("redis")));

		assertEquals(503, response.getStatusCode().value());
		assertEquals("AUTH_LOGIN_COOLDOWN_UNAVAILABLE", response.getBody().getErrorCode());
	}
}
