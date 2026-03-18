package dev.zwazel.springintro.security;

import dev.zwazel.springintro.security.config.CustomAccessDeniedHandler;
import dev.zwazel.springintro.security.config.Http401UnauthorizedEntryPoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object (DTO) for error responses returned by the API.
 *
 * <h2>Purpose</h2>
 * This class standardizes the format of error responses returned to clients when
 * requests fail. Instead of sending raw exception messages or stack traces, the API
 * wraps errors in a consistent JSON structure for better client-side error handling.
 *
 * <h2>When Used</h2>
 * ErrorResponse objects are created and sent to clients in following scenarios:
 * <ul>
 *   <li><b>HTTP 401 Unauthorized</b>: User provides invalid/expired JWT token
 *       ({@link Http401UnauthorizedEntryPoint})</li>
 *   <li><b>HTTP 403 Forbidden</b>: Authenticated user lacks required permissions
 *       ({@link CustomAccessDeniedHandler})</li>
 *   <li><b>HTTP 400 Bad Request</b>: Request validation fails (e.g., invalid email format)</li>
 *   <li><b>HTTP 500 Internal Server Error</b>: Unexpected server exception</li>
 * </ul>
 *
 * <h2>Error Response Example</h2>
 * <pre>
 * HTTP/1.1 401 Unauthorized
 * Content-Type: application/json
 *
 * {
 *   "status": 401,
 *   "error": "Unauthorized",
 *   "timestamp": "2024-01-01T12:00:00Z",
 *   "message": "JWT token has expired",
 *   "path": "/api/v1/resource/123"
 * }
 * </pre>
 *
 * <h2>Client-Side Usage</h2>
 * Applications can parse the structured error response:
 * <pre>
 * try {
 *     const response = await fetch('/api/v1/resource');
 *     if (!response.ok) {
 *         const errorData = await response.json();
 *         if (response.status === 401) {
 *             // Handle authentication error - redirect to login
 *             redirectToLogin();
 *         } else if (response.status === 403) {
 *             // Handle permission error - show message
 *             showError(errorData.message);
 *         }
 *     }
 * } catch (error) {
 *     console.error('Network error:', error);
 * }
 * </pre>
 *
 * <h2>JSON Serialization Notes</h2>
 * The {@code timestamp} field is serialized as ISO 8601 string (e.g., "2024-01-01T12:00:00Z")
 * thanks to Jackson configuration in error handlers:
 * <ul>
 *   <li>JavaTimeModule enables support for Java 8+ date/time types</li>
 *   <li>WRITE_DATES_AS_TIMESTAMPS=false ensures dates are strings, not numeric timestamps</li>
 * </ul>
 *
 * @see Http401UnauthorizedEntryPoint - Creates ErrorResponse for 401 errors
 * @see CustomAccessDeniedHandler - Creates ErrorResponse for 403 errors
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    /**
     * HTTP status code (e.g., 401, 403, 500).
     *
     * <ul>
     *   <li>Duplicates information from HTTP response status header</li>
     *   <li>Included in JSON body for clients that can't easily access headers</li>
     *   <li>Examples: 401 (Unauthorized), 403 (Forbidden), 400 (Bad Request), 500 (Server Error)</li>
     * </ul>
     *
     * Example: 401
     */
    private int status;

    /**
     * Human-readable error category/type.
     *
     * <ul>
     *   <li>Short error label for client-side error handling</li>
     *   <li>Typical values: "Unauthorized", "Forbidden", "BadRequest", "InternalServerError"</li>
     *   <li>Used by frontend to display appropriate UI message or perform actions</li>
     *   <li>Not unique per request (many requests can have same error type)</li>
     * </ul>
     *
     * Example: "Unauthorized"
     */
    private String error;

    /**
     * Moment in time when the error occurred.
     *
     * <ul>
     *   <li>Set to current time (Instant.now()) when error response is created</li>
     *   <li>Serialized as ISO 8601 string in JSON (e.g., "2024-01-01T12:00:00Z")</li>
     *   <li>Useful for logging and debugging: correlate errors with server logs</li>
     *   <li>Helps track when issues occurred during troubleshooting</li>
     * </ul>
     *
     * Example: Instant.parse("2024-01-01T12:00:00Z")
     */
    private Instant timestamp;

    /**
     * Detailed error description explaining what went wrong.
     *
     * <ul>
     *   <li>Usually the exception's message or reason for the error</li>
     *   <li>Should be user-friendly (avoid technical jargon that might expose system details)</li>
     *   <li>Examples:
     *       <ul>
     *         <li>"JWT token has expired"</li>
     *         <li>"Invalid JWT signature"</li>
     *         <li>"Insufficient permissions for this action"</li>
     *       </ul>
     *   </li>
     *   <li>Localization: In production, this message might be translated based on
     *       client's Accept-Language header</li>
     * </ul>
     *
     * Example: "JWT token has expired"
     */
    private String message;

    /**
     * Request path that triggered the error.
     *
     * <ul>
     *   <li>The URL path of the failed request (e.g., "/api/v1/resource/123")</li>
     *   <li>Useful for clients to identify which endpoint/action failed</li>
     *   <li>Helps with logging and debugging: track which paths are problematic</li>
     *   <li>Obtained from HttpServletRequest.getServletPath()</li>
     *   <li>Does not include query string (?key=value)</li>
     * </ul>
     *
     * Example: "/api/v1/resource/123"
     */
    private String path;
}