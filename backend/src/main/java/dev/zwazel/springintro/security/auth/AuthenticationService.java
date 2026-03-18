package dev.zwazel.springintro.security.auth;

import dev.zwazel.springintro.security.auth.payload.AuthenticationRequest;
import dev.zwazel.springintro.security.auth.payload.AuthenticationResponse;
import dev.zwazel.springintro.security.auth.payload.RegisterRequest;

/**
 * Service contract for user authentication and registration.
 *
 * <p>This service handles the two main authentication flows:
 * <ul>
 *   <li><strong>Register:</strong> Create a new user account</li>
 *   <li><strong>Authenticate:</strong> Verify credentials and issue JWT token</li>
 * </ul>
 *
 * @see AuthenticationServiceImpl The implementation
 * @see AuthenticationController Exposes these operations via REST endpoints
 */
public interface AuthenticationService {
    
    /**
     * Registers a new user in the system.
     *
     * <p>Process:
     * <ol>
     *   <li>Validate the registration request (email, password strength)</li>
     *   <li>Hash the password using PasswordEncoder</li>
     *   <li>Save the new user to the database</li>
     *   <li>Generate a JWT token for immediate login</li>
     *   <li>Return user info and token</li>
     * </ol>
     *
     * @param request Registration payload containing email, password, role
     * @return AuthenticationResponse with token and user details
     * @throws IllegalArgumentException if email already exists or validation fails
     */
    AuthenticationResponse register(RegisterRequest request);

    /**
     * Authenticates an existing user with email and password.
     *
     * <p>Process:
     * <ol>
     *   <li>Load user from database by email</li>
     *   <li>Verify password using PasswordEncoder.matches()</li>
     *   <li>Generate JWT token</li>
     *   <li>Return token and user details</li>
     * </ol>
     *
     * <p><strong>Why not plain text comparison?</strong>
     * Passwords are hashed using bcrypt. Even if the database is leaked,
     * attackers cannot directly use the hashes - they'd have to brute force them.
     * PasswordEncoder.matches() hashes the input password and compares it to the stored hash.
     *
     * @param request Login payload containing email and password
     * @return AuthenticationResponse with token and user details
     * @throws IllegalArgumentException if user not found or password incorrect
     */
    AuthenticationResponse authenticate(AuthenticationRequest request);
}