package com.m2i.exam.org.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganisationRepository extends JpaRepository<Organisation, String> {

    List<Organisation> findByStatusOrderByName(String status);

    boolean existsByEmail(String email);
}
