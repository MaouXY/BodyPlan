-- ============================================================
-- 个性化健身管理系统 · 05 动作附加属性（跳跃 / 噪音）
-- 说明：
--   1. 用于“不能跳”“零噪音”两条训练限制的排除式匹配：
--      NO_JUMP  -> 排除 flags 含 JUMP 的动作
--      NO_NOISE -> 排除 flags 含 NOISE 的动作
--   2. 本脚本可重复执行（先判断列是否存在，再决定是否 ALTER）
-- ============================================================

USE body_plan;

SET @column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'exercise' AND column_name = 'flags'
);
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE exercise ADD COLUMN flags VARCHAR(100) DEFAULT NULL COMMENT ''附加属性：JUMP 跳跃 / NOISE 噪音 / HIGH_IMPACT 高冲击'' AFTER equipment',
    'SELECT 1');
PREPARE migrate_stmt FROM @ddl;
EXECUTE migrate_stmt;
DEALLOCATE PREPARE migrate_stmt;

UPDATE exercise SET flags = 'JUMP,NOISE,HIGH_IMPACT' WHERE name IN ('开合跳', '波比跳', '跳绳', '徒手深蹲跳');
UPDATE exercise SET flags = 'NOISE' WHERE name IN ('原地高抬腿', '登山跑');
UPDATE exercise SET flags = NULL WHERE name NOT IN ('开合跳', '波比跳', '跳绳', '徒手深蹲跳', '原地高抬腿', '登山跑');
