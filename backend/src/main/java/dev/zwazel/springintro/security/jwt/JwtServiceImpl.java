package dev.zwazel.springintro.security.jwt;

import dev.zwazel.springintro.security.config.JwtAuthenticationFilter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Implementation of JWT token operations using the JJWT library.
 *
 * <p>This service provides:
 * <ul>
 *   <li><strong>Token Generation:</strong> Creates signed JWT tokens with user claims</li>
 *   <li><strong>Token Validation:</strong> Verifies signature and expiration</li>
 *   <li><strong>Cookie Management:</strong> Stores/retrieves tokens from secure HTTP cookies</li>
 * </ul>
 *
 * <p>The tokens are signed with HMAC-SHA256 using a secret key loaded from properties.
 * All configuration (secret, expiration times) comes from {@code application.properties}.
 *
 * @see JwtService The service contract
 * @see JwtAuthenticationFilter Where tokens are validated on each request
 */
@Service
public class JwtServiceImpl implements JwtService {

    /** Secret key for signing/verifying JWT tokens (must be base64 encoded) */
    @Value("${jwt.secret-key}")
    private String secretKey;

    /** JWT token validity duration in milliseconds (default: 900000 = 15 minutes) */
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /** Cookie name for storing JWT (default: jwt-cookie) */
    @Value("${jwt.cookie-name}")
    private String jwtCookieName;

    /** Cookie expiration in seconds (default: 86400 = 24 hours) */
    @Value("${jwt.cookie-max-age}")
    private long jwtCookieMaxAge;

    @Override
    public String extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String userName = extractUserName(token);
        // Token is valid if: 1) username matches, AND 2) token hasn't expired
        return (userName.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Checks if the JWT has exceeded its expiration time.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts the expiration timestamp from the JWT.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generates a signed JWT token with specified claims and expiration.
     *
     * @param extraClaims optional custom claims to include in the token
     * @param userDetails user information to embed in the token
     * @return a compact, signed JWT string
     */
    private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    /**
     * Builds and signs a JWT token with the HMAC-SHA256 algorithm.
     *
     * <p>The token structure (JWT format):
     * <pre>
     *   header.payload.signature
     * </pre>
     * Where:
     * <ul>
     *   <li>header: Algorithm and token type</li>
     *   <li>payload: Claims (username, issuedAt, expiration) + extra claims</li>
     *   <li>signature: HMAC-SHA256(header+payload, secret)</li>
     * </ul>
     *
     * @param extraClaims optional additional claims
     * @param userDetails user to create token for
     * @param expiration how long (ms) until token expires
     * @return signed JWT string
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts
                .builder()
                // Add the user's email/username as the "subject" claim
                .claims().subject(userDetails.getUsername())
                // Record when token was issued
                .issuedAt(new Date(System.currentTimeMillis()))
                // Calculate and set expiration time
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .and()
                // Sign the token with HMAC-SHA256 using our secret key
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                // Compact the JWT into its final string representation
                .compact();
    }

    /**
     * Generic method to extract any claim from a JWT token.
     *
     * @param token JWT to extract from
     * @param claimsResolvers function to extract specific claim (e.g., Claims::getSubject)
     * @return the extracted claim value
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolvers) {
        final Claims claims = extractAllClaims(token);
        return claimsResolvers.apply(claims);
    }

    /**
     * Parses and validates a JWT token using the secret key.
     *
     * <p>This verifies the signature - if the token was tampered with,
     * JwtException is thrown. The token's current time is checked against
     * the expiration claim (done by Jwts library automatically).
     *
     * @param token JWT to parse
     * @return all claims embedded in the token
     * @throws io.jsonwebtoken.JwtException if signature invalid or token expired
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Decodes the Base64-encoded secret key and creates a cryptographic key object.
     *
     * <p>The secret key is configured in application.properties and must be:
     * <ul>
     *   <li>Base64 encoded</li>
     *   <li>At least 256 bits (32 bytes) for HMAC-SHA256</li>
     * </ul>
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public ResponseCookie generateJwtCookie(String jwt) {
        // Create a secure HTTP cookie containing the JWT
        // Browser cannot access via JavaScript (httpOnly=true) - prevents XSS attacks
        // Only sent over HTTPS (secure=true)
        // Not sent to other domains (sameSite=Strict) - prevents CSRF attacks
        return ResponseCookie.from(jwtCookieName, jwt)
                .path("/")  // Cookie valid for all application paths
                .maxAge(jwtCookieMaxAge)  // How long browser keeps the cookie
                .httpOnly(true)  // JavaScript cannot read this cookie
                .secure(true)  // Only sent over HTTPS
                .sameSite("Strict")  // Not sent in cross-site requests
                .build();
    }

    @Override
    public String getJwtFromCookies(HttpServletRequest request) {
        // Spring's WebUtils.getCookie is a convenience method to find a named cookie
        Cookie cookie = WebUtils.getCookie(request, jwtCookieName);
        if (cookie != null) {
            return cookie.getValue();
        } else {
            return null;
        }
    }

    @Override
    public ResponseCookie getCleanJwtCookie() {
        // To delete a cookie in HTTP, set maxAge to 0
        // Browser will immediately remove the cookie
        // Used in logout endpoint
        return ResponseCookie.from(jwtCookieName, "")
                .path("/")
                .httpOnly(true)
                .maxAge(0)  // Tells browser to delete the cookie
                .build();
    }
}
