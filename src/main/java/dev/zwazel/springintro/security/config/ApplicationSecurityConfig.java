package dev.zwazel.springintro.security.config;

import dev.zwazel.springintro.security.auth.AuthenticationServiceImpl;
import dev.zwazel.springintro.security.auth.payload.RegisterRequest;
import dev.zwazel.springintro.user.User;
import dev.zwazel.springintro.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring Security authentication and password encoding configuration.
 *
 * <h2>Purpose</h2>
 * This configuration class creates and provides four critical Spring Security beans:
 * <ul>
 *   <li><b>UserDetailsService</b>: Loads user data from database for authentication</li>
 *   <li><b>AuthenticationProvider</b>: Handles credential verification (password checking)</li>
 *   <li><b>AuthenticationManager</b>: Orchestrates authentication process</li>
 *   <li><b>PasswordEncoder</b>: Hashes and verifies passwords using bcrypt</li>
 * </ul>
 *
 * <h2>Why Separate Configuration?</h2>
 * While these beans could be defined in {@link SecurityConfiguration}, separating them
 * into ApplicationSecurityConfig makes the code cleaner:
 * <ul>
 *   <li>SecurityConfiguration focuses on HTTP request authorization (filter chain)</li>
 *   <li>ApplicationSecurityConfig focuses on credential verification (authentication)</li>
 *   <li>Each class has a single responsibility, easier to understand and modify</li>
 * </ul>
 *
 * <h2>Authentication Flow (Simplified)</h2>
 * <pre>
 * 1. User submits POST /api/v1/auth/authenticate {email, password}
 * 2. AuthenticationServiceImpl calls authenticationManager.authenticate(...)
 * 3. AuthenticationManager delegates to AuthenticationProvider
 * 4. AuthenticationProvider loads user using UserDetailsService
 * 5. AuthenticationProvider compares password with bcrypt hash
 * 6. If match: return Authentication (contains user details, authorities)
 * 7. JwtService generates JWT token from Authentication
 * 8. Token returned to client in response
 * </pre>
 *
 * <h2>Dependency: UserRepository</h2>
 * This configuration depends on UserRepository being available (injected via constructor).
 * UserRepository must be discovered during component scan for this to work.
 *
 * @see SecurityConfiguration - HTTP-level security (filters, authorization rules)
 * @see UserRepository - Database access for loading users
 * @see User - Entity implementing UserDetails interface
 * @see AuthenticationServiceImpl - Uses the beans from this config
 */
@Configuration
@RequiredArgsConstructor
public class ApplicationSecurityConfig {
    private final UserRepository userRepository;

    /**
     * Provides UserDetailsService bean - loads users from database for authentication.
     *
     * <h3>Purpose</h3>
     * {@code UserDetailsService} is a Spring Security interface with one method:
     * <pre>
     * UserDetails loadUserByUsername(String username) throws UsernameNotFoundException
     * </pre>
     * It loads user data from any source (database, LDAP, file, etc.) and returns
     * a {@code UserDetails} object containing user info, password hash, and authorities.
     *
     * <h3>Implementation Details</h3>
     * This method creates a lambda implementation:
     * <pre>
     * (email) -> userRepository.findUserByEmail(email)
     *     .orElseThrow(() -> new UsernameNotFoundException("User not found"))
     * </pre>
     * <ul>
     *   <li>Takes email (technically "username" in Spring Security terms)</li>
     *   <li>Queries database using UserRepository.findUserByEmail(email)</li>
     *   <li>Returns User entity if found (User implements UserDetails)</li>
     *   <li>Throws UsernameNotFoundException if not found (Spring Security catches this)</li>
     * </ul>
     *
     * <h3>Why UserDetails Interface?</h3>
     * User entity implements Spring Security's UserDetails interface, which provides:
     * <ul>
     *   <li>username (email in our case)</li>
     *   <li>password (bcrypt hash)</li>
     *   <li>authorities (list of roles + privileges from Role enum)</li>
     *   <li>account status flags (enabled, accountNonExpired, credentialsNonExpired, accountNonLocked)</li>
     * </ul>
     *
     * <h3>Usage</h3>
     * This bean is injected into AuthenticationProvider, which calls it during login:
     * <pre>
     * // During login
     * UserDetails user = userDetailsService.loadUserByUsername(email);
     * // Compare submitted password with user.getPassword() (bcrypt hash)
     * </pre>
     *
     * <h3>Note on Method Name</h3>
     * The interface uses "username" in the method name loadUserByUsername(), but
     * our parameter is "email". This is fine because Spring Security is flexible
     * about what the unique identifier is (doesn't have to be literal "username").
     *
     * @return {@code UserDetailsService} implementation (lambda/anonymous class)
     *
     * @see UserRepository#findUserByEmail(String) - Database query
     * @see User - Entity implementing UserDetails
     * @see org.springframework.security.core.userdetails.UserDetails
     * @see org.springframework.security.core.userdetails.UsernameNotFoundException
     */
    @Bean
    public UserDetailsService userDetailsService() {
        // STEP 1: Create lambda implementation of UserDetailsService interface
        // Spring Security will call this method with email parameter during authentication
        return email -> userRepository.findUserByEmail(email)
                // STEP 2: If user found, return User (which implements UserDetails)
                // STEP 3: If not found, throw exception (Spring catches and handles)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    /**
     * Provides AuthenticationProvider bean - handles credential verification during login.
     *
     * <h3>Purpose</h3>
     * {@code AuthenticationProvider} is responsible for:
     * <ol>
     *   <li>Loading user details from UserDetailsService</li>
     *   <li>Comparing submitted password with stored bcrypt hash</li>
     *   <li>Returning Authentication object if credentials valid</li>
     *   <li>Throwing AuthenticationException if invalid</li>
     * </ol>
     *
     * <h3>What is DaoAuthenticationProvider?</h3>
     * {@code DaoAuthenticationProvider} is Spring Security's built-in authentication provider that:
     * <ul>
     *   <li>Uses a UserDetailsService (Data Access Object) to load users
     *       ("Dao" stands for Data Access Object)</li>
     *   <li>Delegates password comparison to a PasswordEncoder (bcrypt in our case)</li>
     *   <li>Handles various edge cases (user not found, account locked, credentials expired)</li>
     *   <li>Is stateless (doesn't store session state)</li>
     * </ul>
     *
     * <h3>Configuration Steps</h3>
     * <pre>
     * DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
     * authProvider.setPasswordEncoder(passwordEncoder());
     * return authProvider;
     * </pre>
     * <ol>
     *   <li>Create DaoAuthenticationProvider with UserDetailsService
     *       (Spring Security will call userDetailsService.loadUserByUsername() during login)</li>
     *   <li>Set PasswordEncoder to bcrypt
     *       (Spring Security will use passwordEncoder.matches() to verify password)</li>
     *   <li>Return configured provider</li>
     * </ol>
     *
     * <h3>Authentication Flow (Detailed)</h3>
     * <pre>
     * 1. AuthenticationManager receives Authentication with email + plaintext password
     * 2. AuthenticationManager delegates to this AuthenticationProvider
     * 3. Provider calls userDetailsService.loadUserByUsername(email)
     * 4. UserDetails returned with bcrypt-hashed password
     * 5. Provider calls passwordEncoder.matches(plaintext, hash)
     * 6. bcrypt comparison (timing-safe, brute-force resistant)
     * 7. If match: return new Authentication with user details + authorities
     * 8. If no match: throw BadCredentialsException
     * </pre>
     *
     * <h3>Usage</h3>
     * This bean is registered in SecurityConfiguration.securityFilterChain():
     * <pre>
     * .authenticationProvider(authenticationProvider)
     * </pre>
     * Spring Security uses it for all authentication attempts.
     *
     * @return {@code AuthenticationProvider} configured with UserDetailsService + PasswordEncoder
     *
     * @see #userDetailsService() - Loads user from database
     * @see #passwordEncoder() - Verifies password against bcrypt hash
     * @see #authenticationManager(AuthenticationConfiguration) - Orchestrates authentication
     * @see org.springframework.security.authentication.dao.DaoAuthenticationProvider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        // STEP 1: Create DaoAuthenticationProvider with our UserDetailsService
        // This provider will use our userDetailsService to load users from database
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
        
        // STEP 2: Configure the provider to use our PasswordEncoder (bcrypt)
        // Without this, provider wouldn't know how to compare passwords
        authProvider.setPasswordEncoder(passwordEncoder());
        
        // STEP 3: Return configured provider
        // Spring Security will use this for all authentication attempts
        return authProvider;
    }

    /**
     * Provides AuthenticationManager bean - orchestrates the authentication process.
     *
     * <h3>Purpose</h3>
     * {@code AuthenticationManager} is the main entry point for authentication.
     * When you call authenticationManager.authenticate(authentication), it:
     * <ol>
     *   <li>Iterates through registered AuthenticationProviders</li>
     *   <li>Finds one that supports the authentication type</li>
     *   <li>Delegates to that provider</li>
     *   <li>Returns Authentication result or throws AuthenticationException</li>
     * </ol>
     *
     * <h3>Why a Manager Pattern?</h3>
     * The manager pattern allows multiple providers:
     * <pre>
     * // Pseudocode
     * authenticationManager.authenticate(auth) {
     *     for (provider : providers) {
     *         if (provider.supports(auth.getClass())) {
     *             return provider.authenticate(auth);
     *         }
     *     }
     *     throw new AuthenticationException();
     * }
     * </pre>
     * This allows using different authentication methods (LDAP, OAuth, SAML) alongside
     * our DaoAuthenticationProvider. In our simple app, only one provider is registered,
     * but the pattern is flexible.
     *
     * <h3>How to Obtain AuthenticationManager</h3>
     * This method takes {@code AuthenticationConfiguration} which is automatically
     * provided by Spring Boot. The configuration has a method getAuthenticationManager()
     * that returns the configured manager.
     *
     * <h3>Usage</h3>
     * AuthenticationManager is injected into AuthenticationServiceImpl:
     * <pre>
     * @Service
     * class AuthenticationServiceImpl {
     *     AuthenticationManager authenticationManager;
     *     
     *     public authenticate(AuthenticationRequest request) {
     *         Authentication auth = authenticationManager.authenticate(
     *             new UsernamePasswordAuthenticationToken(
     *                 request.getEmail(),
     *                 request.getPassword()
     *             )
     *         );
     *         // auth now contains user details + authorities
     *         return jwtService.generateToken(auth);
     *     }
     * }
     * </pre>
     *
     * <h3>Never Create Your Own</h3>
     * Important: Don't create AuthenticationManager manually. Always get it from
     * AuthenticationConfiguration (which knows about all configured providers).
     * This method is the "Spring-approved" way to provision it as a bean.
     *
     * @param config Spring's AuthenticationConfiguration (auto-provided, contains manager)
     * @return {@code AuthenticationManager} orchestrating all authentication providers
     * @throws Exception if configuration fails
     *
     * @see AuthenticationProvider - Individual providers registered with the manager
     * @see #authenticationProvider() - Our DaoAuthenticationProvider
     * @see AuthenticationServiceImpl - Uses this to verify login credentials
     * @see org.springframework.security.authentication.AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        // STEP 1: Get the AuthenticationManager from Spring's AuthenticationConfiguration
        // Configuration has already set up all providers (including our authenticationProvider)
        // This is the Spring-approved way to obtain the manager
        return config.getAuthenticationManager();
    }

    /**
     * Provides PasswordEncoder bean - hashes passwords using bcrypt.
     *
     * <h3>Purpose</h3>
     * {@code PasswordEncoder} is responsible for two operations:
     * <ol>
     *   <li><b>Encode</b>: Hash plaintext password before storing in database
     *       (never store plaintext passwords)</li>
     *   <li><b>Matches</b>: Verify plaintext password against stored hash
     *       (during login)</li>
     * </ol>
     *
     * <h3>What is BCryptPasswordEncoder?</h3>
     * {@code BCryptPasswordEncoder} is Spring Security's implementation of bcrypt,
     * a strong password hashing algorithm:
     * <ul>
     *   <li><b>One-Way</b>: Cannot reverse hash to get original password
     *       (unlike encryption which can be reversed)</li>
     *   <li><b>Salted</b>: Includes random salt in hash to prevent rainbow tables
     *       (two identical passwords have different hashes)</li>
     *   <li><b>Adaptive</b>: Gets slower over time as computers get faster
     *       (configurable via strength parameter, default 10)</li>
     *   <li><b>Timing-Safe Comparison</b>: Comparison time doesn't leak information
     *       about whether password is correct (prevents timing attacks)</li>
     * </ul>
     *
     * <h3>Bcrypt Example</h3>
     * <pre>
     * String plaintext = "Str0ng!Pass123";
     * String hash1 = passwordEncoder.encode(plaintext);
     * // hash1 = "$2a$10$N9qo8uLOickgx2ZMRZoMle..." (different each time)
     * 
     * String hash2 = passwordEncoder.encode(plaintext);
     * // hash2 = "$2a$10$LZwm5KlHKLzmNNk0vAk6u."  (different from hash1)
     * 
     * // But both verify correctly:
     * passwordEncoder.matches(plaintext, hash1)  // true
     * passwordEncoder.matches(plaintext, hash2)  // true
     * 
     * String wrongPassword = "WrongPassword";
     * passwordEncoder.matches(wrongPassword, hash1)  // false
     * </pre>
     *
     * <h3>Where Used</h3>
     * <ul>
     *   <li><b>Registration</b>: AuthenticationServiceImpl uses it to hash password
     *       before storing new user
     *       <pre>user.setPassword(passwordEncoder.encode(request.getPassword())</pre>
     *   </li>
     *   <li><b>Login</b>: DaoAuthenticationProvider uses it to verify password
     *       <pre>passwordEncoder.matches(submittedPassword, user.getPassword())</pre>
     *   </li>
     * </ul>
     *
     * <h3>Never Use Plain Text</h3>
     * Anti-pattern you'll see in tutorials (WRONG):
     * <pre>
     * // DON'T DO THIS!
     * user.setPassword(request.getPassword());  // Plaintext in database!
     * </pre>
     * Even if entire database is compromised, attacker can't recover passwords
     * because they're hashed with bcrypt.
     *
     * <h3>Performance Consideration</h3>
     * bcrypt is intentionally slow (to resist brute-force attacks):
     * <ul>
     *   <li>encode() takes ~100ms (default strength 10)</li>
     *   <li>matches() takes ~100ms</li>
     * </ul>
     * This is acceptable for authentication (few times per user per day), but
     * unsuitable for checking passwords frequently in tight loops. Cache the
     * result in JWT token to avoid repeated encoding/matching.
     *
     * @return {@code PasswordEncoder} using bcrypt algorithm
     *
     * @see #authenticationProvider() - Uses this encoder to verify passwords
     * @see AuthenticationServiceImpl#register(RegisterRequest) - Uses to hash password at registration
     * @see BCryptPasswordEncoder - Bcrypt implementation
     * @see org.springframework.security.crypto.password.PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // STEP 1: Create BCryptPasswordEncoder with default strength (10)
        // Strength 10 means 2^10 iterations (balance between security and speed)
        // Default is good for most applications
        // Higher strength (12+) makes encode/matches slower, better for future hardware
        return new BCryptPasswordEncoder();
    }
}