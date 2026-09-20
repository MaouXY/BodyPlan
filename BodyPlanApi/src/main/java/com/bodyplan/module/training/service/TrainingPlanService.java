package com.bodyplan.module.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bodyplan.common.enums.UserRole;
import com.bodyplan.common.exception.BusinessException;
import com.bodyplan.common.exception.ErrorCode;
import com.bodyplan.common.security.CurrentUser;
import com.bodyplan.common.security.CurrentUserProvider;
import com.bodyplan.module.fitness.entity.FitnessTest;
import com.bodyplan.module.fitness.mapper.FitnessTestMapper;
import com.bodyplan.module.profile.entity.UserProfile;
import com.bodyplan.module.profile.mapper.UserProfileMapper;
import com.bodyplan.module.training.dto.PlanDayResponse;
import com.bodyplan.module.training.dto.PlanGenerateRequest;
import com.bodyplan.module.training.dto.PlanItemResponse;
import com.bodyplan.module.training.dto.RecommendReasonResponse;
import com.bodyplan.module.training.dto.TrainingPlanResponse;
import com.bodyplan.module.training.dto.TrainingPlanSummaryResponse;
import com.bodyplan.module.training.entity.RecommendLog;
import com.bodyplan.module.training.entity.TrainingPlan;
import com.bodyplan.module.training.entity.TrainingPlanItem;
import com.bodyplan.module.training.mapper.RecommendLogMapper;
import com.bodyplan.module.training.mapper.TrainingPlanItemMapper;
import com.bodyplan.module.training.mapper.TrainingPlanMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 训练计划：生成（调用推荐引擎）、当前计划、详情、历史与推荐依据。
 * 一个用户同时只有一条 ACTIVE 计划；force=true 时把旧计划置为 REPLACED 后重新生成（复测调整场景）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingPlanService {

    private static final String PLAN_STATUS_ACTIVE = "ACTIVE";
    private static final String PLAN_STATUS_REPLACED = "REPLACED";
    private static final String ITEM_STATUS_PENDING = "PENDING";
    private static final String ITEM_STATUS_DONE = "DONE";
    private static final int HISTORY_LIMIT = 20;
    private static final String[] WEEKDAY_LABELS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private static final Map<String, String> GOAL_LABELS = Map.of(
            "LOSE_FAT", "减脂塑形",
            "BUILD_MUSCLE", "增肌塑形",
            "CARDIO", "心肺提升",
            "POSTURE", "体态改善",
            "LOCAL_STRENGTH", "局部强化",
            "DAILY_FITNESS", "日常活力"
    );

    private final TrainingPlanMapper trainingPlanMapper;
    private final TrainingPlanItemMapper trainingPlanItemMapper;
    private final RecommendLogMapper recommendLogMapper;
    private final UserProfileMapper userProfileMapper;
    private final FitnessTestMapper fitnessTestMapper;
    private final PlanRecommendationService planRecommendationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public TrainingPlanResponse generate(PlanGenerateRequest request) {
        CurrentUser currentUser = CurrentUserProvider.require();
        Long userId = currentUser.getId();
        UserProfile profile = requireCompletedProfile(userId);
        FitnessTest fitnessTest = requireLatestFitnessTest(userId);

        String goal = request != null && StringUtils.hasText(request.goal()) ? request.goal() : profile.getPrimaryGoal();
        if (!StringUtils.hasText(goal)) {
            throw new BusinessException(ErrorCode.PROFILE_INCOMPLETE, "请先在建档问卷中选择训练目标");
        }

        boolean force = request != null && Boolean.TRUE.equals(request.force());
        TrainingPlan activePlan = findActivePlan(userId);
        if (activePlan != null && !force) {
            log.warn("plan generate rejected, scenario=plan-generate, reason=active-plan-exists, userId={}, planId={}",
                    userId, activePlan.getId());
            throw new BusinessException(ErrorCode.PLAN_ALREADY_ACTIVE, "已有执行中的计划，如需重新生成请传 force=true");
        }
        if (activePlan != null) {
            activePlan.setStatus(PLAN_STATUS_REPLACED);
            activePlan.setUpdatedAt(LocalDateTime.now());
            trainingPlanMapper.updateById(activePlan);
            log.info("previous active plan replaced, scenario=plan-generate, userId={}, replacedPlanId={}",
                    userId, activePlan.getId());
        }

        PlanRecommendationService.RecommendationPlan recommendation =
                planRecommendationService.recommend(profile, fitnessTest, goal);

        LocalDate startDate = request != null && request.startDate() != null ? request.startDate() : mondayOfCurrentWeek();
        LocalDateTime timestamp = LocalDateTime.now();

        TrainingPlan plan = new TrainingPlan();
        plan.setUserId(userId);
        plan.setName(buildPlanName(goal, recommendation.days().size(), fitnessTest.getLevel()));
        plan.setGoal(goal);
        plan.setLevel(StringUtils.hasText(fitnessTest.getLevel()) ? fitnessTest.getLevel() : "L1");
        plan.setDaysPerWeek(recommendation.days().size());
        plan.setMinutesPerSession(profile.getDailyMinutes() == null ? 30 : profile.getDailyMinutes());
        plan.setStartDate(startDate);
        plan.setEndDate(startDate.plusWeeks(PlanRecommendationService.PLAN_WEEKS).minusDays(1));
        plan.setStatus(PLAN_STATUS_ACTIVE);
        plan.setReason(recommendation.reason());
        plan.setCreatedAt(timestamp);
        plan.setUpdatedAt(timestamp);
        plan.setDeleted(0);
        trainingPlanMapper.insert(plan);

        List<TrainingPlanItem> items = new ArrayList<>();
        // 计划按 PLAN_WEEKS 周循环排期：每周的同一 weekday 重复一次，
        // 否则计划只有第一周有任务，第二周起“今日任务”会全空。
        for (int week = 0; week < PlanRecommendationService.PLAN_WEEKS; week++) {
            for (PlanRecommendationService.RecommendedDay day : recommendation.days()) {
                int sortNo = 0;
                for (PlanRecommendationService.RecommendedItem item : day.items()) {
                    TrainingPlanItem entity = new TrainingPlanItem();
                    entity.setPlanId(plan.getId());
                    entity.setDayIndex(day.dayIndex());
                    entity.setTrainingDate(startDate.plusDays(day.dayIndex() - 1L).plusWeeks(week));
                    entity.setExerciseId(item.exercise().getId());
                    entity.setExerciseName(item.exercise().getName());
                    entity.setSets(item.sets());
                    entity.setReps(item.reps());
                    entity.setDurationSec(item.durationSec());
                    entity.setRestSec(item.restSec());
                    entity.setSortNo(++sortNo);
                    entity.setStatus(ITEM_STATUS_PENDING);
                    entity.setCreatedAt(timestamp);
                    entity.setUpdatedAt(timestamp);
                    trainingPlanItemMapper.insert(entity);
                    items.add(entity);
                }
            }
        }

        RecommendLog recommendLog = new RecommendLog();
        recommendLog.setUserId(userId);
        recommendLog.setPlanId(plan.getId());
        recommendLog.setInputSnapshot(writeJson(recommendation.inputSnapshot()));
        recommendLog.setMatchedRule(recommendation.matchedRule());
        recommendLog.setExcludedCount(recommendation.excludedCount());
        recommendLog.setReason(recommendation.reason());
        recommendLog.setCreatedAt(timestamp);
        recommendLogMapper.insert(recommendLog);

        log.info("training plan generated, scenario=plan-generate, userId={}, planId={}, goal={}, level={}, daysPerWeek={}, itemCount={}, excludedCount={}, matchedRule={}",
                userId, plan.getId(), goal, plan.getLevel(), plan.getDaysPerWeek(), items.size(),
                recommendation.excludedCount(), recommendation.matchedRule());

        return toResponse(plan, items);
    }

    public TrainingPlanResponse current() {
        CurrentUser currentUser = CurrentUserProvider.require();
        TrainingPlan plan = findActivePlan(currentUser.getId());
        if (plan == null) {
            return null;
        }
        return toResponse(plan, listItems(plan.getId()));
    }

    public TrainingPlanResponse detail(Long planId) {
        CurrentUser currentUser = CurrentUserProvider.require();
        TrainingPlan plan = trainingPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ErrorCode.PLAN_NOT_FOUND, "训练计划不存在");
        }
        if (!plan.getUserId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            log.warn("plan detail denied, scenario=plan-detail, userId={}, planId={}, reason=not-owner",
                    currentUser.getId(), planId);
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看他人的训练计划");
        }
        return toResponse(plan, listItems(plan.getId()));
    }

    public List<TrainingPlanSummaryResponse> history() {
        CurrentUser currentUser = CurrentUserProvider.require();
        List<TrainingPlan> plans = trainingPlanMapper.selectList(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getUserId, currentUser.getId())
                .orderByDesc(TrainingPlan::getCreatedAt)
                .orderByDesc(TrainingPlan::getId)
                .last("limit " + HISTORY_LIMIT));
        return plans.stream().map(this::toSummary).toList();
    }

    public RecommendReasonResponse currentReason() {
        CurrentUser currentUser = CurrentUserProvider.require();
        TrainingPlan plan = findActivePlan(currentUser.getId());
        if (plan == null) {
            throw new BusinessException(ErrorCode.PLAN_NOT_FOUND, "当前没有执行中的训练计划");
        }
        RecommendLog recommendLog = recommendLogMapper.selectOne(new LambdaQueryWrapper<RecommendLog>()
                .eq(RecommendLog::getUserId, currentUser.getId())
                .eq(RecommendLog::getPlanId, plan.getId())
                .orderByDesc(RecommendLog::getCreatedAt)
                .orderByDesc(RecommendLog::getId)
                .last("limit 1"));
        if (recommendLog == null) {
            throw new BusinessException(ErrorCode.PLAN_NOT_FOUND, "暂无该计划的推荐记录");
        }
        return new RecommendReasonResponse(
                recommendLog.getPlanId(),
                recommendLog.getReason(),
                recommendLog.getMatchedRule(),
                recommendLog.getExcludedCount(),
                recommendLog.getInputSnapshot(),
                recommendLog.getCreatedAt()
        );
    }

    private TrainingPlan findActivePlan(Long userId) {
        return trainingPlanMapper.selectOne(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getUserId, userId)
                .eq(TrainingPlan::getStatus, PLAN_STATUS_ACTIVE)
                .orderByDesc(TrainingPlan::getCreatedAt)
                .orderByDesc(TrainingPlan::getId)
                .last("limit 1"));
    }

    private List<TrainingPlanItem> listItems(Long planId) {
        return trainingPlanItemMapper.selectList(new LambdaQueryWrapper<TrainingPlanItem>()
                .eq(TrainingPlanItem::getPlanId, planId)
                .orderByAsc(TrainingPlanItem::getDayIndex)
                .orderByAsc(TrainingPlanItem::getSortNo)
                .orderByAsc(TrainingPlanItem::getId));
    }

    private UserProfile requireCompletedProfile(Long userId) {
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("limit 1"));
        if (profile == null || profile.getProfileCompleted() == null || profile.getProfileCompleted() != 1) {
            log.warn("plan generate rejected, scenario=plan-generate, reason=profile-incomplete, userId={}", userId);
            throw new BusinessException(ErrorCode.PROFILE_INCOMPLETE, "请先完成建档问卷再生成训练计划");
        }
        return profile;
    }

    private FitnessTest requireLatestFitnessTest(Long userId) {
        List<FitnessTest> tests = fitnessTestMapper.selectList(new LambdaQueryWrapper<FitnessTest>()
                .eq(FitnessTest::getUserId, userId)
                .orderByDesc(FitnessTest::getTestedAt)
                .orderByDesc(FitnessTest::getId)
                .last("limit 1"));
        if (tests.isEmpty()) {
            log.warn("plan generate rejected, scenario=plan-generate, reason=fitness-test-missing, userId={}", userId);
            throw new BusinessException(ErrorCode.FITNESS_TEST_REQUIRED, "请先完成体能测试再生成训练计划");
        }
        return tests.get(0);
    }

    /**
     * 计划按“周”排布，起始日固定取本周周一：这样 day_index（1-7 与周几对应）换算出的训练日期，
     * 就能和“今日打卡”的自然日期对齐，避免计划刚生成时今天却没有任务。
     */
    private LocalDate mondayOfCurrentWeek() {
        return LocalDate.now().with(DayOfWeek.MONDAY);
    }

    private String buildPlanName(String goal, int daysPerWeek, String level) {
        String goalLabel = GOAL_LABELS.getOrDefault(goal, "个性化训练");
        String levelLabel = StringUtils.hasText(level) ? level : "L1";
        return "%s · %d 天/周 · %s".formatted(goalLabel, daysPerWeek, levelLabel);
    }

    private TrainingPlanResponse toResponse(TrainingPlan plan, List<TrainingPlanItem> items) {
        // 按“具体训练日期”分组：计划跨多周，同一天只出现一次
        Map<LocalDate, List<TrainingPlanItem>> grouped = items.stream()
                .filter(item -> item.getTrainingDate() != null)
                .collect(Collectors.groupingBy(TrainingPlanItem::getTrainingDate, LinkedHashMap::new, Collectors.toList()));
        List<PlanDayResponse> days = new ArrayList<>();
        grouped.forEach((trainingDate, dayItems) -> days.add(new PlanDayResponse(
                dayItems.get(0).getDayIndex(),
                trainingDate,
                weekdayLabel(trainingDate),
                dayItems.stream().map(this::toItemResponse).toList()
        )));
        days.sort((left, right) -> left.trainingDate().compareTo(right.trainingDate()));

        int doneItems = (int) items.stream()
                .filter(item -> ITEM_STATUS_DONE.equals(item.getStatus()))
                .count();
        return new TrainingPlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getGoal(),
                plan.getLevel(),
                plan.getDaysPerWeek(),
                plan.getMinutesPerSession(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getStatus(),
                plan.getReason(),
                plan.getCreatedAt(),
                items.size(),
                doneItems,
                days
        );
    }

    private PlanItemResponse toItemResponse(TrainingPlanItem item) {
        return new PlanItemResponse(
                item.getId(),
                item.getDayIndex(),
                item.getTrainingDate(),
                item.getExerciseId(),
                item.getExerciseName(),
                item.getSets(),
                item.getReps(),
                item.getDurationSec(),
                item.getRestSec(),
                item.getSortNo(),
                item.getStatus()
        );
    }

    private TrainingPlanSummaryResponse toSummary(TrainingPlan plan) {
        return new TrainingPlanSummaryResponse(
                plan.getId(),
                plan.getName(),
                plan.getGoal(),
                plan.getLevel(),
                plan.getDaysPerWeek(),
                plan.getMinutesPerSession(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getStatus(),
                plan.getCreatedAt()
        );
    }

    private String weekdayLabel(Integer dayIndex) {
        if (dayIndex == null || dayIndex < 1 || dayIndex > WEEKDAY_LABELS.length) {
            return null;
        }
        return WEEKDAY_LABELS[dayIndex - 1];
    }

    /** 由具体日期推导周几（多周计划里同一天只对应一个日期） */
    private String weekdayLabel(LocalDate trainingDate) {
        if (trainingDate == null) {
            return null;
        }
        return WEEKDAY_LABELS[trainingDate.getDayOfWeek().getValue() - 1];
    }

    private String writeJson(Map<String, Object> snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            log.error("recommend input snapshot json write failed, scenario=plan-recommend-json", exception);
            return "{}";
        }
    }
}
