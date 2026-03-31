-- 兼容初始化：若库不存在则创建
CREATE DATABASE IF NOT EXISTS campus_task
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE campus_task;

-- 推荐模块画像字段扩展（distance 相关字段已移除：本平台为任务接单平台，不依赖地理距离）
-- 兼容 MySQL 5.7/8.0：去除 AFTER/IF NOT EXISTS，避免列缺失/重复导致报错
-- 脚本默认只执行一次，如需可多次执行请先手动检查列是否存在
ALTER TABLE `user`
  ADD COLUMN `ability_tags` VARCHAR(500) DEFAULT NULL COMMENT '推荐能力标签（JSON数组）',
  ADD COLUMN `preferred_categories` VARCHAR(255) DEFAULT NULL COMMENT '偏好任务分类ID（JSON数组）',
  ADD COLUMN `preferred_delivery_type` TINYINT DEFAULT 0 COMMENT '偏好交付方式 0线上 1线下',
  ADD COLUMN `min_accept_amount` DECIMAL(10,2) DEFAULT 1.00 COMMENT '最低可接受金额',
  ADD COLUMN `max_accept_amount` DECIMAL(10,2) DEFAULT 500.00 COMMENT '最高可接受金额',
  ADD COLUMN `daily_recommend_limit` INT DEFAULT 10 COMMENT '每日推荐数量上限',
  ADD COLUMN `recommend_weights` VARCHAR(500) DEFAULT NULL COMMENT '推荐权重(JSON对象)';

UPDATE `user`
SET `preferred_delivery_type` = COALESCE(`preferred_delivery_type`, 0),
    `min_accept_amount` = COALESCE(`min_accept_amount`, 1.00),
    `max_accept_amount` = COALESCE(`max_accept_amount`, 500.00),
    `daily_recommend_limit` = COALESCE(`daily_recommend_limit`, 10),
    `recommend_weights` = COALESCE(`recommend_weights`, '{"ability":0.35,"category":0.30,"amount":0.25,"delivery":0.10}');

CREATE TABLE IF NOT EXISTS `recommend_weight_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `task_id` BIGINT DEFAULT NULL,
  `trigger_type` TINYINT NOT NULL COMMENT '1完成订单自动更新 2用户手动保存画像 3点击任务详情轻量反馈',
  `before_weights` JSON DEFAULT NULL COMMENT '调整前权重',
  `after_weights` JSON DEFAULT NULL COMMENT '调整后权重',
  `trigger_reason` VARCHAR(255) DEFAULT NULL COMMENT '调整原因',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_created` (`user_id`, `created_at`),
  KEY `idx_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐权重调整日志表';
