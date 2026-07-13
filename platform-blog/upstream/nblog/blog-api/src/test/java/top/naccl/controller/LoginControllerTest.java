package top.naccl.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import top.naccl.exception.LoginBadCredentialsException;
import top.naccl.model.dto.LoginInfo;
import top.naccl.service.LoginAttemptLimiter;
import top.naccl.service.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
class LoginControllerTest {
	@Test
	void failedCredentialsKeepTheFiveSecondReservation() {
		UserService userService = mock(UserService.class);
		LoginAttemptLimiter limiter = mock(LoginAttemptLimiter.class);
		LoginController controller = new LoginController(userService, limiter);
		LoginInfo loginInfo = new LoginInfo();
		loginInfo.setUsername("player1");
		loginInfo.setPassword("wrong");
		when(userService.findUserByUsernameAndPassword("player1", "wrong"))
				.thenThrow(new UsernameNotFoundException("密码错误"));

		LoginBadCredentialsException exception = assertThrows(
				LoginBadCredentialsException.class, () -> controller.login(loginInfo));

		assertEquals(5, exception.retryAfterSeconds());
		verify(limiter).acquire("player1");
		verify(limiter, never()).release("player1");
	}
}
