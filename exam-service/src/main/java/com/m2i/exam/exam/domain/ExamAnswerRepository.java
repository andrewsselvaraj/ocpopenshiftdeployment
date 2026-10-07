package com.m2i.exam.exam.domain;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamAnswerRepository extends JpaRepository<ExamAnswer, String> {

    List<ExamAnswer> findBySessionIdAndStatusOrderByTakenAtAsc(String sessionId, String status);

    @Query(value = """
            select new com.m2i.exam.exam.domain.SessionSummary(
                       e.sessionId, e.orgId, e.subjectId, e.takenBy, min(e.takenAt), count(e))
            from ExamAnswer e
            where e.status = 'A'
              and (:orgId is null or e.orgId = :orgId)
              and (:takenBy is null or e.takenBy = :takenBy)
              and (:subjectId is null or e.subjectId = :subjectId)
            group by e.sessionId, e.orgId, e.subjectId, e.takenBy
            order by min(e.takenAt) desc
            """,
            countQuery = """
            select count(distinct e.sessionId) from ExamAnswer e
            where e.status = 'A'
              and (:orgId is null or e.orgId = :orgId)
              and (:takenBy is null or e.takenBy = :takenBy)
              and (:subjectId is null or e.subjectId = :subjectId)
            """)
    Page<SessionSummary> findSessions(@Param("orgId") String orgId, @Param("takenBy") String takenBy,
                                      @Param("subjectId") String subjectId, Pageable pageable);
}
