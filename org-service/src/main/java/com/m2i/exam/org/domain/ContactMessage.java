package com.m2i.exam.org.domain;

import com.m2i.exam.common.domain.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/** "Contact us" message from the public website - {@code contact_m2i}. */
@Entity
@Table(name = "contact_m2i")
public class ContactMessage extends AssignedIdEntity {

    @Id
    @Column(name = "m_contact_id", length = 50)
    private String id;

    @Column(name = "m_name", nullable = false, length = 50)
    private String name;

    @Column(name = "m_email", nullable = false, length = 50)
    private String email;

    @Column(name = "m_subject", nullable = false, length = 50)
    private String subject;

    @Column(name = "m_message", nullable = false, length = 500)
    private String message;

    @Column(name = "m_message_updated_date_time", nullable = false)
    private Instant createdAt;

    @Column(name = "m_message_status", nullable = false, length = 50)
    private String status;

    protected ContactMessage() {
    }

    public ContactMessage(String id, String name, String email, String subject, String message, String status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.subject = subject;
        this.message = message;
        this.status = status;
    }

    @PrePersist
    void touch() {
        createdAt = Instant.now();
    }

    @Override
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getStatus() {
        return status;
    }
}
