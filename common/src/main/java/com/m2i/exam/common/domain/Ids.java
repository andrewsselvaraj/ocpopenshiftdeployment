package com.m2i.exam.common.domain;

import java.util.UUID;

/** Primary keys are varchar(50); legacy rows hold PHP uniqid() values, new rows get 32-char UUIDs. */
public final class Ids {

    private Ids() {
    }

    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
