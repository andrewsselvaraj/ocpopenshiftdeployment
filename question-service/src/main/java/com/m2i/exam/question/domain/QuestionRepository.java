package com.m2i.exam.question.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, String>, JpaSpecificationExecutor<Question> {

    /** Candidate ids for a question paper; sampling happens in memory on ids only. */
    @Query("""
            select q.id from Question q
            where q.orgId = :orgId and q.subjectId = :subjectId and q.status = 'A'
              and (:chapterId is null or q.chapterId = :chapterId)
            """)
    List<String> findActiveIds(@Param("orgId") String orgId, @Param("subjectId") String subjectId,
                               @Param("chapterId") String chapterId);

    List<Question> findByIdIn(Collection<String> ids);
}
