-- BodyPlan 演示验收数据。执行顺序：01 -> 02 -> 03 -> 04 -> 05 -> 06。
-- 仅由项目使用者执行。UTF-8，无 BOM。MySQL 8。
-- 新增 demo_ready / demo_new / demo_compare，密码均为 123456。
-- 不删除、不重置已有业务数据；使用固定演示 ID，重复执行不会增加相同记录。
-- 日期以第一次执行时的 CURDATE() 为准。再次执行不会把已打卡日期搬到今天。
-- 若过了演示周期，在 App 修改档案并生成新计划，或直接使用自由训练打卡。
-- 预置计划是明确标注的演示样例；实际推荐效果请通过新账号或复测生成来验证。
-- 本脚本占用 ID 910001-910003、911001-911003、912001-912003、
-- 920001-920003、930000-930999、940000-940999、950000-950999、960001-960003。

SET NAMES utf8mb4;
USE body_plan;
START TRANSACTION;
SET @demo_today = CURDATE();
SET @demo_start = DATE_SUB(@demo_today, INTERVAL WEEKDAY(@demo_today) DAY);

INSERT INTO user_account (id, username, password_hash, nickname, role, status)
SELECT 910001, 'demo_ready', '$2a$10$YzHrUnbQhVJ9Aza4tEdqQuUtVMhGr6iy.5WHZFLO1Bqun9SH2Eaxa', '演示：完整流程', 'USER', 1
WHERE NOT EXISTS (SELECT 1 FROM user_account WHERE username = 'demo_ready' OR id = 910001);
INSERT INTO user_account (id, username, password_hash, nickname, role, status)
SELECT 910002, 'demo_new', '$2a$10$YzHrUnbQhVJ9Aza4tEdqQuUtVMhGr6iy.5WHZFLO1Bqun9SH2Eaxa', '演示：首次建档', 'USER', 1
WHERE NOT EXISTS (SELECT 1 FROM user_account WHERE username = 'demo_new' OR id = 910002);
INSERT INTO user_account (id, username, password_hash, nickname, role, status)
SELECT 910003, 'demo_compare', '$2a$10$YzHrUnbQhVJ9Aza4tEdqQuUtVMhGr6iy.5WHZFLO1Bqun9SH2Eaxa', '演示：对比档案', 'USER', 1
WHERE NOT EXISTS (SELECT 1 FROM user_account WHERE username = 'demo_compare' OR id = 910003);

INSERT INTO user_profile (id, user_id, gender, age, height_cm, weight_kg, body_type, primary_goal,
    focus_parts, sport_types, weekly_days, daily_minutes, remind_enabled, injuries, train_limits, profile_completed)
SELECT 911001, u.id, 'MALE', 28, 175, 76.5, 'APPLE', 'LOSE_FAT',
    'ABDOMEN,LEG', 'STRENGTH,STRETCH', '1,2,3,4,5,6,7', 20, 0, 'KNEE', 'DORM,NO_JUMP,NO_EQUIPMENT', 1
FROM user_account u WHERE u.id = 910001 AND u.username = 'demo_ready'
AND NOT EXISTS (SELECT 1 FROM user_profile WHERE user_id = u.id OR id = 911001);
INSERT INTO user_profile (id, user_id, gender, age, height_cm, weight_kg, body_type, primary_goal,
    focus_parts, sport_types, weekly_days, daily_minutes, remind_enabled, injuries, profile_completed)
SELECT 911003, u.id, 'FEMALE', 24, 163, 52, 'PEAR', 'BUILD_MUSCLE',
    'HIP,ARM', 'STRENGTH,STRETCH', '2,4,6', 40, 0, 'NONE', 1
FROM user_account u WHERE u.id = 910003 AND u.username = 'demo_compare'
AND NOT EXISTS (SELECT 1 FROM user_profile WHERE user_id = u.id OR id = 911003);

INSERT INTO fitness_test (id, user_id, push_up, squat, crunch, stair_status, level, tested_at)
SELECT 912000 + u.id - 910000, u.id, 15, 25, 20, 'SLIGHT_HARD', 'L2', @demo_today
FROM user_account u
WHERE ((u.id = 910001 AND u.username = 'demo_ready') OR (u.id = 910003 AND u.username = 'demo_compare'))
AND NOT EXISTS (SELECT 1 FROM fitness_test f WHERE f.id = 912000 + u.id - 910000);

INSERT INTO body_data (id, user_id, height_cm, weight_kg, bmi, body_type, score, remark, recorded_at)
SELECT 950000 + (p.user_id - 910000) * 10 + n.n, p.user_id, p.height_cm,
    p.weight_kg + CASE WHEN p.user_id = 910001 THEN (2 - n.n) * 0.75 ELSE (n.n - 2) * 0.5 END,
    ROUND((p.weight_kg + CASE WHEN p.user_id = 910001 THEN (2 - n.n) * 0.75 ELSE (n.n - 2) * 0.5 END)
        / POW(p.height_cm / 100, 2), 1), p.body_type, 70 + n.n * 3,
    '演示预置身体变化样例', DATE_SUB(@demo_today, INTERVAL ((2 - n.n) * 14) DAY)
FROM user_profile p
JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2) n
WHERE p.id IN (911001, 911003) AND p.user_id IN (910001, 910003)
AND NOT EXISTS (SELECT 1 FROM body_data b WHERE b.id = 950000 + (p.user_id - 910000) * 10 + n.n);

INSERT INTO training_plan (id, user_id, name, goal, level, days_per_week, minutes_per_session,
    start_date, end_date, status, reason)
SELECT 920001, p.user_id, '演示样例 · 每日轻量训练', p.primary_goal, 'L2', 7, 20,
    @demo_start, DATE_ADD(@demo_start, INTERVAL 27 DAY), 'ACTIVE',
    '预置演示计划：每天安排站姿提踵与肩颈拉伸，供打卡演示。此记录不是推荐算法输出；修改档案或新用户建档后将生成真实规则推荐及依据。'
FROM user_profile p WHERE p.id = 911001 AND p.user_id = 910001
AND NOT EXISTS (SELECT 1 FROM training_plan WHERE id = 920001 OR (user_id = 910001 AND status = 'ACTIVE' AND deleted = 0));

INSERT INTO training_plan_item (id, plan_id, day_index, training_date, exercise_id, exercise_name,
    sets, reps, duration_sec, rest_sec, sort_no, status)
SELECT 930000 + (w.w * 7 + d.d) * 10 + e.sort_no, p.id, d.d + 1,
    DATE_ADD(p.start_date, INTERVAL (w.w * 7 + d.d) DAY), e.id, e.name,
    2, CASE WHEN e.id = 2014 THEN 12 ELSE NULL END,
    CASE WHEN e.id = 2018 THEN 30 ELSE NULL END, 30, e.sort_no, 'PENDING'
FROM training_plan p
JOIN (SELECT 0 w UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3) w
JOIN (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) d
JOIN (SELECT id, name, CASE WHEN id = 2014 THEN 1 ELSE 2 END sort_no FROM exercise WHERE id IN (2014, 2018) AND deleted = 0) e
WHERE p.id = 920001 AND p.user_id = 910001
AND NOT EXISTS (SELECT 1 FROM training_plan_item i WHERE i.id = 930000 + (w.w * 7 + d.d) * 10 + e.sort_no);

INSERT INTO recommend_log (id, user_id, plan_id, input_snapshot, matched_rule, excluded_count, reason)
SELECT 960001, user_id, id, '{"source":"demo-fixture"}', 'DEMO_FIXTURE', 0, reason
FROM training_plan p WHERE p.id = 920001 AND p.user_id = 910001
AND NOT EXISTS (SELECT 1 FROM recommend_log WHERE id = 960001);

-- 最近 14 天历史；不预填今天，保留现场自由打卡入口。
INSERT INTO checkin_record (id, user_id, training_date, duration_min, feeling, remark, created_at, finished, deleted)
SELECT 940000 + (u.id - 910000) * 100 + n.n, u.id, DATE_SUB(@demo_today, INTERVAL n.n DAY),
    10 + MOD(n.n, 4) * 5, CASE WHEN MOD(n.n, 3) = 0 THEN 'EASY' ELSE 'NORMAL' END,
    '演示预置自由训练记录', DATE_SUB(@demo_today, INTERVAL n.n DAY), 1, 0
FROM user_account u
JOIN (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
    UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14) n
WHERE ((u.id = 910001 AND u.username = 'demo_ready') OR
    (u.id = 910003 AND u.username = 'demo_compare' AND MOD(n.n, 2) = 0))
AND NOT EXISTS (SELECT 1 FROM checkin_record c WHERE c.id = 940000 + (u.id - 910000) * 100 + n.n)
AND NOT EXISTS (SELECT 1 FROM checkin_record c WHERE c.user_id = u.id
    AND c.training_date = DATE_SUB(@demo_today, INTERVAL n.n DAY) AND c.plan_item_id IS NULL);

COMMIT;

-- 验证：应有 3 个演示账号；demo_ready 有 1 份初始计划、56 条明细、14 条历史打卡。
SELECT id, username, status FROM user_account WHERE username IN ('demo_ready', 'demo_new', 'demo_compare');
SELECT id, user_id, status, start_date, end_date FROM training_plan WHERE id = 920001;
SELECT COUNT(*) AS demo_plan_items FROM training_plan_item WHERE plan_id = 920001;
SELECT user_id, COUNT(*) AS history_count FROM checkin_record WHERE user_id IN (910001, 910003) AND deleted = 0 GROUP BY user_id;
