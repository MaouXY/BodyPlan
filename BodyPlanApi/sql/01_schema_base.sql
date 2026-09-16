-- ============================================================
-- 个性化健身管理系统 · 01 基础表（账号 / 档案 / 身体数据 / 体能测试）
-- 说明：
--   1. 执行顺序：01_schema_base.sql -> 02_schema_training.sql -> 03_schema_record.sql -> 04_seed_data.sql
--   2. 主键由后端 MyBatis-Plus（id-type: assign_id）生成雪花 ID，故建表不写 AUTO_INCREMENT
--   3. 逻辑删除字段 deleted：0 正常、1 已删除
--   4. 建档问卷（目标 / 偏好 / 时间 / 伤病 / 限制）合并进 user_profile 一张表，
--      只有“可多次复测”的 body_data 与 fitness_test 单独建表
-- ============================================================

CREATE DATABASE IF NOT EXISTS body_plan DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE body_plan;

-- ------------------------------------------------------------
-- 用户账号
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_account (
    id            BIGINT       NOT NULL COMMENT '主键',
    username      VARCHAR(64)  NOT NULL COMMENT '登录账号',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码散列',
    nickname      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '昵称',
    phone         VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：USER / ADMIN',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1 启用、0 禁用',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常、1 删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_account_username (username),
    KEY idx_user_account_role (role)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户账号';

-- ------------------------------------------------------------
-- 用户档案（建档问卷合并表，一个用户一条）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_profile (
    id                BIGINT       NOT NULL COMMENT '主键',
    user_id           BIGINT       NOT NULL COMMENT '用户ID',
    gender            VARCHAR(10)  NOT NULL DEFAULT 'UNKNOWN' COMMENT '性别：MALE / FEMALE / UNKNOWN',
    age               INT                   DEFAULT NULL COMMENT '年龄',
    height_cm         DECIMAL(5, 1)         DEFAULT NULL COMMENT '身高（cm）',
    weight_kg         DECIMAL(5, 1)         DEFAULT NULL COMMENT '体重（kg）',
    body_type         VARCHAR(20)           DEFAULT NULL COMMENT '身体类型：STRAIGHT / PEAR / HOURGLASS / APPLE',
    primary_goal      VARCHAR(30)           DEFAULT NULL COMMENT '主目标：LOSE_FAT / BUILD_MUSCLE / CARDIO / POSTURE / LOCAL_STRENGTH / DAILY_FITNESS',
    sub_goals         VARCHAR(200)          DEFAULT NULL COMMENT '次目标，逗号分隔',
    focus_parts       VARCHAR(200)          DEFAULT NULL COMMENT '重点部位，逗号分隔：ABDOMEN,LEG,HIP,ARM,BACK,SHOULDER_NECK,FULL_BODY',
    sport_types       VARCHAR(200)          DEFAULT NULL COMMENT '偏好运动类型，逗号分隔：CARDIO,STRENGTH,STRETCH,RUNNING,HIIT,YOGA',
    weekly_days       VARCHAR(30)           DEFAULT NULL COMMENT '每周训练日，逗号分隔：1,3,5 表示周一三五',
    daily_minutes     INT                   DEFAULT NULL COMMENT '每日运动时长（分钟）',
    remind_enabled    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否开启训练提醒：1 开、0 关',
    remind_time       VARCHAR(5)            DEFAULT NULL COMMENT '提醒时间 HH:mm',
    injuries          VARCHAR(200)          DEFAULT NULL COMMENT '伤病：KNEE,WAIST,SHOULDER,WRIST,CARDIO,NONE',
    train_limits      VARCHAR(200)          DEFAULT NULL COMMENT '训练限制：SMALL_SPACE,NO_NOISE,NO_JUMP,NO_EQUIPMENT,DORM',
    profile_completed TINYINT      NOT NULL DEFAULT 0 COMMENT '建档是否完成：1 完成、0 未完成',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_profile_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户档案（建档问卷合并表）';

-- ------------------------------------------------------------
-- 身体数据记录（可多条，用于复测趋势与身体评分）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS body_data (
    id          BIGINT        NOT NULL COMMENT '主键',
    user_id     BIGINT        NOT NULL COMMENT '用户ID',
    height_cm   DECIMAL(5, 1)          DEFAULT NULL COMMENT '身高（cm）',
    weight_kg   DECIMAL(5, 1)          DEFAULT NULL COMMENT '体重（kg）',
    bmi         DECIMAL(4, 1)          DEFAULT NULL COMMENT 'BMI',
    body_type   VARCHAR(20)            DEFAULT NULL COMMENT '身体类型',
    score       INT                    DEFAULT NULL COMMENT '身体评分 0-100（复测时重新计算）',
    remark      VARCHAR(255)           DEFAULT NULL COMMENT '备注',
    recorded_at DATETIME      NOT NULL COMMENT '记录时间（复测时间）',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_body_data_user_time (user_id, recorded_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='身体数据记录（含复测）';

-- ------------------------------------------------------------
-- 体能测试记录（可多次，用于判断计划难度等级）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fitness_test (
    id           BIGINT      NOT NULL COMMENT '主键',
    user_id      BIGINT      NOT NULL COMMENT '用户ID',
    push_up      INT                  DEFAULT NULL COMMENT '一次可完成俯卧撑个数',
    squat        INT                  DEFAULT NULL COMMENT '一次可完成深蹲个数',
    crunch       INT                  DEFAULT NULL COMMENT '一次可完成卷腹个数',
    stair_status VARCHAR(20)          DEFAULT NULL COMMENT '上五楼状态：EASY / SLIGHT_HARD / HARD / FAIL',
    level        VARCHAR(10)          DEFAULT NULL COMMENT '体能等级：L1 入门 / L2 进阶 / L3 挑战',
    tested_at    DATETIME    NOT NULL COMMENT '测试时间',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_fitness_test_user_time (user_id, tested_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='体能测试记录';
