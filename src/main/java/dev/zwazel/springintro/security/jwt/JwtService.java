package dev.zwazel.springintro.security.jwt;

import dev.zwazel.springintro.security.config.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Service interface for JWT (JSON Web Token) operations.
 *
 * <p>This service handles the complete lifecycle of JWT tokens:
 * <ul>
 *   <li><strong>Validation:</strong> Extract and verify token information</li>
 *   <li><strong>Generation:</strong> Create new tokens for authenticated users</li>
 *   <li><strong>Storage:</strong> Manage tokens in HTTP cookies</li>
 * </ul>
 *
 * <p>JWT tokens are stateless - they contain all user information encoded within them.
 * No server-side session storage is needed, making this approach scalable.
 *
 * @see JwtServiceImpl The implementation
 * @see JwtAuthenticationFilter Where JWT validation happens during request processing
 */
public interface JwtService {
    
    /**
     * Extracts the username/email claim from a JWT token.
     *
     * @param token The JWT string to extract from
     * @return The username embedded in the token
     * @throws io.jsonwebtoken.JwtException if token is invalid or expired
     */
    String extractUserName(String token);

    /**
     * Generates a new JWT token for an authenticated user.
     *
     * <p>The token includes:
     * <ul>
     *   <li>User's username (email)</li>
     *   <li>Issued-at timestamp</li>
     *   <li>Expiration timestamp (configurable via jwt.expiration property)</li>
     *   <li>Digital signature (HMAC-SHA256)</li>
     * </ul>
     *
     * @param userDetails The user to create a token for
     * @return A signed JWT token string
     */
    String generateToken(UserDetails userDetails);

    /**
     * Validates that a JWT token is authentic and not expired.
     *
     * <p>Checks:
     * <ul>
     *   <li>Digital signature is valid (wasn't tampered with)</li>
     *   <li>Token hasn't expired</li>
     *   <li>Username in token matches the provided user</li>
     * </ul>
     *
     * @param token The JWT to validate
     * @param userDetails The user to validate against
     * @return true if token is valid, false otherwise
     */
    boolean isTokenValid(String token, UserDetails userDetails);

    /**
     * Wraps a JWT token in an HTTP cookie with security attributes.
     *
     * <p>Cookie attributes:
     * <ul>
     *   <li>httpOnly: Prevents JavaScript access (XSS protection)</li>
     *   <li>secure: Only sent over HTTPS</li>
     *   <li>sameSite=Strict: Prevents CSRF attacks</li>
     *   <li>maxAge: Configured via jwt.cookie-max-age property</li>
     * </ul>
     *
     * @param jwt The JWT token to wrap
     * @return An HTTP response cookie containing the token
     */
    ResponseCookie generateJwtCookie(String jwt);

    /**
     * Extracts a JWT token from an HTTP request's cookies.
     *
     * @param request The HTTP request to extract from
     * @return The JWT token if found, null otherwise
     */
    String getJwtFromCookies(HttpServletRequest request);

    /**
     * Creates an empty cookie that clears/deletes the JWT from the browser.
     *
     * <p>Used during logout to remove the authentication cookie.
     * Sets maxAge to 0, which tells the browser to delete the cookie.
     *
     * @return An empty HTTP response cookie with maxAge=0
     */
    ResponseCookie getCleanJwtCookie();
}