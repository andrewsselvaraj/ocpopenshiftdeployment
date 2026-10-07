package com.m2i.exam.exam.web;

import com.m2i.exam.exam.domain.SessionSummary;
import com.m2i.exam.exam.service.ExamService;
import com.m2i.exam.exam.web.ExamDtos.ExamResult;
import com.m2i.exam.exam.web.ExamDtos.SubmitExamRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Exams", description = "Exam attempts and results (exam_info)")
@Validated
@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService service;

    public ExamController(ExamService service) {
        this.service = service;
    }

    @Operation(summary = "Submit answers for a subject; returns the scored result")
    @PostMapping("/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public ExamResult submit(@Valid @RequestBody SubmitExamRequest request) {
        return service.submit(request);
    }

    @Operation(summary = "Scored result of one attempt (owner, organisation admin or super admin)")
    @GetMapping("/sessions/{sessionId}")
    public ExamResult result(@PathVariable String sessionId) {
        return service.result(sessionId);
    }

    @Operation(summary = "Attempt history, newest first")
    @GetMapping("/sessions")
    public Page<SessionSummary> sessions(@RequestParam(required = false) String orgId,
                                         @RequestParam(required = false) String takenBy,
                                         @RequestParam(required = false) String subjectId,
                                         @RequestParam(defaultValue = "0") @Min(0) int page,
                                         @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return service.sessions(orgId, takenBy, subjectId, PageRequest.of(page, size));
    }
}
