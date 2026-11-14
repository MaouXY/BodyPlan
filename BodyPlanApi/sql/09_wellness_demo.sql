-- 独立的新功能演示账号。密码均为 123456。不修改 demo 等原有账号。
-- 在 08 之后执行；重跑不重置演示进度，也不重排已生成的计划。
USE body_plan;

INSERT IGNORE INTO user_account (id,username,password_hash,nickname,role,status)
VALUES
(940001,'coach_demo','$2a$10$YzHrUnbQhVJ9Aza4tEdqQuUtVMhGr6iy.5WHZFLO1Bqun9SH2Eaxa','林教练','COACH',1),
(940002,'wellness_demo','$2a$10$YzHrUnbQhVJ9Aza4tEdqQuUtVMhGr6iy.5WHZFLO1Bqun9SH2Eaxa','运动演示用户','USER',1);

SET @coach_id = (SELECT id FROM user_account WHERE username='coach_demo' AND role='COACH' AND deleted=0);
SET @student_id = (SELECT id FROM user_account WHERE username='wellness_demo' AND role='USER' AND deleted=0);

INSERT IGNORE INTO coach_student (student_id,coach_id,assigned_by)
SELECT @student_id,@coach_id,id FROM user_account
WHERE username='admin' AND role='ADMIN' AND deleted=0 AND @student_id IS NOT NULL AND @coach_id IS NOT NULL;

INSERT IGNORE INTO user_profile
(id,user_id,gender,age,height_cm,weight_kg,body_type,primary_goal,focus_parts,sport_types,weekly_days,daily_minutes,injuries,profile_completed)
SELECT 941001,@student_id,'MALE',25,175,70,'STRAIGHT','DAILY_FITNESS','FULL_BODY','RUNNING,STRENGTH','1,2,3,4,5,6,7',30,'NONE',1
WHERE @student_id IS NOT NULL;

INSERT INTO fitness_test (id,user_id,push_up,squat,crunch,stair_status,level,tested_at)
SELECT 941002,@student_id,15,25,20,'SLIGHT_HARD','L2',NOW()
WHERE @student_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM fitness_test WHERE user_id=@student_id);

INSERT INTO body_data (id,user_id,height_cm,weight_kg,bmi,body_type,score,remark,recorded_at)
SELECT 941003,@student_id,175,70,22.9,'STRAIGHT',80,'新功能演示档案',NOW()
WHERE @student_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM body_data WHERE user_id=@student_id);

INSERT IGNORE INTO training_plan
(id,user_id,name,goal,level,days_per_week,minutes_per_session,start_date,end_date,status,reason)
SELECT 942001,@student_id,'新功能演示 · 每日轻量训练','DAILY_FITNESS','L2',7,30,CURDATE(),DATE_ADD(CURDATE(),INTERVAL 6 DAY),'ACTIVE','独立演示计划，用于教练督促与任务进度验收'
WHERE @student_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM training_plan WHERE user_id=@student_id AND status='ACTIVE' AND deleted=0);

INSERT IGNORE INTO training_plan_item
(id,plan_id,day_index,training_date,exercise_id,exercise_name,sets,reps,rest_sec,sort_no,status)
SELECT 942100+n.n*10+e.rn,p.id,WEEKDAY(DATE_ADD(p.start_date,INTERVAL n.n DAY))+1,
DATE_ADD(p.start_date,INTERVAL n.n DAY),e.id,e.name,2,12,45,e.rn,'PENDING'
FROM training_plan p
CROSS JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) n
CROSS JOIN (SELECT id,name,ROW_NUMBER() OVER(ORDER BY difficulty,sort_no,id) AS rn
            FROM exercise WHERE status=1 AND deleted=0 AND (contra_flags IS NULL OR contra_flags='')) e
WHERE p.id=942001 AND p.user_id=@student_id AND e.rn<=3;
