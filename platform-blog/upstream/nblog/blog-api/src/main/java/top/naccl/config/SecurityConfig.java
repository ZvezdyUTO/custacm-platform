package top.naccl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import top.naccl.service.impl.UserServiceImpl;
import top.naccl.util.JwtUtils;

/**
 * @Description: Spring Security配置类
 * @Author: Naccl
 * @Date: 2020-07-19
 */
@Configuration
public class SecurityConfig {
	private final UserServiceImpl userService;
	private final MyAuthenticationEntryPoint myAuthenticationEntryPoint;
	private final JwtUtils jwtUtils;

	public SecurityConfig(
			UserServiceImpl userService,
			MyAuthenticationEntryPoint myAuthenticationEntryPoint,
			JwtUtils jwtUtils) {
		this.userService = userService;
		this.myAuthenticationEntryPoint = myAuthenticationEntryPoint;
		this.jwtUtils = jwtUtils;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				//禁用 csrf 防御
				.csrf(AbstractHttpConfigurer::disable)
				//开启跨域支持
				.cors(cors -> {})
				//基于Token，不创建会话
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/login").permitAll()
						.requestMatchers("/admin/**").hasRole("admin")
						.requestMatchers("/player/**").hasAnyRole("admin", "player")
						.requestMatchers(HttpMethod.GET, "/**").permitAll()
						.anyRequest().denyAll())
				//校验显式携带的JWT，并按数据库中的当前角色授权
				.addFilterBefore(new JwtFilter(userService, jwtUtils), UsernamePasswordAuthenticationFilter.class)
				//未登录时，返回json，在前端执行重定向
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(myAuthenticationEntryPoint)
						.accessDeniedHandler((request, response, denied) -> {
							response.setStatus(403);
							response.setContentType("application/json;charset=utf-8");
							response.getWriter().write(top.naccl.util.JacksonUtils.writeValueAsString(
									top.naccl.model.vo.Result.error(403, "AUTH_FORBIDDEN", "无权限")));
						}));
		return http.build();
	}
}
