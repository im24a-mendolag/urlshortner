package dev.zwazel.springintro.link;

import dev.zwazel.springintro.link.payload.DashboardResponse;
import dev.zwazel.springintro.link.payload.LinkResponse;
import dev.zwazel.springintro.link.payload.LinkStatsResponse;
import dev.zwazel.springintro.link.payload.ResolveLinkResponse;
import dev.zwazel.springintro.link.payload.ShortenRequest;
import dev.zwazel.springintro.link.payload.ToggleLinkRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class LinkController {

    private final LinkService linkService;

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

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(Authentication authentication, HttpServletRequest request) {
        List<LinkResponse> links = linkService.getDashboard(authentication, request);
        return new DashboardResponse(links);
    }

    @GetMapping("/stats/{code}")
    public LinkStatsResponse stats(@PathVariable String code, Authentication authentication) {
        return linkService.getStatsForCode(code, authentication);
    }

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

    @GetMapping("/resolve/{code}")
    public ResolveLinkResponse resolve(@PathVariable String code, HttpServletRequest request) {
        String originalUrl = linkService.resolveAndTrackRedirect(code, request);
        return new ResolveLinkResponse(originalUrl);
    }
}
