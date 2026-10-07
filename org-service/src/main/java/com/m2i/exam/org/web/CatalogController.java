package com.m2i.exam.org.web;

import com.m2i.exam.org.service.CatalogService;
import com.m2i.exam.org.web.OrgDtos.ChapterRequest;
import com.m2i.exam.org.web.OrgDtos.ChapterResponse;
import com.m2i.exam.org.web.OrgDtos.ClassRequest;
import com.m2i.exam.org.web.OrgDtos.ClassResponse;
import com.m2i.exam.org.web.OrgDtos.RenameRequest;
import com.m2i.exam.org.web.OrgDtos.SubjectRequest;
import com.m2i.exam.org.web.OrgDtos.SubjectResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Classes, subjects and chapters. Reads are open to every member of the organisation, writes to admins. */
@Tag(name = "Catalog", description = "class_info, subject_info, chapter_info")
@RestController
public class CatalogController {

    private static final String ADMINS = "hasAnyRole('SUPER_ADMIN','ADMIN')";

    private final CatalogService service;

    public CatalogController(CatalogService service) {
        this.service = service;
    }

    @PreAuthorize(ADMINS)
    @PostMapping("/api/classes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassResponse createClass(@Valid @RequestBody ClassRequest request) {
        return service.createClass(request);
    }

    @GetMapping("/api/classes")
    public List<ClassResponse> listClasses(@RequestParam(required = false) String orgId) {
        return service.listClasses(orgId);
    }

    @GetMapping("/api/classes/{id}")
    public ClassResponse getClass(@PathVariable String id) {
        return service.getClass(id);
    }

    @PreAuthorize(ADMINS)
    @PutMapping("/api/classes/{id}")
    public ClassResponse renameClass(@PathVariable String id, @Valid @RequestBody RenameRequest request) {
        return service.renameClass(id, request.name());
    }

    @PreAuthorize(ADMINS)
    @DeleteMapping("/api/classes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClass(@PathVariable String id) {
        service.deleteClass(id);
    }

    @PreAuthorize(ADMINS)
    @PostMapping("/api/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectResponse createSubject(@Valid @RequestBody SubjectRequest request) {
        return service.createSubject(request);
    }

    @GetMapping("/api/subjects")
    public List<SubjectResponse> listSubjects(@RequestParam(required = false) String orgId,
                                              @RequestParam(required = false) String classId) {
        return service.listSubjects(orgId, classId);
    }

    @GetMapping("/api/subjects/{id}")
    public SubjectResponse getSubject(@PathVariable String id) {
        return service.getSubject(id);
    }

    @PreAuthorize(ADMINS)
    @PutMapping("/api/subjects/{id}")
    public SubjectResponse renameSubject(@PathVariable String id, @Valid @RequestBody RenameRequest request) {
        return service.renameSubject(id, request.name());
    }

    @PreAuthorize(ADMINS)
    @DeleteMapping("/api/subjects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubject(@PathVariable String id) {
        service.deleteSubject(id);
    }

    @PreAuthorize(ADMINS)
    @PostMapping("/api/chapters")
    @ResponseStatus(HttpStatus.CREATED)
    public ChapterResponse createChapter(@Valid @RequestBody ChapterRequest request) {
        return service.createChapter(request);
    }

    @GetMapping("/api/chapters")
    public List<ChapterResponse> listChapters(@RequestParam String subjectId) {
        return service.listChapters(subjectId);
    }

    @PreAuthorize(ADMINS)
    @PutMapping("/api/chapters/{id}")
    public ChapterResponse renameChapter(@PathVariable String id, @Valid @RequestBody RenameRequest request) {
        return service.renameChapter(id, request.name());
    }

    @PreAuthorize(ADMINS)
    @DeleteMapping("/api/chapters/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChapter(@PathVariable String id) {
        service.deleteChapter(id);
    }
}
