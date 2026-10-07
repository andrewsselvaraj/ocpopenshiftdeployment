package com.m2i.exam.auth.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserAccount, String> {

    Optional<UserAccount> findFirstByEmailAndStatusOrderByUpdatedAtDesc(String email, String status);

    boolean existsByEmail(String email);

    Page<UserAccount> findByOrgIdAndStatus(String orgId, String status, Pageable pageable);

    Page<UserAccount> findByStatus(String status, Pageable pageable);
}
