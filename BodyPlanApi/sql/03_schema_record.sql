-- ============================================================
-- 个性化健身管理系统 · 03 记录表（每日打卡 / 跑步记录）
-- 说明：
--   1. 打卡表对 (user_id, plan_item_id, training_date) 建唯一键，天然拦截重复打卡（幂等）
--   2. 跑步记录属于后置阶段（阶段 3），先建表，代码在阶段 3 再实现
--   3. 排行榜（阶段 5）不建表，直接对 checkin_record / running_record 做 SQL 聚合计算
-- ============================================================

USE body_plan;

-- ------------------------------------------------------------
-- 每日打卡记录
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS checkin_record (
    id            BIGINT       NOT NULL COMMENT '主键',
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    plan_id       BIGINT                DEFAULT NULL COMMENT '计划ID（自由训练可为空）',
    plan_item_id  BIGINT                DEFAULT NULL COMMENT '计划明细ID（自由训练可为空）',
    training_date DATE         NOT NULL COMMENT '训练日期',
    finished      TINYINT      NOT NULL DEFAULT 1 COMMENT '是否完成计划任务：1 是、0 否',
    duration_min  INT                   DEFAULT NULL COMMENT '本次训练时长（分钟）',
    feeling       VARCHAR(20)           DEFAULT NULL COMMENT '训练感受：EASY / NORMAL / HARD',
    remark        VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_checkin_item_date (user_id, plan_item_id, training_date),
    KEY idx_checkin_user_date (user_id, training_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='每日打卡记录';

-- ------------------------------------------------------------
-- 跑步记录（阶段 3）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS running_record (
    id               BIGINT        NOT NULL COMMENT '主键',
    user_id          BIGINT        NOT NULL COMMENT '用户ID',
    distance_km      DECIMAL(6, 2) NOT NULL COMMENT '跑步距离（公里）',
    duration_sec     INT           NOT NULL COMMENT '用时（秒）',
    pace_sec_per_km  INT                    DEFAULT NULL COMMENT '配速（秒/公里，后端计算写入）',
    kcal             INT                    DEFAULT NULL COMMENT '消耗估算（千卡）',
    run_date         DATE          NOT NULL COMMENT '跑步日期',
    remark           VARCHAR(255)           DEFAULT NULL COMMENT '备注',
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted          TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_running_user_date (user_id, run_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='跑步记录';
