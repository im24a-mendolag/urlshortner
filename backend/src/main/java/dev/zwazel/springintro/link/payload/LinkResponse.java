package dev.zwazel.springintro.link.payload;

public record LinkResponse(
        String shortCode,
        String shortUrl,
        String originalUrl,
        long clickCount,
        boolean disabled
) {
}
