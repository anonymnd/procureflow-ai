package com.project.app.identity.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TokenValuesTest {
    @Test
    void createsHighEntropyUrlSafeTokenAndStableOneWayHash() {
        TokenValues values = new TokenValues();
        String raw = values.newToken();
        assertEquals(43, raw.length());
        assertTrue(raw.matches("[A-Za-z0-9_-]+"));
        assertEquals(64, values.hash(raw).length());
        assertEquals(values.hash(raw), values.hash(raw));
        assertNotEquals(raw, values.hash(raw));
        assertNotEquals(raw, values.newToken());
    }
}
