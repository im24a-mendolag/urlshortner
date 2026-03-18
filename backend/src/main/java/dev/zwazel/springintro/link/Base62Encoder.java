package dev.zwazel.springintro.link;

import org.springframework.stereotype.Component;

@Component
public class Base62Encoder {

    private static final String BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = BASE62_ALPHABET.length();

    public String encode(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Base62 encoding requires a positive value.");
        }

        StringBuilder encoded = new StringBuilder();
        long current = value;

        while (current > 0) {
            int remainder = (int) (current % BASE);
            encoded.append(BASE62_ALPHABET.charAt(remainder));
            current /= BASE;
        }

        return encoded.reverse().toString();
    }

    public String encodePersistedLinkId(Link link) {
        if (link == null || link.getId() == null) {
            throw new IllegalArgumentException("Link must be persisted before generating a short code.");
        }

        // Integration point for /shorten flow: save link first, then encode generated ID.
        return encode(link.getId());
    }
}
