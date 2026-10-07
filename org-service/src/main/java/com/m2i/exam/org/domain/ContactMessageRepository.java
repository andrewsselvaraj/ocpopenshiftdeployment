package com.m2i.exam.org.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, String> {

    Page<ContactMessage> findByStatus(String status, Pageable pageable);
}
