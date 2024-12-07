package util;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;

public class JwtService {

    private static final Set<String> invalidatedTokens = ConcurrentHashMap.newKeySet();

    public static void invalidateToken(String token) throws CustomException {
        if (isValidToken(token)) {
            invalidatedTokens.add(token);
        } else {
            throw new CustomException("Invalid token");
        }
    }

    public static boolean isTokenInvalidated(String token) {
        return invalidatedTokens.contains(token);
    }

    private static boolean isValidToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    private static Key getSecretKey() {
        return Keys.hmacShaKeyFor("your-256-bit-secret-key".getBytes(StandardCharsets.UTF_8));
    }
}
