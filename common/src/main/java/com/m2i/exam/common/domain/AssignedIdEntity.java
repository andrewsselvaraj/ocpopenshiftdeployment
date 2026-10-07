package com.m2i.exam.common.domain;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

/**
 * Base for entities whose varchar primary key is assigned by the application.
 * Implements {@link Persistable} so {@code save()} on a new entity issues a plain INSERT
 * instead of Spring Data's default SELECT-then-merge.
 */
@MappedSuperclass
public abstract class AssignedIdEntity implements Persistable<String> {

    @Transient
    private boolean newEntity = true;

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.newEntity = false;
    }
}
