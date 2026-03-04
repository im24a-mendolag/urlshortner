package dev.zwazel.springintro.security;

/**
 * Enum defining fine-grained permissions in the application.
 *
 * <p>Privileges are the actual permissions that control what actions a user can perform.
 * They are mapped to roles (see {@link Role}) so that different user types can be
 * easily granted different permissions.
 *
 * <p>Example usage in controllers:
 * <pre>
 *   @PreAuthorize("hasAuthority('DELETE_PRIVILEGE')")
 *   public ResponseEntity<Void> deleteUser(UUID userId) { ... }
 * </pre>
 *
 * @see Role Associates privileges with user roles
 */
public enum Privilege {
    
    /** Permission to read/view data */
    READ_PRIVILEGE,
    
    /** Permission to create new data */
    WRITE_PRIVILEGE,
    
    /** Permission to remove/delete data */
    DELETE_PRIVILEGE,
    
    /** Permission to modify existing data */
    UPDATE_PRIVILEGE,
}