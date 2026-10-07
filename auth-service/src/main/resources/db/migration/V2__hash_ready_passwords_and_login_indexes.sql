-- BCrypt hashes are 60 characters; the legacy columns are varchar(50).
ALTER TABLE `admin_info` MODIFY `admin_password` varchar(100) COLLATE utf8mb4_general_ci NOT NULL;
ALTER TABLE `user_info` MODIFY `user_password` varchar(100) COLLATE utf8mb4_general_ci NOT NULL;

-- Login looks users up by e-mail; the legacy schema has no index for it.
CREATE INDEX `idx_admin_email` ON `admin_info` (`admin_email`);
CREATE INDEX `idx_user_email` ON `user_info` (`user_email`);
CREATE INDEX `idx_user_org` ON `user_info` (`user_org_id`, `user_status`);
