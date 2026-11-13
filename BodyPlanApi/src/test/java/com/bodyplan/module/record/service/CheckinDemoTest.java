package com.bodyplan.module.record.service;

import com.bodyplan.common.exception.BusinessException;
import com.bodyplan.common.exception.ErrorCode;
import com.bodyplan.common.security.CurrentUser;
import com.bodyplan.module.record.dto.CheckinSaveRequest;
import com.bodyplan.module.record.entity.CheckinRecord;
import com.bodyplan.module.record.mapper.CheckinRecordMapper;
import com.bodyplan.module.training.entity.TrainingPlan;
import com.bodyplan.module.training.entity.Exercise;
import com.bodyplan.module.training.mapper.ExerciseMapper;
import com.bodyplan.module.training.entity.TrainingPlanItem;
import com.bodyplan.module.training.mapper.TrainingPlanItemMapper;
import com.bodyplan.module.training.mapper.TrainingPlanMapper;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CheckinDemoTest {
    private final CheckinRecordMapper records = mock(CheckinRecordMapper.class);
    private final TrainingPlanMapper plans = mock(TrainingPlanMapper.class);
    private final TrainingPlanItemMapper items = mock(TrainingPlanItemMapper.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ExerciseMapper exercises = mock(ExerciseMapper.class);
    private final CheckinService service = new CheckinService(records, plans, items, jdbc, exercises);

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                CurrentUser.builder().principalId(42L).roleCode("USER").build(), null, List.of()));
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void restDayFreeTrainingPreservesActualFeedback() {
        when(plans.selectList(any())).thenReturn(List.of());
        var result = service.create(new CheckinSaveRequest(null, null, null, 8, "HARD", "实际训练反馈"));
        assertNull(result.planItemId());
        assertEquals(LocalDate.now(), result.trainingDate());
        assertEquals(8, result.durationMin());
        assertEquals("HARD", result.feeling());
        assertEquals("实际训练反馈", result.remark());
        verify(records).insert(any(CheckinRecord.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void duplicateFreeTrainingIsRejected() {
        var existing = new CheckinRecord();
        existing.setId(9L);
        existing.setDeleted(0);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of(existing));
        var failure = assertThrows(BusinessException.class,
                () -> service.create(new CheckinSaveRequest(null, null, null, 8, "NORMAL", null)));
        assertEquals(ErrorCode.CHECKIN_ALREADY_EXISTS, failure.getErrorCode());
        verify(records, never()).insert(any(CheckinRecord.class));
    }

    @Test
    void futurePlanItemCannotBeCheckedIn() {
        var plan = new TrainingPlan();
        plan.setId(100L);
        plan.setUserId(42L);
        var item = new TrainingPlanItem();
        item.setId(200L);
        item.setPlanId(100L);
        item.setTrainingDate(LocalDate.now().plusDays(1));
        when(items.selectById(200L)).thenReturn(item);
        when(plans.selectById(100L)).thenReturn(plan);
        var failure = assertThrows(BusinessException.class,
                () -> service.create(new CheckinSaveRequest(200L, null, null, 8, "NORMAL", null)));
        assertEquals(ErrorCode.CHECKIN_DATE_IN_FUTURE, failure.getErrorCode());
        verify(records, never()).insert(any(CheckinRecord.class));
    }

    @Test
    void expiredScheduleDoesNotAdvertisePastDateAsNextTrainingDay() {
        var plan = new TrainingPlan();
        plan.setId(100L);
        plan.setUserId(42L);
        var item = new TrainingPlanItem();
        item.setId(200L);
        item.setTrainingDate(LocalDate.now().minusDays(1));
        when(plans.selectList(any())).thenReturn(List.of(plan));
        when(items.selectList(any())).thenReturn(List.of(item));
        var result = service.todayTask();
        assertFalse(result.hasTaskToday());
        assertNull(result.nextTrainingDate());
    }

    @Test
    void freeExerciseIsValidatedAndNameIsSnapshotted() {
        availableExercise(7L, "慢跑");
        var result = service.createFree(new CheckinSaveRequest(null, null, null, 12, "NORMAL", "户外", 7L));
        assertEquals(7L, result.exerciseId());
        assertEquals("慢跑", result.exerciseName());
        assertEquals("户外", result.remark());
        assertNull(result.planItemId());
        verify(records).insert(argThat((CheckinRecord row) -> row.getExerciseId().equals(7L)
                && row.getExerciseName().equals("慢跑")));
    }

    @Test
    void freeExerciseMustBeSelectedAndDurationMustBeValid() {
        assertThrows(BusinessException.class, () -> service.createFree(
                new CheckinSaveRequest(null, null, null, 12, "NORMAL", null)));
        assertThrows(BusinessException.class, () -> service.createFree(
                new CheckinSaveRequest(null, null, null, 0, "NORMAL", null, 7L)));
        assertThrows(BusinessException.class, () -> service.createFree(
                new CheckinSaveRequest(99L, null, null, 12, "NORMAL", null, 7L)));
        verifyNoInteractions(records, exercises);
    }

    @Test
    void unavailableExerciseCannotBeRecorded() {
        var hidden = availableExercise(7L, "慢跑");
        hidden.setStatus(0);
        var failure = assertThrows(BusinessException.class, () -> service.createFree(
                new CheckinSaveRequest(null, null, null, 12, "NORMAL", null, 7L)));
        assertEquals(ErrorCode.EXERCISE_NOT_FOUND, failure.getErrorCode());
        verify(records, never()).insert(any(CheckinRecord.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void revivedFreeRecordReplacesOldExerciseSnapshot() {
        availableExercise(7L, "慢跑");
        var old = new CheckinRecord();
        old.setId(9L);
        old.setDeleted(1);
        old.setExerciseId(2L);
        old.setExerciseName("卷腹");
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of(old));
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        var result = service.createFree(new CheckinSaveRequest(null, null, null, 12, "NORMAL", null, 7L));
        assertEquals(7L, result.exerciseId());
        assertEquals("慢跑", result.exerciseName());
        verify(jdbc).update(contains("exercise_id = ?"), nullable(Long.class), eq(12), eq("NORMAL"),
                isNull(), eq(7L), eq("慢跑"), any(java.sql.Timestamp.class), eq(9L));
        verify(records, never()).insert(any(CheckinRecord.class));
    }

    @Test
    void planCheckinUsesPlanSnapshotInsteadOfMutableCatalogName() {
        var plan = new TrainingPlan();
        plan.setId(100L);
        plan.setUserId(42L);
        var item = new TrainingPlanItem();
        item.setId(200L);
        item.setPlanId(100L);
        item.setExerciseId(7L);
        item.setExerciseName("计划动作快照");
        item.setTrainingDate(LocalDate.now());
        when(items.selectById(200L)).thenReturn(item);
        when(plans.selectById(100L)).thenReturn(plan);
        var result = service.create(new CheckinSaveRequest(200L, null, null, 8, "NORMAL", null));
        assertEquals("计划动作快照", result.exerciseName());
        assertEquals(7L, result.exerciseId());
        verifyNoInteractions(exercises);
        assertThrows(BusinessException.class, () -> service.create(
                new CheckinSaveRequest(200L, null, null, 8, "NORMAL", null, 99L)));
    }

    private Exercise availableExercise(Long id, String name) {
        var exercise = new Exercise();
        exercise.setId(id);
        exercise.setName(name);
        exercise.setStatus(1);
        when(exercises.selectById(id)).thenReturn(exercise);
        return exercise;
    }
}
