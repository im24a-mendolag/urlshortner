package dev.zwazel.springintro.link;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Base62EncoderTest {

    private final Base62Encoder encoder = new Base62Encoder();

    @Test
    void encodesKnownBase62Values() {
        assertEquals("1", encoder.encode(1));
        assertEquals("z", encoder.encode(61));
        assertEquals("10", encoder.encode(62));
        assertEquals("zz", encoder.encode(3843));
        assertEquals("100", encoder.encode(3844));
    }

    @Test
    void rejectsZeroAndNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> encoder.encode(0));
        assertThrows(IllegalArgumentException.class, () -> encoder.encode(-1));
    }

    @Test
    void encodesPersistedLinkId() {
        Link link = new Link();
        link.setId(62L);

        assertEquals("10", encoder.encodePersistedLinkId(link));
    }

    @Test
    void rejectsNullOrUnpersistedLink() {
        assertThrows(IllegalArgumentException.class, () -> encoder.encodePersistedLinkId(null));
        assertThrows(IllegalArgumentException.class, () -> encoder.encodePersistedLinkId(new Link()));
    }
}
