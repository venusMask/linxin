-- V1.0.0.3__add_password_version.sql
ALTER TABLE `users` ADD COLUMN `password_version` INT NOT NULL DEFAULT 1 COMMENT '密码版本号，修改密码后自增';
