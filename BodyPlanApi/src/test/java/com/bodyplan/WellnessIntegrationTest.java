package com.bodyplan;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.bodyplan.common.security.CurrentUser;
import com.bodyplan.common.security.JwtTokenService;
import com.bodyplan.common.security.LoginType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

// 使用本地已迁移的数据库；每个用例事务回滚，不给演示账号留下测试记录。
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WellnessIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    long student, other, coach, foreignCoach, plan, item, food;
    String userToken, otherToken, coachToken, foreignToken, adminToken;

    @BeforeEach void setup() {
        student = account("USER"); other = account("USER"); coach = account("COACH"); foreignCoach = account("COACH");
        userToken = token(student, "USER"); otherToken = token(other, "USER");
        coachToken = token(coach, "COACH"); foreignToken = token(foreignCoach, "COACH"); adminToken = token(1, "ADMIN");
        jdbc.update("INSERT INTO coach_student (student_id,coach_id,assigned_by) VALUES (?,?,1)", student, coach);
        plan = IdWorker.getId(); item = IdWorker.getId(); food = IdWorker.getId();
        jdbc.update("""
                INSERT INTO training_plan (id,user_id,name,goal,level,days_per_week,minutes_per_session,start_date,end_date,status)
                VALUES (?,?,'测试计划','DAILY_FITNESS','L1',7,20,CURDATE(),CURDATE(),'ACTIVE')
                """, plan, student);
        jdbc.update("""
                INSERT INTO training_plan_item (id,plan_id,day_index,training_date,exercise_id,exercise_name,sets,reps)
                VALUES (?,?,1,CURDATE(),1,'测试任务',2,12)
                """, item, plan);
        jdbc.update("""
                INSERT INTO nutrition_food (id,name,category,kcal,protein,fat,carbohydrate,fiber,source)
                VALUES (?,'事务测试米饭','主食',116,2.6,0.3,25.9,0.3,'集成测试，事务回滚')
                """, food);
    }
    long account(String role) {
        long id = IdWorker.getId();
        jdbc.update("INSERT INTO user_account (id,username,password_hash,nickname,role,status) VALUES (?,?,?,'集成测试',?,1)",
                id, "test_" + id, "not-a-login-password", role);
        return id;
    }
    String token(long id, String role) { return tokens.generateToken(CurrentUser.builder().principalId(id).subject("integration").loginType(LoginType.ACCOUNT).roleCode(role).build()); }
    JsonNode call(MockHttpServletRequestBuilder request, String token, Object body, int status) throws Exception {
        request.header("Authorization", "Bearer " + token);
        if (body != null) request.contentType("application/json").content(body instanceof String ? (String) body : json.writeValueAsString(body));
        var response = mvc.perform(request).andReturn().getResponse();
        assertEquals(status, response.getStatus(), response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        return json.readTree(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    @Test void roleBoundaryIsEnforcedByRealSecurityChain() throws Exception {
        call(get("/api/v1/admin/coaches"), userToken, null, 403);
        call(get("/api/v1/admin/foods"), coachToken, null, 403);
        call(get("/api/v1/coach/students").param("date", java.time.LocalDate.now().toString()), userToken, null, 403);
        assertEquals(0, call(get("/api/v1/admin/coaches"), adminToken, null, 200).path("code").asInt());
    }
    @Test void disabledDeletedOrRoleChangedAccountsLoseAccessImmediately() throws Exception {
        jdbc.update("UPDATE user_account SET status=0 WHERE id=?", coach);
        call(get("/api/v1/coach/students"), coachToken, null, 401);
        jdbc.update("UPDATE user_account SET status=1,role='USER' WHERE id=?", coach);
        call(get("/api/v1/coach/students"), coachToken, null, 401);
        jdbc.update("UPDATE user_account SET deleted=1 WHERE id=?", student);
        call(get("/api/v1/messages"), userToken, null, 401);
    }
    @Test void adminUserIdsPreserveSnowflakePrecision() throws Exception {
        var result = call(get("/api/v1/admin/users").param("keyword", "test_" + coach), adminToken, null, 200);
        var row = result.path("data").path("items").get(0);
        assertTrue(row.path("id").isTextual());
        assertEquals(Long.toString(coach), row.path("id").asText());
    }
    @Test void coachCannotReadOtherStudentsOrRoutes() throws Exception {
        assertEquals(0, call(get("/api/v1/coach/students/" + student).param("date", java.time.LocalDate.now().toString()), coachToken, null, 200).path("code").asInt());
        assertEquals(403, call(get("/api/v1/coach/students/" + student).param("date", java.time.LocalDate.now().toString()), foreignToken, null, 200).path("code").asInt());
        var list = call(get("/api/v1/coach/students").param("date", java.time.LocalDate.now().toString()), coachToken, null, 200).path("data");
        assertEquals(1, list.size());
        assertEquals(Long.toString(student), list.get(0).path("studentId").asText());
        assertTrue(list.get(0).path("studentId").isTextual());
    }
    @Test void remindersReachOnlyRecipientAndHaveCooldown() throws Exception {
        String id = call(post("/api/v1/coach/students/" + student + "/reminders"), coachToken, "{\"content\":\"训练提醒测试\"}", 200).path("data").asText();
        var inbox = call(get("/api/v1/messages"), userToken, null, 200).path("data");
        assertEquals(1, inbox.path("unreadCount").asInt());
        assertEquals("训练提醒测试", inbox.path("items").get(0).path("content").asText());
        assertEquals(429, call(post("/api/v1/coach/students/" + student + "/reminders"), coachToken, "{}", 200).path("code").asInt());
        assertEquals(404, call(put("/api/v1/messages/" + id + "/read"), otherToken, null, 200).path("code").asInt());
        assertEquals(0, call(put("/api/v1/messages/" + id + "/read"), userToken, null, 200).path("code").asInt());
        assertEquals(0, call(get("/api/v1/messages"), userToken, null, 200).path("data").path("unreadCount").asInt());
    }
    @Test void completedOrRestDayCannotBeNagged() throws Exception {
        jdbc.update("INSERT INTO checkin_record (id,user_id,plan_id,plan_item_id,training_date,finished) VALUES (?,?,?,?,CURDATE(),1)",
                IdWorker.getId(),student,plan,item);
        assertEquals(409, call(post("/api/v1/coach/students/" + student + "/reminders"), coachToken, "{}", 200).path("code").asInt());
        jdbc.update("DELETE FROM training_plan_item WHERE id=?", item);
        assertEquals(409, call(post("/api/v1/coach/students/" + student + "/reminders"), coachToken, "{}", 200).path("code").asInt());
    }
    @Test void dietScalesSnapshotsAndChecksOwnership() throws Exception {
        String today = java.time.LocalDate.now().toString();
        String body = "{\"entryDate\":\""+today+"\",\"meal\":\"LUNCH\",\"foodId\":\""+food+"\",\"grams\":150}";
        String id = call(post("/api/v1/nutrition/entries"),userToken,body,200).path("data").asText();
        assertEquals(0, call(get("/api/v1/nutrition/day").param("date",today),userToken,null,200).path("code").asInt());
        jdbc.update("UPDATE nutrition_food SET kcal=200 WHERE id=?", food);
        var day = call(get("/api/v1/nutrition/day").param("date",today),userToken,null,200).path("data");
        assertEquals(174, day.path("totals").path("kcal").asDouble());
        assertEquals(404,call(delete("/api/v1/nutrition/entries/"+id),otherToken,null,200).path("code").asInt());
        assertEquals(0,call(put("/api/v1/nutrition/entries/"+id),userToken,body.replace("\"grams\":150","\"grams\":100"),200).path("code").asInt());
        assertEquals(200,call(get("/api/v1/nutrition/day").param("date",today),userToken,null,200).path("data").path("totals").path("kcal").asDouble());
        assertEquals(0,call(delete("/api/v1/nutrition/entries/"+id),userToken,null,200).path("code").asInt());
    }
    @Test void targetsAreSavedPerDate() throws Exception {
        String today = java.time.LocalDate.now().toString();
        String body = "{\"targetDate\":\""+today+"\",\"goal\":\"LOSE_FAT\",\"kcal\":1800,\"protein\":100,\"fat\":60,\"carbohydrate\":200,\"fiber\":25}";
        assertEquals(0, call(put("/api/v1/nutrition/target"),userToken,body,200).path("code").asInt());
        assertEquals(1800,call(get("/api/v1/nutrition/day").param("date",today),userToken,null,200).path("data").path("target").path("kcal").asInt());
        assertTrue(call(get("/api/v1/nutrition/day").param("date",java.time.LocalDate.now().minusDays(1).toString()),userToken,null,200).path("data").path("target").isMissingNode());
    }
    @Test void sportSaveIsIdempotentAndPrivate() throws Exception {
        long end = System.currentTimeMillis() - 1000, start = end - 10000;
        String body = """
                {"clientRecordId":"integration-12345678","sportType":"RUN","trainingMode":"AEROBIC","startedAt":%d,"endedAt":%d,"durationSec":10,
                "points":[{"latitude":0,"longitude":0,"timestampMs":%d,"segment":0,"accuracy":5},
                {"latitude":0,"longitude":0.001,"timestampMs":%d,"segment":0,"accuracy":5}]}
                """.formatted(start,end,start,end);
        var first = call(post("/api/v1/sport-sessions"), userToken, body,200);
        assertEquals(0, first.path("code").asInt(), first.toString());
        String id = first.path("data").path("id").asText();
        assertEquals(111.32,first.path("data").path("distanceM").asDouble(),0.2);
        assertEquals(id,call(post("/api/v1/sport-sessions"),userToken,body,200).path("data").path("id").asText());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM sport_session WHERE user_id=?",Integer.class,student));
        assertEquals(409,call(post("/api/v1/sport-sessions"),userToken,body.replace("\"durationSec\":10","\"durationSec\":9"),200).path("code").asInt());
        assertEquals(404,call(get("/api/v1/sport-sessions/"+id),otherToken,null,200).path("code").asInt());
        assertEquals(404,call(get("/api/v1/sport-sessions/"+id),coachToken,null,200).path("code").asInt());
        assertEquals(2,call(get("/api/v1/sport-sessions/"+id),userToken,null,200).path("data").path("points").size());
    }
}
