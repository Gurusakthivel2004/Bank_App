package util;import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.util.Date;
import java.util.Map;

public class JwtUtil {
    private static final String SECRET = "your-secret-key";
    private static final Algorithm ALGORITHM = Algorithm.HMAC256(SECRET);
    private static final long EXPIRATION_TIME = 3600_000; // 1 hour in milliseconds

    /**
     * Generates a JWT token with the provided claims using the simplified approach.
     *
     * @param claims A map of claims to include in the token.
     * @return A signed JWT token as a String.
     */
    public static String generateToken(Map<String, Object> claims) {
        return JWT.create() 
                .withPayload(claims)
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .sign(ALGORITHM);
    }

    /**
     * Verifies a JWT token and returns the decoded JWT.
     *
     * @param token The JWT token to verify.
     * @return A DecodedJWT object containing the claims and other token
     *         information.
     * @throws JWTVerificationException if the token is invalid or cannot be
     *                                  verified.
     */
    public static DecodedJWT verifyToken(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(ALGORITHM).build();
        return verifier.verify(token);
    }

    /**
     * Extracts the "id" claim from a JWT token.
     *
     * @param token The JWT token.
     * @return The "id" claim as a Long, or null if not present.
     */
    public static Long extractUserId(String token) {
        DecodedJWT jwt = verifyToken(token);
        return jwt.getClaim("id").asLong();
    }

    /**
     * Extracts the "role" claim from a JWT token.
     *
     * @param token The JWT token.
     * @return The "role" claim as a String, or null if not present.
     */
    public static String extractRole(String token) {
        DecodedJWT jwt = verifyToken(token);
        return jwt.getClaim("role").asString();
    }

    /**
     * Extracts the "branch" claim from a JWT token.
     *
     * @param token The JWT token.
     * @return The "branch" claim as a Long, or null if not present.
     */
    public static Long extractBranchId(String token) {
        DecodedJWT jwt = verifyToken(token);
        return jwt.getClaim("branchId").asLong();
    }
}
