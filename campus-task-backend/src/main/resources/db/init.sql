-- 校园任务接单平台 数据库初始化脚本
-- 执行前请先创建数据库: CREATE DATABASE campus_task CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE campus_task;

-- ----------------------------
-- 用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id`            BIGINT        NOT NULL COMMENT '用户ID（雪花算法）',
  `student_id`    VARCHAR(20)   DEFAULT NULL COMMENT '学号',
  `phone`         VARCHAR(11)   NOT NULL COMMENT '手机号',
  `password`      VARCHAR(100)  NOT NULL COMMENT '密码（优先BCrypt，兼容历史SHA256+盐）',
  `salt`          VARCHAR(16)   NOT NULL COMMENT '历史加密盐值（兼容字段）',
  `nickname`      VARCHAR(50)   NOT NULL COMMENT '昵称',
  `avatar`        VARCHAR(255)  DEFAULT NULL COMMENT '头像URL',
  `bio`           VARCHAR(200)  DEFAULT NULL COMMENT '个人简介',
  `skills`        VARCHAR(500)  DEFAULT NULL COMMENT '技能标签（JSON数组）',
  `role`          TINYINT       NOT NULL DEFAULT 0 COMMENT '0普通用户 1管理员',
  `credit_score`  INT           NOT NULL DEFAULT 100 COMMENT '信誉分',
  `balance`       DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
  `total_earned`  DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '历史总收入',
  `exp`           INT           NOT NULL DEFAULT 0 COMMENT '经验值',
  `level`         TINYINT       NOT NULL DEFAULT 1 COMMENT '接单等级1-6',
  `status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '0正常 1禁用',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 任务表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `task` (
  `id`              BIGINT        NOT NULL COMMENT '任务ID',
  `publisher_id`    BIGINT        NOT NULL COMMENT '发布者ID',
  `acceptor_id`     BIGINT        DEFAULT NULL COMMENT '当前抢单者ID',
  `title`           VARCHAR(100)  NOT NULL COMMENT '任务标题',
  `category`        TINYINT       NOT NULL COMMENT '1代取 2资料 3编程 4代课 5其他',
  `description`     TEXT          NOT NULL COMMENT '任务描述',
  `amount`          DECIMAL(10,2) NOT NULL COMMENT '任务金额',
  `fee_amount`      DECIMAL(10,2) DEFAULT NULL COMMENT '实际手续费',
  `delivery_type`   TINYINT       NOT NULL DEFAULT 0 COMMENT '0线上 1线下',
  `deadline`        DATETIME      NOT NULL COMMENT '截止时间',
  `status`          TINYINT       NOT NULL DEFAULT 0 COMMENT '0待审核 1待接单 2已抢单待协商 3待支付 4进行中 5已完成 6已结算 7已互评 8已取消',
  `need_audit`      TINYINT       NOT NULL DEFAULT 0 COMMENT '是否需要审核',
  `task_images`     JSON          DEFAULT NULL COMMENT '任务图片列表',
  `delivery_proof`  JSON          DEFAULT NULL COMMENT '交付成果列表',
  `reject_reason`   VARCHAR(200)  DEFAULT NULL COMMENT '验收不通过原因',
  `lock_expire_at`  DATETIME      DEFAULT NULL COMMENT '锁单过期时间',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_publisher` (`publisher_id`),
  INDEX `idx_status_category` (`status`, `category`),
  INDEX `idx_lock_expire` (`lock_expire_at`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

-- ----------------------------
-- 抢单行为记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `grab_record` (
  `id`           BIGINT   NOT NULL,
  `task_id`      BIGINT   NOT NULL,
  `user_id`      BIGINT   NOT NULL,
  `grab_date`    DATE     NOT NULL COMMENT '抢单日期',
  `status`       TINYINT  NOT NULL DEFAULT 0 COMMENT '0锁单中 1已确认 2拒绝 3超时 4主动取消',
  `is_violation` TINYINT  NOT NULL DEFAULT 0 COMMENT '是否记违规',
  `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_user_date` (`user_id`, `grab_date`),
  INDEX `idx_task_user_date` (`task_id`, `user_id`, `grab_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抢单行为记录表';

-- ----------------------------
-- 用户违规记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_violation` (
  `id`             BIGINT  NOT NULL,
  `user_id`        BIGINT  NOT NULL,
  `task_id`        BIGINT  NOT NULL,
  `violation_type` TINYINT NOT NULL COMMENT '1超时未确认 2主动取消 3协商超时',
  `violation_date` DATE    NOT NULL,
  `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_user_date` (`user_id`, `violation_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户违规记录表';

-- ----------------------------
-- 支付记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `payment` (
  `id`         BIGINT        NOT NULL,
  `task_id`    BIGINT        NOT NULL,
  `payer_id`   BIGINT        NOT NULL,
  `amount`     DECIMAL(10,2) NOT NULL,
  `pay_type`   TINYINT       NOT NULL COMMENT '0模拟支付宝 1模拟微信',
  `pay_status` TINYINT       NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付 2已退款',
  `trade_no`   VARCHAR(64)   DEFAULT NULL,
  `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_no` (`trade_no`),
  INDEX `idx_task` (`task_id`),
  INDEX `idx_payer` (`payer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';

-- ----------------------------
-- 结算记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `settlement` (
  `id`               BIGINT        NOT NULL,
  `task_id`          BIGINT        NOT NULL,
  `acceptor_id`      BIGINT        NOT NULL,
  `task_amount`      DECIMAL(10,2) NOT NULL,
  `base_fee_rate`    DECIMAL(5,4)  NOT NULL COMMENT '基础手续费率',
  `level_at_settle`  TINYINT       NOT NULL COMMENT '结算时等级快照',
  `fee_discount`     DECIMAL(5,4)  NOT NULL COMMENT '等级折扣率快照',
  `fee_rate`         DECIMAL(5,4)  NOT NULL COMMENT '实际手续费率',
  `fee_amount`       DECIMAL(10,2) NOT NULL,
  `real_amount`      DECIMAL(10,2) NOT NULL,
  `exp_gained`       INT           NOT NULL DEFAULT 0,
  `status`           TINYINT       NOT NULL DEFAULT 0 COMMENT '0已结算 1已提现',
  `settled_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算记录表';

-- ----------------------------
-- 评价表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `review` (
  `id`          BIGINT       NOT NULL,
  `task_id`     BIGINT       NOT NULL,
  `reviewer_id` BIGINT       NOT NULL,
  `reviewee_id` BIGINT       NOT NULL,
  `score`       TINYINT      NOT NULL COMMENT '1-5星',
  `content`     VARCHAR(200) NOT NULL,
  `type`        TINYINT      NOT NULL COMMENT '0发布者评接单者 1接单者评发布者',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_reviewer` (`task_id`, `reviewer_id`),
  INDEX `idx_reviewee` (`reviewee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- ----------------------------
-- 消息表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `message` (
  `id`          BIGINT       NOT NULL,
  `sender_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '0=系统',
  `receiver_id` BIGINT       NOT NULL,
  `task_id`     BIGINT       DEFAULT NULL,
  `type`        TINYINT      NOT NULL DEFAULT 0 COMMENT '0系统 1私信',
  `content`     VARCHAR(500) NOT NULL,
  `is_read`     TINYINT      NOT NULL DEFAULT 0,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_receiver` (`receiver_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- ----------------------------
-- 协商聊天消息表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `chat_message` (
  `id`              BIGINT        NOT NULL COMMENT '消息ID（雪花）',
  `grab_record_id`  BIGINT        NOT NULL COMMENT '接单记录ID（隔离不同接单轮次）',
  `task_id`         BIGINT        NOT NULL COMMENT '任务ID',
  `sender_id`       BIGINT        NOT NULL COMMENT '发送者ID',
  `content`         VARCHAR(1000) NOT NULL COMMENT '消息内容',
  `images`          JSON          DEFAULT NULL COMMENT '图片URL列表',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_grab_record_id` (`grab_record_id`, `created_at`),
  INDEX `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协商聊天消息表';

-- ----------------------------
-- 手续费配置表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `fee_config` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT,
  `min_amount` DECIMAL(10,2) NOT NULL COMMENT '阶梯下限（含）',
  `max_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '阶梯上限（不含，NULL=无上限）',
  `fee_rate`   DECIMAL(5,4)  NOT NULL COMMENT '手续费率',
  `is_active`  TINYINT       NOT NULL DEFAULT 1,
  `updated_by` BIGINT        NOT NULL DEFAULT 0,
  `updated_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手续费配置表';

-- ----------------------------
-- 任务分类标签表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `task_category` (
  `id`         INT          NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name`       VARCHAR(20)  NOT NULL COMMENT '分类名称',
  `icon`       VARCHAR(50)  DEFAULT NULL COMMENT '图标（前端emoji或icon class）',
  `sort`       INT          NOT NULL DEFAULT 0 COMMENT '排序权重（越小越靠前）',
  `is_active`  TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用：0禁用 1启用',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务分类标签表';

-- 初始化默认分类标签
INSERT IGNORE INTO `task_category` (`id`, `name`, `icon`, `sort`, `is_active`) VALUES
  (1, '代取快递', '📦', 1, 1),
  (2, '资料整理', '📝', 2, 1),
  (3, '编程开发', '💻', 3, 1),
  (4, '代课占位', '🎓', 4, 1),
  (5, '其他',     '🔧', 5, 1);

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
  `start_time`    DATETIME      NOT NULL COMMENT '发起时间',
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
  `id`         BIGINT       NOT NULL,
  `rally_id`   BIGINT       NOT NULL,
  `user_id`    BIGINT       NOT NULL,
  `role`       TINYINT      NOT NULL DEFAULT 1 COMMENT '0发起人 1参与者',
  `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '1在队 0已退出',
  `joined_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `quit_at`    DATETIME     DEFAULT NULL,
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
  `id`         BIGINT        NOT NULL,
  `rally_id`   BIGINT        NOT NULL,
  `sender_id`  BIGINT        NOT NULL,
  `content`    VARCHAR(1000) NOT NULL,
  `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_rally_created` (`rally_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='召集聊天消息表';

-- ----------------------------
-- 初始化数据
-- ----------------------------

-- 默认手续费配置：100元以下5%，100元以上3%
INSERT IGNORE INTO `fee_config` (`id`, `min_amount`, `max_amount`, `fee_rate`, `is_active`, `updated_by`)
VALUES
  (1, 0.00, 100.00, 0.0500, 1, 1),
  (2, 100.00, NULL,  0.0300, 1, 1);

-- 默认管理员账号
INSERT IGNORE INTO `user` (`id`, `phone`, `password`, `salt`, `nickname`, `role`, `credit_score`, `balance`, `total_earned`, `exp`, `level`, `status`)
VALUES
  (1000000000000000001, '13800000000', '95a750d56a9c9da823f38e708b797956caefa00986c6d4d77aa02bca5a63776d', 'defaultsalt1234', '管理员', 1, 100, 0.00, 0.00, 0, 1, 0);

-- 备用管理员账号（手机号: 13900000000，密码: 123456）
INSERT IGNORE INTO `user` (`id`, `phone`, `password`, `salt`, `nickname`, `role`, `credit_score`, `balance`, `total_earned`, `exp`, `level`, `status`)
VALUES
  (1000000000000000002, '13900000000', 'fa3a4544c74db2deee39c7013214ac53c2e12f9cb84fd5c7e582f7e51934ac06', 'adminsalt20260321', '系统管理员2', 1, 100, 0.00, 0.00, 0, 1, 0);
