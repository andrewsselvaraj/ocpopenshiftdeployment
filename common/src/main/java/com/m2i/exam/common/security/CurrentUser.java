package com.m2i.exam.common.security;

import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Accessors for the authenticated caller plus tenant (organisation) isolation rules:
 * a SUPER_ADMIN can act on any organisation, everybody else only on the one in their token.
 */
public final class CurrentUser {

    public static final String ORG_CLAIM = "orgId";
    public static final String USER_ID_CLAIM = "uid";
    public static final String NAME_CLAIM = "name";

    private CurrentUser() {
    }

    public static Jwt jwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token) {
            return token.getToken();
        }
        throw new AccessDeniedException("No authenticated user");
    }

    public static String email() {
        return jwt().getSubject();
    }

    public static String orgId() {
        return jwt().getClaimAsString(ORG_CLAIM);
    }

    public static boolean hasRole(Role role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role.name()));
    }

    public static boolean isSuperAdmin() {
        return hasRole(Role.SUPER_ADMIN);
    }

    public static void assertOrgAccess(String orgId) {
        if (!isSuperAdmin() && !Objects.equals(orgId, orgId())) {
            throw new AccessDeniedException("Access to organisation " + orgId + " is not allowed");
        }
    }

    /**
     * Resolves the organisation a request applies to. Super admins get what they asked for
     * ({@code null} meaning all organisations); everybody else gets their own and may not name another.
     */
    public static String resolveOrg(String requestedOrgId) {
        if (isSuperAdmin()) {
            return requestedOrgId;
        }
        if (requestedOrgId != null && !requestedOrgId.isBlank()) {
            assertOrgAccess(requestedOrgId);
        }
        return orgId();
    }
}
