package com.m2i.exam.question.service;

import static com.m2i.exam.question.domain.QuestionSpecs.active;
import static com.m2i.exam.question.domain.QuestionSpecs.attributeEquals;

import com.m2i.exam.common.domain.Ids;
import com.m2i.exam.common.domain.RecordStatus;
import com.m2i.exam.common.security.CurrentUser;
import com.m2i.exam.common.web.ApiException;
import com.m2i.exam.question.domain.AnswerOption;
import com.m2i.exam.question.domain.AnswerOptionRepository;
import com.m2i.exam.question.domain.Question;
import com.m2i.exam.question.domain.QuestionRepository;
import com.m2i.exam.question.web.QuestionDtos.AnswerKey;
import com.m2i.exam.question.web.QuestionDtos.OptionRequest;
import com.m2i.exam.question.web.QuestionDtos.PaperQuestion;
import com.m2i.exam.question.web.QuestionDtos.QuestionPaper;
import com.m2i.exam.question.web.QuestionDtos.QuestionRequest;
import com.m2i.exam.question.web.QuestionDtos.QuestionResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionService {

    private final QuestionRepository questions;
    private final AnswerOptionRepository options;

    public QuestionService(QuestionRepository questions, AnswerOptionRepository options) {
        this.questions = questions;
        this.options = options;
    }

    @Transactional
    public QuestionResponse create(QuestionRequest req) {
        validateOptions(req.options());
        String orgId = requireOrg(req.orgId());
        Question q = new Question(Ids.newId(), orgId, req.classId(), req.subjectId(), req.chapterId(),
                req.text().trim(), typeOf(req.type()), CurrentUser.email());
        req.options().forEach(o -> q.addOption(Ids.newId(), o.text().trim(), o.correct()));
        return QuestionResponse.from(questions.save(q));
    }

    @Transactional(readOnly = true)
    public Page<QuestionResponse> search(String orgId, String classId, String subjectId, String chapterId,
                                         Pageable pageable) {
        Specification<Question> spec = Specification.allOf(active(),
                attributeEquals("orgId", requireOrg(orgId)),
                attributeEquals("classId", classId),
                attributeEquals("subjectId", subjectId),
                attributeEquals("chapterId", chapterId));
        return questions.findAll(spec, pageable).map(QuestionResponse::from);
    }

    @Transactional(readOnly = true)
    public QuestionResponse get(String id) {
        return QuestionResponse.from(loadActive(id));
    }

    /**
     * Updates text/type and reconciles options: options with an id are edited, options without
     * one are added, and existing options missing from the request are soft-deleted.
     */
    @Transactional
    public QuestionResponse update(String id, QuestionRequest req) {
        validateOptions(req.options());
        Question q = loadActive(id);
        String by = CurrentUser.email();
        q.edit(req.text().trim(), typeOf(req.type()), by);

        Set<String> kept = new HashSet<>();
        List<AnswerOption> added = new ArrayList<>();
        for (OptionRequest o : req.options()) {
            AnswerOption option;
            if (o.id() == null || o.id().isBlank()) {
                option = q.addOption(Ids.newId(), o.text().trim(), o.correct());
                added.add(option);
            } else {
                option = q.findOption(o.id())
                        .orElseThrow(() -> ApiException.badRequest(
                                "Option " + o.id() + " does not belong to question " + id));
                option.edit(o.text().trim(), o.correct(), by);
            }
            kept.add(option.getId());
        }
        q.getOptions().stream()
                .filter(o -> !kept.contains(o.getId()))
                .forEach(o -> o.deactivate(by));
        options.saveAll(added);
        questions.flush();
        return QuestionResponse.from(q);
    }

    @Transactional
    public void delete(String id) {
        loadActive(id).deactivate(CurrentUser.email());
    }

    /** Random selection of active questions for an exam, without correct-answer flags. */
    @Transactional(readOnly = true)
    public QuestionPaper paper(String orgId, String subjectId, String chapterId, int limit, boolean shuffle) {
        String org = requireOrg(orgId);
        String chapter = chapterId == null || chapterId.isBlank() ? null : chapterId;
        List<String> ids = new ArrayList<>(questions.findActiveIds(org, subjectId, chapter));
        if (shuffle) {
            Collections.shuffle(ids);
        } else {
            Collections.sort(ids);
        }
        List<String> selected = ids.subList(0, Math.min(limit, ids.size()));
        Map<String, Question> byId = questions.findByIdIn(selected).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        List<PaperQuestion> paper = selected.stream()
                .map(byId::get)
                .map(PaperQuestion::from)
                .filter(p -> !p.options().isEmpty())
                .toList();
        return new QuestionPaper(org, subjectId, chapter, ids.size(), paper);
    }

    /** Internal (SERVICE role): correct answers for scoring. Organisation checks are done by the caller. */
    @Transactional(readOnly = true)
    public List<AnswerKey> answerKeys(List<String> questionIds) {
        return questions.findByIdIn(new HashSet<>(questionIds)).stream().map(AnswerKey::from).toList();
    }

    private Question loadActive(String id) {
        Question q = questions.findById(id)
                .filter(x -> RecordStatus.isActive(x.getStatus()))
                .orElseThrow(() -> ApiException.notFound("Question", id));
        CurrentUser.assertOrgAccess(q.getOrgId());
        return q;
    }

    private static String requireOrg(String orgId) {
        String org = CurrentUser.resolveOrg(orgId);
        if (org == null || org.isBlank()) {
            throw ApiException.badRequest("orgId is required");
        }
        return org;
    }

    private static String typeOf(String type) {
        return type == null || type.isBlank() ? "text" : type;
    }

    private static void validateOptions(List<OptionRequest> options) {
        if (options.stream().noneMatch(OptionRequest::correct)) {
            throw ApiException.badRequest("At least one option must be marked correct");
        }
    }
}
