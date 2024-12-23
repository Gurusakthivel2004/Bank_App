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

import service.TransactionService;
import util.Helper;
import util.JwtUtil;

@SuppressWarnings("serial")
@WebFilter("/api/*")
public class AuthFilter extends HttpFilter implements Filter {
	
	private final Logger logger = LogManager.getLogger(AuthFilter.class);

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		String path = request.getRequestURI();
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		// Allow public endpoints
		if (path.equals("/Bank_Application/api/Login")) {
			chain.doFilter(request, response);
			return;
		}

		String authorizationHeader = request.getHeader("Authorization");

		if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
			String token = authorizationHeader.substring(7);

			try {
				Long userId = JwtUtil.extractUserId(token);
				String role = JwtUtil.extractRole(token);
				Long branchId = JwtUtil.extractBranchId(token);
				
				logger.info("branch id is ,", branchId);
				
				Map<String, Object> claimsMap = new HashMap<>();
				claimsMap.put("id", userId);
				claimsMap.put("role", role);
				claimsMap.put("branchId", branchId);
				Helper.setThreadLocalValue(claimsMap);

			} catch (JWTVerificationException e) {
				response.getWriter().println("Invalid or expired token");
				return;
			}
		} else {
			response.getWriter().println("Authorization token not found");
			return;
		}
		try {
			chain.doFilter(request, response);
		} finally {
			Helper.clearThreadLocal();
		}
	}
}
