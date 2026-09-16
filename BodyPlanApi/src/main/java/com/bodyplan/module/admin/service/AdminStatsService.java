package com.bodyplan.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bodyplan.common.exception.BusinessException;
import com.bodyplan.common.exception.ErrorCode;
import com.bodyplan.module.admin.dto.stats.AdminCheckinRankingItem;
import com.bodyplan.module.admin.dto.stats.AdminCheckinTrendPoint;
import com.bodyplan.module.admin.dto.stats.AdminOverviewResponse;
import com.bodyplan.module.profile.entity.UserProfile;
import com.bodyplan.module.profile.mapper.UserProfileMapper;
import com.bodyplan.module.record.entity.CheckinRecord;
import com.bodyplan.module.record.mapper.CheckinRecordMapper;
import com.bodyplan.module.training.entity.TrainingPlan;
import com.bodyplan.module.training.entity.TrainingPlanItem;
import com.bodyplan.module.training.mapper.ExerciseMapper;
import com.bodyplan.module.training.mapper.ExerciseTagMapper;
import com.bodyplan.module.training.mapper.TrainingPlanItemMapper;
import com.bodyplan.module.training.mapper.TrainingPlanMapper;
import com.bodyplan.module.user.entity.UserAccount;
import com.bodyplan.module.user.mapper.UserAccountMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 管理端打卡数据统计服务。
 * 全部基于 MyBatis-Plus 的 selectCount / selectList 在 Java 内聚合（数据量小，避免自定义 SQL）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    /** 趋势接口允许的最长时间跨度（天） */
    private static final int TREND_MAX_DAYS = 365;

    /** 排行榜默认条数 */
    private static final int RANKING_DEFAULT_LIMIT = 10;

    /** 排行榜最大条数 */
    private static final int RANKING_MAX_LIMIT = 50;

    /** 计划明细状态：已完成 */
    private static final String PLAN_ITEM_DONE = "DONE";

    /** 计划明细状态：未完成 */
    private static final String PLAN_ITEM_PENDING = "PENDING";

    private final UserAccountMapper userAccountMapper;
    private final UserProfileMapper userProfileMapper;
    private final TrainingPlanMapper trainingPlanMapper;
    private final TrainingPlanItemMapper trainingPlanItemMapper;
    private final ExerciseMapper exerciseMapper;
    private final ExerciseTagMapper exerciseTagMapper;
    private final CheckinRecordMapper checkinRecordMapper;

    /**
     * 数据概览。
     */
    public AdminOverviewResponse overview() {
        LocalDate today = LocalDate.now();

        long userTotal = userAccountMapper.selectCount(new LambdaQueryWrapper<>());
        long profileCompletedTotal = userProfileMapper.selectCount(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getProfileCompleted, 1));
        long planTotal = trainingPlanMapper.selectCount(new LambdaQueryWrapper<>());
        long activePlanTotal = trainingPlanMapper.selectCount(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getStatus, "ACTIVE"));
        long exerciseTotal = exerciseMapper.selectCount(new LambdaQueryWrapper<>());
        long exerciseTagTotal = exerciseTagMapper.selectCount(new LambdaQueryWrapper<>());
        long checkinTotal = checkinRecordMapper.selectCount(new LambdaQueryWrapper<>());
        long todayCheckinTotal = checkinRecordMapper.selectCount(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getTrainingDate, today));

        List<CheckinRecord> todayRecords = checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getTrainingDate, today)
                .select(CheckinRecord::getUserId));
        long activeUserTodayTotal = todayRecords.stream()
                .map(CheckinRecord::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        long planItemDoneTotal = trainingPlanItemMapper.selectCount(new LambdaQueryWrapper<TrainingPlanItem>()
                .eq(TrainingPlanItem::getStatus, PLAN_ITEM_DONE));
        long planItemPendingTotal = trainingPlanItemMapper.selectCount(new LambdaQueryWrapper<TrainingPlanItem>()
                .eq(TrainingPlanItem::getStatus, PLAN_ITEM_PENDING));

        AdminOverviewResponse response = new AdminOverviewResponse(
                userTotal,
                profileCompletedTotal,
                planTotal,
                activePlanTotal,
                exerciseTotal,
                exerciseTagTotal,
                checkinTotal,
                todayCheckinTotal,
                activeUserTodayTotal,
                planItemDoneTotal,
                planItemPendingTotal
        );
        log.info("admin overview queried, scenario=admin-stats-overview, userTotal={}, planTotal={}, checkinTotal={}, activeUserTodayTotal={}",
                userTotal, planTotal, checkinTotal, activeUserTodayTotal);
        return response;
    }

    /**
     * 打卡趋势：按日期升序返回连续日期，无数据的日期补 0。
     *
     * @param days 最近天数（包含今天），默认 14、最大 365
     */
    public List<AdminCheckinTrendPoint> checkinTrend(Integer days) {
        int range = days == null ? 14 : days;
        if (range < 1 || range > TREND_MAX_DAYS) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "天数取值范围：1-" + TREND_MAX_DAYS);
        }
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(range - 1L);
        LocalDate endDateExclusive = today.plusDays(1);

        Map<LocalDate, Long> checkinCountMap = new LinkedHashMap<>();
        Map<LocalDate, Set<Long>> activeUserMap = new LinkedHashMap<>();
        for (int offset = 0; offset < range; offset++) {
            LocalDate date = startDate.plusDays(offset);
            checkinCountMap.put(date, 0L);
            activeUserMap.put(date, new HashSet<>());
        }

        List<CheckinRecord> records = checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .ge(CheckinRecord::getTrainingDate, startDate)
                .lt(CheckinRecord::getTrainingDate, endDateExclusive)
                .select(CheckinRecord::getUserId, CheckinRecord::getTrainingDate));
        for (CheckinRecord record : records) {
            LocalDate date = record.getTrainingDate();
            if (date == null) {
                continue;
            }
            checkinCountMap.merge(date, 1L, Long::sum);
            Set<Long> userSet = activeUserMap.get(date);
            if (userSet != null && record.getUserId() != null) {
                userSet.add(record.getUserId());
            }
        }

        // 该区间内的注册用户数按自然日归集
        Map<LocalDate, Long> newUserCountMap = new LinkedHashMap<>();
        for (LocalDate date : checkinCountMap.keySet()) {
            newUserCountMap.put(date, 0L);
        }
        List<UserAccount> users = userAccountMapper.selectList(new LambdaQueryWrapper<UserAccount>()
                .ge(UserAccount::getCreatedAt, startDate.atStartOfDay())
                .lt(UserAccount::getCreatedAt, endDateExclusive.atStartOfDay())
                .select(UserAccount::getId, UserAccount::getCreatedAt));
        for (UserAccount user : users) {
            LocalDate date = resolveRegisterDate(user.getCreatedAt());
            if (date != null && newUserCountMap.containsKey(date)) {
                newUserCountMap.merge(date, 1L, Long::sum);
            }
        }

        List<AdminCheckinTrendPoint> points = new ArrayList<>(range);
        for (Map.Entry<LocalDate, Long> entry : checkinCountMap.entrySet()) {
            LocalDate date = entry.getKey();
            points.add(new AdminCheckinTrendPoint(
                    date.toString(),
                    entry.getValue(),
                    (long) activeUserMap.getOrDefault(date, Set.of()).size(),
                    newUserCountMap.getOrDefault(date, 0L)
            ));
        }
        log.info("admin checkin trend queried, scenario=admin-stats-checkin-trend, days={}, startDate={}, pointCount={}",
                range, startDate, points.size());
        return points;
    }

    /**
     * 打卡排行榜：按打卡总数倒序，limit 默认 10、最大 50。
     */
    public List<AdminCheckinRankingItem> checkinRanking(Integer limit) {
        int size = limit == null ? RANKING_DEFAULT_LIMIT : limit;
        if (size < 1 || size > RANKING_MAX_LIMIT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "排行榜条数取值范围：1-" + RANKING_MAX_LIMIT);
        }

        List<CheckinRecord> records = checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .select(CheckinRecord::getUserId, CheckinRecord::getTrainingDate));

        Map<Long, Long> countMap = new HashMap<>();
        Map<Long, LocalDate> latestDateMap = new HashMap<>();
        Map<Long, Set<LocalDate>> dateSetMap = new HashMap<>();
        for (CheckinRecord record : records) {
            Long userId = record.getUserId();
            if (userId == null) {
                continue;
            }
            countMap.merge(userId, 1L, Long::sum);
            LocalDate trainingDate = record.getTrainingDate();
            if (trainingDate == null) {
                continue;
            }
            LocalDate latest = latestDateMap.get(userId);
            if (latest == null || trainingDate.isAfter(latest)) {
                latestDateMap.put(userId, trainingDate);
            }
            dateSetMap.computeIfAbsent(userId, key -> new HashSet<>()).add(trainingDate);
        }

        List<Long> rankedUserIds = countMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(size)
                .map(Map.Entry::getKey)
                .toList();

        Map<Long, UserAccount> accountMap = rankedUserIds.isEmpty()
                ? Map.of()
                : selectAccountMap(rankedUserIds);

        LocalDate today = LocalDate.now();
        List<AdminCheckinRankingItem> items = rankedUserIds.stream()
                .map(userId -> {
                    UserAccount account = accountMap.get(userId);
                    return new AdminCheckinRankingItem(
                            userId,
                            account == null ? null : account.getUsername(),
                            account == null ? null : account.getNickname(),
                            countMap.getOrDefault(userId, 0L),
                            latestDateMap.get(userId),
                            AdminCheckinSupport.currentStreak(dateSetMap.getOrDefault(userId, Set.of()), today)
                    );
                })
                .toList();

        log.info("admin checkin ranking queried, scenario=admin-stats-checkin-ranking, limit={}, countedUsers={}, itemCount={}",
                size, countMap.size(), items.size());
        return items;
    }

    private Map<Long, UserAccount> selectAccountMap(List<Long> userIds) {
        List<UserAccount> accounts = userAccountMapper.selectList(new LambdaQueryWrapper<UserAccount>()
                .in(UserAccount::getId, userIds));
        Map<Long, UserAccount> accountMap = new HashMap<>();
        for (UserAccount account : accounts) {
            accountMap.put(account.getId(), account);
        }
        return accountMap;
    }

    /** 注册时间归集到自然日 */
    private LocalDate resolveRegisterDate(LocalDateTime createdAt) {
        return createdAt == null ? null : createdAt.toLocalDate();
    }
}
