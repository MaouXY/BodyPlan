package com.bodyplan.module.coaching;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.bodyplan.common.enums.UserRole;
import com.bodyplan.common.exception.BusinessException;
import com.bodyplan.common.exception.ErrorCode;
import com.bodyplan.common.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoachService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;

    public List<Map<String, Object>> coaches() {
        return jdbc.queryForList("""
                SELECT CAST(id AS CHAR) AS id,username,nickname,status FROM user_account
                WHERE role='COACH' AND deleted=0 ORDER BY created_at DESC,id DESC
                """);
    }

    @Transactional
    public String createCoach(CoachModels.CreateCoach request) {
        long id = IdWorker.getId();
        try {
            jdbc.update("""
                    INSERT INTO user_account (id,username,password_hash,nickname,role,status,deleted)
                    VALUES (?,?,?,?,'COACH',1,0)
                    """, id, request.username().trim(), passwords.encode(request.password()), request.nickname().trim());
        } catch (DuplicateKeyException exception) {
            log.warn("coach username duplicate, scenario=coach-create, operatorId={}, username={}",
                    CurrentUserProvider.require().getId(), request.username(), exception);
            throw new BusinessException(ErrorCode.CONFLICT, "账号已存在");
        }
        log.info("coach created, scenario=coach-create, operatorId={}, coachId={}", CurrentUserProvider.require().getId(), id);
        return Long.toString(id);
    }

    public List<Map<String, Object>> candidates(String keyword) {
        return jdbc.queryForList("""
                SELECT CAST(u.id AS CHAR) AS id,u.username,u.nickname,CAST(cs.coach_id AS CHAR) AS coachId,
                c.nickname AS coachName FROM user_account u
                LEFT JOIN coach_student cs ON cs.student_id=u.id
                LEFT JOIN user_account c ON c.id=cs.coach_id AND c.deleted=0
                WHERE u.role='USER' AND u.deleted=0 AND u.status=1 AND (u.username LIKE ? OR u.nickname LIKE ?)
                ORDER BY u.id LIMIT 200
                """, "%" + keyword.trim() + "%", "%" + keyword.trim() + "%");
    }

    @Transactional
    public void assign(CoachModels.Assign request) {
        var student = jdbc.queryForList("SELECT id FROM user_account WHERE id=? AND role='USER' AND deleted=0 AND status=1 FOR UPDATE", request.studentId());
        if (student.isEmpty()) throw new BusinessException(ErrorCode.NOT_FOUND, "学员不存在或已停用");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM user_account WHERE id=? AND role='COACH' AND status=1 AND deleted=0",
                Integer.class, request.coachId()) == 0) throw new BusinessException(ErrorCode.NOT_FOUND, "教练不存在或已停用");
        jdbc.update("""
                INSERT INTO coach_student (student_id,coach_id,assigned_by) VALUES (?,?,?)
                ON DUPLICATE KEY UPDATE coach_id=VALUES(coach_id),assigned_by=VALUES(assigned_by),assigned_at=CURRENT_TIMESTAMP
                """, request.studentId(), request.coachId(), CurrentUserProvider.require().getId());
        log.info("student assigned, scenario=coach-assign, operatorId={}, coachId={}, studentId={}",
                CurrentUserProvider.require().getId(), request.coachId(), request.studentId());
    }

    @Transactional
    public void unassign(long studentId) {
        jdbc.queryForList("SELECT id FROM user_account WHERE id=? FOR UPDATE", studentId);
        jdbc.update("DELETE FROM coach_student WHERE student_id=?", studentId);
        log.info("student unassigned, scenario=coach-unassign, operatorId={}, studentId={}", CurrentUserProvider.require().getId(), studentId);
    }

    public List<Map<String, Object>> students(LocalDate date) {
        var user = CurrentUserProvider.require();
        String owner = user.getRole() == UserRole.ADMIN ? "" : " AND cs.coach_id=? ";
        String sql = """
                SELECT CAST(u.id AS CHAR) AS studentId,u.username,u.nickname,CAST(cs.coach_id AS CHAR) AS coachId,
                c.nickname AS coachName,p.primary_goal AS goal,
                (SELECT COUNT(*) FROM training_plan_item i JOIN training_plan tp ON tp.id=i.plan_id
                 WHERE tp.user_id=u.id AND tp.deleted=0 AND tp.status='ACTIVE' AND i.training_date=?) AS totalCount,
                (SELECT COUNT(DISTINCT i.id) FROM training_plan_item i JOIN training_plan tp ON tp.id=i.plan_id
                 JOIN checkin_record cr ON cr.plan_item_id=i.id AND cr.user_id=u.id AND cr.training_date=i.training_date AND cr.deleted=0 AND cr.finished=1
                 WHERE tp.user_id=u.id AND tp.deleted=0 AND tp.status='ACTIVE' AND i.training_date=?) AS doneCount
                FROM coach_student cs JOIN user_account u ON u.id=cs.student_id AND u.deleted=0 AND u.status=1
                JOIN user_account c ON c.id=cs.coach_id AND c.deleted=0
                LEFT JOIN user_profile p ON p.user_id=u.id AND p.deleted=0
                WHERE 1=1
                """ + owner + " ORDER BY u.nickname,u.id LIMIT 500";
        return user.getRole() == UserRole.ADMIN ? jdbc.queryForList(sql, date, date)
                : jdbc.queryForList(sql, date, date, user.getId());
    }

    public Map<String, Object> student(long studentId, LocalDate date) {
        requireStudent(studentId);
        var tasks = jdbc.queryForList("""
                SELECT CAST(i.id AS CHAR) AS id,i.exercise_name AS name,i.sets,i.reps,i.duration_sec AS durationSec,
                EXISTS(SELECT 1 FROM checkin_record cr WHERE cr.plan_item_id=i.id AND cr.user_id=tp.user_id
                AND cr.training_date=i.training_date AND cr.deleted=0 AND cr.finished=1) AS done
                FROM training_plan_item i JOIN training_plan tp ON tp.id=i.plan_id
                WHERE tp.user_id=? AND tp.deleted=0 AND tp.status='ACTIVE' AND i.training_date=? ORDER BY i.sort_no,i.id
                """, studentId, date);
        long from = date.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
        long to = date.plusDays(1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
        var sport = jdbc.queryForList("""
                SELECT sport_type AS sportType,distance_m AS distanceM,duration_sec AS durationSec,started_at AS startedAt
                FROM sport_session WHERE user_id=? AND started_at>=? AND started_at<? ORDER BY started_at DESC
                """, studentId, from, to);
        var diet = jdbc.queryForMap("""
                SELECT COALESCE(SUM(kcal),0) AS kcal,COALESCE(SUM(protein),0) AS protein,COUNT(*) AS entryCount
                FROM diet_entry WHERE user_id=? AND entry_date=?
                """, studentId, date);
        return Map.of("tasks", tasks, "sports", sport, "diet", diet, "date", date);
    }

    @Transactional
    public String remind(long studentId, String content) {
        long senderId = CurrentUserProvider.require().getId();
        // 与分配操作锁定同一学员行，避免转教练和发送提醒发生越权竞态。
        jdbc.queryForList("SELECT id FROM user_account WHERE id=? FOR UPDATE", studentId);
        requireStudent(studentId);
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        Map<String, Object> snapshot = student(studentId, date);
        @SuppressWarnings("unchecked") List<Map<String, Object>> tasks = (List<Map<String, Object>>) snapshot.get("tasks");
        long done = tasks.stream().filter(t -> Boolean.TRUE.equals(t.get("done")) || "1".equals(String.valueOf(t.get("done")))).count();
        if (tasks.isEmpty() || done == tasks.size())
            throw new BusinessException(ErrorCode.CONFLICT, "今日没有待完成的计划任务，无需提醒");
        int recent = jdbc.queryForObject("""
                SELECT COUNT(*) FROM user_message WHERE user_id=? AND
                (created_at>DATE_SUB(NOW(),INTERVAL 1 MINUTE))
                """, Integer.class, studentId);
        int daily = jdbc.queryForObject("SELECT COUNT(*) FROM user_message WHERE user_id=? AND task_date=?", Integer.class, studentId, date);
        if (recent > 0 || daily >= 5) {
            log.warn("reminder limited, scenario=coach-remind, coachId={}, studentId={}, recent={}, daily={}", senderId, studentId, recent, daily);
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "提醒过于频繁，请稍后再试（每天最多5次）");
        }
        String text = content == null || content.isBlank()
                ? "今天已完成 " + done + "/" + tasks.size() + " 项训练，有空记得完成剩余任务。"
                : content.trim();
        long id = IdWorker.getId();
        jdbc.update("INSERT INTO user_message (id,user_id,sender_id,task_date,title,content) VALUES (?,?,?,?,?,?)",
                id, studentId, senderId, date, "今日训练提醒", text);
        log.info("reminder created, scenario=coach-remind, coachId={}, studentId={}, messageId={}, taskDate={}, defaultContent={}",
                senderId, studentId, id, date, content == null || content.isBlank());
        return Long.toString(id);
    }

    private void requireStudent(long studentId) {
        var user = CurrentUserProvider.require();
        int count = user.getRole() == UserRole.ADMIN
                ? jdbc.queryForObject("SELECT COUNT(*) FROM coach_student WHERE student_id=?", Integer.class, studentId)
                : jdbc.queryForObject("""
                    SELECT COUNT(*) FROM coach_student cs JOIN user_account u ON u.id=cs.student_id
                    WHERE cs.student_id=? AND cs.coach_id=? AND u.status=1 AND u.deleted=0
                    """, Integer.class, studentId, user.getId());
        if (count == 0) {
            log.warn("student access denied, scenario=coach-student-access, operatorId={}, studentId={}", user.getId(), studentId);
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看或提醒该学员");
        }
    }

    public Map<String, Object> messages() {
        long userId = CurrentUserProvider.require().getId();
        var items = jdbc.queryForList("""
                SELECT CAST(m.id AS CHAR) AS id,m.title,m.content,m.task_date AS taskDate,m.is_read AS isRead,
                m.created_at AS createdAt,u.nickname AS senderName
                FROM user_message m LEFT JOIN user_account u ON u.id=m.sender_id
                WHERE m.user_id=? ORDER BY m.is_read,m.created_at DESC,m.id DESC LIMIT 200
                """, userId);
        Integer unread = jdbc.queryForObject("SELECT COUNT(*) FROM user_message WHERE user_id=? AND is_read=0", Integer.class, userId);
        return Map.of("items", items, "unreadCount", unread == null ? 0 : unread);
    }

    public void readMessage(long id) {
        long userId = CurrentUserProvider.require().getId();
        if (jdbc.queryForObject("SELECT COUNT(*) FROM user_message WHERE id=? AND user_id=?", Integer.class, id, userId) == 0)
            throw new BusinessException(ErrorCode.NOT_FOUND, "消息不存在");
        jdbc.update("UPDATE user_message SET is_read=1,read_at=COALESCE(read_at,NOW()) WHERE id=? AND user_id=?", id, userId);
        log.info("message read, scenario=message-read, userId={}, messageId={}", userId, id);
    }
}
