package com.m2i.exam.exam.client;

import com.m2i.exam.common.web.ApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls question-service's internal answer-key API with a SERVICE token.
 * Retry (outer) and circuit breaker (inner) are configured under resilience4j.*.questionService.
 */
@Component
public class QuestionClient {

    private static final Logger log = LoggerFactory.getLogger(QuestionClient.class);

    private final RestClient questionRestClient;

    public QuestionClient(RestClient questionRestClient) {
        this.questionRestClient = questionRestClient;
    }

    @Retry(name = "questionService", fallbackMethod = "unavailable")
    @CircuitBreaker(name = "questionService")
    public List<AnswerKey> answerKeys(Collection<String> questionIds) {
        return questionRestClient.post()
                .uri("/internal/answer-keys")
                .body(Map.of("questionIds", List.copyOf(questionIds)))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    @SuppressWarnings("unused")
    private List<AnswerKey> unavailable(Collection<String> questionIds, Throwable cause) {
        log.error("question-service call failed: {}", cause.toString());
        throw ApiException.unavailable("Question service is unavailable, please retry shortly");
    }

    public record KeyOption(String optionId, boolean correct) {
    }

    public record AnswerKey(String questionId, String orgId, String classId, String subjectId, String chapterId,
                            boolean active, List<KeyOption> options) {
    }
}
