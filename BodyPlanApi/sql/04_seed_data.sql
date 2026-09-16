-- ============================================================
-- 个性化健身管理系统 · 04 种子数据（演示账号 / 标签字典 / 动作视频 / 演示建档数据）
-- 说明：
--   1. 全部使用 INSERT IGNORE，可重复执行
--   2. 演示账号密码统一为 123456，密码散列由 Spring Security BCryptPasswordEncoder 生成并校验通过
--   3. exercise.video_url / cover_url 为占位路径，阶段 2 落地视频时替换为真实素材地址（静态目录或对象存储）
--   4. 演示用户刻意带“膝盖不适 + 宿舍 + 不能跳 + 无器械”的限制，
--      用于现场演示推荐算法的“排除式匹配”：开合跳、深蹲、箭步蹲、波比跳、跳绳、慢跑等动作应被排除
-- ============================================================

USE body_plan;

-- ------------------------------------------------------------
-- 演示账号：管理员 / 普通用户（密码 123456）
-- ------------------------------------------------------------
INSERT IGNORE INTO user_account (id, username, password_hash, nickname, phone, role, status) VALUES
(1, 'admin',  '$2a$10$YzHrUnbQhVJ9Aza4tEdqQuUtVMhGr6iy.5WHZFLO1Bqun9SH2Eaxa', '系统管理员', '13800000000', 'ADMIN', 1),
(2, 'demo',   '$2a$10$YzHrUnbQhVJ9Aza4tEdqQuUtVMhGr6iy.5WHZFLO1Bqun9SH2Eaxa', '演示用户',   '13900000000', 'USER',  1),
(3, 'demo02', '$2a$10$YzHrUnbQhVJ9Aza4tEdqQuUtVMhGr6iy.5WHZFLO1Bqun9SH2Eaxa', '演示用户二', '13700000000', 'USER',  1);

-- ------------------------------------------------------------
-- 动作标签字典（动作筛选 / 新增动作时的可选项）
-- ------------------------------------------------------------
INSERT IGNORE INTO exercise_tag (id, tag_type, tag_code, tag_name, sort_no, status) VALUES
(3001, 'PART', 'ABDOMEN',       '腹部',     1, 1),
(3002, 'PART', 'LEG',           '腿部',     2, 1),
(3003, 'PART', 'HIP',           '臀部',     3, 1),
(3004, 'PART', 'ARM',           '手臂',     4, 1),
(3005, 'PART', 'BACK',          '背部',     5, 1),
(3006, 'PART', 'SHOULDER_NECK', '肩颈',     6, 1),
(3007, 'PART', 'FULL_BODY',     '全身综合', 7, 1),
(3011, 'SPORT_TYPE', 'CARDIO',   '有氧训练',   1, 1),
(3012, 'SPORT_TYPE', 'STRENGTH', '力量训练',   2, 1),
(3013, 'SPORT_TYPE', 'STRETCH',  '拉伸放松',   3, 1),
(3014, 'SPORT_TYPE', 'RUNNING',  '跑步',       4, 1),
(3015, 'SPORT_TYPE', 'HIIT',     '高强度间歇', 5, 1),
(3016, 'SPORT_TYPE', 'YOGA',     '瑜伽/低强度', 6, 1),
(3021, 'SCENE', 'HOME',    '居家',   1, 1),
(3022, 'SCENE', 'DORM',    '宿舍',   2, 1),
(3023, 'SCENE', 'GYM',     '健身房', 3, 1),
(3024, 'SCENE', 'OUTDOOR', '户外',   4, 1),
(3031, 'EQUIPMENT', 'NONE',     '无器械', 1, 1),
(3032, 'EQUIPMENT', 'MAT',      '瑜伽垫', 2, 1),
(3033, 'EQUIPMENT', 'DUMBBELL', '哑铃',   3, 1),
(3034, 'EQUIPMENT', 'BAND',     '弹力带', 4, 1),
(3041, 'DIFFICULTY', '1', '入门', 1, 1),
(3042, 'DIFFICULTY', '2', '进阶', 2, 1),
(3043, 'DIFFICULTY', '3', '挑战', 3, 1);

-- ------------------------------------------------------------
-- 动作视频（24 个基础动作）
-- ------------------------------------------------------------
INSERT IGNORE INTO exercise (id, name, part, sport_type, difficulty, scene, equipment, contra_flags, video_url, cover_url, key_points, cautions, suitable_for, sort_no) VALUES
(2001, '开合跳',       'FULL_BODY',     'CARDIO',   1, 'HOME',    'NONE',     'KNEE,CARDIO',        'static/video/open-jump.mp4',        'static/cover/open-jump.jpg',        '落地时膝盖微屈缓冲，手臂在头顶合拢，节奏均匀。',         '膝关节不适或心肺较弱者不做；避免在硬地面长时间连续跳。', '需要快速提升心率的有氧入门者', 1),
(2002, '原地高抬腿',   'LEG',           'CARDIO',   1, 'HOME',    'NONE',     'KNEE,CARDIO',        'static/video/high-knee.mp4',        'static/cover/high-knee.jpg',        '大腿抬至与地面平行，核心收紧，前脚掌落地。',             '膝关节不适者降低抬腿高度或改为原地踏步。',               '居家有氧、热身激活', 2),
(2003, '深蹲',         'LEG',           'STRENGTH', 1, 'HOME',    'NONE',     'KNEE',               'static/video/squat.mp4',            'static/cover/squat.jpg',            '臀部向后坐，膝盖不超过脚尖，背部保持挺直。',             '膝关节疼痛时改为靠墙静蹲或半程深蹲。',                   '腿部力量入门训练', 3),
(2004, '箭步蹲',       'LEG',           'STRENGTH', 2, 'HOME',    'NONE',     'KNEE',               'static/video/lunge.mp4',            'static/cover/lunge.jpg',            '前后脚距离约一腿长，下蹲时两膝均呈 90 度。',             '膝关节不稳者不做；重心保持在前脚脚跟。',                 '腿部力量与平衡进阶', 4),
(2005, '臀桥',         'HIP',           'STRENGTH', 1, 'HOME',    'MAT',      NULL,                 'static/video/glute-bridge.mp4',     'static/cover/glute-bridge.jpg',     '脚跟发力顶髋，最高点停 1 秒，肩髋膝成一条线。',         '避免用腰部代偿，动作全程匀速。',                         '久坐人群、臀部激活', 5),
(2006, '跪姿俯卧撑',   'ARM',           'STRENGTH', 1, 'HOME',    'MAT',      'WRIST,SHOULDER',     'static/video/kneeling-push-up.mp4', 'static/cover/kneeling-push-up.jpg', '双手略宽于肩，肘部约 45 度，胸口贴近地面。',             '手腕或肩部不适者不做；核心收紧避免塌腰。',               '上肢力量入门（女性友好）', 6),
(2007, '标准俯卧撑',   'ARM',           'STRENGTH', 2, 'HOME',    'MAT',      'WRIST,SHOULDER',     'static/video/push-up.mp4',          'static/cover/push-up.jpg',          '身体成一条直线，下放至胸口离地一拳，推起时呼气。',       '手腕、肩部不适者改为跪姿俯卧撑。',                       '有一定基础的增肌塑形人群', 7),
(2008, '卷腹',         'ABDOMEN',       'STRENGTH', 1, 'HOME',    'MAT',      'WAIST',              'static/video/crunch.mp4',           'static/cover/crunch.jpg',           '下背贴地，用腹部发力卷起肩胛骨离开地面，颈部放松。',     '腰部不适者不做；不要用手抱头猛拉颈部。',                 '腹部塑形入门', 8),
(2009, '平板支撑',     'ABDOMEN',       'STRENGTH', 2, 'HOME',    'MAT',      'WAIST,SHOULDER',     'static/video/plank.mp4',            'static/cover/plank.jpg',            '肘关节在肩正下方，臀部不塌不翘，保持均匀呼吸。',         '腰部或肩部不适者不做，改用跪姿平板支撑。',               '核心稳定性训练', 9),
(2010, '登山跑',       'ABDOMEN',       'HIIT',     2, 'HOME',    'NONE',     'WRIST,KNEE',         'static/video/mountain-climber.mp4', 'static/cover/mountain-climber.jpg', '手臂垂直支撑，膝盖交替快速向胸口收，臀部保持稳定。',     '手腕、膝关节不适者不做；速度服从动作质量。',             '核心与心肺综合进阶', 10),
(2011, '波比跳',       'FULL_BODY',     'HIIT',     3, 'HOME',    'NONE',     'KNEE,CARDIO,SHOULDER','static/video/burpee.mp4',           'static/cover/burpee.jpg',           '下蹲撑地、后跳成平板、收腿起跳，全程连贯。',             '膝关节、心肺或肩部不适者不做；初学者去掉跳跃环节。',     '体能较好的燃脂挑战人群', 11),
(2012, '靠墙静蹲',     'LEG',           'STRENGTH', 1, 'HOME',    'NONE',     NULL,                 'static/video/wall-sit.mp4',         'static/cover/wall-sit.jpg',         '背部贴墙，大腿与地面平行，膝盖约 90 度，保持呼吸。',     '膝盖不超过脚尖；出现刺痛立即停止。',                     '膝关节康复期、腿部耐力入门', 12),
(2013, '侧卧抬腿',     'HIP',           'STRENGTH', 1, 'HOME',    'MAT',      NULL,                 'static/video/side-leg-raise.mp4',   'static/cover/side-leg-raise.jpg',   '侧卧身体成直线，上侧腿抬起时脚尖朝前，慢起慢落。',       '避免骨盆前后翻转，幅度以稳定为准。',                     '臀部塑形、髋部稳定', 13),
(2014, '站姿提踵',     'LEG',           'STRENGTH', 1, 'HOME',    'NONE',     NULL,                 'static/video/calf-raise.mp4',       'static/cover/calf-raise.jpg',       '前脚掌踩实，脚跟尽量抬高，顶点停 1 秒。',               '踝关节不适者减幅或扶墙完成。',                           '小腿线条、跑步辅助训练', 14),
(2015, '弹力带划船',   'BACK',          'STRENGTH', 2, 'HOME',    'BAND',     'SHOULDER',           'static/video/band-row.mp4',         'static/cover/band-row.jpg',         '肩胛骨先收紧再拉，肘部贴身向后，躯干保持稳定。',         '肩部不适者不做；弹力带固定点必须牢靠。',                 '改善圆肩驼背、背部力量', 15),
(2016, '哑铃俯身划船', 'BACK',          'STRENGTH', 2, 'HOME',    'DUMBBELL', 'WAIST,SHOULDER',     'static/video/dumbbell-row.mp4',     'static/cover/dumbbell-row.jpg',     '俯身约 45 度，背部挺直，哑铃拉至腹部两侧。',             '腰部不适者不做；重量从小到大循序渐进。',                 '背部增肌进阶', 16),
(2017, '哑铃弯举',     'ARM',           'STRENGTH', 2, 'HOME',    'DUMBBELL', 'WRIST',              'static/video/dumbbell-curl.mp4',    'static/cover/dumbbell-curl.jpg',    '大臂固定贴身，肘关节为轴心，下放时控制速度。',           '手腕不适者不做；避免用身体摆动借力。',                   '手臂塑形进阶', 17),
(2018, '肩颈拉伸',     'SHOULDER_NECK', 'STRETCH',  1, 'HOME',    'NONE',     NULL,                 'static/video/neck-stretch.mp4',     'static/cover/neck-stretch.jpg',     '头部侧屈找到轻微牵拉感，保持 20 秒后换侧。',             '出现头晕、手麻立即停止；不要用力按压颈椎。',             '久坐办公、肩颈紧张人群', 18),
(2019, '猫牛式拉伸',   'BACK',          'STRETCH',  1, 'HOME',    'MAT',      'WAIST',              'static/video/cat-cow.mp4',          'static/cover/cat-cow.jpg',          '四点支撑，随呼吸塌腰抬头与拱背低头交替。',               '腰部急性疼痛期不做；动作缓慢配合呼吸。',                 '腰背放松、训练后收操', 19),
(2020, '下肢拉伸',     'LEG',           'STRETCH',  1, 'HOME',    'MAT',      NULL,                 'static/video/leg-stretch.mp4',      'static/cover/leg-stretch.jpg',      '大腿前后侧分别拉伸，每侧保持 20-30 秒。',               '拉伸到轻微紧绷即可，不要弹震式拉伸。',                   '跑步、腿部训练后放松', 20),
(2021, '慢跑',         'LEG',           'RUNNING',  1, 'OUTDOOR', 'NONE',     'KNEE,CARDIO',        'static/video/jogging.mp4',          'static/cover/jogging.jpg',          '身体稍前倾，步幅自然，呼吸与步频配合。',                 '膝关节或心肺不适者改为快走；注意跑前热身。',             '有氧减脂、提升心肺', 21),
(2022, '跳绳',         'FULL_BODY',     'CARDIO',   2, 'OUTDOOR', 'NONE',     'KNEE,CARDIO',        'static/video/jump-rope.mp4',        'static/cover/jump-rope.jpg',        '手腕摇绳，前脚掌轻落地，跳起高度 3-5 厘米。',           '膝关节不适者不做；宿舍环境注意噪音与层高。',             '高效有氧燃脂', 22),
(2023, '瑜伽拜日式',   'FULL_BODY',     'YOGA',     2, 'HOME',    'MAT',      'SHOULDER,WRIST',     'static/video/sun-salutation.mp4',   'static/cover/sun-salutation.jpg',   '动作跟随呼吸串联，前屈时脊柱延展而非弓背。',             '肩腕不适者减少支撑类体式；经期避免倒置体式。',           '柔韧性与体态改善', 23),
(2024, '徒手深蹲跳',   'LEG',           'HIIT',     3, 'HOME',    'NONE',     'KNEE,CARDIO',        'static/video/squat-jump.mp4',       'static/cover/squat-jump.jpg',       '深蹲后爆发起跳，落地时屈膝缓冲，连续进行。',             '膝关节不适者不做；落地要轻，避免关节锁死。',             '下肢爆发力挑战训练', 24);

-- ------------------------------------------------------------
-- 演示用户建档数据（档案 + 两次身体数据 + 一次体能测试）
-- 说明：刻意设置为“膝盖不适 + 宿舍 + 不能跳 + 无器械”，用于演示排除式推荐
-- ------------------------------------------------------------
INSERT IGNORE INTO user_profile (id, user_id, gender, age, height_cm, weight_kg, body_type, primary_goal, sub_goals, focus_parts, sport_types, weekly_days, daily_minutes, remind_enabled, remind_time, injuries, train_limits, profile_completed) VALUES
(4001, 2, 'MALE', 28, 175.0, 78.0, 'APPLE', 'LOSE_FAT', 'CARDIO,DAILY_FITNESS', 'ABDOMEN,LEG', 'CARDIO,STRENGTH,STRETCH', '1,3,5', 30, 1, '20:30', 'KNEE', 'DORM,NO_JUMP,NO_EQUIPMENT', 1),
(4002, 3, 'FEMALE', 24, 163.0, 52.0, 'PEAR', 'BUILD_MUSCLE', 'POSTURE', 'HIP,ARM', 'STRENGTH,STRETCH', '2,4,6', 40, 0, NULL, 'NONE', 'HOME', 1);

INSERT IGNORE INTO body_data (id, user_id, height_cm, weight_kg, bmi, body_type, score, remark, recorded_at) VALUES
(5001, 2, 175.0, 78.0, 25.5, 'APPLE', 62, '首次建档数据',   '2026-07-01 09:00:00'),
(5002, 2, 175.0, 76.5, 25.0, 'APPLE', 68, '两周后复测',     '2026-07-15 09:00:00'),
(5003, 3, 163.0, 52.0, 19.6, 'PEAR',  74, '首次建档数据',   '2026-07-05 20:00:00');

INSERT IGNORE INTO fitness_test (id, user_id, push_up, squat, crunch, stair_status, level, tested_at) VALUES
(6001, 2, 15, 25, 20, 'SLIGHT_HARD', 'L2', '2026-07-01 09:10:00'),
(6002, 3, 5,  30, 25, 'EASY',        'L2', '2026-07-05 20:10:00');
