package dev.zwazel.springintro.link;

import dev.zwazel.springintro.link.payload.LinkResponse;
import dev.zwazel.springintro.link.payload.LinkStatsResponse;
import dev.zwazel.springintro.user.User;
import dev.zwazel.springintro.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LinkService {

    /**
     * Short codes that must never be generated because they collide with real routes.
     * Keep this list lowercase; comparisons are done case-insensitively.
     */
    private static final Set<String> RESERVED_SHORT_CODES = Set.of(
            "api",
            "error",
            "login",
            "logout",
            "register",
            "dashboard",
            "stats",
            "links",
            "shorten",
            "resolve",
            "v1",
            "auth"
    );

    // Main links table.
    private final LinkRepository linkRepository;
    // Stores click events for each link.
    private final LinkClickRepository linkClickRepository;
    // Needed to resolve Authentication -> User entity.
    private final UserRepository userRepository;
    // Encodes numeric DB id into Base62 short code.
    private final Base62Encoder base62Encoder;

    /**
     * Create a new short link.
     *
     * Implementation detail:
     * Link.shortCode is NOT NULL in DB, but the final short code depends on generated ID.
     * So we save twice:
     * 1) save with temporary shortCode
     * 2) encode generated ID to Base62 and save final shortCode
     */
    public LinkResponse createShortLink(String originalUrl, Authentication authentication, HttpServletRequest request) {
        if (!isValidHttpUrl(originalUrl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid URL.");
        }

        // Returns null for anonymous calls; user entity for logged-in calls.
        User currentUser = resolveCurrentUser(authentication);

        // Temporary code just to satisfy NOT NULL/UNIQUE before real code exists.
        Link link = Link.builder()
                .originalUrl(originalUrl.trim())
                .user(currentUser)
                .shortCode("tmp-" + UUID.randomUUID())
                .build();

        // First save creates auto-increment ID.
        link = linkRepository.save(link);
        // Real code is Base62(ID), e.g. 62 -> "10" (but must not collide with reserved routes).
        link.setShortCode(generateSafeUniqueShortCode(link));
        // Second save persists final code.
        link = linkRepository.save(link);

        return toLinkResponse(link, request, 0);
    }

    /**
     * Dashboard endpoint:
     * Return only links owned by currently authenticated user.
     */
    @Transactional(readOnly = true)
    public List<LinkResponse> getDashboard(Authentication authentication, HttpServletRequest request) {
        User currentUser = requireAuthenticatedUser(authentication);
        List<Link> links = linkRepository.findAllByUser(currentUser);

        return links.stream()
                .map(link -> toLinkResponse(link, request, linkClickRepository.countByLink(link)))
                .toList();
    }

    /**
     * Enable/disable one link owned by the current user.
     */
    public LinkResponse setLinkDisabled(String code, boolean disabled, Authentication authentication, HttpServletRequest request) {
        User currentUser = requireAuthenticatedUser(authentication);
        Link link = findByShortCodeOrThrow(code);
        requireOwnership(link, currentUser);

        link.setDisabled(disabled);
        link = linkRepository.save(link);

        return toLinkResponse(link, request, linkClickRepository.countByLink(link));
    }

    /**
     * Stats endpoint:
     * - requires authenticated user
     * - requires ownership of the link
     */
    @Transactional(readOnly = true)
    public LinkStatsResponse getStatsForCode(String code, Authentication authentication) {
        User currentUser = requireAuthenticatedUser(authentication);
        Link link = findByShortCodeOrThrow(code);

        if (link.getUser() == null || !link.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to view stats for this link.");
        }

        List<LinkClick> clicks = linkClickRepository.findAllByLink(link);
        long clickCount = clicks.size();

        // Compute first and last click timestamps from stored click events.
        var firstClick = clicks.stream()
                .map(LinkClick::getClickedAt)
                .min(Comparator.naturalOrder())
                .orElse(null);
        var lastClick = clicks.stream()
                .map(LinkClick::getClickedAt)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new LinkStatsResponse(link.getShortCode(), clickCount, firstClick, lastClick);
    }

    /**
     * Redirect flow:
     * 1) resolve short code
     * 2) record click event
     * 3) return original URL for controller redirect
     */
    public String resolveAndTrackRedirect(String code, HttpServletRequest request) {
        Link link = findByShortCodeOrThrow(code);
        if (link.isDisabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Short code not found.");
        }

        LinkClick click = LinkClick.builder()
                .link(link)
                .referrer(request.getHeader("Referer"))
                .country(null)
                .build();
        linkClickRepository.save(click);

        return link.getOriginalUrl();
    }

    // Helper: load link by short code or return HTTP 404.
    private Link findByShortCodeOrThrow(String code) {
        return linkRepository.findByShortCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Short code not found."));
    }

    // Helper: enforce authenticated user and convert to User entity.
    private User requireAuthenticatedUser(Authentication authentication) {
        User user = resolveCurrentUser(authentication);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        return user;
    }

    private void requireOwnership(Link link, User currentUser) {
        if (link.getUser() == null || !link.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to modify this link.");
        }
    }

    // Helper: supports both authenticated and anonymous calls.
    private User resolveCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String email = authentication.getName();
        if (email == null || email.isBlank() || "anonymousUser".equalsIgnoreCase(email)) {
            return null;
        }

        return userRepository.findUserByEmail(email).orElse(null);
    }

    // Simple URL validation (http/https + host).
    private boolean isValidHttpUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isBlank()) {
            return false;
        }

        try {
            URI uri = URI.create(originalUrl.trim());
            String scheme = uri.getScheme();
            return uri.getHost() != null
                    && (scheme != null)
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    // Convert entity -> API DTO consumed by frontend.
    private LinkResponse toLinkResponse(Link link, HttpServletRequest request, long clickCount) {
        String shortUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath("/{code}")
                .replaceQuery(null)
                .buildAndExpand(link.getShortCode())
                .toUriString();

        return new LinkResponse(link.getShortCode(), shortUrl, link.getOriginalUrl(), clickCount, link.isDisabled());
    }

    private String generateSafeUniqueShortCode(Link persistedLink) {
        String base = base62Encoder.encodePersistedLinkId(persistedLink);
        String candidate = base;

        int suffix = 0;
        while (isReservedShortCode(candidate) || linkRepository.findByShortCode(candidate).isPresent()) {
            suffix++;
            candidate = base + base62Encoder.encode(suffix);
        }

        return candidate;
    }

    private boolean isReservedShortCode(String code) {
        if (code == null || code.isBlank()) {
            return true;
        }
        return RESERVED_SHORT_CODES.contains(code.toLowerCase());
    }
}
