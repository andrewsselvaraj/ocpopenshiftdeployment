-- Tables owned by question-service, matching the legacy m2its dump.
-- IF NOT EXISTS makes this a no-op when the legacy dump has already been imported.
-- On a fresh database the foreign keys to org-service tables (org/class/subject) are intentionally
-- not created: each service owns its tables and references across services are by id only.

CREATE TABLE IF NOT EXISTS `question_info_v_two` (
  `pk_question_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `fk_org_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `fk_class_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `fk_subject_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `fk_chapter_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'None',
  `question_name` varchar(10000) COLLATE utf8mb4_general_ci NOT NULL,
  `question_type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `question_updated_by` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `question_updated` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `question_status` varchar(50) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'A',
  PRIMARY KEY (`pk_question_id`),
  KEY `ques_sub` (`fk_subject_id`),
  KEY `ques_class` (`fk_class_id`),
  KEY `ques_org` (`fk_org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `answer_info_v_two` (
  `pk_answer_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `fk_org_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `fk_class_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `fk_subject_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `fk_chapter_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `fk_question_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `answers` mediumtext COLLATE utf8mb4_general_ci NOT NULL,
  `correct_answer` varchar(500) COLLATE utf8mb4_general_ci NOT NULL,
  `answer_updated_by` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `answer_updated_date_time` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `answer_status` varchar(50) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'A',
  PRIMARY KEY (`pk_answer_id`),
  KEY `ans_sub` (`fk_subject_id`),
  KEY `ans_class` (`fk_class_id`),
  KEY `ans_org` (`fk_org_id`),
  KEY `ans_ques` (`fk_question_id`),
  CONSTRAINT `ans_ques` FOREIGN KEY (`fk_question_id`) REFERENCES `question_info_v_two` (`pk_question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
