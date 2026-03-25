package dev.zwazel.springintro;

import dev.zwazel.springintro.security.auth.AuthenticationController;
import dev.zwazel.springintro.security.config.ApplicationSecurityConfig;
import dev.zwazel.springintro.security.config.SecurityConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Spring Boot application.
 *
 * <h2>Purpose</h2>
 * This class contains the {@code main} method that launches the entire Spring Boot application.
 * Spring Boot scans packages containing this class for components, configurations, entities,
 * repositories, and other Spring-managed beans.
 *
 * <h2>Annotations</h2>
 * <ul>
 *   <li><b>{@code @SpringBootApplication}</b>: Composite annotation combining:
 *       <ul>
 *         <li>{@code @Configuration}: Marks class as bean definition source</li>
 *         <li>{@code @EnableAutoConfiguration}: Enables Spring Boot's auto-configuration
 *             (automatically configures Spring based on classpath dependencies).</li>
 *         <li>{@code @ComponentScan}: Enables auto-discovery of @Component, @Service,
 *             @Repository, @Controller beans in this package and sub-packages.
 *             Depth: Starting from package dev.zwazel.springintro, scans:
 *             <ul>
 *               <li>dev.zwazel.springintro (and all subpackages)</li>
 *               <li>dev.zwazel.springintro.security (and its subpackages)</li>
 *               <li>dev.zwazel.springintro.user (and its subpackages)</li>
 *               <li>etc.</li>
 *             </ul>
 *         </li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h2>Auto-Configuration Examples</h2>
 * Thanks to classpath dependencies and auto-configuration:
 * <ul>
 *   <li><b>Spring Data JPA</b>: Automatically configures JpaRepository implementations,
 *       EntityManager, DataSource pooling, Hibernate settings.</li>
 *   <li><b>Spring Security</b>: Initializes security filter chain, authentication,
 *       authorization mechanisms.</li>
 *   <li><b>Jackson</b>: JSON serialization/deserialization configured automatically.</li>
 *   <li><b>Tomcat</b>: Embedded web server initialized automatically (no separate deploy).</li>
 *   <li><b>Logging</b>: SLF4J + Logback configured automatically.</li>
 * </ul>
 *
 * <h2>Component Scan Discovery</h2>
 * When the application starts, Spring discovers and registers these beans (in this project):
 * <ul>
 *   <li><b>Controllers</b>: AuthenticationController, AuthorizationController (marked @RestController)</li>
 *   <li><b>Services</b>: AuthenticationServiceImpl, JwtServiceImpl (marked @Service)</li>
 *   <li><b>Repositories</b>: UserRepository (extends JpaRepository, marked @Repository)</li>
 *   <li><b>Configurations</b>: SecurityConfiguration, ApplicationSecurityConfig (marked @Configuration)</li>
 *   <li><b>Components</b>: JwtAuthenticationFilter, Http401UnauthorizedEntryPoint,
 *       CustomAccessDeniedHandler (marked @Component)</li>
 * </ul>
 *
 * <h2>Startup Sequence (Simplified)</h2>
 * <ol>
 *   <li>JVM executes SpringintroApplication.main(args)</li>
 *   <li>SpringApplication.run() initializes Spring container</li>
 *   <li>Auto-configuration detects spring-boot-starter-web, spring-boot-starter-security,
 *       spring-boot-starter-data-jpa, and configures them</li>
 *   <li>Component scan discovers @Configuration and @Bean definitions</li>
 *   <li>Spring creates all beans in correct order (dependency injection happens here)</li>
 *   <li>Embedded Tomcat server starts (default: port 8080)</li>
 *   <li>"applicationContext started successfully" logged</li>
 *   <li>Application ready for HTTP requests</li>
 * </ol>
 *
 * <h2>Configuration Properties</h2>
 * During startup, Spring loads application.properties from src/main/resources/:
 * <pre>
 * spring.datasource.url=jdbc:mysql://localhost:3306/springintro
 * spring.jpa.hibernate.ddl-auto=update
 * jwt.secret-key=VGhpcy1pcy1hLXNob3djYXNlLXNlY3JldC1rZXktZm9yLXNwcmluZ2ludHJvLTIwMjY=
 * jwt.expiration=900000
 * </pre>
 * These properties are injected via @Value or @ConfigurationProperties into beans.
 *
 * <h2>Exit Points</h2>
 * Application runs until:
 * <ul>
 *   <li>User presses Ctrl+C (SIGINT) in terminal - Spring container shuts down gracefully</li>
 *   <li>Process receives SIGTERM signal - Spring performs cleanup (closing database connections, etc.)</li>
 *   <li>Fatal error occurs (e.g., database connection fails) - Application exits with error code</li>
 * </ul>
 *
 * @see SecurityConfiguration - Security configuration loaded during startup
 * @see ApplicationSecurityConfig - Authentication/authorization beans created
 * @see AuthenticationController - REST endpoint initialized and ready
 */
@SpringBootApplication
public class SpringintroApplication {

    /**
     * Main method - entry point for the JVM.
     *
     * <h3>Execution Flow</h3>
     * <ol>
     *   <li>JVM loads this class and calls main() method</li>
     *   <li>SpringApplication.run() creates Spring ApplicationContext
     *       (the container holding all beans)</li>
     *   <li>Class parameter specifies which class to scan for components
     *       (SpringintroApplication and its package + subpackages)</li>
     *   <li>args parameter allows command-line arguments to be passed to application
     *       Example: java -jar app.jar --server.port=9000</li>
     *   <li>Method returns an ApplicationContext object (not used here)</li>
     * </ol>
     *
     * <h3>Parameters</h3>
     * <ul>
     *   <li><b>args</b>: Command-line arguments passed from terminal.
     *       Examples: --server.port=9000, --spring.profiles.active=dev
     *       Useful for environment-specific configuration without rebuilding.</li>
     * </ul>
     *
     * <h3>Example Invocations</h3>
     * <pre>
     * // Default port 8080
     * java -jar spring-intro.jar
     *
     * // Custom port 9000
     * java -jar spring-intro.jar --server.port=9000
     *
     * // Production profile (different config file: application-prod.properties)
     * java -jar spring-intro.jar --spring.profiles.active=prod
     * </pre>
     *
     * <h3>Return Value</h3>
     * SpringApplication.run() returns ApplicationContext, but this example
     * doesn't assign it to a variable. In advanced scenarios, you might:
     * <pre>
     * ApplicationContext context = SpringApplication.run(SpringintroApplication.class, args);
     * MyBean bean = context.getBean(MyBean.class);  // Retrieve specific bean
     * </pre>
     *
     * @param args Command-line arguments (optional). Passed to Spring for configuration override.
     *             If empty, application uses defaults from application.properties.
     */
    static void main(String[] args) {
        SpringApplication.run(SpringintroApplication.class, args);
    }

}
