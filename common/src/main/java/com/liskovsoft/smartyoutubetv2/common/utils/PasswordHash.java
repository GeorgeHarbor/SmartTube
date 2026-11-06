package com.liskovsoft.smartyoutubetv2.common.utils;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHash {
    private static final String PREFIX = "pbkdf2v1$";

    private PasswordHash() {}

    public static boolean encoded(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    public static String create(String password) {
        if (password == null || encoded(password)) return password;
        return hashInput(password);
    }

    public static String hashInput(String password) {
        if (password == null || password.isEmpty()) throw new IllegalArgumentException("Empty password");
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return PREFIX
                + Base64.encodeToString(salt, Base64.NO_WRAP)
                + "$"
                + Base64.encodeToString(derive(password, salt), Base64.NO_WRAP);
    }

    public static boolean matches(String saved, String typed) {
        if (saved == null) return true;
        if (typed == null || typed.isEmpty()) return false;
        if (!encoded(saved))
            return MessageDigest.isEqual(
                    saved.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    typed.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        try {
            String[] parts = saved.split("\\$");
            if (parts.length != 3) return false;
            byte[] salt = Base64.decode(parts[1], Base64.NO_WRAP),
                    expected = Base64.decode(parts[2], Base64.NO_WRAP);
            return salt.length == 16
                    && expected.length == 32
                    && MessageDigest.isEqual(expected, derive(typed, salt));
        } catch (RuntimeException failure) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 210000, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                    .generateSecret(spec)
                    .getEncoded();
        } catch (Exception failure) {
            throw new IllegalStateException("Password protection unavailable", failure);
        } finally {
            spec.clearPassword();
        }
    }
}
