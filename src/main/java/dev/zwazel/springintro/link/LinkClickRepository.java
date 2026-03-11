package dev.zwazel.springintro.link;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LinkClickRepository extends JpaRepository<LinkClick, UUID> {

    List<LinkClick> findAllByLink(Link link);

    long countByLink(Link link);
}
