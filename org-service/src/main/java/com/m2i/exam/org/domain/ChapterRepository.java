package com.m2i.exam.org.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterRepository extends JpaRepository<Chapter, String> {

    List<Chapter> findBySubjectIdAndStatusOrderByName(String subjectId, String status);
}
