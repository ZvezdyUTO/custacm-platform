package top.naccl.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import top.naccl.entity.User;
import top.naccl.service.RedisService;
import top.naccl.service.impl.UserServiceImpl;
import top.naccl.util.JwtUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = SecurityConfigTest.ProbeController.class,
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class))
@Import({SecurityConfig.class, MyAuthenticationEntryPoint.class, SecurityConfigTest.ProbeController.class,
		JwtUtils.class})
@TestPropertySource(properties = {
		"token.secretKey=abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ab",
		"token.expireTime=60000"
})
class SecurityConfigTest {
	@Autowired private MockMvc mockMvc;
	@Autowired private JwtUtils jwtUtils;
	@MockitoBean private UserServiceImpl userService;
	@MockitoBean private RedisService redisService;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Test
	void guestCanReadButCannotWritePublicEndpoints() throws Exception {
		mockMvc.perform(get("/public-probe")).andExpect(status().isOk());
		mockMvc.perform(post("/comment").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void playerCanUsePlayerEndpointsButNotAdminEndpoints() throws Exception {
		stubUser("player1", "ROLE_player");
		String token = token("player1", "ROLE_player");
		mockMvc.perform(get("/player/probe").header("Authorization", token)).andExpect(status().isOk());
		mockMvc.perform(get("/admin/probe").header("Authorization", token)).andExpect(status().isForbidden());
	}

	@Test
	void validJwtAuthenticatesAnOptionalPublicReadButInvalidJwtStaysAnonymous() throws Exception {
		stubUser("player1", "ROLE_player");
		String token = token("player1", "ROLE_player");

		mockMvc.perform(get("/public-probe").header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(content().string("player1"));
		mockMvc.perform(get("/public-probe").header("Authorization", "Bearer not-a-jwt"))
				.andExpect(status().isOk())
				.andExpect(content().string("guest"));
	}

	@Test
	void bearerSchemeIsCaseInsensitiveButMalformedAuthorizationIsRejected() throws Exception {
		stubUser("player1", "ROLE_player");
		String rawToken = jwtUtils.generateToken(
				"player1", AuthorityUtils.createAuthorityList("ROLE_player"));

		mockMvc.perform(get("/player/probe").header("Authorization", "bearer " + rawToken))
				.andExpect(status().isOk());
		mockMvc.perform(get("/player/probe").header("Authorization", rawToken))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_INVALID"));
		mockMvc.perform(get("/player/probe").header("Authorization", "BearerBearer " + rawToken))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_INVALID"));
	}

	@Test
	void protectedRouteDetectionDoesNotBleedIntoSimilarPublicPrefixes() throws Exception {
		mockMvc.perform(get("/administrator-probe").header("Authorization", "not-a-bearer-token"))
				.andExpect(status().isOk())
				.andExpect(content().string("guest"));
	}

	@Test
	void adminCanUseBothProtectedTiers() throws Exception {
		stubUser("admin", "ROLE_admin");
		String token = token("admin", "ROLE_admin");
		mockMvc.perform(get("/player/probe").header("Authorization", token)).andExpect(status().isOk());
		mockMvc.perform(get("/admin/probe").header("Authorization", token)).andExpect(status().isOk());
	}

	@Test
	void onlyCurrentPublicLoginRouteIsPermitted() throws Exception {
		mockMvc.perform(post("/login"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/admin/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void currentDatabaseRoleOverridesAuthorityStoredInToken() throws Exception {
		stubUser("changed-user", "ROLE_player");
		String staleAdminToken = token("changed-user", "ROLE_admin");

		mockMvc.perform(get("/admin/probe").header("Authorization", staleAdminToken))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/player/probe").header("Authorization", staleAdminToken))
				.andExpect(status().isOk());
	}

	@Test
	void invalidOrDeletedUserTokenReturnsUnauthorized() throws Exception {
		when(userService.loadUserByUsername("deleted-user"))
				.thenThrow(new UsernameNotFoundException("deleted user"));
		String deletedUserToken = token("deleted-user", "ROLE_player");
		mockMvc.perform(get("/player/probe").header("Authorization", deletedUserToken))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_INVALID"));

		mockMvc.perform(get("/player/probe").header("Authorization", "Bearer not-a-jwt"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_INVALID"));
	}

	@Test
	void userLookupInfrastructureFailureIsNotMisreportedAsAnInvalidToken() throws Exception {
		when(userService.loadUserByUsername("lookup-fails"))
				.thenThrow(new DataAccessResourceFailureException("database unavailable"));
		String token = token("lookup-fails", "ROLE_player");

		mockMvc.perform(get("/player/probe").header("Authorization", token))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.errorCode").value("AUTH_CONTEXT_RESOLUTION_FAILED"));
		mockMvc.perform(get("/public-probe").header("Authorization", token))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.errorCode").value("AUTH_CONTEXT_RESOLUTION_FAILED"));
	}

	private String token(String username, String authority) {
		return "Bearer " + jwtUtils.generateToken(username, AuthorityUtils.createAuthorityList(authority));
	}

	private void stubUser(String username, String role) {
		User user = new User();
		user.setUsername(username);
		user.setPassword(passwordEncoder.encode("unused-password"));
		user.setRole(role);
		when(userService.loadUserByUsername(username)).thenReturn(user);
	}

	@RestController
	static class ProbeController {
		@GetMapping("/public-probe") String publicProbe(org.springframework.security.core.Authentication authentication) {
			return authentication == null ? "guest" : authentication.getName();
		}
		@GetMapping("/administrator-probe") String administratorProbe(
				org.springframework.security.core.Authentication authentication) {
			return authentication == null ? "guest" : authentication.getName();
		}
		@GetMapping("/player/probe") String playerProbe() { return "ok"; }
		@GetMapping("/admin/probe") String adminProbe() { return "ok"; }
			@PostMapping("/comment") String publicWriteProbe() { return "not allowed"; }
			@PostMapping("/login") String loginProbe() { return "ok"; }
		}
}
