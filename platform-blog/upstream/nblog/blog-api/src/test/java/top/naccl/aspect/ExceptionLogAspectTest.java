package top.naccl.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.naccl.entity.ExceptionLog;
import top.naccl.service.ExceptionLogService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
class ExceptionLogAspectTest {

	@AfterEach
	void clearRequestContext() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	void shouldNotPersistControllerArguments() {
		ExceptionLogService service = mock(ExceptionLogService.class);
		ExceptionLogAspect aspect = new ExceptionLogAspect();
		ReflectionTestUtils.setField(aspect, "exceptionLogService", service);

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRequestURI()).thenReturn("/login");
		when(request.getMethod()).thenReturn("POST");
		when(request.getHeader("User-Agent")).thenReturn("test-agent");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		JoinPoint joinPoint = mock(JoinPoint.class);
		MethodSignature signature = mock(MethodSignature.class);
		when(joinPoint.getSignature()).thenReturn(signature);
		when(joinPoint.getArgs()).thenReturn(new Object[]{new Credentials("player", "plain-password")});
		when(signature.getMethod()).thenReturn(TestController.class.getDeclaredMethods()[0]);

		aspect.logAfterThrowing(joinPoint, new IllegalArgumentException("login failed"));

		verify(service).saveExceptionLog(org.mockito.ArgumentMatchers.argThat(log -> log.getParam() == null));
	}

	private record Credentials(String username, String password) {
	}

	private static class TestController {
		@SuppressWarnings("unused")
		void login(Credentials credentials) {
		}
	}
}
