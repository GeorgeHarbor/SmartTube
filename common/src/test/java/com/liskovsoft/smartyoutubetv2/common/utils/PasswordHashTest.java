package com.liskovsoft.smartyoutubetv2.common.utils;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class PasswordHashTest {
    @Test
    public void saltedHashRejectsWrongAndEmptyPasswords() {
        String first = PasswordHash.create("correct horse"),
                second = PasswordHash.create("correct horse");
        assertNotEquals(first, second);
        assertFalse(first.contains("correct horse"));
        assertTrue(PasswordHash.matches(first, "correct horse"));
        assertFalse(PasswordHash.matches(first, "smarttube"));
        assertFalse(PasswordHash.matches(first, ""));
        assertFalse(PasswordHash.matches(first, null));
    }

    @Test
    public void legacyMatchingHasNoMasterBypass() {
        assertTrue(PasswordHash.matches("legacy", "legacy"));
        assertFalse(PasswordHash.matches("legacy", "smarttube"));
    }

    @Test
    public void malformedHashFailsClosed() {
        assertFalse(PasswordHash.matches("pbkdf2v1$bad$bad", "value"));
    }
}
