package dev.zwazel.springintro.link;

import dev.zwazel.springintro.link.payload.DashboardResponse;
import dev.zwazel.springintro.link.payload.LinkResponse;
import dev.zwazel.springintro.link.payload.LinkStatsResponse;
import dev.zwazel.springintro.link.payload.ResolveLinkResponse;
import dev.zwazel.springintro.link.payload.ShortenRequest;
import dev.zwazel.springintro.link.payload.ToggleLinkRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class LinkController {

    // Service contains all business logic (DB writes, ownership checks, URL validation).
    private final LinkService linkService;

    /**
     * Create a short link from a long URL.
     *
     * Works for both:
     * - anonymous users (user_id will be null)
     * - authenticated users (user_id will be set)
     */
    @PostMapping("/shorten")
    public ResponseEntity<LinkResponse> shorten(
            @RequestBody ShortenRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String originalUrl = request == null ? null : request.originalUrl();
        LinkResponse created = linkService.createShortLink(originalUrl, authentication, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Return all links created by the currently logged-in user.
     * Anonymous users are rejected by the service with 401.
     */
    @GetMapping("/dashboard")
    public DashboardResponse dashboard(Authentication authentication, HttpServletRequest request) {
        List<LinkResponse> links = linkService.getDashboard(authentication, request);
        return new DashboardResponse(links);
    }

    /**
     * Return stats for one short code (click count + first/last click time).
     * Access is owner-only (403 if the link belongs to another user).
     */
    @GetMapping("/stats/{code}")
    public LinkStatsResponse stats(@PathVariable String code, Authentication authentication) {
        return linkService.getStatsForCode(code, authentication);
    }

    /**
     * Toggle disabled flag for one link.
     * Only owner can change this.
     */
    @PatchMapping("/links/{code}/disabled")
    public LinkResponse toggleDisabled(
            @PathVariable String code,
            @RequestBody ToggleLinkRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        boolean disabled = request != null && request.disabled();
        return linkService.setLinkDisabled(code, disabled, authentication, httpRequest);
    }

    /**
     * Public endpoint used by frontend to resolve a short code.
     * Also records the click before returning the destination URL.
     */
    @GetMapping("/resolve/{code}")
    public ResolveLinkResponse resolve(@PathVariable String code, HttpServletRequest request) {
        String originalUrl = linkService.resolveAndTrackRedirect(code, request);
        return new ResolveLinkResponse(originalUrl);
    }

    /**
     * Public short URL endpoint.
     * Example: GET /abc123 -> records click -> sends HTTP redirect to original URL.
     */
    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        String originalUrl = linkService.resolveAndTrackRedirect(code, request);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, originalUrl)
                .build();
    }
}
