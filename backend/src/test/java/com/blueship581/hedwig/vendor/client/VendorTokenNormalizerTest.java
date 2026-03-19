package com.blueship581.hedwig.vendor.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VendorTokenNormalizerTest {

    @Test
    void keepsRawTokenUntouched() {
        assertEquals("abc.def.ghi", VendorTokenNormalizer.normalize("abc.def.ghi"));
    }

    @Test
    void stripsBearerPrefix() {
        assertEquals("abc.def.ghi", VendorTokenNormalizer.normalize("Bearer abc.def.ghi"));
    }

    @Test
    void stripsAuthorizationHeaderPrefix() {
        assertEquals("abc.def.ghi", VendorTokenNormalizer.normalize("Authorization: Bearer abc.def.ghi"));
    }

    @Test
    void stripsQuotesAndWhitespace() {
        assertEquals("abc.def.ghi", VendorTokenNormalizer.normalize("  \"Bearer abc.\ndef.ghi\"  "));
    }

    @Test
    void keepsNullAsNull() {
        assertNull(VendorTokenNormalizer.normalize(null));
    }
}
