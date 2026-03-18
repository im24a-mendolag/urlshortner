package dev.zwazel.springintro.user;

import dev.zwazel.springintro.security.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

/**
 * User entity that represents an application user and implements Spring Security's UserDetails interface.
 *
 * <p>This class bridges the gap between your database and Spring Security:
 * <ul>
 *   <li>As a JPA entity, it's persisted in the database under the "users" table</li>
 *   <li>As a UserDetails, it provides Spring Security with user authentication details (username, password, authorities)</li>
 * </ul>
 *
 * <p>Key Security Feature: The password is stored hashed (via PasswordEncoder) and should never be compared as plain text.
 *
 * @see UserDetails Spring Security's user contract
 * @see Role User's role and authorities
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "users")
public class User implements UserDetails {
    
    /** Unique identifier generated as UUID */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Unique email - used as username for authentication */
    @Column(unique = true)
    private String email;
    
    /** Hashed password - never store plain text passwords! */
    private String password;

    /** User's role which determines their authorities/permissions */
    @Enumerated(EnumType.STRING)
    private Role role;

    /**
     * Maps the user's role to a collection of granted authorities.
     * 
     * <p>Spring Security uses this to determine what actions a user can perform.
     * For example: an ADMIN role might have READ, WRITE, and DELETE privileges.
     *
     * @return Collection of authorities derived from the user's role
     */
    @Override
    @NullMarked
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role.getAuthorities();
    }

    /**
     * Returns the username used for authentication (in this case, the email).
     * 
     * <p>Spring Security calls this to get the principal identifier during authentication.
     *
     * @return the user's email as username
     */
    @Override
    @NullMarked
    public String getUsername() {
        return email;
    }
}
