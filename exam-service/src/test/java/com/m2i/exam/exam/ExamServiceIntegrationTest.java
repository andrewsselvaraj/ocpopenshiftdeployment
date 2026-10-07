package com.m2i.exam.exam;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.m2i.exam.common.security.Role;
import com.m2i.exam.common.security.TokenService;
import com.m2i.exam.exam.client.QuestionClient;
import com.m2i.exam.exam.client.QuestionClient.AnswerKey;
import com.m2i.exam.exam.client.QuestionClient.KeyOption;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ExamServiceIntegrationTest {

    private static final String SUBMISSION = """
            {"subjectId":"sci","answers":[
              {"questionId":"q1","selectedOptionId":"q1-a"},
              {"questionId":"q2","selectedOptionId":"q2-b"}]}""";

    @Autowired
    MockMvc mvc;
    @Autowired
    TokenService tokens;
    @MockitoBean
    QuestionClient questionClient;

    @BeforeEach
    void answerKeys() {
        when(questionClient.answerKeys(anyCollection())).thenReturn(List.of(
                new AnswerKey("q1", "org1", "c1", "sci", "None", true,
                        List.of(new KeyOption("q1-a", true), new KeyOption("q1-b", false))),
                new AnswerKey("q2", "org1", "c1", "sci", "None", true,
                        List.of(new KeyOption("q2-a", true), new KeyOption("q2-b", false)))));
    }

    @Test
    void submissionIsScoredServerSideAndVisibleToOwnerAndAdminOnly() throws Exception {
        String student = token("kid@abc.com", Role.USER, "org1");
        String json = mvc.perform(post("/api/exams/submissions").header("Authorization", "Bearer " + student)
                        .contentType(MediaType.APPLICATION_JSON).content(SUBMISSION))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalQuestions").value(2))
                .andExpect(jsonPath("$.correctAnswers").value(1))
                .andExpect(jsonPath("$.scorePercent").value(50.0))
                .andExpect(jsonPath("$.answers[1].correctOptionIds[0]").value("q2-a"))
                .andReturn().getResponse().getContentAsString();
        String sessionId = JsonPath.read(json, "$.sessionId");

        mvc.perform(get("/api/exams/sessions/" + sessionId).header("Authorization", "Bearer " + student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correctAnswers").value(1));
        mvc.perform(get("/api/exams/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + token("admin@abc.com", Role.ADMIN, "org1")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/exams/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + token("peer@abc.com", Role.USER, "org1")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/exams/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + token("admin@other.com", Role.ADMIN, "org2")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/exams/sessions").header("Authorization", "Bearer " + student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].answeredQuestions").value(2));
        mvc.perform(get("/api/exams/sessions")
                        .header("Authorization", "Bearer " + token("peer@abc.com", Role.USER, "org1")))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void answersOutsideTheSubjectOrOrganisationAreRejected() throws Exception {
        mvc.perform(post("/api/exams/submissions")
                        .header("Authorization", "Bearer " + token("kid@other.com", Role.USER, "org2"))
                        .contentType(MediaType.APPLICATION_JSON).content(SUBMISSION))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/exams/submissions")
                        .header("Authorization", "Bearer " + token("kid@abc.com", Role.USER, "org1"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"subjectId":"sci","answers":[{"questionId":"q1","selectedOptionId":"q2-a"}]}"""))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/exams/submissions")
                        .header("Authorization", "Bearer " + token("kid@abc.com", Role.USER, "org1"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"subjectId":"sci","answers":[{"questionId":"q1","selectedOptionId":"q1-a"},
                                                              {"questionId":"q1","selectedOptionId":"q1-b"}]}"""))
                .andExpect(status().isBadRequest());
    }

    private String token(String email, Role role, String orgId) {
        return tokens.issue(email, List.of(role), Map.of("orgId", orgId)).value();
    }
}
