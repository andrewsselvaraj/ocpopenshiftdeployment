-- Tables owned by auth-service, identical to the legacy m2its dump.
-- IF NOT EXISTS makes this a no-op when the legacy dump has already been imported.

CREATE TABLE IF NOT EXISTS `admin_info` (
  `pk_admin_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `admin_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `admin_email` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `admin_password` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `admin_role` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `admin_updated_date_time` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `admin_status` varchar(50) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'A',
  PRIMARY KEY (`pk_admin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `user_info` (
  `pk_user_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `user_org_id` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `user_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `user_email` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `user_password` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `user_role` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `user_updated_by` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `user_updated_date_time` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `user_status` varchar(50) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'A',
  PRIMARY KEY (`pk_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
