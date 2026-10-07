package com.m2i.exam.common.security;

import java.util.Arrays;

/**
 * Application roles. The legacy schema stores human-readable labels in
 * {@code admin_info.admin_role} and {@code user_info.user_role}; this enum maps them.
 */
public enum Role {
    SUPER_ADMIN("Super Admin"),
    ADMIN("Admin"),
    DATA_ENTRY_OPERATOR("Data Entry Operator"),
    USER("User"),
    /** Machine identity used for service-to-service calls; never stored in the database. */
    SERVICE("Service");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Role fromLabel(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Role is required");
        }
        String v = value.trim();
        return Arrays.stream(values())
                .filter(r -> r.label.equalsIgnoreCase(v) || r.name().equalsIgnoreCase(v))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown role: " + value));
    }
}
