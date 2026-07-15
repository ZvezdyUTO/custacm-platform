package top.naccl.handler;

import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * @author huangbingrui.awa
 */
class HttpErrorContractTest {
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = standaloneSetup(new ErrorProbeController())
				.setControllerAdvice(new ControllerExceptionHandler())
				.build();
	}

	@Test
	void typeMismatchReturnsStructuredBadRequest() throws Exception {
		mockMvc.perform(get("/error-probe/number").param("value", "not-a-number"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.errorCode").value("REQUEST_PARAMETER_INVALID"));
	}

	@Test
	void missingRequestParameterReturnsStructuredBadRequest() throws Exception {
		mockMvc.perform(get("/error-probe/number"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.errorCode").value("REQUEST_PARAMETER_INVALID"));
	}

	@Test
	void methodParameterValidationReturnsStructuredBadRequest() throws Exception {
		mockMvc.perform(get("/error-probe/minimum").param("value", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.errorCode").value("REQUEST_VALIDATION_FAILED"));
	}

	@Test
	void malformedJsonReturnsStructuredBadRequest() throws Exception {
		mockMvc.perform(post("/error-probe/body")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.errorCode").value("REQUEST_BODY_INVALID"));
	}

	@Test
	void missingMultipartPartKeepsTheFrameworkBadRequestStatus() throws Exception {
		mockMvc.perform(multipart("/error-probe/part"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.errorCode").value("REQUEST_ERROR"));
	}

	@Test
	void unsupportedHttpMethodKeepsTheFrameworkMethodNotAllowedStatus() throws Exception {
		mockMvc.perform(post("/error-probe/number"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(header().string(HttpHeaders.ALLOW, "GET"))
				.andExpect(jsonPath("$.code").value(405))
				.andExpect(jsonPath("$.errorCode").value("REQUEST_ERROR"));
	}

	@Test
	void unsupportedMediaTypeKeepsTheFrameworkStatus() throws Exception {
		mockMvc.perform(post("/error-probe/body")
						.contentType(MediaType.TEXT_PLAIN)
						.content("plain text"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.code").value(415))
				.andExpect(jsonPath("$.errorCode").value("REQUEST_ERROR"));
	}

	@Test
	void beanValidationReturnsStructuredBadRequest() throws Exception {
		mockMvc.perform(post("/error-probe/body")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.errorCode").value("REQUEST_VALIDATION_FAILED"));
	}

	@Test
	void internalIllegalArgumentReturnsStructuredInternalError() throws Exception {
		mockMvc.perform(get("/error-probe/illegal"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value(500))
				.andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
	}

	@Test
	void methodReturnValueValidationRemainsAnInternalError() throws Exception {
		mockMvc.perform(get("/error-probe/invalid-return"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value(500))
				.andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
	}

	@Test
	void missingControllerPathVariableRemainsAnInternalError() throws Exception {
		mockMvc.perform(get("/error-probe/path/value"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value(500))
				.andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
	}

	@Test
	void genericConstraintViolationRemainsAnInternalError() throws Exception {
		mockMvc.perform(get("/error-probe/constraint"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value(500))
				.andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
	}

	@RestController
	static class ErrorProbeController {
		@GetMapping("/error-probe/number")
		String number(@RequestParam int value) {
			return Integer.toString(value);
		}

		@GetMapping("/error-probe/minimum")
		String minimum(@RequestParam @Min(1) int value) {
			return Integer.toString(value);
		}

		@PostMapping("/error-probe/body")
		String body(@Valid @RequestBody ProbeRequest request) {
			return request.name();
		}

		@PostMapping(value = "/error-probe/part", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
		String part(@RequestPart("file") MultipartFile file) {
			return file.getOriginalFilename();
		}

		@GetMapping("/error-probe/illegal")
		String illegal() {
			throw new IllegalArgumentException("非法参数");
		}

		@GetMapping("/error-probe/invalid-return")
		@Min(1)
		int invalidReturn() {
			return 0;
		}

		@GetMapping("/error-probe/path/{actual}")
		String missingPathVariable(@PathVariable("missing") String missing) {
			return missing;
		}

		@GetMapping("/error-probe/constraint")
		String constraintViolation() {
			throw new ConstraintViolationException(java.util.Set.of());
		}
	}

	record ProbeRequest(@NotBlank String name) {
	}
}
