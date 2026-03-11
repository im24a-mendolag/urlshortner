package dev.zwazel.springintro.link;

import dev.zwazel.springintro.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinkRepository extends JpaRepository<Link, UUID> {

    Optional<Link> findByShortCode(String shortCode);

    List<Link> findAllByUser(User user);
}
