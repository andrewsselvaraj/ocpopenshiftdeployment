package com.m2i.exam.exam.service;

import com.m2i.exam.common.domain.Ids;
import com.m2i.exam.common.domain.RecordStatus;
import com.m2i.exam.common.security.CurrentUser;
import com.m2i.exam.common.security.Role;
import com.m2i.exam.common.web.ApiException;
import com.m2i.exam.exam.client.QuestionClient;
import com.m2i.exam.exam.client.QuestionClient.AnswerKey;
import com.m2i.exam.exam.client.QuestionClient.KeyOption;
import com.m2i.exam.exam.domain.ExamAnswer;
import com.m2i.exam.exam.domain.ExamAnswerRepository;
import com.m2i.exam.exam.domain.SessionSummary;
import com.m2i.exam.exam.web.ExamDtos.AnswerResult;
import com.m2i.exam.exam.web.ExamDtos.AnswerSubmission;
import com.m2i.exam.exam.web.ExamDtos.ExamResult;
import com.m2i.exam.exam.web.ExamDtos.SubmitExamRequest;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exam submission and scoring. Scoring is always done server-side against answer keys fetched
 * from question-service; the client never sees which option is correct before submitting.
 */
@Service
public class ExamService {

    private final ExamAnswerRepository answers;
    private final QuestionClient questionClient;
    private final MeterRegistry meterRegistry;

    public ExamService(ExamAnswerRepository answers, QuestionClient questionClient, MeterRegistry meterRegistry) {
        this.answers = answers;
        this.questionClient = questionClient;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public ExamResult submit(SubmitExamRequest req) {
        String orgId = CurrentUser.orgId();
        if (orgId == null) {
            throw ApiException.badRequest("Only organisation members can take exams");
        }
        Set<String> questionIds = new HashSet<>();
        for (AnswerSubmission a : req.answers()) {
            if (!questionIds.add(a.questionId())) {
                throw ApiException.badRequest("Question " + a.questionId() + " is answered more than once");
            }
        }

        Map<String, AnswerKey> keys = fetchKeys(questionIds);
        String sessionId = Ids.newId();
        String takenBy = CurrentUser.email();
        // MySQL timestamp(6) keeps microseconds; truncate so the response matches what is stored.
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        List<ExamAnswer> rows = new ArrayList<>();
        for (AnswerSubmission a : req.answers()) {
            AnswerKey key = keys.get(a.questionId());
            if (key == null || !key.active() || !orgId.equals(key.orgId()) || !req.subjectId().equals(key.subjectId())) {
                throw ApiException.badRequest("Question " + a.questionId() + " is not part of subject " + req.subjectId());
            }
            boolean optionBelongs = key.options().stream().anyMatch(o -> o.optionId().equals(a.selectedOptionId()));
            if (!optionBelongs) {
                throw ApiException.badRequest("Option " + a.selectedOptionId() + " does not belong to question "
                        + a.questionId());
            }
            rows.add(new ExamAnswer(Ids.newId(), sessionId, orgId, key.classId(), key.subjectId(), a.questionId(),
                    a.selectedOptionId(), takenBy, now, RecordStatus.ACTIVE));
        }
        answers.saveAll(rows);
        ExamResult result = score(sessionId, rows, keys);
        meterRegistry.counter("exam.submissions").increment();
        meterRegistry.summary("exam.score.percent").record(result.scorePercent());
        return result;
    }

    @Transactional(readOnly = true)
    public ExamResult result(String sessionId) {
        List<ExamAnswer> rows = answers.findBySessionIdAndStatusOrderByTakenAtAsc(sessionId, RecordStatus.ACTIVE);
        if (rows.isEmpty()) {
            throw ApiException.notFound("Exam session", sessionId);
        }
        ExamAnswer first = rows.get(0);
        boolean owner = Objects.equals(first.getTakenBy(), CurrentUser.email());
        boolean orgAdmin = CurrentUser.hasRole(Role.ADMIN) && Objects.equals(first.getOrgId(), CurrentUser.orgId());
        if (!owner && !orgAdmin && !CurrentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Not allowed to view exam session " + sessionId);
        }
        Set<String> questionIds = rows.stream().map(ExamAnswer::getQuestionId).collect(Collectors.toSet());
        return score(sessionId, rows, fetchKeys(questionIds));
    }

    /** Exam takers see their own attempts, organisation admins their organisation's, super admins any. */
    @Transactional(readOnly = true)
    public Page<SessionSummary> sessions(String orgId, String takenBy, String subjectId, Pageable pageable) {
        String org = CurrentUser.resolveOrg(orgId);
        String who = takenBy;
        if (!CurrentUser.isSuperAdmin() && !CurrentUser.hasRole(Role.ADMIN)) {
            who = CurrentUser.email();
        }
        return answers.findSessions(org, blankToNull(who), blankToNull(subjectId), pageable);
    }

    private Map<String, AnswerKey> fetchKeys(Set<String> questionIds) {
        return questionClient.answerKeys(questionIds).stream()
                .collect(Collectors.toMap(AnswerKey::questionId, Function.identity()));
    }

    private static ExamResult score(String sessionId, List<ExamAnswer> rows, Map<String, AnswerKey> keys) {
        List<AnswerResult> details = new ArrayList<>();
        int correct = 0;
        for (ExamAnswer row : rows) {
            AnswerKey key = keys.get(row.getQuestionId());
            List<String> correctIds = key == null ? List.of()
                    : key.options().stream().filter(KeyOption::correct).map(KeyOption::optionId).toList();
            boolean isCorrect = correctIds.contains(row.getSelectedOptionId());
            if (isCorrect) {
                correct++;
            }
            details.add(new AnswerResult(row.getQuestionId(), row.getSelectedOptionId(), isCorrect, correctIds));
        }
        double percent = rows.isEmpty() ? 0 : BigDecimal.valueOf(correct * 100.0 / rows.size())
                .setScale(2, RoundingMode.HALF_UP).doubleValue();
        ExamAnswer first = rows.get(0);
        return new ExamResult(sessionId, first.getOrgId(), first.getSubjectId(), first.getTakenBy(),
                first.getTakenAt(), rows.size(), correct, percent, details);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
