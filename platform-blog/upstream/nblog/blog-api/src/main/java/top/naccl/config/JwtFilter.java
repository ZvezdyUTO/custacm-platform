package top.naccl.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.GenericFilterBean;
import top.naccl.model.vo.Result;
import top.naccl.service.impl.UserServiceImpl;
import top.naccl.util.JacksonUtils;
import top.naccl.util.JwtUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description: JWT请求过滤器
 * @Author: Naccl
 * @Date: 2020-07-21
 */
public class JwtFilter extends GenericFilterBean {
	private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);
	private static final Pattern BEARER_AUTHORIZATION = Pattern.compile(
			"^Bearer +([^\\s]+)$",
			Pattern.CASE_INSENSITIVE
	);

	private final UserServiceImpl userService;
	private final JwtUtils jwtUtils;

	public JwtFilter(UserServiceImpl userService, JwtUtils jwtUtils) {
		this.userService = userService;
		this.jwtUtils = jwtUtils;
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;
		// 受保护路由必须解析 JWT；公开读取端点携带有效 JWT 时也解析，以便按登录态返回内部可见内容。
		String requestUri = request.getRequestURI();
		String contextPath = request.getContextPath();
		boolean protectedRoute = isWithinPath(requestUri, contextPath + "/admin")
				|| isWithinPath(requestUri, contextPath + "/player");
		String authorization = request.getHeader("Authorization");
		if (authorization == null || authorization.isBlank()) {
			filterChain.doFilter(request, servletResponse);
			return;
		}
		String username = null;
		try {
			String jwt = extractBearerToken(authorization);
			Claims claims = jwtUtils.getTokenBody(jwt);
			String parsedUsername = claims.getSubject();
			if (parsedUsername == null || parsedUsername.isBlank()) {
				throw new IllegalArgumentException("JWT subject must not be blank");
			}
			username = parsedUsername;
		} catch (JwtException | IllegalArgumentException e) {
			if (rejectInvalidToken(response, protectedRoute)) {
				return;
			}
		}
		if (username != null) {
			try {
				UserDetails currentUser = userService.loadUserByUsername(username);
				if (!currentUser.isEnabled()) {
					throw new UsernameNotFoundException("用户不可用");
				}
				UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
						currentUser.getUsername(), null, currentUser.getAuthorities());
				SecurityContextHolder.getContext().setAuthentication(token);
			} catch (UsernameNotFoundException e) {
				if (rejectInvalidToken(response, protectedRoute)) {
					return;
				}
			} catch (Exception e) {
				SecurityContextHolder.clearContext();
				log.error("errorCode=AUTH_CONTEXT_RESOLUTION_FAILED requestUrl={}", requestUri, e);
				writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"AUTH_CONTEXT_RESOLUTION_FAILED", "认证服务暂时不可用");
				return;
			}
		}
		filterChain.doFilter(servletRequest, servletResponse);
	}

	private static boolean isWithinPath(String requestUri, String pathPrefix) {
		return requestUri.equals(pathPrefix) || requestUri.startsWith(pathPrefix + "/");
	}

	private static boolean rejectInvalidToken(HttpServletResponse response, boolean protectedRoute)
			throws IOException {
		SecurityContextHolder.clearContext();
		if (!protectedRoute) {
			return false;
		}
		writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
				"AUTH_TOKEN_INVALID", "凭证已失效，请重新登录！");
		return true;
	}

	private static void writeError(HttpServletResponse response, int status, String errorCode, String message)
			throws IOException {
		response.setStatus(status);
		response.setContentType("application/json;charset=utf-8");
		Result result = Result.error(status, errorCode, message);
		PrintWriter out = response.getWriter();
		out.write(JacksonUtils.writeValueAsString(result));
		out.flush();
	}

	private static String extractBearerToken(String authorization) {
		Matcher matcher = BEARER_AUTHORIZATION.matcher(authorization.trim());
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Authorization must use the Bearer scheme");
		}
		return matcher.group(1);
	}
}
