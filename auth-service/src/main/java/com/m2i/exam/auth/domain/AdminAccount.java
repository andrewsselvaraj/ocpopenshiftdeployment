package com.m2i.exam.auth.domain;

import com.m2i.exam.common.domain.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/** Platform-level super administrator ({@code admin_info}). Not bound to an organisation. */
@Entity
@Table(name = "admin_info")
public class AdminAccount extends AssignedIdEntity {

    @Id
    @Column(name = "pk_admin_id", length = 50)
    private String id;

    @Column(name = "admin_name", nullable = false, length = 50)
    private String name;

    @Column(name = "admin_email", nullable = false, length = 50)
    private String email;

    @Column(name = "admin_password", nullable = false, length = 100)
    private String password;

    @Column(name = "admin_role", nullable = false, length = 50)
    private String role;

    @Column(name = "admin_updated_date_time", nullable = false)
    private Instant updatedAt;

    @Column(name = "admin_status", nullable = false, length = 50)
    private String status;

    protected AdminAccount() {
    }

    public AdminAccount(String id, String name, String email, String password, String role, String status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = status;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getStatus() {
        return status;
    }
}
