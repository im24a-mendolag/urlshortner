package dev.zwazel.springintro.security.config;

import dev.zwazel.springintro.security.jwt.JwtService;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP request filter that validates JWT tokens on every request.
 *
 * <p><strong>When does this run?</strong>
 * This filter is executed ONCE per request (before it reaches the controller).
 * It extends OncePerRequestFilter to prevent execution in nested forward/include scenarios.
 *
 * <p><strong>What does it do?</strong>
 * For each incoming request:
 * <ol>
 *   <li>Tries to extract JWT from HTTP cookie or Authorization header</li>
 *   <li>If JWT found, validates it using JwtService</li>
 *   <li>If valid, loads the user from database and populates SecurityContext</li>
 *   <li>Spring Security then uses this context to authorize the request</li>
 * </ol>
 *
 * <p><strong>Request Flow:</strong>
 * <pre>
 *   Client Request
 *        ↓
 *   JwtAuthenticationFilter (this class)
 *        ↓
 *   SecurityConfiguration rules (@PreAuthorize)
 *        ↓
 *   Controller endpoint
 * </pre>
 *
 * <p><strong>Key Security Concept:</strong>
 * The SecurityContextHolder is Spring Security's ThreadLocal storage where authentication
 * information is maintained. Once we set it here, @PreAuthorize annotations in controllers
 * can access it to determine if a user has the right permissions.
 *
 * @see SecurityConfiguration Registers this filter
 * @see JwtService Where JWT validation happens
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    
    /** Spring's component for loading user details from database by username */
    private final UserDetailsService userDetailsService;

    /**
     * Main filter logic executed for each HTTP request.
     *
     * <p>This method performs JWT validation and populates Spring Security's SecurityContext
     * so that the application knows who is making the request and what they can do.
     *
     * @param request The incoming HTTP request (may contain JWT in cookie or header)
     * @param response The HTTP response object
     * @param filterChain The chain of remaining filters to pass the request through
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // STEP 1: Try to extract JWT token from cookie first, then Authorization header
        // Browser cookies are sent automatically; Authorization header is for non-browser clients
        String jwt = jwtService.getJwtFromCookies(request);
        final String authHeader = request.getHeader("Authorization");

        // STEP 2: Skip authentication for auth endpoints and requests without JWT
        // We don't need to authenticate /auth/register or /auth/authenticate endpoints
        if ((jwt == null && (authHeader == null || !authHeader.startsWith("Bearer "))) || request.getRequestURI().contains("/auth")) {
            filterChain.doFilter(request, response);  // Pass to next filter without authentication
            return;
        }

        // STEP 3: If JWT not in cookie, extract it from Authorization header
        // Authorization header format: "Bearer <token>"
        if (jwt == null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7); // Extract token after "Bearer " prefix
        }

        // STEP 4: Extract the username/email encoded in the JWT
        final String userEmail = jwtService.extractUserName(jwt);

        // STEP 5: Only process if username exists and user not already authenticated
        // (Skip if SecurityContext already has an authenticated principal)
        if (StringUtils.isNotEmpty(userEmail)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            // STEP 6: Load user details from the database
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
            
            // STEP 7: Validate the JWT signature and expiration
            if (jwtService.isTokenValid(jwt, userDetails)) {
                // STEP 8: Create a new authentication token representing the authenticated user
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                
                // UsernamePasswordAuthenticationToken:
                // - Constructor(principal, credentials, authorities)
                // - principal: UserDetails object
                // - credentials: null (we don't store password in session)
                // - authorities: user's roles and privileges
                UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(
                                userDetails,           // Who is authenticated
                                null,                  // No credentials stored (stateless)
                                userDetails.getAuthorities());  // Their permissions
                
                // STEP 9: Add request details (IP, user agent, etc) for audit logging
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // STEP 10: Store the authentication in SecurityContextHolder
                // This is the critical step - makes the user "logged in" for this request
                context.setAuthentication(authToken);
                SecurityContextHolder.setContext(context);
            }
        }
        
        // STEP 11: Pass request to next filter in the chain
        filterChain.doFilter(request, response);
    }
}