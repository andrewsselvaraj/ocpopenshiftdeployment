package com.m2i.exam.question;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.m2i.exam.common.security.Role;
import com.m2i.exam.common.security.TokenService;
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
class QuestionServiceIntegrationTest {

    private static final String QUESTION = """
            {"classId":"c1","subjectId":"%s","text":"Which is the biggest planet?",
             "options":[{"text":"Jupiter","correct":true},{"text":"Mars","correct":false},
                        {"text":"Venus","correct":false}]}""";

    @Autowired
    MockMvc mvc;
    @Autowired
    TokenService tokens;

    @Test
    void paperHidesCorrectAnswersWhileAnswerKeysExposeThemToServicesOnly() throws Exception {
        String deo = token("deo@abc.com", Role.DATA_ENTRY_OPERATOR, "org1");
        String json = mvc.perform(post("/api/questions").header("Authorization", "Bearer " + deo)
                        .contentType(MediaType.APPLICATION_JSON).content(QUESTION.formatted("sci")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orgId").value("org1"))
                .andExpect(jsonPath("$.chapterId").value("None"))
                .andExpect(jsonPath("$.options", hasSize(3)))
                .andReturn().getResponse().getContentAsString();
        String questionId = JsonPath.read(json, "$.id");

        String student = token("kid@abc.com", Role.USER, "org1");
        mvc.perform(get("/api/question-papers").param("subjectId", "sci").header("Authorization", "Bearer " + student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions", hasSize(1)))
                .andExpect(jsonPath("$.questions[0].options", hasSize(3)))
                .andExpect(jsonPath("$.questions[0].options[0].correct").doesNotExist());

        // Exam takers cannot read the bank or the answer keys.
        mvc.perform(get("/api/questions/" + questionId).header("Authorization", "Bearer " + student))
                .andExpect(status().isForbidden());
        String keysRequest = "{\"questionIds\":[\"" + questionId + "\"]}";
        mvc.perform(post("/internal/answer-keys").header("Authorization", "Bearer " + student)
                        .contentType(MediaType.APPLICATION_JSON).content(keysRequest))
                .andExpect(status().isForbidden());

        mvc.perform(post("/internal/answer-keys").header("Authorization", "Bearer " + tokens.serviceToken("test"))
                        .contentType(MediaType.APPLICATION_JSON).content(keysRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].questionId").value(questionId))
                .andExpect(jsonPath("$[0].options[?(@.correct == true)]", hasSize(1)));

        // Another organisation cannot see the question.
        mvc.perform(get("/api/question-papers").param("subjectId", "sci")
                        .header("Authorization", "Bearer " + token("x@other.com", Role.USER, "org2")))
                .andExpect(jsonPath("$.questions", hasSize(0)));
    }

    @Test
    void updateReconcilesOptionsWithSoftDelete() throws Exception {
        String admin = token("admin@abc.com", Role.ADMIN, "org1");
        String json = mvc.perform(post("/api/questions").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(QUESTION.formatted("geo")))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(json, "$.id");
        String jupiterId = JsonPath.read(json, "$.options[0].id");

        mvc.perform(put("/api/questions/" + id).header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"classId":"c1","subjectId":"geo","text":"Largest planet?",
                                 "options":[{"id":"%s","text":"Jupiter","correct":true},
                                            {"text":"Saturn","correct":false}]}""".formatted(jupiterId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Largest planet?"))
                .andExpect(jsonPath("$.options", hasSize(2)))
                .andExpect(jsonPath("$.options[0].id").value(jupiterId))
                .andExpect(jsonPath("$.options[1].text").value("Saturn"));

        mvc.perform(get("/api/questions/" + id).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.options", hasSize(2)));
    }

    @Test
    void questionWithoutCorrectOptionIsRejected() throws Exception {
        mvc.perform(post("/api/questions").header("Authorization", "Bearer " + token("a@abc.com", Role.ADMIN, "org1"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"classId":"c1","subjectId":"s","text":"Q?",
                                 "options":[{"text":"A","correct":false},{"text":"B","correct":false}]}"""))
                .andExpect(status().isBadRequest());
    }

    private String token(String email, Role role, String orgId) {
        return tokens.issue(email, List.of(role), Map.of("orgId", orgId)).value();
    }
}
