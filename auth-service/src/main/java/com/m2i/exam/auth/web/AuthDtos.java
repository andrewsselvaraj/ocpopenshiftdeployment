package com.m2i.exam.auth.web;

import com.m2i.exam.auth.domain.AdminAccount;
import com.m2i.exam.auth.domain.UserAccount;
import com.m2i.exam.common.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Request/response payloads of auth-service. Passwords never leave the service. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 50) String email,
            @NotBlank @Size(max = 100) String password) {
    }

    public record LoginResponse(String accessToken, String tokenType, long expiresIn, Profile user) {
    }

    public record Profile(String id, String name, String email, Role role, String orgId) {
    }

    public record CreateUserRequest(
            @NotBlank @Size(max = 50) String orgId,
            @NotBlank @Size(max = 50) String name,
            @NotBlank @Email @Size(max = 50) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Pattern(regexp = "(?i)ADMIN|DATA_ENTRY_OPERATOR|USER|Admin|Data Entry Operator|User",
                    message = "must be ADMIN, DATA_ENTRY_OPERATOR or USER") String role) {
    }

    public record UpdateUserRequest(
            @Size(min = 1, max = 50) String name,
            @Pattern(regexp = "(?i)ADMIN|DATA_ENTRY_OPERATOR|USER|Admin|Data Entry Operator|User",
                    message = "must be ADMIN, DATA_ENTRY_OPERATOR or USER") String role) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }

    public record CreateAdminRequest(
            @NotBlank @Size(max = 50) String name,
            @NotBlank @Email @Size(max = 50) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record UserResponse(String id, String orgId, String name, String email, Role role,
                               String status, String updatedBy, Instant updatedAt) {

        public static UserResponse from(UserAccount u) {
            return new UserResponse(u.getId(), u.getOrgId(), u.getName(), u.getEmail(), roleOf(u.getRole()),
                    u.getStatus(), u.getUpdatedBy(), u.getUpdatedAt());
        }
    }

    public record AdminResponse(String id, String name, String email, String status, Instant updatedAt) {

        public static AdminResponse from(AdminAccount a) {
            return new AdminResponse(a.getId(), a.getName(), a.getEmail(), a.getStatus(), a.getUpdatedAt());
        }
    }

    /** Unknown legacy labels degrade to the least privileged role rather than failing. */
    static Role roleOf(String label) {
        try {
            Role role = Role.fromLabel(label);
            return role == Role.SUPER_ADMIN || role == Role.SERVICE ? Role.USER : role;
        } catch (IllegalArgumentException e) {
            return Role.USER;
        }
    }
}
