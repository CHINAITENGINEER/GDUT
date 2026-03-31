-- ============================================
-- 推荐 / 协同过滤测试假数据
-- 50个用户 + 500个已完成任务 + 500条抢单完成记录
-- 使用方式：
-- 1. 先执行 init.sql
-- 2. 再执行 recommendation_agent.sql
-- 3. 最后执行本脚本
-- 默认测试密码：123456
-- BCrypt: $2a$10$YX8r5IB0v9WPe6oIAl7GwOwOQX.jRiPb2vRIQAds0XZj8ow/z6ANO
-- ============================================

USE campus_task;

DROP PROCEDURE IF EXISTS generate_recommendation_mock_data;
DELIMITER $$
CREATE PROCEDURE generate_recommendation_mock_data()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE grp INT;
    DECLARE uid BIGINT;
    DECLARE tid BIGINT;
    DECLARE gid BIGINT;
    DECLARE publisher_idx INT;
    DECLARE acceptor_idx INT;
    DECLARE publisher_id BIGINT;
    DECLARE acceptor_id BIGINT;
    DECLARE category_id INT;
    DECLARE amount_val DECIMAL(10,2);
    DECLARE fee_val DECIMAL(10,2);
    DECLARE delivery_val TINYINT;
    DECLARE need_audit_val TINYINT;
    DECLARE created_days INT;
    DECLARE deadline_days INT;
    DECLARE updated_days INT;
    DECLARE student_no VARCHAR(20);
    DECLARE phone_no VARCHAR(20);
    DECLARE nickname_val VARCHAR(50);
    DECLARE avatar_val VARCHAR(255);
    DECLARE bio_val VARCHAR(255);
    DECLARE skills_val VARCHAR(500);
    DECLARE ability_val VARCHAR(500);
    DECLARE preferred_val VARCHAR(255);
    DECLARE weights_val VARCHAR(500);
    DECLARE title_val VARCHAR(100);
    DECLARE desc_val TEXT;
    DECLARE proof_val JSON;

    SET @base_user_id = 307213865930100000;
    SET @base_task_id = 307213865931000000;
    SET @base_grab_id = 307213865932000000;
    SET @pwd = '$2a$10$YX8r5IB0v9WPe6oIAl7GwOwOQX.jRiPb2vRIQAds0XZj8ow/z6ANO';

    -- =========================
    -- 1) 生成50个用户
    -- =========================
    SET i = 1;
    WHILE i <= 50 DO
        SET uid = @base_user_id + i;
        SET grp = MOD(i - 1, 10);
        SET student_no = CONCAT('20223', LPAD(i, 4, '0'));
        SET phone_no = CONCAT('189', LPAD(i, 8, '0'));

        CASE grp
            WHEN 0 THEN
                SET nickname_val = CONCAT('星野', LPAD(i, 2, '0'));
                SET bio_val = '推荐测试用户，偏好跑腿代办与线下执行任务。';
                SET skills_val = '["跑腿代办","校园导航","快递代取"]';
                SET ability_val = '["跑腿","快递","代取"]';
                SET preferred_val = '[1,5]';
                SET weights_val = '{"ability":0.35,"category":0.30,"amount":0.25,"delivery":0.10}';
                SET delivery_val = 1;
            WHEN 1 THEN
                SET nickname_val = CONCAT('木槿', LPAD(i, 2, '0'));
                SET bio_val = '推荐测试用户，擅长资料整理和文档排版。';
                SET skills_val = '["资料整理","Word排版","Excel"]';
                SET ability_val = '["文档","整理","排版"]';
                SET preferred_val = '[2,5]';
                SET weights_val = '{"ability":0.33,"category":0.32,"amount":0.25,"delivery":0.10}';
                SET delivery_val = 0;
            WHEN 2 THEN
                SET nickname_val = CONCAT('南桥', LPAD(i, 2, '0'));
                SET bio_val = '推荐测试用户，擅长 Python 与前端开发。';
                SET skills_val = '["Python","前端开发","脚本编写"]';
                SET ability_val = '["编程","Python","开发"]';
                SET preferred_val = '[3,2]';
                SET weights_val = '{"ability":0.42,"category":0.24,"amount":0.24,"delivery":0.10}';
                SET delivery_val = 0;
            WHEN 3 THEN
                SET nickname_val = CONCAT('简宁', LPAD(i, 2, '0'));
                SET bio_val = '推荐测试用户，偏好设计、PPT 与排版任务。';
                SET skills_val = '["PPT美化","海报排版","文案润色"]';
                SET ability_val = '["设计","海报","PPT"]';
                SET preferred_val = '[2,3]';
                SET weights_val = '{"ability":0.30,"category":0.34,"amount":0.26,"delivery":0.10}';
                SET delivery_val = 0;
            WHEN 4 THEN
                SET nickname_val = CONCAT('鹿鸣', LPAD(i, 2, '0'));
                SET bio_val = '推荐测试用户，经常接线下占位和到场协助任务。';
                SET skills_val = '["课堂占位","活动协助","线下执行"]';
                SET ability_val = '["占位","线下","到场"]';
                SET preferred_val = '[4,1]';
                SET weights_val = '{"ability":0.28,"category":0.36,"amount":0.26,"delivery":0.10}';
                SET delivery_val = 1;
            WHEN 5 THEN
                SET nickname_val = CONCAT('未晞', LPAD(i, 2, '0'));
                SET bio_val = '推荐测试用户，擅长 Java 后端与接口开发。';
                SET skills_val = '["Java","SpringBoot","接口开发"]';
                SET ability_val = '["后端","Java","接口"]';
                SET preferred_val = '[3,5]';
                SET weights_val = '{"ability":0.40,"category":0.26,"amount":0.24,"delivery":0.10}';
                SET delivery_val = 0;
            WHEN 6 THEN
                SET nickname_val = CONCAT('知夏', LPAD(i, 2, '0'));
                SET bio_val = '推荐测试用户，擅长数据分析与表格处理。';
                SET skills_val = '["数据分析","Excel","信息检索"]';
                SET ability_val = '["数据","表格","统计"]';
                SET preferred_val = '[2,1]';
                SET weights_val = '{"ability":0.31,"category":0.33,"amount":0.26,"delivery":0.10}';
                SET delivery_val = 0;
            WHEN 7 THEN
                SET nickname_val = CONCAT('听雨', LPAD(i, 2, '0'));
                SET bio_val = '推荐测试用户，擅长图片处理与短视频剪辑。';
                SET skills_val = '["PS修图","短视频剪辑","封面设计"]';
                SET ability_val = '["剪辑","图片","美化"]';
                SET preferred_val = '[5,2]';
                SET weights_val = '{"ability":0.29,"category":0.31,"amount":0.30,"delivery":0.10}';
                SET delivery_val = 1;
            WHEN 8 THEN
                SET nickname_val = CONCAT('云栖', LPAD(i, 2, '0'));
                SET bio_val = '推荐测试用户，擅长翻译、论文和文献整理。';
                SET skills_val = '["英语翻译","论文格式","文献整理"]';
                SET ability_val = '["翻译","论文","英语"]';
                SET preferred_val = '[2,5]';
                SET weights_val = '{"ability":0.34,"category":0.31,"amount":0.25,"delivery":0.10}';
                SET delivery_val = 0;
            ELSE
                SET nickname_val = CONCAT('清禾', LPAD(i, 2, '0'));
                SET bio_val = '推荐测试用户，擅长爬虫采集和自动化脚本。';
                SET skills_val = '["爬虫采集","自动化脚本","数据库"]';
                SET ability_val = '["爬虫","脚本","采集"]';
                SET preferred_val = '[3,2]';
                SET weights_val = '{"ability":0.41,"category":0.25,"amount":0.24,"delivery":0.10}';
                SET delivery_val = 0;
        END CASE;

        SET avatar_val = CONCAT('/uploads/avatar/mock-user-', LPAD(i, 2, '0'), '.png');

        INSERT IGNORE INTO `user` (
            `id`, `student_id`, `phone`, `password`, `salt`, `nickname`, `avatar`, `bio`,
            `skills`, `ability_tags`, `preferred_categories`, `preferred_delivery_type`,
            `min_accept_amount`, `max_accept_amount`, `daily_recommend_limit`, `recommend_weights`,
            `role`, `credit_score`, `balance`, `total_earned`, `exp`, `level`, `status`, `created_at`, `updated_at`
        ) VALUES (
            uid,
            student_no,
            phone_no,
            @pwd,
            CONCAT('bcrypt_seed_cf_', LPAD(i, 2, '0')),
            nickname_val,
            avatar_val,
            bio_val,
            skills_val,
            ability_val,
            preferred_val,
            delivery_val,
            5 + grp * 8,
            60 + grp * 45,
            8 + MOD(i, 5),
            weights_val,
            0,
            96 + MOD(i, 10),
            ROUND(MOD(i, 7) * 13.5, 2),
            ROUND(300 + i * 21.7, 2),
            80 + i * 16,
            LEAST(6, 1 + FLOOR(i / 9)),
            0,
            DATE_SUB(NOW(), INTERVAL (90 - i) DAY),
            DATE_SUB(NOW(), INTERVAL MOD(i, 5) DAY)
        );

        SET i = i + 1;
    END WHILE;

    -- =========================
    -- 2) 生成500个已完成任务 + 500条抢单记录
    -- =========================
    SET i = 1;
    WHILE i <= 500 DO
        SET tid = @base_task_id + i;
        SET gid = @base_grab_id + i;
        SET grp = FLOOR((i - 1) / 10);

        -- 前25个用户主要做发布者，后25个用户主要做接单者
        SET publisher_idx = MOD(grp, 25) + 1;
        SET acceptor_idx = 26 + MOD(grp + MOD(i - 1, 5), 25);

        SET publisher_id = @base_user_id + publisher_idx;
        SET acceptor_id = @base_user_id + acceptor_idx;

        SET category_id = MOD(grp, 5) + 1;

        CASE category_id
            WHEN 1 THEN
                SET title_val = CONCAT('代取快递到宿舍楼下 #', LPAD(i, 3, '0'));
                SET desc_val = CONCAT('菜鸟驿站有包裹待领取，帮忙送到宿舍楼下。测试任务编号 ', LPAD(i, 3, '0'), '，用于协同过滤行为数据验证。');
                SET amount_val = 6 + MOD(i * 7, 20);
                SET delivery_val = 1;
                SET proof_val = JSON_ARRAY(CONCAT('/uploads/mock/proof/task-', LPAD(i, 3, '0'), '-done.jpg'));
            WHEN 2 THEN
                SET title_val = CONCAT('整理课堂笔记为电子版 #', LPAD(i, 3, '0'));
                SET desc_val = CONCAT('根据拍照笔记整理成 Word 文档，要求层级清晰。测试任务编号 ', LPAD(i, 3, '0'), '，用于推荐算法验证。');
                SET amount_val = 20 + MOD(i * 9, 60);
                SET delivery_val = 0;
                SET proof_val = JSON_ARRAY(CONCAT('/uploads/mock/proof/task-', LPAD(i, 3, '0'), '-notes.pdf'));
            WHEN 3 THEN
                SET title_val = CONCAT('编写 Python 数据处理脚本 #', LPAD(i, 3, '0'));
                SET desc_val = CONCAT('读取 Excel 并输出统计结果，代码需附注释。测试任务编号 ', LPAD(i, 3, '0'), '，用于推荐与协同过滤联合测试。');
                SET amount_val = 80 + MOD(i * 11, 180);
                SET delivery_val = 0;
                SET proof_val = JSON_ARRAY(CONCAT('/uploads/mock/proof/task-', LPAD(i, 3, '0'), '-code.zip'));
            WHEN 4 THEN
                SET title_val = CONCAT('帮忙上课占前排座位 #', LPAD(i, 3, '0'));
                SET desc_val = CONCAT('指定课程需要提前到教室占位，到场后拍照确认。测试任务编号 ', LPAD(i, 3, '0'), '，用于历史行为数据构造。');
                SET amount_val = 10 + MOD(i * 5, 25);
                SET delivery_val = 1;
                SET proof_val = JSON_ARRAY(CONCAT('/uploads/mock/proof/task-', LPAD(i, 3, '0'), '-seat.jpg'));
            ELSE
                SET title_val = CONCAT('社团活动素材整理 #', LPAD(i, 3, '0'));
                SET desc_val = CONCAT('整理活动照片和文案素材并分类打包。测试任务编号 ', LPAD(i, 3, '0'), '，用于协同过滤召回测试。');
                SET amount_val = 15 + MOD(i * 8, 75);
                SET delivery_val = 0;
                SET proof_val = JSON_ARRAY(CONCAT('/uploads/mock/proof/task-', LPAD(i, 3, '0'), '-material.zip'));
        END CASE;

        SET fee_val = ROUND(amount_val * 0.03, 2);
        SET need_audit_val = IF(amount_val > 200, 1, 0);
        SET created_days = 140 - MOD(i, 120);
        SET deadline_days = GREATEST(created_days - 3, 1);
        SET updated_days = GREATEST(created_days - 1, 1);

        INSERT IGNORE INTO `task` (
            `id`, `publisher_id`, `acceptor_id`, `title`, `category`, `description`, `amount`, `fee_amount`,
            `delivery_type`, `deadline`, `status`, `need_audit`, `task_images`, `delivery_proof`,
            `reject_reason`, `lock_expire_at`, `created_at`, `updated_at`
        ) VALUES (
            tid,
            publisher_id,
            acceptor_id,
            title_val,
            category_id,
            desc_val,
            amount_val,
            fee_val,
            delivery_val,
            DATE_SUB(NOW(), INTERVAL deadline_days DAY),
            7,
            need_audit_val,
            NULL,
            proof_val,
            NULL,
            NULL,
            DATE_SUB(NOW(), INTERVAL created_days DAY),
            DATE_SUB(NOW(), INTERVAL updated_days DAY)
        );

        INSERT IGNORE INTO `grab_record` (
            `id`, `task_id`, `user_id`, `grab_date`, `status`, `is_violation`, `created_at`
        ) VALUES (
            gid,
            tid,
            acceptor_id,
            DATE_SUB(CURDATE(), INTERVAL updated_days DAY),
            1,
            0,
            DATE_SUB(NOW(), INTERVAL updated_days DAY)
        );

        SET i = i + 1;
    END WHILE;
END $$
DELIMITER ;

CALL generate_recommendation_mock_data();

-- 可选：生成完后删除过程
DROP PROCEDURE IF EXISTS generate_recommendation_mock_data;

-- 数据说明：
-- 1. user：新增 50 条测试用户
-- 2. task：新增 500 条已互评任务（status=7）
-- 3. grab_record：新增 500 条已确认抢单记录（status=1）
-- 4. 前 25 个用户偏发布者，后 25 个用户偏接单者
-- 5. 每 10 个任务形成一组相似行为簇，便于协同过滤算出用户相似度
