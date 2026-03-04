package dev.zwazel.springintro.user;

import dev.zwazel.springintro.security.config.ApplicationSecurityConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository interface for User entity database operations.
 *
 * <h2>Purpose</h2>
 * This interface provides database access methods for the User entity.
 * Spring Data JPA automatically implements this interface at runtime, generating
 * SQL queries based on method names and signatures.
 *
 * <h2>What is JpaRepository?</h2>
 * {@code JpaRepository<User, UUID>} is a Spring Data interface that provides:
 * <ul>
 *   <li><b>Automatic CRUD Methods</b>: save(), findById(), findAll(), delete(), deleteAll(), etc.
 *       No need to write SQL or implement these methods.</li>
 *   <li><b>Type Safety</b>: Generic type parameters <User, UUID> ensure compile-time checking.</li>
 *   <li><b>Pagination and Sorting</b>: Built-in support for Page, Slice, Sort parameters.</li>
 *   <li><b>Custom Queries</b>: Can add @Query annotation for complex queries.</li>
 * </ul>
 *
 * <h2>Type Parameters Explained</h2>
 * <ul>
 *   <li><b>&lt;User&gt;</b>: The entity class this repository manages.
 *       Must exist in packages scanned by @SpringBootApplication.</li>
 *   <li><b>&lt;UUID&gt;</b>: The data type of the entity's primary key (@Id field).
 *       User.java has: {@code @Id private UUID id;}</li>
 * </ul>
 *
 * <h2>Usage in Application</h2>
 * This repository is used in several places:
 * <ul>
 *   <li><b>ApplicationSecurityConfig</b>: In UserDetailsService to load users by email for login</li>
 *   <li><b>AuthenticationServiceImpl</b>: To save new users during registration</li>
 *   <li><b>JwtAuthenticationFilter</b>: Indirectly (via UserDetailsService) to load user authorities</li>
 * </ul>
 *
 * <h2>Inherited Methods (Automatic)</h2>
 * You get these "for free" from JpaRepository (no implementation needed):
 * <pre>
 * // CRUD Operations
 * User save(User user);                    // Create or update user
 * Optional<User> findById(UUID id);        // Find user by primary key
 * List<User> findAll();                    // Get all users
 * void delete(User user);                  // Delete user
 * long count();                            // Count total users
 *
 * // Pagination
 * Page<User> findAll(Pageable pageable);   // Get users with pagination
 * </pre>
 *
 * @see User - Entity this repository manages
 * @see UserDetailsService - Uses this repository to load users for authentication
 * @see ApplicationSecurityConfig - Where this repository is injected
 */
public interface UserRepository extends JpaRepository<User, UUID> {
    /**
     * Retrieves a user from the database by email address.
     *
     * <h3>Why This Method?</h3>
     * In a typical web application with username/password login, users log in with
     * a username or email. This repository method retrieves a single user by that
     * email for credential verification.
     *
     * <h3>How Spring Data Implements This</h3>
     * Spring Data JPA reads the method name and signature, then generates SQL:
     * <pre>
     * SELECT u FROM User u WHERE u.email = ?1
     * </pre>
     * Method name pattern: {@code findUserByEmail}
     * <ul>
     *   <li>{@code find}: Indicates SELECT query</li>
     *   <li>{@code User}: Return type (single entity, wrapped in Optional)</li>
     *   <li>{@code By}: Keyword separating return type from criteria</li>
     *   <li>{@code Email}: Property name to match (User.email field)</li>
     * </ul>
     *
     * <h3>Return Type: Optional<User></h3>
     * {@code Optional} handles the case where email doesn't exist:
     * <ul>
     *   <li><b>User found</b>: Optional.of(user) - contains the User</li>
     *   <li><b>User not found</b>: Optional.empty() - no User</li>
     * </ul>
     * This is safer than returning null, which can cause NullPointerException.
     *
     * <h3>Usage in Application</h3>
     * In {@link ApplicationSecurityConfig#userDetailsService()}:
     * <pre>
     * userRepository.findUserByEmail(email)
     *     .orElseThrow(() -> new UsernameNotFoundException("User not found"));
     * </pre>
     * Retrieves user for login; throws exception if email not found.
     *
     * <h3>Database Behavior</h3>
     * <ul>
     *   <li><b>Case Sensitivity</b>: By default, database comparison is case-sensitive
     *       ("user@example.com" != "User@example.com"). For case-insensitive login,
     *       either normalize email to lowercase before saving, or use custom @Query
     *       with LOWER() function.</li>
     *   <li><b>Performance</b>: Ensure email column has database index for O(log n) lookup.
     *       {@code @Column(unique=true)} in User.java likely adds index automatically.</li>
     *   <li><b>Uniqueness</b>: Only one user per email (enforced by unique constraint).</li>
     * </ul>
     *
     * @param email The email address to search for. Must not be null.
     * @return {@code Optional<User>} containing the user if found, or empty if not found.
     * Never returns null (always returns Optional object).
     * @see User - Entity with email field
     * @see ApplicationSecurityConfig#userDetailsService() - Where this is used
     * @see org.springframework.security.core.userdetails.UsernameNotFoundException
     */
    Optional<User> findUserByEmail(String email);
}