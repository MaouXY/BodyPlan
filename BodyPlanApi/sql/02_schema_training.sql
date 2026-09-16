-- ============================================================
-- 个性化健身管理系统 · 02 训练相关表（动作视频 / 标签 / 训练计划 / 推荐记录）
-- 说明：
--   1. 计划生成不依赖“计划模板表”，而是按 目标 + 体能等级 + 重点部位 + 伤病禁忌 + 场地器械 从 exercise 中筛选动作，
--      因此没有 plan_template 表，推荐过程写入 recommend_log，保证推荐可解释
--   2. exercise.contra_flags 与 user_profile.injuries / train_limits 做“排除式”匹配
-- ============================================================

USE body_plan;

-- ------------------------------------------------------------
-- 动作视频
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS exercise (
    id           BIGINT       NOT NULL COMMENT '主键',
    name         VARCHAR(64)  NOT NULL COMMENT '动作名称',
    part         VARCHAR(30)  NOT NULL COMMENT '目标部位：ABDOMEN/LEG/HIP/ARM/BACK/SHOULDER_NECK/FULL_BODY',
    sport_type   VARCHAR(30)  NOT NULL COMMENT '运动类型：CARDIO/STRENGTH/STRETCH/RUNNING/HIIT/YOGA',
    difficulty   TINYINT      NOT NULL DEFAULT 1 COMMENT '难度：1 入门、2 进阶、3 挑战',
    scene        VARCHAR(30)  NOT NULL DEFAULT 'HOME' COMMENT '场景：HOME 居家 / DORM 宿舍 / GYM 健身房 / OUTDOOR 户外',
    equipment    VARCHAR(50)  NOT NULL DEFAULT 'NONE' COMMENT '器械：NONE/DUMBBELL/MAT/BAND',
    contra_flags VARCHAR(100)          DEFAULT NULL COMMENT '禁忌标签：KNEE,WAIST,SHOULDER,WRIST,CARDIO（命中用户伤病则排除）',
    video_url    VARCHAR(500)          DEFAULT NULL COMMENT '视频地址（占位，替换为真实素材）',
    cover_url    VARCHAR(500)          DEFAULT NULL COMMENT '封面地址（占位，替换为真实素材）',
    key_points   TEXT                  DEFAULT NULL COMMENT '动作要点',
    cautions     TEXT                  DEFAULT NULL COMMENT '注意事项',
    suitable_for VARCHAR(255)          DEFAULT NULL COMMENT '适用人群说明',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1 上架、0 下架',
    sort_no      INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_exercise_filter (part, sport_type, difficulty, status),
    KEY idx_exercise_scene (scene, equipment)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='动作视频';

-- ------------------------------------------------------------
-- 动作标签字典（用于动作筛选与新增动作时的选项）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS exercise_tag (
    id         BIGINT      NOT NULL COMMENT '主键',
    tag_type   VARCHAR(30) NOT NULL COMMENT '标签类型：PART/SPORT_TYPE/SCENE/EQUIPMENT/DIFFICULTY',
    tag_code   VARCHAR(40) NOT NULL COMMENT '标签编码',
    tag_name   VARCHAR(50) NOT NULL COMMENT '标签名称',
    sort_no    INT         NOT NULL DEFAULT 0 COMMENT '排序号',
    status     TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1 启用、0 停用',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_exercise_tag (tag_type, tag_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='动作标签字典';

-- ------------------------------------------------------------
-- 训练计划（一个用户同时只有一条 ACTIVE）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS training_plan (
    id                  BIGINT      NOT NULL COMMENT '主键',
    user_id             BIGINT      NOT NULL COMMENT '用户ID',
    name                VARCHAR(80) NOT NULL COMMENT '计划名称',
    goal                VARCHAR(30) NOT NULL COMMENT '计划目标',
    level               VARCHAR(10) NOT NULL COMMENT '计划难度：L1/L2/L3',
    days_per_week       INT         NOT NULL COMMENT '每周训练天数',
    minutes_per_session INT         NOT NULL COMMENT '每次训练时长（分钟）',
    start_date          DATE        NOT NULL COMMENT '开始日期',
    end_date            DATE                 DEFAULT NULL COMMENT '结束日期',
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE 执行中 / FINISHED 已完成 / REPLACED 已替换',
    reason              VARCHAR(500)         DEFAULT NULL COMMENT '推荐依据（展示给用户）',
    created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_training_plan_user_status (user_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='训练计划';

-- ------------------------------------------------------------
-- 训练计划明细（计划第几天做什么动作）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS training_plan_item (
    id            BIGINT      NOT NULL COMMENT '主键',
    plan_id       BIGINT      NOT NULL COMMENT '计划ID',
    day_index     INT         NOT NULL COMMENT '计划第几天：1-7',
    training_date DATE                 DEFAULT NULL COMMENT '对应日期（按 start_date 推算）',
    exercise_id   BIGINT      NOT NULL COMMENT '动作ID',
    exercise_name VARCHAR(64) NOT NULL COMMENT '动作名称快照（避免列表页联表）',
    sets          INT         NOT NULL DEFAULT 1 COMMENT '组数',
    reps          INT                  DEFAULT NULL COMMENT '每组次数',
    duration_sec  INT                  DEFAULT NULL COMMENT '每组时长（秒，计时类动作）',
    rest_sec      INT         NOT NULL DEFAULT 30 COMMENT '组间休息（秒）',
    sort_no       INT         NOT NULL DEFAULT 0 COMMENT '动作顺序',
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING 未完成 / DONE 已完成 / SKIPPED 跳过',
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_training_plan_item_plan (plan_id, day_index, sort_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='训练计划明细';

-- ------------------------------------------------------------
-- 推荐记录（保留推荐依据，支持复测后解释“计划为什么变了”）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recommend_log (
    id              BIGINT       NOT NULL COMMENT '主键',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    plan_id         BIGINT                DEFAULT NULL COMMENT '生成的计划ID',
    input_snapshot  TEXT                  DEFAULT NULL COMMENT '参与匹配的输入快照（JSON：目标/体能等级/重点部位/伤病/限制/每周天数）',
    matched_rule    VARCHAR(255)          DEFAULT NULL COMMENT '命中的匹配规则说明',
    excluded_count  INT          NOT NULL DEFAULT 0 COMMENT '因伤病或场地限制被排除的动作数',
    reason          VARCHAR(500)          DEFAULT NULL COMMENT '推荐原因',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_recommend_log_user (user_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='推荐记录';
