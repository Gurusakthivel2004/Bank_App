package servlet;

import java.io.IOException;
import java.util.HashMap;
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
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.core.type.TypeReference;
import service.CacheService;
import util.Helper;
import util.JwtUtil;

@SuppressWarnings("serial")
@WebFilter("/api/*")
public class AuthFilter extends HttpFilter implements Filter {

	private final Logger logger = LogManager.getLogger(AuthFilter.class);
	CacheService cacheService = new CacheService();

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		String path = request.getRequestURI();
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		logger.info("Request path: {}", path);

		if (path.equals("/Bank_Application/api/Login")) {
			logger.info("Skipping authentication for login endpoint.");
			chain.doFilter(request, response);
			return;
		}

		String authorizationHeader = request.getHeader("Authorization");

		if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
			String token = authorizationHeader.substring(7);
			logger.info("Authorization token found. Verifying token...");

			try {

				Long userId = JwtUtil.extractUserId(token);

				// Retrieve blacklist map from cache
				Map<String, String> blacklist = cacheService.get("blacklist", new TypeReference<Map<String, String>>() {
				});
				if (blacklist != null && blacklist.containsKey(token)) {
					logger.warn("Token is blacklisted. Denying access.");
					response.getWriter().println("Blacklisted token");
					return;
				}

				String role = JwtUtil.extractRole(token);
				String username = JwtUtil.extractUsername(token);
				Long branchId = JwtUtil.extractBranchId(token);

				Map<String, Object> claimsMap = new HashMap<>();
				claimsMap.put("id", userId);
				claimsMap.put("role", role);
				claimsMap.put("username", username);
				claimsMap.put("branchId", branchId);

				Helper.setThreadLocalValue(claimsMap);
				logger.info("Token verified successfully. Claims: {}", claimsMap);

			} catch (JWTVerificationException e) {
				logger.error("Invalid or expired token: {}", e.getMessage());
				response.getWriter().println("Invalid or expired token");
				return;
			}
		} else {
			logger.warn("Authorization token not found in request.");
			response.getWriter().println("Authorization token not found");
			return;
		}

		try {
			// Proceed with the request chain
			chain.doFilter(request, response);
		} finally {
			// Clear thread-local storage after request processing
			Helper.clearThreadLocal();
			logger.info("Cleared thread-local values.");
		}
	}
}
