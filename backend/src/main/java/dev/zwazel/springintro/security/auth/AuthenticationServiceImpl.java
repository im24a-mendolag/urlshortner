package dev.zwazel.springintro.security.auth;

import dev.zwazel.springintro.security.TokenType;
import dev.zwazel.springintro.security.auth.payload.AuthenticationRequest;
import dev.zwazel.springintro.security.auth.payload.AuthenticationResponse;
import dev.zwazel.springintro.security.auth.payload.RegisterRequest;
import dev.zwazel.springintro.security.jwt.JwtService;
import dev.zwazel.springintro.user.User;
import dev.zwazel.springintro.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of user authentication and registration.
 *
 * <p><strong>Key Security Practices:</strong>
 * <ul>
 *   <li>Passwords are encrypted using PasswordEncoder (bcrypt) - never stored in plain text</li>
 *   <li>AuthenticationManager validates credentials against the hashed password</li>
 *   <li>JWT tokens are issued after successful authentication</li>
 *   <li>@Transactional ensures database consistency for user registration</li>
 * </ul>
 *
 * <p><strong>Data Flow:</strong>
 * <pre>
 *   Client sends credentials
 *        ↓
 *   Spring AuthenticationManager verifies them
 *        ↓
 *   Load user from database
 *        ↓
 *   Generate JWT token
 *        ↓
 *   Return to client (stored in cookie or local storage)
 * </pre>
 *
 * @see AuthenticationService The service contract
 * @see AuthenticationController REST endpoints
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    /** Spring Security's password hashing/verification utility (uses bcrypt) */
    private final PasswordEncoder passwordEncoder;
    
    /** Service for JWT token operations */
    private final JwtService jwtService;
    
    /** Database access for User entity */
    private final UserRepository userRepository;
    
    /** Spring Security's authentication component - validates email/password credentials */
    private final AuthenticationManager authenticationManager;

    /**
     * Registers a new user in the system.
     *
     * <p>Process:
     * <ol>
     *   <li>Receive email, password, and role from client</li>
     *   <li>Hash the password using bcrypt (PasswordEncoder)</li>
     *   <li>Save new User entity to database</li>
     *   <li>Generate JWT token for immediate session</li>
     *   <li>Return user info and token</li>
     * </ol>
     *
     * <p><strong>Why hash the password?</strong>
     * If database is compromised, attackers get hashes, not passwords.
     * Bcrypt makes brute-forcing extremely expensive (computationally slow on purpose).
     *
     * @param request Contains email, password, and role for the new user
     * @return Response with JWT token and user details
     */
    @Override
    public AuthenticationResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.findUserByEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyExistsException("Email is already registered.");
        }

        // Create new user with hashed password
        var user = User.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))  // Hash password - never store plain text!
                .role(request.getRole())  // Assign role (ADMIN or USER)
                .build();
        
        // Persist user to database
        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // Protect against race conditions where the same email is inserted concurrently.
            throw new EmailAlreadyExistsException("Email is already registered.");
        }
        
        // Generate JWT token for this user
        var jwt = jwtService.generateToken(user);

        // Extract role and its authorities for response
        var roles = user.getRole().getAuthorities()
                .stream()
                .map(SimpleGrantedAuthority::getAuthority)
                .toList();

        // Return user info and token to client
        return AuthenticationResponse.builder()
                .accessToken(jwt)
                .email(user.getEmail())
                .id(user.getId())
                .roles(roles)
                .tokenType(TokenType.BEARER.name())  // "BEARER" indicates JWT token
                .build();
    }

    /**
     * Authenticates a user with email and password credentials.
     *
     * <p>Security Flow:
     * <ol>
     *   <li>Create token from email/password (credentials not yet verified)</li>
     *   <li>Pass to AuthenticationManager - it:
     *       <ul>
     *         <li>Loads user by email</li>
     *         <li>Hashes provided password</li>
     *         <li>Compares with stored hash</li>
     *       </ul>
     *   </li>
     *   <li>If successful, generate JWT token</li>
     *   <li>Return token to client</li>
     * </ol>
     *
     * <p><strong>Why is AuthenticationManager needed?</strong>
     * It integrates with Spring Security's authentication providers, handles password encoding,
     * and triggers security events (like login auditing).
     *
     * @param request Contains email and password to verify
     * @return Response with JWT token and user details
     * @throws IllegalArgumentException if user not found or password doesn't match
     */
    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // Step 1: Create a token representing the user's credentials (not yet verified)
        // Spring's AuthenticationManager will handle the actual verification
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Step 2: If authenticate() didn't throw exception, credentials were valid
        // Now load the user from the database
        var user = userRepository.findUserByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
        
        // Step 3: Extract authorities from user's role
        var roles = user.getRole().getAuthorities()
                .stream()
                .map(SimpleGrantedAuthority::getAuthority)
                .toList();
        
        // Step 4: Generate JWT token for this authenticated user
        var jwt = jwtService.generateToken(user);

        // Step 5: Return token and user details
        return AuthenticationResponse.builder()
                .accessToken(jwt)
                .email(user.getEmail())
                .id(user.getId())
                .roles(roles)
                .tokenType(TokenType.BEARER.name())  // "BEARER" indicates JWT token
                .build();
    }
}