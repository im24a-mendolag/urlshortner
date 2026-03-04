package dev.zwazel.springintro.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.zwazel.springintro.security.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Spring Security component that handles HTTP 403 (Forbidden) responses.
 *
 * <h2>Purpose</h2>
 * When an authenticated user attempts to access a protected resource without sufficient
 * permissions, Spring Security triggers this handler to send a meaningful error response.
 * This class returns a JSON error response instead of the default HTML error page,
 * making it suitable for REST APIs (as opposed to traditional web applications).
 *
 * <h2>When Triggered</h2>
 * This handler is invoked by Spring Security when:
 * <ul>
 *   <li><b>Missing Required Role</b>: User has valid JWT but role doesn't match
 *       endpoint requirements (e.g., USER trying to POST to /api/v1/resource
 *       which requires ADMIN role).</li>
 *   <li><b>Missing Required Permission</b>: User role lacks a specific permission/authority
 *       required by endpoint or @PreAuthorize annotation.</li>
 *   <li><b>Authorization Filter Check</b>: Request matches authorization rules but
 *       user's authorities don't satisfy the rule.</li>
 * </ul>
 *
 * <h2>Difference from 401 (Unauthorized)</h2>
 * <ul>
 *   <li><b>401 Unauthorized</b>: User is NOT authenticated (missing/invalid JWT)
 *       Handler: {@link Http401UnauthorizedEntryPoint}</li>
 *   <li><b>403 Forbidden</b>: User IS authenticated but NOT authorized for this resource
 *       Handler: {@link CustomAccessDeniedHandler} (THIS CLASS)</li>
 * </ul>
 *
 * <h3>Real-World Example</h3>
 * <pre>
 * REQUEST: POST /api/v1/resource (create new resource, requires ADMIN role)
 *
 * Case 1 - No JWT token (401):
 *   Authentication: None
 *   Response: HTTP 401 Unauthorized
 *   Handler: Http401UnauthorizedEntryPoint
   *   Client Action: User needs to log in first
 *
 * Case 2 - Valid JWT but USER role (403):
 *   Authentication: john_user (UserDetails with [USER_READ, USER_WRITE] authorities)
 *   Authorization Check: hasRole("ADMIN") ✗
 *   Response: HTTP 403 Forbidden
 *   Handler: CustomAccessDeniedHandler (THIS CLASS)
 *   Client Action: User is logged in but doesn't have permission for this operation
 * </pre>
 *
 * <h2>Error Flow</h2>
 * <pre>
 * HTTP Request (with valid JWT, but insufficient permissions)
 *    ↓
 * JwtAuthenticationFilter (validation succeeds)
 *    → SecurityContextHolder.setAuthentication(new UsernamePasswordAuthenticationToken(...))
 *    ↓
 * AuthorizationFilter (checks permissions)
 *    → User is authenticated but role/authority insufficient
 *    ↓
 * Spring Security throws AccessDeniedException
 *    ↓
 * CustomAccessDeniedHandler.handle() [THIS CLASS]
 *    ↓
 * ErrorResponse with HTTP 403
 *    ↓
 * HTTP Response sent to client
 * </pre>
 *
 * <h2>Configuration</h2>
 * This component is registered in {@link SecurityConfiguration#securityFilterChain(HttpSecurity)}:
 * <pre>
 * .exceptionHandling(exception -> exception
 *     .accessDeniedHandler(accessDeniedHandler)  // THIS LINE
 * )
 * </pre>
 *
 * @see CustomAccessDeniedHandler#handle(HttpServletRequest, HttpServletResponse, AccessDeniedException)
 *      - Main method triggered by Spring Security
 * @see ErrorResponse - DTO for error response body
 * @see SecurityConfiguration - Where this handler is registered
 * @see Http401UnauthorizedEntryPoint - Handler for 401 (not authenticated)
 */
@Component
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    /**
     * Handles authorization failures by returning a JSON error response.
     *
     * <h3>Process</h3>
     * <ol>
     *   <li>Log the access denied error (useful for audit/security monitoring)</li>
     *   <li>Set HTTP response status to 403 Forbidden</li>
     *   <li>Set response content type to application/json</li>
     *   <li>Build {@link ErrorResponse} object with error details</li>
     *   <li>Serialize ErrorResponse to JSON and write to response body</li>
     *   <li>Client receives JSON error explaining why access was denied</li>
     * </ol>
     *
     * <h3>Response Example</h3>
     * <pre>
     * HTTP/1.1 403 Forbidden
     * Content-Type: application/json
     *
     * {
     *   "status": 403,
     *   "error": "Forbidden",
     *   "timestamp": "2024-01-01T12:00:00Z",
     *   "message": "Access Denied",
     *   "path": "/api/v1/resource"
     * }
     * </pre>
     *
     * <h3>Jackson Configuration (JSON Serialization)</h3>
     * The code configures ObjectMapper to properly serialize Instant:
     * <pre>
     * // Enable Java 8+ date/time type support
     * mapper.registerModule(new JavaTimeModule());
     * 
     * // Serialize dates as ISO 8601 strings (e.g., "2024-01-01T12:00:00Z")
     * // Instead of numeric ms-since-epoch
     * mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
     * </pre>
     *
     * This ensures the timestamp field in response is human-readable string
     * rather than a numeric value, making it easier for clients to parse.
     *
     * <h3>Method Parameters</h3>
     * <ul>
     *   <li><b>request</b>: Original HTTP request that was denied.
     *       Used to get path (request.getServletPath()) for error response.</li>
     *   <li><b>response</b>: HTTP response object. Method sets status and body here.</li>
     *   <li><b>accessDeniedException</b>: Exception thrown by Spring Security indicating
     *       why access was denied (often generic "Access Denied" message).</li>
     * </ul>
     *
     * <h3>Why This Matters for Beginners</h3>
     * Understanding the difference between 401 and 403 is crucial:
     * <ul>
     *   <li><b>401</b>: "Who are you?" → User needs to authenticate first (log in)</li>
     *   <li><b>403</b>: "I know who you are, but you can't do this" → User is logged in
     *       but needs higher permissions (e.g., admin role)</li>
     * </ul>
     *
     * @param request HTTP request that was denied
     * @param response HTTP response object to write error to
     * @param accessDeniedException Exception indicating why access was denied
     * @throws IOException if writing to response output stream fails
     *
     * @see ErrorResponse - Error response DTO
     * @see org.springframework.security.access.AccessDeniedException
     * @see Http401UnauthorizedEntryPoint - Handler for missing/invalid authentication (401)
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        // STEP 1: Log the access denied error for security and audit purposes
        // Use error level so it appears in error logs (IT/Security teams review these)
        // Multiple 403 errors from same user might indicate security incident or misconfiguration
        log.error("Access denied error: {}", accessDeniedException.getMessage());

        // STEP 2: Set response header to indicate JSON content
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // STEP 3: Set HTTP status code to 403 Forbidden
        // 403 signals to client that authentication succeeded but authorization failed
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        // STEP 4: Build error response object with details
        ErrorResponse body = ErrorResponse.builder()
                .status(HttpServletResponse.SC_FORBIDDEN)              // 403
                .error("Forbidden")                                    // Error type (authorization failure)
                .timestamp(Instant.now())                              // When error occurred
                .message(accessDeniedException.getMessage())           // Why access denied
                .path(request.getServletPath())                        // Which endpoint was denied
                .build();

        // STEP 5: Configure Jackson ObjectMapper to properly serialize Instant objects
        final ObjectMapper mapper = new ObjectMapper();
        
        // Register JavaTimeModule: enables Jackson to serialize/deserialize Java 8+ date/time types
        // Without this, Instant.now() would cause serialization error
        mapper.registerModule(new JavaTimeModule());
        
        // Configure date serialization format: serialize as ISO 8601 string ("2024-01-01T12:00:00Z")
        // Instead of numeric milliseconds-since-epoch (1704067200000)
        // ISO 8601 is human-readable, standardized (RFC 3339), and widely understood
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // STEP 6: Write error response as JSON to HTTP response body
        // ObjectMapper handles serialization (ErrorResponse → JSON string)
        // response.getOutputStream() sends bytes to client
        mapper.writeValue(response.getOutputStream(), body);
    }
}