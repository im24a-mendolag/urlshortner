package dev.zwazel.springintro.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.zwazel.springintro.security.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;


/**
 * Spring Security component that handles HTTP 401 (Unauthorized) responses.
 *
 * <h2>Purpose</h2>
 * When a user makes a request without a valid JWT token, or with an invalid/expired token,
 * Spring Security triggers this entry point to send a meaningful error response.
 * This class converts Spring Security's exception into a JSON error response instead of
 * the default HTML error page, making it suitable for REST APIs.
 *
 * <h2>When Triggered</h2>
 * This handler is invoked by Spring Security in following scenarios:
 * <ul>
 *   <li><b>No JWT Token Provided</b>: Request lacks JWT cookie or Authorization header.  *       Spring's SecurityContextHolder has no authentication, so request is denied.</li>
 *   <li><b>Invalid JWT Token</b>: JWT is malformed or signature is invalid.
 *       JwtAuthenticationFilter catches validation error and doesn't set authentication.</li>
 *   <li><b>Expired JWT Token</b>: JWT claims.exp < current time.
 *       JwtService.isTokenValid() returns false, auth not set.</li>
 *   <li><b>Missing Permissions Check</b>: Request method matches an endpoint requiring auth
       but SecurityContext has no Authentication object.</li>
 * </ul>
 *
 * <h2>Error Flow</h2>
 * <pre>
 * HTTP Request (without valid JWT)
 *    ↓
 * JwtAuthenticationFilter (validation fails or skipped)
 *    → SecurityContextHolder.getContext().setAuthentication(null)
 *    ↓
 * AuthorizationFilter (checks if auth required)
 *    → No Authentication found in SecurityContext
 *    ↓
 * Spring Security triggers AuthenticationException
 *    ↓
 * Http401UnauthorizedEntryPoint.commence() [THIS CLASS]
 *    ↓
 * ErrorResponse with HTTP 401
 *    ↓
 * HTTP Response sent to client
 * </pre>
 *
 * <h2>Configuration</h2>
 * This component is registered in {@link SecurityConfiguration#securityFilterChain(HttpSecurity)}:
 * <pre>
 * .exceptionHandling(exception -> exception
 *     .authenticationEntryPoint(unauthorizedEntryPoint)
 * )
 * </pre>
 *
 * @see Http401UnauthorizedEntryPoint#commence(HttpServletRequest, HttpServletResponse, AuthenticationException)
 *      - Main method triggered by Spring Security
 * @see ErrorResponse - DTO for error response body
 * @see SecurityConfiguration - Where this handler is registered
 */
@Component
@Slf4j
public class Http401UnauthorizedEntryPoint implements AuthenticationEntryPoint {
    /**
     * Handles authentication failures by returning a JSON error response.
     *
     * <h3>Process</h3>
     * <ol>
     *   <li>Log the authentication error (rate limit to avoid log spam)</li>
     *   <li>Set HTTP response status to 401 Unauthorized</li>
     *   <li>Set response content type to application/json</li>
     *   <li>Build {@link ErrorResponse} object with error details</li>
     *   <li>Serialize ErrorResponse to JSON and write to response body</li>
     *   <li>Client receives JSON error instead of HTML error page</li>
     * </ol>
     *
     * <h3>Response Example</h3>
     * <pre>
     * HTTP/1.1 401 Unauthorized
     * Content-Type: application/json
     *
     * {
     *   "status": 401,
     *   "error": "Unauthorized",
     *   "timestamp": "2024-01-01T12:00:00Z",
     *   "message": "JWT token has expired",
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
     *   <li><b>request</b>: Original HTTP request that triggered the error.
     *       Used to get path (request.getServletPath()) for error response.</li>
     *   <li><b>response</b>: HTTP response object. Method sets status and body here.</li>
     *   <li><b>authException</b>: Exception thrown by Spring Security's authentication.
     *       Message is included in response for client debugging.</li>
     * </ul>
     *
     * @param request HTTP request that lacked valid authentication
     * @param response HTTP response object to write error to
     * @param authException Exception indicating why authentication failed
     * @throws IOException if writing to response output stream fails
     * @throws ServletException if servlet-level error occurs
     *
     * @see ErrorResponse - Error response DTO
     * @see AuthenticationException - Exception types: BadCredentialsException, TokenExpiredException, etc.
     * @see org.springframework.security.core.AuthenticationException
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        // STEP 1: Log the authentication error for debugging and monitoring
        // Use error level so it appears in error logs (helps DevOps track auth issues)
        log.error("Unauthorized error: {}", authException.getMessage());

        // STEP 2: Set response header to indicate JSON content
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // STEP 3: Set HTTP status code to 401 Unauthorized
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // STEP 4: Build error response object with details
        ErrorResponse body = ErrorResponse.builder()
                .status(HttpServletResponse.SC_UNAUTHORIZED)  // 401
                .error("Unauthorized")                         // Error type
                .timestamp(Instant.now())                      // When error occurred
                .message(authException.getMessage())           // Why auth failed
                .path(request.getServletPath())                // Which endpoint
                .build();

        // STEP 5: Configure Jackson ObjectMapper to properly serialize Instant objects
        final ObjectMapper mapper = new ObjectMapper();
        
        // Register JavaTimeModule: enables Jackson to serialize/deserialize Java 8+ date/time types
        // Without this, Instant.now() would cause serialization error
        mapper.registerModule(new JavaTimeModule());
        
        // Configure date serialization format: serialize as ISO 8601 string ("2024-01-01T12:00:00Z")
        // Instead of numeric milliseconds-since-epoch (1704067200000)
        // ISO 8601 is human-readable and standardized (RFC 3339)
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // STEP 6: Write error response as JSON to HTTP response body
        // ObjectMapper handles serialization (ErrorResponse → JSON string)
        // response.getOutputStream() sends bytes to client
        mapper.writeValue(response.getOutputStream(), body);
    }
}