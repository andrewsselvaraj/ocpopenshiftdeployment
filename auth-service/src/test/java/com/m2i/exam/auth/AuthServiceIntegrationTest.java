package com.m2i.exam.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.m2i.exam.auth.domain.AdminAccount;
import com.m2i.exam.auth.domain.AdminRepository;
import com.m2i.exam.auth.domain.UserAccount;
import com.m2i.exam.auth.domain.UserRepository;
import com.m2i.exam.common.security.Role;
import com.m2i.exam.common.security.TokenService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthServiceIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    UserRepository users;
    @Autowired
    AdminRepository admins;
    @Autowired
    TokenService tokens;

    @BeforeEach
    void seedLegacyRows() {
        users.deleteAll();
        admins.deleteAll();
        // Plain-text passwords exactly as in the legacy dump.
        admins.save(new AdminAccount("adm1", "Danny", "danny@example.com", "danny", "Super Admin", "A"));
        users.save(new UserAccount("usr1", "org1", "Abi", "abi@example.com", "abi-legacy", "Admin",
                "danny@example.com", "A"));
        users.save(new UserAccount("usr2", "org1", "Gone", "gone@example.com", "gone", "User",
                "danny@example.com", "D"));
    }

    @Test
    void legacyPlainTextLoginSucceedsAndIsUpgradedToBcrypt() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"abi@example.com\",\"password\":\"abi-legacy\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.orgId").value("org1"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        assertThat(users.findById("usr1").orElseThrow().getPassword()).startsWith("$2");

        // Second login goes through the BCrypt path.
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"abi@example.com\",\"password\":\"abi-legacy\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void superAdminLoginHasNoOrganisation() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"danny@example.com\",\"password\":\"danny\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.user.orgId").doesNotExist());
    }

    @Test
    void wrongPasswordAndInactiveUsersAreRejected() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"abi@example.com\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"gone@example.com\",\"password\":\"gone\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointsRequireToken() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void orgAdminCanManageOnlyOwnOrganisation() throws Exception {
        String adminToken = token("abi@example.com", Role.ADMIN, "org1");

        mvc.perform(post("/api/users").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orgId":"org1","name":"Student","email":"Student@Example.com",
                                 "password":"Secret#123","role":"User"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("student@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mvc.perform(post("/api/users").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orgId":"org2","name":"Intruder","email":"x@example.com",
                                 "password":"Secret#123","role":"User"}"""))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void duplicateEmailIsConflictAndUsersCannotListUsers() throws Exception {
        mvc.perform(post("/api/users").header("Authorization", "Bearer " + token("abi@example.com", Role.ADMIN, "org1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orgId":"org1","name":"Dup","email":"danny@example.com",
                                 "password":"Secret#123","role":"User"}"""))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/users").header("Authorization", "Bearer " + token("s@example.com", Role.USER, "org1")))
                .andExpect(status().isForbidden());
    }

    private String token(String email, Role role, String orgId) {
        return tokens.issue(email, List.of(role), Map.of("orgId", orgId)).value();
    }
}
