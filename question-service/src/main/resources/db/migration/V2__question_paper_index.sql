-- Question papers select active questions of one subject (optionally one chapter) of one organisation.
CREATE INDEX `idx_question_paper` ON `question_info_v_two` (`fk_org_id`, `fk_subject_id`, `question_status`, `fk_chapter_id`);
