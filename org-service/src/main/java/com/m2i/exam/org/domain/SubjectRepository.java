package com.m2i.exam.org.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, String> {

    List<Subject> findByOrgIdAndStatusOrderByName(String orgId, String status);

    List<Subject> findByClassIdAndStatusOrderByName(String classId, String status);
}
