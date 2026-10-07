-- Table owned by exam-service, matching the legacy m2its dump.
-- IF NOT EXISTS makes this a no-op when the legacy dump has already been imported (the dump also
-- carries foreign keys to org/question tables; on a fresh database references are by id only).

CREATE TABLE IF NOT EXISTS `exam_info` (
  `pk_exam_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `exam_session_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `fk_org_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `fk_class_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `fk_subject_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `fk_question_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `fk_user_selected_answer_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `exam_taken_by` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `exam_taken_date_time` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `exam_status` varchar(50) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'A',
  PRIMARY KEY (`pk_exam_id`),
  KEY `ex_org` (`fk_org_id`),
  KEY `ex_class` (`fk_class_id`),
  KEY `ex_subject` (`fk_subject_id`),
  KEY `ex_question` (`fk_question_id`),
  KEY `ex_answer` (`fk_user_selected_answer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
