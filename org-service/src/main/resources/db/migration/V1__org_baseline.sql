-- Tables owned by org-service, identical to the legacy m2its dump.
-- IF NOT EXISTS makes this a no-op when the legacy dump has already been imported.

CREATE TABLE IF NOT EXISTS `org_info` (
  `pk_org_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `org_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `org_email` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `org_password` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `org_updated_by` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `org_updated_on` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `org_status` varchar(50) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'A',
  PRIMARY KEY (`pk_org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `class_info` (
  `pk_class_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `fk_org_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `class_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `class_updated` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `class_status` varchar(50) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'A',
  PRIMARY KEY (`pk_class_id`),
  KEY `class_org` (`fk_org_id`),
  CONSTRAINT `class_org` FOREIGN KEY (`fk_org_id`) REFERENCES `org_info` (`pk_org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `subject_info` (
  `pk_subject_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `fk_org_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `fk_class_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `subject_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `subject_updated` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `subject_status` varchar(50) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'A',
  PRIMARY KEY (`pk_subject_id`),
  KEY `sub_class` (`fk_class_id`),
  KEY `sub_org` (`fk_org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `chapter_info` (
  `pk_chapter_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `fk_org_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `fk_class_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `fk_subject_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `chapter_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `chapter_updated` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `chapter_status` varchar(50) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'A',
  PRIMARY KEY (`pk_chapter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `contact_m2i` (
  `m_contact_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `m_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `m_email` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `m_subject` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `m_message` varchar(500) COLLATE utf8mb4_general_ci NOT NULL,
  `m_message_updated_date_time` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `m_message_status` varchar(50) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'A'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
