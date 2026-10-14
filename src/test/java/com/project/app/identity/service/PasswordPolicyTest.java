package com.project.app.identity.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

class PasswordPolicyTest {
    @Test
    void enforcesInclusiveUnicodeCodePointBounds() {
        assertFalse(PasswordPolicy.accepts("x".repeat(11)));
        assertTrue(PasswordPolicy.accepts("x".repeat(12)));
        assertTrue(PasswordPolicy.accepts("x".repeat(128)));
        assertFalse(PasswordPolicy.accepts("x".repeat(129)));
        assertFalse(PasswordPolicy.accepts(null));
    }

    @Test
    void allowsSpacesAndCountsSupplementaryCharactersAsOneCodePoint() {
        assertTrue(PasswordPolicy.accepts("            "));
        assertTrue(PasswordPolicy.accepts("😀".repeat(12)));
        assertFalse(PasswordPolicy.accepts("😀".repeat(11)));
    }

    @Test
    void frameworkHashProcessesLongUnicodeInputsWithoutTrimmingOrTruncation() {
        var encoder = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        String first = "😀".repeat(40) + "a".repeat(48);
        String differentTail = "😀".repeat(40) + "b".repeat(48);
        String hash = encoder.encode(first);
        assertTrue(encoder.matches(first, hash));
        assertFalse(encoder.matches(differentTail, hash));
        String spacedHash = encoder.encode(" " + first);
        assertTrue(encoder.matches(" " + first, spacedHash));
        assertFalse(encoder.matches(first, spacedHash));
    }
}
