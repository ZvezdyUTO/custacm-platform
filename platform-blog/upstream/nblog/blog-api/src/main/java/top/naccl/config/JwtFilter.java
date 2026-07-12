package top.naccl.config;

import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.GenericFilterBean;
import top.naccl.model.vo.Result;
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
import top.naccl.service.impl.UserServiceImpl;

/**
 * @Description: JWT请求过滤器
 * @Author: Naccl
 * @Date: 2020-07-21
 */
public class JwtFilter extends GenericFilterBean {
	private final UserServiceImpl userService;

	public JwtFilter(UserServiceImpl userService) {
		this.userService = userService;
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;
		// 受保护路由必须解析 JWT；公开读取端点携带有效 JWT 时也解析，以便按登录态返回内部可见内容。
		String requestUri = request.getRequestURI();
		String contextPath = request.getContextPath();
		boolean protectedRoute = requestUri.startsWith(contextPath + "/admin")
				|| requestUri.startsWith(contextPath + "/player");
		String jwt = request.getHeader("Authorization");
		if (!JwtUtils.judgeTokenIsExist(jwt)) {
			filterChain.doFilter(request, servletResponse);
			return;
		}
		try {
			Claims claims = JwtUtils.getTokenBody(jwt);
			String username = claims.getSubject();
			UserDetails currentUser = userService.loadUserByUsername(username);
			if (!currentUser.isEnabled()) {
				throw new UsernameNotFoundException("用户不可用");
			}
			UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
					currentUser.getUsername(), null, currentUser.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(token);
		} catch (Exception e) {
			SecurityContextHolder.clearContext();
			if (protectedRoute) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.setContentType("application/json;charset=utf-8");
				Result result = Result.error(401, "AUTH_TOKEN_INVALID", "凭证已失效，请重新登录！");
				PrintWriter out = response.getWriter();
				out.write(JacksonUtils.writeValueAsString(result));
				out.flush();
				out.close();
				return;
			}
		}
		filterChain.doFilter(servletRequest, servletResponse);
	}
}
