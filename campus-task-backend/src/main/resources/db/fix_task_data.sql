-- ============================================
-- 数据修复脚本：清理待接单任务的接单者信息
-- ============================================
-- 说明：将所有待接单(status=1)状态的任务的接单者信息清空
-- 执行时机：在修改状态编号后执行一次

-- 清理待接单任务的接单者信息
UPDATE `task` 
SET `acceptor_id` = NULL, 
    `lock_expire_at` = NULL 
WHERE `status` = 1 AND `acceptor_id` IS NOT NULL;

-- 验证修复结果
SELECT id, title, status, acceptor_id, lock_expire_at 
FROM `task` 
WHERE `status` = 1;
