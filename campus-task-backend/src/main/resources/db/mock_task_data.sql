-- ============================================
-- 任务表假数据生成脚本
-- ============================================
-- 说明：假设已有以下用户ID（请根据实际数据库中的用户ID调整）
-- publisher_id: 307213865924562944 (发布者)
-- acceptor_id: 307213865924562945, 307213865924562946, 307213865924562947 (接单者)

-- 使用雪花ID格式生成任务ID（时间戳部分：2026-03-12 00:00:00开始）
-- 实际使用时，请确保ID不与现有数据冲突

-- ============================================
-- 1. 待审核任务 (status=0)
-- ============================================
INSERT INTO `task` (`id`, `publisher_id`, `acceptor_id`, `title`, `category`, `description`, `amount`, `fee_amount`, `delivery_type`, `deadline`, `status`, `need_audit`, `task_images`, `delivery_proof`, `reject_reason`, `lock_expire_at`, `created_at`, `updated_at`) VALUES
(307213865930000001, 307213865924562944, NULL, '帮忙代取快递到宿舍楼下', 1, '有一个快递在菜鸟驿站，需要帮忙取到宿舍楼下，我在3号楼。快递比较大，需要能搬动的同学。', 15.00, NULL, 1, DATE_ADD(NOW(), INTERVAL 2 DAY), 0, 0, '["/uploads/2026/03/express1.jpg"]', NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(307213865930000002, 307213865924562944, NULL, '整理课程笔记PDF文档', 2, '需要将手写的课程笔记整理成PDF文档，共约30页，要求排版清晰，字体工整。', 50.00, NULL, 0, DATE_ADD(NOW(), INTERVAL 3 DAY), 0, 0, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(307213865930000003, 307213865924562944, NULL, '开发一个简单的待办事项小程序', 3, '需要一个简单的待办事项管理小程序，功能包括添加、删除、标记完成。使用Vue3开发，代码要规范。', 300.00, NULL, 0, DATE_ADD(NOW(), INTERVAL 7 DAY), 0, 1, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 15 MINUTE));

-- ============================================
-- 2. 待接单任务 (status=1) - 审核通过后，等待接单
-- ============================================
INSERT INTO `task` (`id`, `publisher_id`, `acceptor_id`, `title`, `category`, `description`, `amount`, `fee_amount`, `delivery_type`, `deadline`, `status`, `need_audit`, `task_images`, `delivery_proof`, `reject_reason`, `lock_expire_at`, `created_at`, `updated_at`) VALUES
(307213865930000004, 307213865924562944, NULL, '帮忙代课占位，周三下午2点', 4, '周三下午2点的课程需要帮忙占位，坐在前排位置即可，不需要回答问题。', 20.00, NULL, 1, DATE_ADD(NOW(), INTERVAL 1 DAY), 1, 0, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(307213865930000005, 307213865924562944, NULL, '整理实验报告数据', 2, '需要将实验数据整理成表格形式，并生成简单的图表。数据量不大，约20组数据。', 35.00, NULL, 0, DATE_ADD(NOW(), INTERVAL 4 DAY), 1, 0, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(307213865930000006, 307213865924562944, NULL, '代取外卖到图书馆', 1, '外卖送到校门口，需要帮忙取到图书馆3楼，我在那里学习。', 10.00, NULL, 1, DATE_ADD(NOW(), INTERVAL 3 HOUR), 1, 0, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(307213865930000007, 307213865924562944, NULL, '帮忙修改简历格式', 2, '简历内容已经有了，只需要帮忙调整格式，使其更加美观专业。', 25.00, NULL, 0, DATE_ADD(NOW(), INTERVAL 2 DAY), 1, 0, '["/uploads/2026/03/resume1.jpg"]', NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(307213865930000008, 307213865924562944, NULL, '开发一个简单的计算器网页', 3, '需要一个功能完整的计算器网页，支持加减乘除和括号运算。使用原生JavaScript或Vue都可以。', 80.00, NULL, 0, DATE_ADD(NOW(), INTERVAL 5 DAY), 1, 0, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(307213865930000009, 307213865924562944, NULL, '帮忙打印资料并送到宿舍', 5, '需要打印约50页资料，送到5号楼。打印费用我出，只需要帮忙跑腿。', 15.00, NULL, 1, DATE_ADD(NOW(), INTERVAL 1 DAY), 1, 0, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 7 HOUR), DATE_SUB(NOW(), INTERVAL 7 HOUR)),
(307213865930000010, 307213865924562944, NULL, '整理课程PPT为Word文档', 2, '将老师提供的PPT内容整理成Word文档，保持格式清晰，便于打印复习。', 40.00, NULL, 0, DATE_ADD(NOW(), INTERVAL 3 DAY), 1, 0, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 8 HOUR), DATE_SUB(NOW(), INTERVAL 8 HOUR));

-- ============================================
-- 3. 已抢单待协商任务 (status=2) - 锁单中
-- ============================================
INSERT INTO `task` (`id`, `publisher_id`, `acceptor_id`, `title`, `category`, `description`, `amount`, `fee_amount`, `delivery_type`, `deadline`, `status`, `need_audit`, `task_images`, `delivery_proof`, `reject_reason`, `lock_expire_at`, `created_at`, `updated_at`) VALUES
(307213865930000013, 307213865924562944, 307213865924562945, '代取快递到宿舍（锁单中）', 1, '快递在菜鸟驿站，帮忙取到6号楼楼下即可。', 12.00, NULL, 1, DATE_ADD(NOW(), INTERVAL 1 DAY), 2, 0, NULL, NULL, NULL, DATE_ADD(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 10 HOUR), DATE_SUB(NOW(), INTERVAL 5 MINUTE)),
(307213865930000014, 307213865924562944, 307213865924562946, '帮忙写一个Python爬虫脚本（锁单中）', 3, '需要爬取某个网站的数据，保存为CSV格式。网站结构比较简单。', 120.00, NULL, 0, DATE_ADD(NOW(), INTERVAL 6 DAY), 2, 0, NULL, NULL, NULL, DATE_ADD(NOW(), INTERVAL 18 MINUTE), DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 2 MINUTE));

-- ============================================
-- 4. 待支付任务 (status=3) - 发布者确认接单者后，需要支付
-- ============================================
INSERT INTO `task` (`id`, `publisher_id`, `acceptor_id`, `title`, `category`, `description`, `amount`, `fee_amount`, `delivery_type`, `deadline`, `status`, `need_audit`, `task_images`, `delivery_proof`, `reject_reason`, `lock_expire_at`, `created_at`, `updated_at`) VALUES
(307213865930000011, 307213865924562944, 307213865924562945, '代取快递到宿舍', 1, '快递在菜鸟驿站，帮忙取到6号楼楼下即可。', 12.00, NULL, 1, DATE_ADD(NOW(), INTERVAL 1 DAY), 3, 0, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 10 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(307213865930000012, 307213865924562944, 307213865924562946, '帮忙写一个Python爬虫脚本', 3, '需要爬取某个网站的数据，保存为CSV格式。网站结构比较简单。', 120.00, NULL, 0, DATE_ADD(NOW(), INTERVAL 6 DAY), 3, 0, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR));

-- ============================================
-- 5. 进行中任务 (status=4) - 已支付，正在执行
-- ============================================
INSERT INTO `task` (`id`, `publisher_id`, `acceptor_id`, `title`, `category`, `description`, `amount`, `fee_amount`, `delivery_type`, `deadline`, `status`, `need_audit`, `task_images`, `delivery_proof`, `reject_reason`, `lock_expire_at`, `created_at`, `updated_at`) VALUES
(307213865930000015, 307213865924562944, 307213865924562945, '帮忙整理学习资料', 2, '将各科的学习资料分类整理，建立文件夹结构，便于查找。', 30.00, NULL, 0, DATE_ADD(NOW(), INTERVAL 2 DAY), 4, 0, '["/uploads/2026/03/materials1.jpg", "/uploads/2026/03/materials2.jpg"]', NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(307213865930000016, 307213865924562944, 307213865924562947, '开发一个简单的博客系统', 3, '需要一个简单的博客系统，包括文章发布、编辑、删除功能。使用Vue3 + Spring Boot开发。', 250.00, NULL, 0, DATE_ADD(NOW(), INTERVAL 10 DAY), 4, 1, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ============================================
-- 6. 已完成任务 (status=5) - 已完成但未验收
-- ============================================
INSERT INTO `task` (`id`, `publisher_id`, `acceptor_id`, `title`, `category`, `description`, `amount`, `fee_amount`, `delivery_type`, `deadline`, `status`, `need_audit`, `task_images`, `delivery_proof`, `reject_reason`, `lock_expire_at`, `created_at`, `updated_at`) VALUES
(307213865930000017, 307213865924562944, 307213865924562945, '代取外卖', 1, '帮忙取外卖到图书馆。', 8.00, NULL, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), 5, 0, NULL, '["/uploads/2026/03/delivery1.jpg"]', NULL, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(307213865930000018, 307213865924562944, 307213865924562946, '整理会议记录', 2, '将会议录音整理成文字记录，格式要求清晰。', 45.00, NULL, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), 5, 0, NULL, '["/uploads/2026/03/meeting1.docx"]', NULL, NULL, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ============================================
-- 7. 已结算任务 (status=6) - 已结算但未互评
-- ============================================
INSERT INTO `task` (`id`, `publisher_id`, `acceptor_id`, `title`, `category`, `description`, `amount`, `fee_amount`, `delivery_type`, `deadline`, `status`, `need_audit`, `task_images`, `delivery_proof`, `reject_reason`, `lock_expire_at`, `created_at`, `updated_at`) VALUES
(307213865930000019, 307213865924562944, 307213865924562945, '帮忙代课占位', 4, '周三上午的课程需要占位。', 18.00, 0.90, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), 6, 0, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(307213865930000020, 307213865924562944, 307213865924562946, '开发一个待办事项应用', 3, '简单的待办事项管理应用。', 150.00, 4.50, 0, DATE_SUB(NOW(), INTERVAL 4 DAY), 6, 0, NULL, '["/uploads/2026/03/todo1.zip"]', NULL, NULL, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(307213865930000021, 307213865924562944, 307213865924562947, '整理课程笔记', 2, '将手写笔记整理成电子版。', 35.00, 1.75, 0, DATE_SUB(NOW(), INTERVAL 5 DAY), 6, 0, NULL, '["/uploads/2026/03/notes1.pdf"]', NULL, NULL, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY));

-- ============================================
-- 8. 已互评任务 (status=7) - 已完成互评
-- ============================================
INSERT INTO `task` (`id`, `publisher_id`, `acceptor_id`, `title`, `category`, `description`, `amount`, `fee_amount`, `delivery_type`, `deadline`, `status`, `need_audit`, `task_images`, `delivery_proof`, `reject_reason`, `lock_expire_at`, `created_at`, `updated_at`) VALUES
(307213865930000022, 307213865924562944, 307213865924562945, '代取快递', 1, '帮忙取快递到宿舍。', 10.00, 0.50, 1, DATE_SUB(NOW(), INTERVAL 6 DAY), 7, 0, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(307213865930000023, 307213865924562944, 307213865924562946, '帮忙修改代码bug', 3, '修复一个简单的JavaScript bug。', 60.00, 3.00, 0, DATE_SUB(NOW(), INTERVAL 7 DAY), 7, 0, NULL, '["/uploads/2026/03/code1.js"]', NULL, NULL, DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
(307213865930000024, 307213865924562944, 307213865924562947, '整理实验数据', 2, '将实验数据整理成表格。', 28.00, 1.40, 0, DATE_SUB(NOW(), INTERVAL 8 DAY), 7, 0, NULL, '["/uploads/2026/03/data1.xlsx"]', NULL, NULL, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
(307213865930000025, 307213865924562944, 307213865924562945, '代课占位', 4, '帮忙占位上课。', 15.00, 0.75, 1, DATE_SUB(NOW(), INTERVAL 9 DAY), 7, 0, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY));

-- ============================================
-- 9. 已取消任务 (status=8)
-- ============================================
INSERT INTO `task` (`id`, `publisher_id`, `acceptor_id`, `title`, `category`, `description`, `amount`, `fee_amount`, `delivery_type`, `deadline`, `status`, `need_audit`, `task_images`, `delivery_proof`, `reject_reason`, `lock_expire_at`, `created_at`, `updated_at`) VALUES
(307213865930000026, 307213865924562944, NULL, '帮忙打印资料', 5, '需要打印一些资料。', 20.00, NULL, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), 8, 0, NULL, NULL, '时间冲突，无法完成', NULL, DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY)),
(307213865930000027, 307213865924562944, 307213865924562945, '开发一个小程序', 3, '需要开发一个简单的小程序。', 200.00, NULL, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), 8, 1, NULL, NULL, '需求不明确，取消任务', NULL, DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY));

-- ============================================
-- 数据说明
-- ============================================
-- 1. 任务ID使用雪花算法格式（实际使用时请确保ID唯一）
-- 2. publisher_id 和 acceptor_id 需要根据实际数据库中的用户ID调整
-- 3. 金额范围：1-500元（符合业务规则）
-- 4. need_audit: 金额>200的任务自动设为1
-- 5. fee_amount: 只有已结算(status>=6)的任务才有手续费
-- 6. lock_expire_at: 只有已抢单待协商(status=3)的任务才有锁单时间
-- 7. delivery_proof: 只有已完成(status>=5)的任务才有交付成果
-- 8. 时间设置：created_at和updated_at根据任务状态合理设置
-- 9. deadline: 根据任务状态设置合理的截止时间
-- 
-- 任务状态流转说明：
-- 待审核(0) → 待接单(1) → 已抢单待协商(2) → 待支付(3) → 进行中(4) → 已完成(5) → 已结算(6) → 已互评(7) / 已取消(8)
-- 注意：发布任务后，如果不需要审核，直接进入"待接单"(status=1)状态
--      如果需要审核，先"待审核"(status=0)，审核通过后进入"待接单"(status=1)
--      有人抢单后，进入"已抢单待协商"(status=2)
--      发布者确认接单者后，需要支付，进入"待支付"(status=3)
--      支付完成后，进入"进行中"(status=4)