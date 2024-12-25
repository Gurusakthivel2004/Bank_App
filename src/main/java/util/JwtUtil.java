package util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import java.util.Date;
import java.util.Map;

public class JwtUtil {
    private static final String SECRET = "leomessi_30";
    private static final Algorithm ALGORITHM = Algorithm.HMAC256(SECRET);
    private static final long EXPIRATION_TIME = 3600_000;

    public static String generateToken(Map<String, Object> claims) {
        return JWT.create() 
                .withPayload(claims)
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .sign(ALGORITHM);
    }

    public static DecodedJWT verifyToken(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(ALGORITHM).build();
        return verifier.verify(token);
    }

    public static Long extractUserId(String token) {
        DecodedJWT jwt = verifyToken(token);
        return jwt.getClaim("id").asLong();
    }

    public static String extractRole(String token) {
        DecodedJWT jwt = verifyToken(token);
        return jwt.getClaim("role").asString();
    }
    
    public static String extractUsername(String token) {
        DecodedJWT jwt = verifyToken(token);
        return jwt.getClaim("username").asString();
    }

    public static Long extractBranchId(String token) {
        DecodedJWT jwt = verifyToken(token);
        return jwt.getClaim("branchId").asLong();
    }
}
