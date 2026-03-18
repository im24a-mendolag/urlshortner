package dev.zwazel.springintro.security.config;

import dev.zwazel.springintro.security.auth.AuthenticationController;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

/**
 * Spring Security configuration class that defines the security filter chain and authorization rules.
 *
 * <p>This class configures how Spring Security processes HTTP requests, including which endpoints
 * require authentication, which roles are needed for specific endpoints, and how to handle errors.</p>
 *
 * <h2>Key Responsibilities:</h2>
 * <ul>
 *   <li>Define which endpoints are public vs protected</li>
 *   <li>Configure role-based access control for specific endpoints</li>
 *   <li>Handle authentication and authorization failures with custom JSON responses</li>
 *   <li>Configure the JWT authentication filter in the filter chain</li>
 *   <li>Set up stateless session management for REST API</li>
 * </ul>
 *
 * @see JwtAuthenticationFilter - Validates JWT tokens on each request
 * @see ApplicationSecurityConfig - Provides authentication beans (UserDetailsService, PasswordEncoder)
 * @see Http401UnauthorizedEntryPoint - Handles 401 responses (not authenticated)
 * @see CustomAccessDeniedHandler - Handles 403 responses (not authorized)
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authenticationProvider;
    private final Http401UnauthorizedEntryPoint unauthorizedEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    /**
     * Configures the Spring Security filter chain and authorization rules for the REST API.
     *
     * <p>This method sets up six key steps for request filtering and authorization:</p>
     * <ol>
     *   <li>Disable CSRF protection (not needed for stateless JWT REST APIs)</li>
     *   <li>Configure 401/403 exception handlers to return JSON error responses</li>
     *   <li>Define HTTP authorization rules (public, admin-only, and authenticated endpoints)</li>
     *   <li>Use STATELESS session management (JWT-based, no server-side sessions)</li>
     *   <li>Set authentication provider for password verification during login</li>
     *   <li>Add JWT filter before the default authentication filter</li>
     * </ol>
     *
     * <p><b>Authorization Rules (evaluated top-to-bottom):</b></p>
     * <ul>
     *   <li>/error, /api/v1/auth/** - permitAll() (public endpoints)</li>
     *   <li>POST /api/v1/resource - hasRole("ADMIN") (admin-only endpoint)</li>
     *   <li>All others - authenticated() (requires valid JWT token)</li>
     * </ul>
     *
     * @param http Spring Security's HttpSecurity configuration builder
     * @return SecurityFilterChain with all configured rules and filters
     * @throws Exception if any configuration step fails
     * @see JwtAuthenticationFilter - Extracts and validates JWT on every request
     * @see Http401UnauthorizedEntryPoint - Handles 401 when JWT missing or invalid
     * @see CustomAccessDeniedHandler - Handles 403 when user lacks required role
     * @see ApplicationSecurityConfig - Provides authentication beans
     * @see AuthenticationController - Login/register endpoints that use this configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // STEP 1: Disable CSRF protection (not needed for stateless JWT-based REST APIs)
        http.csrf(AbstractHttpConfigurer::disable)
                // STEP 2: Configure exception handlers for authentication/authorization failures
                .exceptionHandling(exception -> exception
                        // Return HTTP 401 with custom JSON error when JWT invalid/expired
                        .authenticationEntryPoint(unauthorizedEntryPoint)
                        // Return HTTP 403 with custom JSON error when user lacks permission
                        .accessDeniedHandler(accessDeniedHandler))
                // STEP 3: Define authorization rules (top-to-bottom evaluation, first match wins)
                .authorizeHttpRequests(request ->
                        request
                                // Rule 1: Public endpoints (no authentication required)
                                .requestMatchers("/error", "/api/v1/auth/**").permitAll()
                                // Rule 1b: Public shortener endpoints (anonymous and authenticated)
                                .requestMatchers(HttpMethod.POST, "/shorten").permitAll()
                                .requestMatchers(HttpMethod.GET, "/{code}").permitAll()
                                // Rule 2: Admin-only resource creation
                                .requestMatchers(HttpMethod.POST, "/api/v1/resource").hasRole("ADMIN")
                                // Rule 3: Catch-all (all other endpoints require authentication)
                                .anyRequest().authenticated())
                // STEP 4: Use stateless session policy (no server-side sessions)
                // Each request is independent; state is in JWT token only
                .sessionManagement(manager -> manager.sessionCreationPolicy(STATELESS))
                // STEP 5: Set the authentication provider (for login credential verification)
                .authenticationProvider(authenticationProvider)
                // STEP 6: Add JWT filter BEFORE the default authentication filter
                // This ensures JWT is extracted and validated on every request
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
