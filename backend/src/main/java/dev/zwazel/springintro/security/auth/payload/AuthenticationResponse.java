package dev.zwazel.springintro.security.auth.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.zwazel.springintro.security.Role;
import dev.zwazel.springintro.security.auth.AuthenticationController;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object (DTO) for authentication response.
 *
 * <h2>Purpose</h2>
 * This class represents the JSON data returned by both {@code /api/v1/auth/register}
 * and {@code /api/v1/auth/authenticate} endpoints. It contains the generated JWT token
 * along with user information for the client to display or use.
 *
 * <h2>Usage</h2>
 * Returned to client after successful registration or login. Client extracts the
 * {@code access_token} and uses it in subsequent API requests via:
 * <ul>
 *   <li><b>Automatic Cookie</b>: Browser automatically includes JWT cookie in all
 *       requests to the same domain (handled by HTTP Set-Cookie mechanism)</li>
 *   <li><b>Manual Bearer Token</b>: Client can extract {@code access_token} and manually
 *       set Authorization header: {@code Authorization: Bearer <access_token>}</li>
 * </ul>
 *
 * <h2>Token Security</h2>
 * The JWT token returned is read-only (cannot be tampered with) because it's signed
 * with HMAC-SHA256 using a secret key known only to the server. If any byte of the token
 * is modified, the signature becomes invalid and server rejects it. This ensures:
 * <ul>
 *   <li><b>Integrity</b>: Token content hasn't been modified in transit or by client</li>
 *   <li><b>Authentication</b>: Token was issued by this server (only server knows secret key)</li>
 *   <li><b>Not Encryption</b>: Token payload is Base64-encoded (readable if decoded), not encrypted.
 *       This is intentional to support stateless verification (server doesn't need to look up token).</li>
 * </ul>
 *
 * <h2>Token Payload Example</h2>
 * When decoded, JWT contains three parts (separated by dots: header.payload.signature):
 * <pre>
 * HEADER: {"alg":"HS256","typ":"JWT"}
 * PAYLOAD: {
 *   "sub": "john.doe@example.com",
 *   "iad": 1704067200,
 *   "exp": 1704067200,
 *   "authorities": ["USER_READ", "USER_WRITE"]
 * }
 * SIGNATURE: [HMAC-SHA256 hash]
 * </pre>
 *
 * @see AuthenticationController#register(RegisterRequest) - Returned from registration
 * @see AuthenticationController#authenticate(AuthenticationRequest) - Returned from login
 * @see dev.zwazel.springintro.security.jwt.JwtService - Generates JWT tokens
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {
    /**
     * Unique identifier (UUID) of the authenticated/registered user.
     *
     * <ul>
     *   <li>Globally unique across database</li>
     *   <li>Primary key for User entity</li>
     *   <li>Can be used for subsequent API calls that require user ID (e.g., /api/user/{id})</li>
     *   <li>Immutable (never changes during user's lifetime)</li>
     * </ul>
     *
     * Example: "550e8400-e29b-41d4-a716-446655440000"
     */
    private UUID id;

    /**
     * Email address of the authenticated/registered user.
     *
     * <ul>
     *   <li>Unique across database (enforced by constraints)</li>
     *   <li>Primary identifier for login (username in this system)</li>
     *   <li>Also encoded in JWT token payload as "sub" (subject) claim</li>
     *   <li>Usable for REST endpoints that expect user email parameter</li>
     * </ul>
     */
    private String email;

    /**
     * List of role names for the user (human-readable authorities).
     *
     * <ul>
     *   <li>Contains role+privilege combinations as strings (e.g., "USER_READ", "USER_WRITE")</li>
     *   <li>Used for UI display (show user what permissions they have)</li>
     *   <li>Also encoded in JWT token for server-side authorization checks</li>
     *   <li>Populated by {@link Role#getAuthorities()} which combines Role enum
     *       with its associated {@link dev.zwazel.springintro.security.Privilege} set</li>
     *   <li>Examples:
     *       <ul>
     *         <li>USER role: ["USER_READ", "USER_WRITE"]</li>
     *         <li>ADMIN role: ["USER_READ", "USER_WRITE", "USER_DELETE", "USER_UPDATE"]</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * @see dev.zwazel.springintro.security.Role - Enum mapping roles to privileges
     * @see dev.zwazel.springintro.security.Privilege - Fine-grained permission enumeration
     */
    private List<String> roles;

    /**
     * JWT access token (encoded as JSON Web Token).
     *
     * <h3>Format</h3>
     * Three Base64-encoded parts separated by dots:
     * {@code eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIiwiaWF0IjoxNzA0MDY3MjAwLCJleHAiOjE3MDQwNjk2MDB9.signature}
     *
     * <h3>Lifetime</h3>
     * Token is valid for 15 minutes by default (see {@code jwt.expiration} in application.properties).
     * After expiration, token is rejected by JwtService.isTokenValid() and new login required.
     *
     * <h3>Contents (Claims)</h3>
     * <ul>
     *   <li><b>sub</b> (subject): User email/username</li>
     *   <li><b>iat</b> (issued at): Timestamp when token was created (seconds since epoch)</li>
     *   <li><b>exp</b> (expiration): Timestamp when token expires (iat + 900 seconds)</li>
     *   <li><b>authorities</b>: List of role+privilege strings for authorization</li>
     * </ul>
     *
     * <h3>Usage</h3>
     * <ul>
     *   <li><b>Automatic via Cookie</b>: In most flows, token is stored in HTTP-only cookie
     *       by browser (see Set-Cookie response header) and automatically included
     *       in subsequent requests. JwtAuthenticationFilter extracts it and validates.</li>
     *   <li><b>Manual Bearer Token</b>: For non-browser clients (mobile apps, curl, Postman),
     *       client must manually extract and include in Authorization header:
     *       {@code Authorization: Bearer <access_token>}</li>
     * </ul>
     *
     * <h3>Security Notes</h3>
     * <ul>
     *   <li>Token is Base64-encoded (readable when decoded, not encrypted)</li>
     *   <li>Token is signed with HMAC-SHA256 (tampering results in invalid signature)</li>
     *   <li>Server skips database lookup (stateless) - token is self-contained proof
     *       of the user's identity and permissions</li>
     *   <li>Token cannot be revoked server-side (in production, use token blacklist)</li>
     * </ul>
     *
     * @see dev.zwazel.springintro.security.jwt.JwtService - Generates and validates tokens
     * @see dev.zwazel.springintro.security.config.JwtAuthenticationFilter - Extracts and validates token from request
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * Type of access token (always "Bearer" for JWT).
     *
     * <ul>
     *   <li>Indicates how the token should be used in Authorization header</li>
     *   <li>For Bearer tokens: {@code Authorization: Bearer <access_token>}</li>
     *   <li>Other token types (Basic, Digest) use different formats</li>
     *   <li>In most REST APIs, this is always "Bearer"</li>
     * </ul>
     *
     * Example: "Bearer"
     */
    @JsonProperty("token_type")
    private String tokenType;
}