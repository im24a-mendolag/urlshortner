package dev.zwazel.springintro.link;

import dev.zwazel.springintro.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LinkRepository extends JpaRepository<Link, Long> {

    Optional<Link> findByShortCode(String shortCode);

    List<Link> findAllByUser(User user);
}
