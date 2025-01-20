package servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import Enum.Constants.HttpStatusCodes;
import Enum.Constants.RolePermission;
import Enum.Constants.ValidPaths;
import cache.CacheUtil;
import service.AccountService;
import util.CustomException;
import util.Helper;
import util.JwtUtil;

@SuppressWarnings("serial")
@WebFilter("/api/*")
public class AuthFilter extends HttpFilter implements Filter {

	private final Logger logger = LogManager.getLogger(AuthFilter.class);
	CacheUtil cacheUtil = new CacheUtil();

	@SuppressWarnings("unchecked")
	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		String method = request.getMethod();
		String path = request.getPathInfo();
		String handler = Helper.getHandler(request);

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		logger.info("Request path: {}", path);

		if (path.equals("/Login")) {
			logger.info("Skipping authentication for login endpoint.");
			chain.doFilter(request, response);
			return;
		}

		if (!ValidPaths.isValidPath(path)) {
			Helper.sendJsonResponse(response, HttpStatusCodes.NOT_FOUND, "Invalid URL path", null);
			return;
		}

		String authorizationHeader = request.getHeader("Authorization");

		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			logger.warn("Authorization token not found in request.");
			response.getWriter().println("Authorization token not found");
			return;
		}

		String token = authorizationHeader.substring(7);
		logger.info("Authorization token found. Verifying token...");

		try {
			JwtUtil.verifyToken(token);
		} catch (Exception e) {
			Helper.sendJsonResponse(response, HttpStatusCodes.UNAUTHORIZED, "Invalid token", null);
			return;
		}

		Long id = JwtUtil.extractUserId(token);
		String role = JwtUtil.extractRole(token);
		List<String> allowedMethods;

		// token check
		String cachedToken = cacheUtil.get(id.toString(), new TypeReference<String>() {
		});
		if (cachedToken == null || !cachedToken.equals(token)) {
			Helper.sendJsonResponse(response, HttpStatusCodes.UNAUTHORIZED, "Invalid token", null);
			return;
		}

		Map<String, Object> claimsMap = JwtUtil.extractToken(token);

		Helper.setThreadLocalValue(claimsMap);
		logger.info("Token verified successfully. Claims: {}", claimsMap);

		try {
			// Account check
			AccountService accountService = new AccountService();
			Map<String, Object> accountMap = new HashMap<>();
			accountMap.put("userId", id);
			Map<String, Object> accountResult = accountService.getAccountDetails(accountMap);
			if (!accountResult.containsKey("accounts") || ((List<Object>) accountResult.get("accounts")).isEmpty()) {
				Helper.sendJsonResponse(response, HttpStatusCodes.FORBIDDEN, "You dont have a account ", null);
				return;
			}

		} catch (CustomException exception) {
			exception.printStackTrace();
			Helper.sendJsonResponse(response, HttpStatusCodes.FORBIDDEN, "Something wrong with fetching the account id",
					null);
			return;
		}

		allowedMethods = RolePermission.getPermissions(role, handler);

		if (!allowedMethods.contains(request.getMethod())) {
			logger.warn("User with role {} does not have permission to access {} with method {}", role, path,
					request.getMethod());
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.getWriter().println("{\"message\": \"Permission denied\"}");
			return;
		}

		try {
			if (allowedMethods.contains(method)) {
				chain.doFilter(request, response);
			} else {
				Helper.sendJsonResponse(response, HttpStatusCodes.FORBIDDEN,
						"You are not authorized to perform this action", null);
			}
		} finally {
			Helper.clearThreadLocal();
			logger.info("Cleared thread-local values.");
		}

	}
}
