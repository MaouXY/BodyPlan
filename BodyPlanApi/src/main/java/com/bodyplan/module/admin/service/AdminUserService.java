package com.bodyplan.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bodyplan.common.exception.BusinessException;
import com.bodyplan.common.exception.ErrorCode;
import com.bodyplan.common.result.PageResult;
import com.bodyplan.module.admin.dto.user.AdminBodyDataBrief;
import com.bodyplan.module.admin.dto.user.AdminCheckinStatsBrief;
import com.bodyplan.module.admin.dto.user.AdminFitnessTestBrief;
import com.bodyplan.module.admin.dto.user.AdminPlanBrief;
import com.bodyplan.module.admin.dto.user.AdminProfileBrief;
import com.bodyplan.module.admin.dto.user.AdminUserDetailResponse;
import com.bodyplan.module.admin.dto.user.AdminUserItemResponse;
import com.bodyplan.module.fitness.entity.FitnessTest;
import com.bodyplan.module.fitness.mapper.FitnessTestMapper;
import com.bodyplan.module.profile.entity.BodyData;
import com.bodyplan.module.profile.entity.UserProfile;
import com.bodyplan.module.profile.mapper.BodyDataMapper;
import com.bodyplan.module.profile.mapper.UserProfileMapper;
import com.bodyplan.module.record.entity.CheckinRecord;
import com.bodyplan.module.record.mapper.CheckinRecordMapper;
import com.bodyplan.module.training.entity.TrainingPlan;
import com.bodyplan.module.training.mapper.TrainingPlanMapper;
import com.bodyplan.module.user.entity.UserAccount;
import com.bodyplan.module.user.mapper.UserAccountMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端用户管理服务：用户列表（含建档与打卡概览）、启用/禁用、用户详情聚合。
 * 全部使用 MyBatis-Plus 条件构造器与 Java 内聚合，不写自定义 SQL。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    /** 用户详情中身体数据历史最多返回条数 */
    private static final int BODY_DATA_HISTORY_LIMIT = 10;

    /** 用户详情中打卡统计的读取上限，避免单用户历史过大 */
    private static final int CHECKIN_SCAN_LIMIT = 2000;

    private final UserAccountMapper userAccountMapper;
    private final UserProfileMapper userProfileMapper;
    private final BodyDataMapper bodyDataMapper;
    private final FitnessTestMapper fitnessTestMapper;
    private final TrainingPlanMapper trainingPlanMapper;
    private final CheckinRecordMapper checkinRecordMapper;

    /**
     * 分页查询用户列表。
     *
     * @param page    页码，从 1 开始
     * @param size    每页条数
     * @param keyword 模糊匹配 username / nickname / phone，可空
     * @param status  状态过滤，可空（1 启用、0 禁用）
     */
    public PageResult<AdminUserItemResponse> listUsers(Long page, Long size, String keyword, Integer status) {
        long current = AdminPageSupport.normalizePage(page);
        long pageSize = AdminPageSupport.normalizeSize(size);
        if (status != null) {
            AdminEnumValidator.validateStatus(status, "状态");
        }
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        LambdaQueryWrapper<UserAccount> wrapper = new LambdaQueryWrapper<UserAccount>()
                .eq(status != null, UserAccount::getStatus, status)
                .and(trimmedKeyword != null, condition -> condition
                        .like(UserAccount::getUsername, trimmedKeyword)
                        .or()
                        .like(UserAccount::getNickname, trimmedKeyword)
                        .or()
                        .like(UserAccount::getPhone, trimmedKeyword))
                .orderByDesc(UserAccount::getCreatedAt)
                .orderByDesc(UserAccount::getId);

        Page<UserAccount> userPage = userAccountMapper.selectPage(new Page<>(current, pageSize), wrapper);
        List<AdminUserItemResponse> items = buildUserItems(userPage.getRecords());
        log.info("admin user list queried, scenario=admin-user-list, page={}, size={}, total={}, keywordPresent={}, status={}",
                current, pageSize, userPage.getTotal(), trimmedKeyword != null, status);
        return PageResult.of(items, current, pageSize, userPage.getTotal());
    }

    /**
     * 启用 / 禁用用户。
     *
     * @param userId 用户ID
     * @param status 1 启用、0 禁用
     */
    public AdminUserItemResponse updateStatus(Long userId, Integer status) {
        AdminEnumValidator.validateStatus(status, "状态");
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            log.warn("admin user status update failed, scenario=admin-user-status, adminUserId={}, reason=user-not-found", userId);
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        UserAccount updating = new UserAccount();
        updating.setId(userId);
        updating.setStatus(status);
        userAccountMapper.updateById(updating);

        UserAccount updated = userAccountMapper.selectById(userId);
        AdminUserItemResponse item = buildUserItems(List.of(updated)).get(0);
        log.info("admin user status updated, scenario=admin-user-status, userId={}, status={}", userId, status);
        return item;
    }

    /**
     * 用户详情聚合：基础信息 + 建档要点 + 身体数据 + 体能测试 + 计划 + 打卡统计。
     *
     * @param userId 用户ID
     */
    public AdminUserDetailResponse detail(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            log.warn("admin user detail failed, scenario=admin-user-detail, adminUserId={}, reason=user-not-found", userId);
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        AdminUserItemResponse userItem = buildUserItems(List.of(user)).get(0);
        UserProfile profile = selectProfile(userId);
        List<BodyData> bodyDataHistory = selectBodyDataHistory(userId, BODY_DATA_HISTORY_LIMIT);

        AdminUserDetailResponse response = new AdminUserDetailResponse(
                userItem,
                toProfileBrief(profile),
                bodyDataHistory.isEmpty() ? null : toBodyDataBrief(bodyDataHistory.get(0)),
                bodyDataHistory.stream().map(this::toBodyDataBrief).toList(),
                toFitnessTestBrief(selectLatestFitnessTest(userId)),
                toPlanBrief(selectCurrentPlan(userId)),
                buildCheckinStats(selectCheckinRecords(userId))
        );
        log.info("admin user detail queried, scenario=admin-user-detail, adminUserId={}, profileCompleted={}, bodyDataCount={}, checkinCount={}",
                userId, userItem.profileCompleted(), bodyDataHistory.size(), response.checkinStats().totalCount());
        return response;
    }

    /**
     * 批量构建用户列表元素，避免循环单条查询。
     */
    public List<AdminUserItemResponse> buildUserItems(List<UserAccount> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = users.stream().map(UserAccount::getId).filter(Objects::nonNull).distinct().toList();
        Map<Long, UserProfile> profileMap = selectProfiles(userIds);
        Map<Long, CheckinSummary> summaryMap = selectCheckinSummaries(userIds);

        return users.stream()
                .map(user -> {
                    UserProfile profile = profileMap.get(user.getId());
                    CheckinSummary summary = summaryMap.getOrDefault(user.getId(), CheckinSummary.empty());
                    boolean completed = profile != null && Integer.valueOf(1).equals(profile.getProfileCompleted());
                    return new AdminUserItemResponse(
                            user.getId(),
                            user.getUsername(),
                            user.getNickname(),
                            user.getPhone(),
                            user.getRole(),
                            user.getStatus(),
                            user.getCreatedAt(),
                            completed,
                            profile == null ? null : profile.getPrimaryGoal(),
                            summary.lastCheckinDate(),
                            summary.count()
                    );
                })
                .toList();
    }

    /**
     * 查询多个用户的建档档案（仅正常数据）。
     */
    private Map<Long, UserProfile> selectProfiles(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<UserProfile> profiles = userProfileMapper.selectList(new LambdaQueryWrapper<UserProfile>()
                .in(UserProfile::getUserId, userIds));
        Map<Long, UserProfile> profileMap = new HashMap<>();
        for (UserProfile profile : profiles) {
            profileMap.putIfAbsent(profile.getUserId(), profile);
        }
        return profileMap;
    }

    private UserProfile selectProfile(Long userId) {
        return selectProfiles(List.of(userId)).get(userId);
    }

    /**
     * 查询多个用户的打卡概览（打卡总数 + 最近打卡日期）。
     */
    private Map<Long, CheckinSummary> selectCheckinSummaries(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<CheckinRecord> records = checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .in(CheckinRecord::getUserId, userIds)
                .select(CheckinRecord::getUserId, CheckinRecord::getTrainingDate));

        Map<Long, Long> countMap = new HashMap<>();
        Map<Long, LocalDate> latestMap = new HashMap<>();
        for (CheckinRecord record : records) {
            Long userId = record.getUserId();
            if (userId == null) {
                continue;
            }
            countMap.merge(userId, 1L, Long::sum);
            LocalDate trainingDate = record.getTrainingDate();
            if (trainingDate != null && (latestMap.get(userId) == null || trainingDate.isAfter(latestMap.get(userId)))) {
                latestMap.put(userId, trainingDate);
            }
        }
        Map<Long, CheckinSummary> summaryMap = new HashMap<>();
        for (Long userId : countMap.keySet()) {
            summaryMap.put(userId, new CheckinSummary(countMap.get(userId), latestMap.get(userId)));
        }
        return summaryMap;
    }

    private List<BodyData> selectBodyDataHistory(Long userId, int limit) {
        return bodyDataMapper.selectList(new LambdaQueryWrapper<BodyData>()
                .eq(BodyData::getUserId, userId)
                .orderByDesc(BodyData::getRecordedAt)
                .orderByDesc(BodyData::getId)
                .last("LIMIT " + limit));
    }

    private FitnessTest selectLatestFitnessTest(Long userId) {
        List<FitnessTest> tests = fitnessTestMapper.selectList(new LambdaQueryWrapper<FitnessTest>()
                .eq(FitnessTest::getUserId, userId)
                .orderByDesc(FitnessTest::getTestedAt)
                .orderByDesc(FitnessTest::getId)
                .last("LIMIT 1"));
        return tests.isEmpty() ? null : tests.get(0);
    }

    /**
     * 读取用户当前计划：只认 ACTIVE。
     * 一个用户同时只有一条 ACTIVE 计划（重新生成时旧计划会被置为 REPLACED），
     * 因此这里不回退到历史计划，避免管理端把已替换的计划误显示成“当前计划”。
     */
    private TrainingPlan selectCurrentPlan(Long userId) {
        List<TrainingPlan> activePlans = trainingPlanMapper.selectList(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getUserId, userId)
                .eq(TrainingPlan::getStatus, "ACTIVE")
                .orderByDesc(TrainingPlan::getCreatedAt)
                .orderByDesc(TrainingPlan::getId)
                .last("LIMIT 1"));
        return activePlans.isEmpty() ? null : activePlans.get(0);
    }

    private List<CheckinRecord> selectCheckinRecords(Long userId) {
        return checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .orderByDesc(CheckinRecord::getTrainingDate)
                .orderByDesc(CheckinRecord::getId)
                .last("LIMIT " + CHECKIN_SCAN_LIMIT));
    }

    private AdminCheckinStatsBrief buildCheckinStats(List<CheckinRecord> records) {
        if (records.isEmpty()) {
            return new AdminCheckinStatsBrief(0L, false, null, 0);
        }
        LocalDate today = LocalDate.now();
        Set<LocalDate> dates = new HashSet<>();
        LocalDate latest = null;
        for (CheckinRecord record : records) {
            LocalDate trainingDate = record.getTrainingDate();
            if (trainingDate == null) {
                continue;
            }
            dates.add(trainingDate);
            if (latest == null || trainingDate.isAfter(latest)) {
                latest = trainingDate;
            }
        }
        boolean todayChecked = dates.contains(today);
        return new AdminCheckinStatsBrief(
                (long) records.size(),
                todayChecked,
                latest,
                AdminCheckinSupport.currentStreak(dates, today));
    }

    private AdminProfileBrief toProfileBrief(UserProfile profile) {
        if (profile == null) {
            return null;
        }
        return new AdminProfileBrief(
                profile.getGender(),
                profile.getAge(),
                profile.getHeightCm(),
                profile.getWeightKg(),
                profile.getBodyType(),
                profile.getPrimaryGoal(),
                AdminCheckinSupport.splitToArray(profile.getFocusParts()),
                AdminCheckinSupport.splitToArray(profile.getSportTypes()),
                AdminCheckinSupport.splitToArray(profile.getWeeklyDays()),
                profile.getDailyMinutes(),
                AdminCheckinSupport.splitToArray(profile.getInjuries()),
                AdminCheckinSupport.splitToArray(profile.getTrainLimits())
        );
    }

    private AdminBodyDataBrief toBodyDataBrief(BodyData bodyData) {
        return new AdminBodyDataBrief(
                bodyData.getId(),
                bodyData.getHeightCm(),
                bodyData.getWeightKg(),
                bodyData.getBmi(),
                bodyData.getBodyType(),
                bodyData.getScore(),
                bodyData.getRemark(),
                bodyData.getRecordedAt()
        );
    }

    private AdminFitnessTestBrief toFitnessTestBrief(FitnessTest test) {
        if (test == null) {
            return null;
        }
        return new AdminFitnessTestBrief(
                test.getId(),
                test.getPushUp(),
                test.getSquat(),
                test.getCrunch(),
                test.getStairStatus(),
                test.getLevel(),
                test.getTestedAt()
        );
    }

    private AdminPlanBrief toPlanBrief(TrainingPlan plan) {
        if (plan == null) {
            return null;
        }
        return new AdminPlanBrief(
                plan.getId(),
                plan.getName(),
                plan.getGoal(),
                plan.getLevel(),
                plan.getDaysPerWeek(),
                plan.getMinutesPerSession(),
                plan.getStartDate(),
                plan.getStatus(),
                plan.getReason()
        );
    }

    /**
     * 批量读取用户账号（供身体数据列表等其它管理端列表复用），返回 id -> 账号 的映射。
     */
    public Map<Long, UserAccount> selectAccounts(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<UserAccount> accounts = userAccountMapper.selectList(new LambdaQueryWrapper<UserAccount>()
                .in(UserAccount::getId, userIds));
        return accounts.stream().collect(Collectors.toMap(UserAccount::getId, Function.identity(), (left, right) -> left));
    }

    /** 用户打卡概览的内部结构 */
    private record CheckinSummary(Long count, LocalDate lastCheckinDate) {

        static CheckinSummary empty() {
            return new CheckinSummary(0L, null);
        }
    }
}
