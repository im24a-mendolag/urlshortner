package dev.zwazel.springintro.security.auth.payload;

import dev.zwazel.springintro.security.auth.AuthenticationController;
import dev.zwazel.springintro.security.auth.AuthenticationService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) for user login (authentication) requests.
 *
 * <h2>Purpose</h2>
 * This class represents the JSON data sent by a client to the {@code /api/v1/auth/authenticate}
 * endpoint. It carries the user's credentials (email and password) that the server will verify
 * against stored data in the database.
 *
 * <h2>Authentication Process</h2>
 * <ol>
 *   <li>Client sends email and password via POST request as JSON</li>
 *   <li>Spring deserializes JSON into AuthenticationRequest object</li>
 *   <li>{@link AuthenticationController#authenticate(AuthenticationRequest)} receives the request</li>
 *   <li>{@link AuthenticationService#authenticate(AuthenticationRequest)} delegates to
 *       Spring's {@code AuthenticationManager}</li>
 *   <li>AuthenticationManager loads User from database (via {@link dev.zwazel.springintro.user.UserRepository})</li>
 *   <li>AuthenticationManager compares request password with stored bcrypt hash
 *       using timing-safe comparison (prevents timing attacks)</li>
 *   <li>If credentials valid: JWT token generated and returned to client</li>
 *   <li>If credentials invalid: HTTP 401 Unauthorized and exception thrown</li>
 * </ol>
 *
 * <h2>Key Points</h2>
 * <ul>
 *   <li><b>No Validation Annotations</b>: Unlike RegisterRequest, this class has no
 *       validation constraints (in contrast to RegisterRequest's password strength check).
 *       Password strength validation is unnecessary for login (user already set their password).
 *       Basic null checks happen naturally during credential comparison.</li>
 *   <li><b>Security</b>: Password sent over HTTPS only (enforced by browser for https:// URLs).
 *       Password never logged or stored in server logs.</li>
 *   <li><b>Stateless</b>: Server stores no session or login state. JWT token returned
 *       encodes all authorization information needed for subsequent requests.</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>
 * POST /api/v1/auth/authenticate HTTP/1.1
 * Content-Type: application/json
 *
 * {
 *   "email": "john.doe@example.com",
 *   "password": "SecureP@ss123"
 * }
 * </pre>
 *
 * <h3>Response (Success)</h3>
 * <pre>
 * HTTP/1.1 200 OK
 * Set-Cookie: jwt-cookie=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...; HttpOnly; Secure; SameSite=Strict; Max-Age=86400
 * Content-Type: application/json
 *
 * {
 *   "id": "550e8400-e29b-41d4-a716-446655440000",
 *   "email": "john.doe@example.com",
 *   "roles": ["USER_READ", "USER_WRITE"],
 *   "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *   "token_type": "Bearer"
 * }
 * </pre>
 *
 * <h3>Response (Invalid Credentials)</h3>
 * <pre>
 * HTTP/1.1 401 Unauthorized
 * Content-Type: application/json
 *
 * {
 *   "error": "Unauthorized",
 *   "message": "Invalid email or password"
 * }
 * </pre>
 *
 * @see AuthenticationController#authenticate(AuthenticationRequest) - Endpoint handler
 * @see AuthenticationService#authenticate(AuthenticationRequest) - Business logic
 * @see AuthenticationResponse - Response DTO returned upon success
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationRequest {
    /**
     * User's email address (acts as username in this system).
     *
     * <ul>
     *   <li>Used to look up User record in database</li>
     *   <li>Must match email used during registration</li>
     *   <li>Case-sensitive (email addresses are case-sensitive per RFC standards,
     *       though many providers treat them case-insensitively)</li>
     * </ul>
     */
    private String email;

    /**
     * User's password (plaintext; encryption/hashing happens server-side).
     *
     * <ul>
     *   <li>Plaintext in this request (HTTPS encryption protects it in transit)</li>
     *   <li>Server never logs or stores this value</li>
     *   <li>Server compares against bcrypt hash stored in database</li>
     *   <li>bcrypt is timing-safe (comparison time doesn't leak whether password
     *       is correct or not, preventing timing-based attacks)</li>
     * </ul>
     */
    private String password;
}