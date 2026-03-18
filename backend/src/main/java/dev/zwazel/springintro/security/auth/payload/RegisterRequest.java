package dev.zwazel.springintro.security.auth.payload;

import dev.zwazel.springintro.security.Role;
import dev.zwazel.springintro.security.auth.AuthenticationController;
import dev.zwazel.springintro.validation.password.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) for user registration requests.
 *
 * <h2>Purpose</h2>
 * This class represents the JSON data sent by a client to the {@code /api/v1/auth/register}
 * endpoint. It carries the minimal information needed to create a new user account.
 *
 * <h2>Validation</h2>
 * This class uses Jakarta Bean Validation annotations to enforce constraints on input data.
 * When {@link AuthenticationController#register(RegisterRequest)} is called with
 * {@code @Valid}, Spring automatically validates this object before the handler method
 * executes. If validation fails, Spring returns HTTP 400 Bad Request with error details.
 *
 * <h2>Validation Rules</h2>
 * <ul>
 *   <li><b>email</b>: Must not be blank AND must be valid email format (RFC 5321).   *       Examples: "user@example.com" ✓, "invalid-email" ✗, "" ✗</li>
 *   <li><b>password</b>: Must not be blank AND must be a "strong" password.
 *       Strong password rules (see {@link StrongPassword} validator):
 *       <ul>
 *         <li>At least 8 characters long</li>
 *         <li>Contains at least one uppercase letter (A-Z)</li>
 *         <li>Contains at least one lowercase letter (a-z)</li>
 *         <li>Contains at least one digit (0-9)</li>
 *         <li>Contains at least one special character (!@#$%^&*)</li>
 *       </ul>
 *       Examples: "Str0ng!Pass123" ✓, "weak123" ✗ (no special char), "PASSWORD" ✗ (no lowercase)</li>
 *   <li><b>role</b>: Must be a valid {@link Role} enum value (ADMIN or USER).
 *       Cannot be null. Determines initial authorization level for the new account.</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>
 * POST /api/v1/auth/register HTTP/1.1
 * Content-Type: application/json
 *
 * {
 *   "email": "john.doe@example.com",
 *   "password": "SecureP@ss123",
 *   "role": "USER"
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
 * <h3>Response (Validation Failure)</h3>
 * <pre>
 * HTTP/1.1 400 Bad Request
 * Content-Type: application/json
 *
 * {
 *   "error": "Validation failed",
 *   "errors": [
 *     {"field": "password", "message": "Must contain uppercase, lowercase, digit, and special character"}
 *   ]
 * }
 * </pre>
 *
 * @see AuthenticationController#register(RegisterRequest) - Endpoint handler
 * @see StrongPassword - Custom validator for password strength rules
 * @see Role - User role enumeration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    /**
     * Email address to register for the new account.
     *
     * <ul>
     *   <li>Must not be blank</li>
     *   <li>Must be valid email format (RFC 5321)</li>
     *   <li>Used as unique username in the system</li>
     *   <li>Stored as-is in database, used for login</li>
     * </ul>
     *
     * @see Email - Jakarta Email validation annotation
     */
    @NotBlank(message = "email is required")
    @Email(message = "email format is not valid")
    private String email;

    /**
     * Password to protect the account.
     *
     * <ul>
     *   <li>Must not be blank</li>
     *   <li>Must satisfy {@link StrongPassword} constraints</li>
     *   <li>NOT stored plain text; bcrypt hashing applied before database insertion</li>
     *   <li>At least 8 characters with uppercase, lowercase, digit, special char</li>
     *   <li>Used only once (at registration); subsequent logins use email + password verify</li>
     * </ul>
     *
     * @see StrongPassword - Custom validation annotation enforcing password complexity
     */
    @NotBlank(message = "password is required")
    @StrongPassword
    private String password;

    /**
     * Initial role for the new user account.
     *
     * <ul>
     *   <li>Must not be null (required field)</li>
     *   <li>Valid values: ADMIN, USER (see {@link Role} enum)</li>
     *   <li>Determines initial permissions (authorities) for the new account</li>
     *   <li>Role can be changed later via administrative endpoints (if implemented)</li>
     *   <li>Users with ADMIN role can perform all operations (READ, WRITE, UPDATE, DELETE)</li>
     *   <li>Users with USER role can only perform READ and WRITE operations</li>
     * </ul>
     *
     * @see Role - User role enumeration (ADMIN, USER)
     */
    @NotNull
    private Role role;
}