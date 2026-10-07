package com.m2i.exam.org.web;

import com.m2i.exam.org.domain.Chapter;
import com.m2i.exam.org.domain.ContactMessage;
import com.m2i.exam.org.domain.Organisation;
import com.m2i.exam.org.domain.SchoolClass;
import com.m2i.exam.org.domain.Subject;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Request/response payloads of org-service. */
public final class OrgDtos {

    private OrgDtos() {
    }

    public record OrgRequest(@NotBlank @Size(max = 50) String name,
                             @NotBlank @Email @Size(max = 50) String email) {
    }

    public record OrgResponse(String id, String name, String email, String status, String updatedBy,
                              Instant updatedAt) {

        public static OrgResponse from(Organisation o) {
            return new OrgResponse(o.getId(), o.getName(), o.getEmail(), o.getStatus(), o.getUpdatedBy(),
                    o.getUpdatedAt());
        }
    }

    public record ClassRequest(@NotBlank @Size(max = 50) String orgId,
                               @NotBlank @Size(max = 50) String name) {
    }

    public record ClassResponse(String id, String orgId, String name, String status, Instant updatedAt) {

        public static ClassResponse from(SchoolClass c) {
            return new ClassResponse(c.getId(), c.getOrgId(), c.getName(), c.getStatus(), c.getUpdatedAt());
        }
    }

    public record SubjectRequest(@NotBlank @Size(max = 50) String classId,
                                 @NotBlank @Size(max = 50) String name) {
    }

    public record SubjectResponse(String id, String orgId, String classId, String name, String status,
                                  Instant updatedAt) {

        public static SubjectResponse from(Subject s) {
            return new SubjectResponse(s.getId(), s.getOrgId(), s.getClassId(), s.getName(), s.getStatus(),
                    s.getUpdatedAt());
        }
    }

    public record ChapterRequest(@NotBlank @Size(max = 50) String subjectId,
                                 @NotBlank @Size(max = 50) String name) {
    }

    public record ChapterResponse(String id, String orgId, String classId, String subjectId, String name,
                                  String status, Instant updatedAt) {

        public static ChapterResponse from(Chapter c) {
            return new ChapterResponse(c.getId(), c.getOrgId(), c.getClassId(), c.getSubjectId(), c.getName(),
                    c.getStatus(), c.getUpdatedAt());
        }
    }

    /** Rename payload shared by class, subject and chapter updates. */
    public record RenameRequest(@NotBlank @Size(max = 50) String name) {
    }

    public record ContactRequest(@NotBlank @Size(max = 50) String name,
                                 @NotBlank @Email @Size(max = 50) String email,
                                 @NotBlank @Size(max = 50) String subject,
                                 @NotBlank @Size(max = 500) String message) {
    }

    public record ContactResponse(String id, String name, String email, String subject, String message,
                                  Instant createdAt) {

        public static ContactResponse from(ContactMessage m) {
            return new ContactResponse(m.getId(), m.getName(), m.getEmail(), m.getSubject(), m.getMessage(),
                    m.getCreatedAt());
        }
    }
}
