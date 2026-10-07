-- Results are read per session and listed per exam taker.
CREATE INDEX `idx_exam_session` ON `exam_info` (`exam_session_id`);
CREATE INDEX `idx_exam_taken_by` ON `exam_info` (`exam_taken_by`, `exam_taken_date_time`);
