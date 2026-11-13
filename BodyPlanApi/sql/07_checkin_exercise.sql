-- 自选运动打卡：先由使用者执行此脚本，再启动新版后端。
-- 已有库只需执行 07；新库先执行 01~05，06 为可选演示数据。
-- 可重复执行，不删除历史记录，不凭空补填旧自由训练的运动名称。
USE body_plan;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'checkin_record' AND column_name = 'exercise_id');
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE checkin_record ADD COLUMN exercise_id BIGINT DEFAULT NULL COMMENT ''具体运动ID'' AFTER plan_item_id',
    'SELECT 1');
PREPARE migrate_stmt FROM @ddl;
EXECUTE migrate_stmt;
DEALLOCATE PREPARE migrate_stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'checkin_record' AND column_name = 'exercise_name');
SET @ddl = IF(@column_exists = 0,
    'ALTER TABLE checkin_record ADD COLUMN exercise_name VARCHAR(100) DEFAULT NULL COMMENT ''打卡时运动名称快照'' AFTER exercise_id',
    'SELECT 1');
PREPARE migrate_stmt FROM @ddl;
EXECUTE migrate_stmt;
DEALLOCATE PREPARE migrate_stmt;

SHOW COLUMNS FROM checkin_record WHERE Field IN ('exercise_id', 'exercise_name');
