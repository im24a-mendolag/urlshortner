package dev.zwazel.springintro.link;

import dev.zwazel.springintro.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "links")
public class Link {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The Base62-encoded short code, e.g. "aB3xZ" */
    @Column(name = "short_code", unique = true, nullable = false)
    private String shortCode;

    /** The full destination URL */
    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    /** Owner of this link — NULL means it was created anonymously */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
