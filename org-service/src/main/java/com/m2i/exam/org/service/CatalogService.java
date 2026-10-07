package com.m2i.exam.org.service;

import com.m2i.exam.common.domain.Ids;
import com.m2i.exam.common.domain.RecordStatus;
import com.m2i.exam.common.security.CurrentUser;
import com.m2i.exam.common.web.ApiException;
import com.m2i.exam.org.domain.Chapter;
import com.m2i.exam.org.domain.ChapterRepository;
import com.m2i.exam.org.domain.SchoolClass;
import com.m2i.exam.org.domain.SchoolClassRepository;
import com.m2i.exam.org.domain.Subject;
import com.m2i.exam.org.domain.SubjectRepository;
import com.m2i.exam.org.web.OrgDtos.ChapterRequest;
import com.m2i.exam.org.web.OrgDtos.ChapterResponse;
import com.m2i.exam.org.web.OrgDtos.ClassRequest;
import com.m2i.exam.org.web.OrgDtos.ClassResponse;
import com.m2i.exam.org.web.OrgDtos.SubjectRequest;
import com.m2i.exam.org.web.OrgDtos.SubjectResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Academic hierarchy of an organisation: class -> subject -> chapter.
 * Children inherit the organisation (and class) of their parent, so callers cannot attach
 * a subject to another tenant's class.
 */
@Service
public class CatalogService {

    private final OrganisationService organisations;
    private final SchoolClassRepository classes;
    private final SubjectRepository subjects;
    private final ChapterRepository chapters;

    public CatalogService(OrganisationService organisations, SchoolClassRepository classes,
                          SubjectRepository subjects, ChapterRepository chapters) {
        this.organisations = organisations;
        this.classes = classes;
        this.subjects = subjects;
        this.chapters = chapters;
    }

    // ---- classes -------------------------------------------------------------------------

    @Transactional
    public ClassResponse createClass(ClassRequest req) {
        organisations.loadActive(req.orgId());
        return ClassResponse.from(classes.save(
                new SchoolClass(Ids.newId(), req.orgId(), req.name().trim(), RecordStatus.ACTIVE)));
    }

    @Transactional(readOnly = true)
    public List<ClassResponse> listClasses(String orgId) {
        String org = requireOrg(orgId);
        return classes.findByOrgIdAndStatusOrderByName(org, RecordStatus.ACTIVE).stream()
                .map(ClassResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ClassResponse getClass(String id) {
        return ClassResponse.from(loadClass(id));
    }

    @Transactional
    public ClassResponse renameClass(String id, String name) {
        SchoolClass c = loadClass(id);
        c.setName(name.trim());
        return ClassResponse.from(c);
    }

    @Transactional
    public void deleteClass(String id) {
        loadClass(id).setStatus(RecordStatus.DELETED);
    }

    // ---- subjects ------------------------------------------------------------------------

    @Transactional
    public SubjectResponse createSubject(SubjectRequest req) {
        SchoolClass parent = loadClass(req.classId());
        return SubjectResponse.from(subjects.save(new Subject(Ids.newId(), parent.getOrgId(), parent.getId(),
                req.name().trim(), RecordStatus.ACTIVE)));
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> listSubjects(String orgId, String classId) {
        List<Subject> result;
        if (classId != null && !classId.isBlank()) {
            loadClass(classId);
            result = subjects.findByClassIdAndStatusOrderByName(classId, RecordStatus.ACTIVE);
        } else {
            result = subjects.findByOrgIdAndStatusOrderByName(requireOrg(orgId), RecordStatus.ACTIVE);
        }
        return result.stream().map(SubjectResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SubjectResponse getSubject(String id) {
        return SubjectResponse.from(loadSubject(id));
    }

    @Transactional
    public SubjectResponse renameSubject(String id, String name) {
        Subject s = loadSubject(id);
        s.setName(name.trim());
        return SubjectResponse.from(s);
    }

    @Transactional
    public void deleteSubject(String id) {
        loadSubject(id).setStatus(RecordStatus.DELETED);
    }

    // ---- chapters ------------------------------------------------------------------------

    @Transactional
    public ChapterResponse createChapter(ChapterRequest req) {
        Subject parent = loadSubject(req.subjectId());
        return ChapterResponse.from(chapters.save(new Chapter(Ids.newId(), parent.getOrgId(), parent.getClassId(),
                parent.getId(), req.name().trim(), RecordStatus.ACTIVE)));
    }

    @Transactional(readOnly = true)
    public List<ChapterResponse> listChapters(String subjectId) {
        loadSubject(subjectId);
        return chapters.findBySubjectIdAndStatusOrderByName(subjectId, RecordStatus.ACTIVE).stream()
                .map(ChapterResponse::from).toList();
    }

    @Transactional
    public ChapterResponse renameChapter(String id, String name) {
        Chapter c = loadChapter(id);
        c.setName(name.trim());
        return ChapterResponse.from(c);
    }

    @Transactional
    public void deleteChapter(String id) {
        loadChapter(id).setStatus(RecordStatus.DELETED);
    }

    // ---- helpers -------------------------------------------------------------------------

    private String requireOrg(String orgId) {
        String org = CurrentUser.resolveOrg(orgId);
        if (org == null || org.isBlank()) {
            throw ApiException.badRequest("orgId is required");
        }
        return org;
    }

    private SchoolClass loadClass(String id) {
        SchoolClass c = classes.findById(id)
                .filter(x -> RecordStatus.isActive(x.getStatus()))
                .orElseThrow(() -> ApiException.notFound("Class", id));
        CurrentUser.assertOrgAccess(c.getOrgId());
        return c;
    }

    private Subject loadSubject(String id) {
        Subject s = subjects.findById(id)
                .filter(x -> RecordStatus.isActive(x.getStatus()))
                .orElseThrow(() -> ApiException.notFound("Subject", id));
        CurrentUser.assertOrgAccess(s.getOrgId());
        return s;
    }

    private Chapter loadChapter(String id) {
        Chapter c = chapters.findById(id)
                .filter(x -> RecordStatus.isActive(x.getStatus()))
                .orElseThrow(() -> ApiException.notFound("Chapter", id));
        CurrentUser.assertOrgAccess(c.getOrgId());
        return c;
    }
}
