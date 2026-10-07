package com.m2i.exam.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt hashing with a migration path for the legacy dump, which stores passwords in plain text.
 * A legacy password is accepted once and immediately re-hashed (see {@link AuthService#login}).
 */
@Component
public class PasswordService {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
    /** Used to spend the same time on unknown e-mails as on known ones (no user enumeration by timing). */
    private final String dummyHash = bcrypt.encode("timing-equaliser");

    public String hash(String raw) {
        return bcrypt.encode(raw);
    }

    public boolean matches(String raw, String stored) {
        if (raw == null || stored == null) {
            return false;
        }
        if (isHashed(stored)) {
            return bcrypt.matches(raw, stored);
        }
        return MessageDigest.isEqual(raw.getBytes(StandardCharsets.UTF_8), stored.getBytes(StandardCharsets.UTF_8));
    }

    public boolean needsUpgrade(String stored) {
        return !isHashed(stored);
    }

    public void simulateCheck(String raw) {
        bcrypt.matches(raw == null ? "" : raw, dummyHash);
    }

    private static boolean isHashed(String stored) {
        return stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$");
    }
}
