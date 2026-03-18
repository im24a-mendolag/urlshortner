package dev.zwazel.springintro.security;

import dev.zwazel.springintro.user.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.zwazel.springintro.security.Privilege.*;

/**
 * Enum defining the application's user roles and their associated privileges.
 *
 * <p>This is a two-tier authorization model:
 * <ul>
 *   <li><strong>Role</strong> (this enum): High-level grouping like ADMIN or USER</li>
 *   <li><strong>Privilege</strong>: Fine-grained permissions like READ, WRITE, DELETE</li>
 * </ul>
 *
 * <p>When a user logs in, they get both role and privilege authorities which Spring Security
 * uses for authorization checks (e.g., @PreAuthorize("hasRole('ADMIN')") or 
 * @PreAuthorize("hasAuthority('DELETE_PRIVILEGE')")
 *
 * @see Privilege The fine-grained permissions
 * @see User User's role assignment
 */
@RequiredArgsConstructor
public enum Role {
    
    /**
     * ADMIN role - Full access to the application.
     * Privileges: READ, WRITE, UPDATE, DELETE
     */
    ADMIN(
            Set.of(READ_PRIVILEGE, WRITE_PRIVILEGE, UPDATE_PRIVILEGE, DELETE_PRIVILEGE)
    ),
    
    /**
     * USER role - Standard user with limited access.
     * Privileges: READ, WRITE (can view and create, but not modify or delete others' data)
     */
    USER(
            Set.of(READ_PRIVILEGE, WRITE_PRIVILEGE)
    );

    /** Set of fine-grained privileges associated with this role */
    @Getter
    private final Set<Privilege> privileges;

    /**
     * Converts this role and its privileges into Spring Security's GrantedAuthority objects.
     *
     * <p>This method is called by Spring Security to determine what authorities a user has.
     * It creates two types of authorities:
     * <ul>
     *   <li>Privilege authorities (e.g., "READ_PRIVILEGE", "DELETE_PRIVILEGE")</li>
     *   <li>Role authority (e.g., "ROLE_ADMIN", "ROLE_USER") - Spring Security convention</li>
     * </ul>
     *
     * <p>Example: A user with ADMIN role gets authorities:
     * ["READ_PRIVILEGE", "WRITE_PRIVILEGE", "UPDATE_PRIVILEGE", "DELETE_PRIVILEGE", "ROLE_ADMIN"]
     *
     * @return List of GrantedAuthority objects representing this role's permissions
     */
    public List<SimpleGrantedAuthority> getAuthorities() {
        // Map each privilege to a GrantedAuthority
        List<SimpleGrantedAuthority> authorities = getPrivileges()
                .stream()
                .map(privilege -> new SimpleGrantedAuthority(privilege.name()))
                .collect(Collectors.toList());
        
        // Add the role itself as an authority (Spring Security expects "ROLE_" prefix for roles)
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
        
        return authorities;
    }
}