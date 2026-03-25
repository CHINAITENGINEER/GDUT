-- ============================================
-- 实时组队模块补丁 SQL
-- 适用场景：已有 campus_task 库，但缺少 rally_activity / rally_member / rally_message 三张表
-- 执行方式：直接在已选中 campus_task 库后执行，或先执行 USE campus_task;
-- ============================================

USE campus_task;

-- ----------------------------
-- 召集活动表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `rally_activity` (
  `id`            BIGINT        NOT NULL COMMENT '活动ID',
  `organizer_id`  BIGINT        NOT NULL COMMENT '发起人ID',
  `type`          TINYINT       NOT NULL COMMENT '1运动 2游戏',
  `title`         VARCHAR(100)  NOT NULL COMMENT '活动标题',
  `recruit_count` INT           NOT NULL COMMENT '召集人数（含发起人）',
  `current_count` INT           NOT NULL DEFAULT 1 COMMENT '当前已加入人数',
  `start_time`    DATETIME      NOT NULL COMMENT '开始时间',
  `remark`        VARCHAR(300)  DEFAULT NULL COMMENT '备注',
  `status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '0进行中 1已结束',
  `ended_at`      DATETIME      DEFAULT NULL COMMENT '结束时间',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_status_created` (`status`, `created_at`),
  INDEX `idx_organizer` (`organizer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='召集活动表';

-- ----------------------------
-- 召集成员表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `rally_member` (
  `id`         BIGINT       NOT NULL COMMENT '成员记录ID',
  `rally_id`   BIGINT       NOT NULL COMMENT '活动ID',
  `user_id`    BIGINT       NOT NULL COMMENT '用户ID',
  `role`       TINYINT      NOT NULL DEFAULT 1 COMMENT '0发起人 1参与者',
  `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '1在队 0已退出',
  `joined_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `quit_at`    DATETIME     DEFAULT NULL COMMENT '退出时间',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rally_user` (`rally_id`, `user_id`),
  INDEX `idx_rally_status` (`rally_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='召集成员表';

-- ----------------------------
-- 召集聊天消息表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `rally_message` (
  `id`         BIGINT        NOT NULL COMMENT '消息ID',
  `rally_id`   BIGINT        NOT NULL COMMENT '活动ID',
  `sender_id`  BIGINT        NOT NULL COMMENT '发送者ID',
  `content`    VARCHAR(1000) NOT NULL COMMENT '消息内容',
  `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_rally_created` (`rally_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='召集聊天消息表';

-- 可选验证
-- SHOW TABLES LIKE 'rally_%';
