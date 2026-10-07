package com.m2i.exam.org;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.m2i.exam.common.security.Role;
import com.m2i.exam.common.security.TokenService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
class OrgServiceIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    TokenService tokens;

    @Test
    void hierarchyIsCreatedWithinTheTenantAndIsolatedFromOthers() throws Exception {
        String superAdmin = token("root@example.com", Role.SUPER_ADMIN, null);
        String orgId = id(mvc.perform(post("/api/orgs").header("Authorization", "Bearer " + superAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ABC School\",\"email\":\"abc@example.com\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        String admin = token("admin@abc.com", Role.ADMIN, orgId);
        String classId = id(mvc.perform(post("/api/classes").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orgId\":\"" + orgId + "\",\"name\":\"Class 1\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());

        String subjectId = id(mvc.perform(post("/api/subjects").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classId\":\"" + classId + "\",\"name\":\"Science\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orgId").value(orgId))
                .andReturn().getResponse().getContentAsString());

        mvc.perform(post("/api/chapters").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjectId\":\"" + subjectId + "\",\"name\":\"Planets\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.classId").value(classId));

        String student = token("kid@abc.com", Role.USER, orgId);
        mvc.perform(get("/api/subjects").param("classId", classId).header("Authorization", "Bearer " + student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Science"));
        mvc.perform(post("/api/classes").header("Authorization", "Bearer " + student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orgId\":\"" + orgId + "\",\"name\":\"Hack\"}"))
                .andExpect(status().isForbidden());

        String otherAdmin = token("admin@other.com", Role.ADMIN, "other-org");
        mvc.perform(get("/api/classes/" + classId).header("Authorization", "Bearer " + otherAdmin))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/subjects").header("Authorization", "Bearer " + otherAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classId\":\"" + classId + "\",\"name\":\"Injected\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void contactFormIsPublicButListingIsSuperAdminOnly() throws Exception {
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Visitor","email":"v@example.com","subject":"Demo","message":"Hello"}"""))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/contact").header("Authorization", "Bearer " + token("u@x.com", Role.USER, "o1")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/contact").header("Authorization",
                        "Bearer " + token("root@example.com", Role.SUPER_ADMIN, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].subject").value("Demo"));
    }

    @Test
    void validationErrorsAreReportedAsProblemDetails() throws Exception {
        mvc.perform(post("/api/orgs").header("Authorization", "Bearer " + token("r@x.com", Role.SUPER_ADMIN, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    private String token(String email, Role role, String orgId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("orgId", orgId);
        return tokens.issue(email, List.of(role), claims).value();
    }

    private static String id(String json) {
        return JsonPath.read(json, "$.id");
    }
}
