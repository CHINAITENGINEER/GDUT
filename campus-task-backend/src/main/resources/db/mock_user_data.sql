-- ============================================
-- 管理员 / 学生账户假数据脚本
-- 适用库：campus_task
-- 使用说明：
-- 1. 请先执行 init.sql 创建 user 表
-- 2. 若已存在相同手机号/学号/ID，请先删除旧数据或改用 INSERT IGNORE
-- 3. 本脚本密码采用 BCrypt
--
-- 默认测试密码：123456
-- 管理员账号：18800000001
-- 学生账号1：18800000011
-- 学生账号2：18800000012
-- 学生账号3：18800000013
--
-- 学生账号也支持学号登录：
-- 2022110001 / 2022110002 / 2022110003
-- ============================================

USE campus_task;

INSERT INTO `user` (
  `id`, `student_id`, `phone`, `password`, `salt`, `nickname`, `avatar`, `bio`, `skills`,
  `role`, `credit_score`, `balance`, `total_earned`, `exp`, `level`, `status`, `created_at`, `updated_at`
) VALUES
(307213865924562940, '2021000001', '18800000001', '$2a$10$YX8r5IB0v9WPe6oIAl7GwOwOQX.jRiPb2vRIQAds0XZj8ow/z6ANO', 'bcrypt_seed_0001', '平台管理员', '/uploads/avatar/admin-01.png', '负责平台审核、用户管理与交易统计。', '["审核管理","风控处理","系统维护"]', 1, 100, 0.00, 0.00, 0, 1, 0, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(307213865924562944, '2022110001', '18800000011', '$2a$10$YX8r5IB0v9WPe6oIAl7GwOwOQX.jRiPb2vRIQAds0XZj8ow/z6ANO', 'bcrypt_seed_0002', '张小雨', '/uploads/avatar/student-01.png', '软件工程专业，常发布资料整理和跑腿类任务。', '["资料整理","跑腿代办","PPT整理"]', 0, 98, 235.50, 120.00, 35, 2, 0, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(307213865924562945, '2022110002', '18800000012', '$2a$10$YX8r5IB0v9WPe6oIAl7GwOwOQX.jRiPb2vRIQAds0XZj8ow/z6ANO', 'bcrypt_seed_0003', '李晨阳', '/uploads/avatar/student-02.png', '计算机专业，擅长脚本编写与前端开发。', '["Python","前端开发","代取快递"]', 0, 102, 86.00, 420.00, 180, 3, 0, DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(307213865924562946, '2022110003', '18800000013', '$2a$10$YX8r5IB0v9WPe6oIAl7GwOwOQX.jRiPb2vRIQAds0XZj8ow/z6ANO', 'bcrypt_seed_0004', '王可欣', '/uploads/avatar/student-03.png', '信息管理专业，擅长文档整理和表格处理。', '["Word排版","Excel","会议纪要"]', 0, 105, 132.80, 560.00, 260, 4, 0, DATE_SUB(NOW(), INTERVAL 16 DAY), DATE_SUB(NOW(), INTERVAL 5 HOUR));

-- 可选：执行前先清理旧数据
-- DELETE FROM `user` WHERE `id` IN (307213865924562940, 307213865924562944, 307213865924562945, 307213865924562946);
-- DELETE FROM `user` WHERE `phone` IN ('18800000001', '18800000011', '18800000012', '18800000013');
-- DELETE FROM `user` WHERE `student_id` IN ('2021000001', '2022110001', '2022110002', '2022110003');
