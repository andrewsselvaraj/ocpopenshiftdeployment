package com.m2i.exam.org.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, String> {

    List<SchoolClass> findByOrgIdAndStatusOrderByName(String orgId, String status);
}
