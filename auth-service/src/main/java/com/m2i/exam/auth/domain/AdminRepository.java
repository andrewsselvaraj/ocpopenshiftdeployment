package com.m2i.exam.auth.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<AdminAccount, String> {

    Optional<AdminAccount> findFirstByEmailAndStatusOrderByUpdatedAtDesc(String email, String status);

    boolean existsByEmail(String email);

    List<AdminAccount> findByStatusOrderByName(String status);
}
