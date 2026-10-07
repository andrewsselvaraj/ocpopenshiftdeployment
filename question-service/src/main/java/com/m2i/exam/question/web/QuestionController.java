package com.m2i.exam.question.web;

import com.m2i.exam.question.service.QuestionService;
import com.m2i.exam.question.web.QuestionDtos.AnswerKey;
import com.m2i.exam.question.web.QuestionDtos.AnswerKeyRequest;
import com.m2i.exam.question.web.QuestionDtos.QuestionPaper;
import com.m2i.exam.question.web.QuestionDtos.QuestionRequest;
import com.m2i.exam.question.web.QuestionDtos.QuestionResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Questions", description = "Question bank (question_info_v_two + answer_info_v_two)")
@Validated
@RestController
public class QuestionController {

    private static final String AUTHORS = "hasAnyRole('SUPER_ADMIN','ADMIN','DATA_ENTRY_OPERATOR')";

    private final QuestionService service;

    public QuestionController(QuestionService service) {
        this.service = service;
    }

    @PreAuthorize(AUTHORS)
    @PostMapping("/api/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse create(@Valid @RequestBody QuestionRequest request) {
        return service.create(request);
    }

    @Operation(summary = "Search the bank (with correct answers) - authors only")
    @PreAuthorize(AUTHORS)
    @GetMapping("/api/questions")
    public Page<QuestionResponse> search(@RequestParam(required = false) String orgId,
                                         @RequestParam(required = false) String classId,
                                         @RequestParam(required = false) String subjectId,
                                         @RequestParam(required = false) String chapterId,
                                         @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC)
                                         Pageable pageable) {
        return service.search(orgId, classId, subjectId, chapterId, pageable);
    }

    @PreAuthorize(AUTHORS)
    @GetMapping("/api/questions/{id}")
    public QuestionResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @PreAuthorize(AUTHORS)
    @PutMapping("/api/questions/{id}")
    public QuestionResponse update(@PathVariable String id, @Valid @RequestBody QuestionRequest request) {
        return service.update(id, request);
    }

    @PreAuthorize(AUTHORS)
    @DeleteMapping("/api/questions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    @Operation(summary = "Question paper for an exam taker: random questions, correct answers hidden")
    @GetMapping("/api/question-papers")
    public QuestionPaper paper(@RequestParam(required = false) String orgId,
                               @RequestParam String subjectId,
                               @RequestParam(required = false) String chapterId,
                               @RequestParam(defaultValue = "20") @Min(1) @Max(200) int limit,
                               @RequestParam(defaultValue = "true") boolean shuffle) {
        return service.paper(orgId, subjectId, chapterId, limit, shuffle);
    }

    @Hidden
    @PostMapping("/internal/answer-keys")
    public List<AnswerKey> answerKeys(@Valid @RequestBody AnswerKeyRequest request) {
        return service.answerKeys(request.questionIds());
    }
}
