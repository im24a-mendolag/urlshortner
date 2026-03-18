package dev.zwazel.springintro.link.payload;

import java.time.LocalDateTime;

public record LinkStatsResponse(
        String shortCode,
        long clickCount,
        LocalDateTime firstClickAt,
        LocalDateTime lastClickAt
) {
}
