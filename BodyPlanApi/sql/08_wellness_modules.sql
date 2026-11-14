-- 2026-11-15：在 01~07 之后执行。可重复执行，不覆盖现有用户数据。
USE body_plan;

CREATE TABLE IF NOT EXISTS sport_session (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    client_record_id VARCHAR(64) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    sport_type VARCHAR(20) NOT NULL,
    training_mode VARCHAR(20) NOT NULL,
    target_distance_m INT DEFAULT NULL,
    target_pace_sec INT DEFAULT NULL,
    started_at BIGINT NOT NULL,
    ended_at BIGINT NOT NULL,
    duration_sec INT NOT NULL,
    distance_m DECIMAL(12,2) NOT NULL,
    pace_sec_per_km INT DEFAULT NULL,
    average_speed_kmh DECIMAL(8,2) DEFAULT NULL,
    route_json LONGTEXT NOT NULL,
    point_count INT NOT NULL,
    remark VARCHAR(255) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sport_client (user_id, client_record_id),
    KEY idx_sport_user_time (user_id, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运动记录，路线仅本人可见';

CREATE TABLE IF NOT EXISTS coach_student (
    student_id BIGINT NOT NULL PRIMARY KEY,
    coach_id BIGINT NOT NULL,
    assigned_by BIGINT NOT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_coach_students (coach_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='当前教练学员关系';

CREATE TABLE IF NOT EXISTS user_message (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    task_date DATE NOT NULL,
    title VARCHAR(80) NOT NULL,
    content VARCHAR(500) NOT NULL,
    is_read TINYINT NOT NULL DEFAULT 0,
    read_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_message_user_time (user_id, created_at),
    KEY idx_message_sender_student (sender_id, user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教练站内任务提醒';

CREATE TABLE IF NOT EXISTS nutrition_food (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    category VARCHAR(30) NOT NULL,
    kcal DECIMAL(9,2) NOT NULL,
    protein DECIMAL(9,2) NOT NULL,
    fat DECIMAL(9,2) NOT NULL,
    carbohydrate DECIMAL(9,2) NOT NULL,
    fiber DECIMAL(9,2) DEFAULT NULL,
    source VARCHAR(200) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每100克食物营养参考值';

CREATE TABLE IF NOT EXISTS diet_entry (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    entry_date DATE NOT NULL,
    meal VARCHAR(20) NOT NULL,
    food_id BIGINT NOT NULL,
    food_name VARCHAR(80) NOT NULL,
    grams DECIMAL(9,2) NOT NULL,
    kcal DECIMAL(10,2) NOT NULL,
    protein DECIMAL(10,2) NOT NULL,
    fat DECIMAL(10,2) NOT NULL,
    carbohydrate DECIMAL(10,2) NOT NULL,
    fiber DECIMAL(10,2) DEFAULT NULL,
    source VARCHAR(200) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_diet_user_date (user_id, entry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='饮食记录及营养快照';

CREATE TABLE IF NOT EXISTS nutrition_target (
    user_id BIGINT NOT NULL,
    target_date DATE NOT NULL,
    goal VARCHAR(20) NOT NULL,
    kcal INT NOT NULL,
    protein INT NOT NULL,
    fat INT NOT NULL,
    carbohydrate INT NOT NULL,
    fiber INT NOT NULL,
    PRIMARY KEY (user_id, target_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='按日期保存的用户自定营养目标';

-- 基础示例库：近似值，仅用于记录估算，实际以包装和称重为准；不覆盖已有食物。
INSERT IGNORE INTO nutrition_food
    (id,name,category,kcal,protein,fat,carbohydrate,fiber,source) VALUES
    (810001,'米饭（熟）','主食',116,2.6,0.3,25.9,0.3,'基础示例库：近似参考值，实际以包装为准'),
    (810002,'燕麦片（干）','主食',379,13.2,6.5,67.7,10.1,'基础示例库：近似参考值，实际以包装为准'),
    (810003,'鸡胸肉（熟、去皮）','肉蛋',165,31.0,3.6,0,0,'基础示例库：近似参考值，实际以包装为准'),
    (810004,'鸡蛋（煮）','肉蛋',155,12.6,10.6,1.1,0,'基础示例库：近似参考值，实际以包装为准'),
    (810005,'牛奶（全脂）','乳品',61,3.2,3.3,4.8,0,'基础示例库：近似参考值，实际以包装为准'),
    (810006,'芹菜（生）','蔬菜',14,0.7,0.2,3.0,1.6,'基础示例库：近似参考值，实际以包装为准'),
    (810007,'西兰花（生）','蔬菜',34,2.8,0.4,6.6,2.6,'基础示例库：近似参考值，实际以包装为准'),
    (810008,'香蕉（可食部）','水果',89,1.1,0.3,22.8,2.6,'基础示例库：近似参考值，实际以包装为准'),
    (810009,'苹果（带皮可食部）','水果',52,0.3,0.2,13.8,2.4,'基础示例库：近似参考值，实际以包装为准'),
    (810010,'豆腐（北豆腐）','豆制品',98,12.2,4.8,2.0,NULL,'基础示例库：近似参考值，纤维暂缺，实际以包装为准');
