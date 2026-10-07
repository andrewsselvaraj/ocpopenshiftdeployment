package com.m2i.exam.common.domain;

/**
 * Soft-delete flags used by every {@code *_status} column. Legacy data also contains
 * D, DEL, I and C; anything other than {@link #ACTIVE} is treated as inactive.
 */
public final class RecordStatus {

    public static final String ACTIVE = "A";
    public static final String DELETED = "D";

    private RecordStatus() {
    }

    public static boolean isActive(String status) {
        return ACTIVE.equals(status);
    }
}
