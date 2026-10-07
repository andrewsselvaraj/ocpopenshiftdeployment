-- org_password now holds an unusable BCrypt hash (60 chars) for new organisations.
ALTER TABLE `org_info` MODIFY `org_password` varchar(100) COLLATE utf8mb4_general_ci NOT NULL;

-- contact_m2i has no primary key in the legacy dump; JPA needs one.
ALTER TABLE `contact_m2i` ADD PRIMARY KEY (`m_contact_id`);

-- Chapters are listed per subject.
CREATE INDEX `idx_chapter_subject` ON `chapter_info` (`fk_subject_id`, `chapter_status`);
