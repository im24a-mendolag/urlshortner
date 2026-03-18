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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LinkService {

    private final LinkRepository linkRepository;
    private final LinkClickRepository linkClickRepository;
    private final UserRepository userRepository;
    private final Base62Encoder base62Encoder;

    public LinkResponse createShortLink(String originalUrl, Authentication authentication, HttpServletRequest request) {
        if (!isValidHttpUrl(originalUrl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid URL.");
        }

        User currentUser = resolveCurrentUser(authentication);

        Link link = Link.builder()
                .originalUrl(originalUrl.trim())
                .user(currentUser)
                .shortCode("tmp-" + UUID.randomUUID())
                .build();

        link = linkRepository.save(link);
        link.setShortCode(base62Encoder.encodePersistedLinkId(link));
        link = linkRepository.save(link);

        return toLinkResponse(link, request, 0);
    }

    @Transactional(readOnly = true)
    public List<LinkResponse> getDashboard(Authentication authentication, HttpServletRequest request) {
        User currentUser = requireAuthenticatedUser(authentication);
        List<Link> links = linkRepository.findAllByUser(currentUser);

        return links.stream()
                .map(link -> toLinkResponse(link, request, linkClickRepository.countByLink(link)))
                .toList();
    }

    @Transactional(readOnly = true)
    public LinkStatsResponse getStatsForCode(String code, Authentication authentication) {
        User currentUser = requireAuthenticatedUser(authentication);
        Link link = findByShortCodeOrThrow(code);

        if (link.getUser() == null || !link.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to view stats for this link.");
        }

        List<LinkClick> clicks = linkClickRepository.findAllByLink(link);
        long clickCount = clicks.size();

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

    public String resolveAndTrackRedirect(String code, HttpServletRequest request) {
        Link link = findByShortCodeOrThrow(code);

        LinkClick click = LinkClick.builder()
                .link(link)
                .referrer(request.getHeader("Referer"))
                .country(null)
                .build();
        linkClickRepository.save(click);

        return link.getOriginalUrl();
    }

    private Link findByShortCodeOrThrow(String code) {
        return linkRepository.findByShortCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Short code not found."));
    }

    private User requireAuthenticatedUser(Authentication authentication) {
        User user = resolveCurrentUser(authentication);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        return user;
    }

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

    private LinkResponse toLinkResponse(Link link, HttpServletRequest request, long clickCount) {
        String shortUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath("/{code}")
                .replaceQuery(null)
                .buildAndExpand(link.getShortCode())
                .toUriString();

        return new LinkResponse(link.getShortCode(), shortUrl, link.getOriginalUrl(), clickCount);
    }
}
