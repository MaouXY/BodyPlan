package com.bodyplan.module.record.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bodyplan.common.enums.UserRole;
import com.bodyplan.common.exception.BusinessException;
import com.bodyplan.common.exception.ErrorCode;
import com.bodyplan.common.result.PageResult;
import com.bodyplan.common.security.CurrentUser;
import com.bodyplan.common.security.CurrentUserProvider;
import com.bodyplan.module.record.dto.CheckinCalendarDayResponse;
import com.bodyplan.module.record.dto.CheckinCalendarResponse;
import com.bodyplan.module.record.dto.CheckinResponse;
import com.bodyplan.module.record.dto.CheckinSaveRequest;
import com.bodyplan.module.record.dto.CheckinStreakResponse;
import com.bodyplan.module.record.dto.TodayTaskItemResponse;
import com.bodyplan.module.record.dto.TodayTaskResponse;
import com.bodyplan.module.record.entity.CheckinRecord;
import com.bodyplan.module.record.mapper.CheckinRecordMapper;
import com.bodyplan.module.training.entity.TrainingPlan;
import com.bodyplan.module.training.entity.TrainingPlanItem;
import com.bodyplan.module.training.mapper.TrainingPlanItemMapper;
import com.bodyplan.module.training.mapper.TrainingPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * 打卡记录业务：今日任务、打卡、撤销打卡、打卡日历、连续打卡、打卡历史分页。
 *
 * <p>幂等设计：打卡唯一键是 (user_id, plan_item_id, training_date)。写库前先按该三元组查重，
 * 再依赖数据库唯一键 uk_checkin_item_date 兜底，并发重复提交时把 DuplicateKeyException 同样转成
 * CHECKIN_ALREADY_EXISTS，两道保证结果一致。</p>
 *
 * <p>撤销后可以重新打卡：查重时会连"已撤销（deleted=1）"的记录一起看，
 * 命中活记录才算重复打卡；命中的是撤销记录时不新增行，而是把原记录复活（deleted=0 并覆盖本次入参），
 * 否则唯一键（包含逻辑删除的行）会让用户"撤销了却打不了卡"。</p>
 *
 * <p>注意：MySQL 唯一索引对 NULL 不去重，(user_id, plan_item_id, training_date) 在 plan_item_id 为空
 * （自由训练）时不具备去重能力，所以"自由训练一天一次"完全由本服务层的查重保证。</p>
 *
 * <p>日期语义统一用 LocalDate（DATE 列），createdAt 由本服务显式赋值，
 * 因为表字段是 DEFAULT CURRENT_TIMESTAMP，MyBatis-Plus 插入后不会回填。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckinService {

    /** 计划状态：执行中（打卡只认 ACTIVE 计划） */
    private static final String PLAN_STATUS_ACTIVE = "ACTIVE";

    /** 明细状态：未完成 */
    private static final String ITEM_STATUS_PENDING = "PENDING";

    /** 明细状态：已完成 */
    private static final String ITEM_STATUS_DONE = "DONE";

    /** 打卡是否完成计划任务：1 是（打卡即视为完成每日任务） */
    private static final int FINISHED_YES = 1;

    /** 训练感受可选值 */
    private static final Set<String> FEELING_OPTIONS = Set.of("EASY", "NORMAL", "HARD");

    /** 训练感受中文提示 */
    private static final String FEELING_MESSAGE = "feeling 只能为 EASY / NORMAL / HARD";

    /** 备注最大长度，与 checkin_record.remark varchar(255) 对齐 */
    private static final int REMARK_MAX_LENGTH = 255;

    /** 分页每页最大条数，防止一次拉全表 */
    private static final long MAX_PAGE_SIZE = 100L;

    /** 月份格式 yyyy-MM */
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 查同键打卡记录（带明细）。故意不写 deleted = 0：需要看到撤销过的行，用于"复活"。
     * 同一键最多一行（唯一键保证），ORDER BY 只是防御性写法。
     */
    private static final String SELECT_EXISTING_WITH_ITEM_SQL = """
            SELECT id, user_id, plan_id, plan_item_id, training_date, finished, duration_min, feeling, remark, created_at, deleted
              FROM checkin_record
             WHERE user_id = ?
               AND training_date = ?
               AND plan_item_id = ?
             ORDER BY deleted ASC, id DESC
             LIMIT 1
            """;

    /**
     * 查同键打卡记录（自由训练，plan_item_id 为空）。MySQL 唯一索引对 NULL 不去重，
     * 这个键理论上可能有多行：优先取活记录（deleted ASC），都是撤销记录则取最新一条复活。
     */
    private static final String SELECT_EXISTING_FREE_SQL = """
            SELECT id, user_id, plan_id, plan_item_id, training_date, finished, duration_min, feeling, remark, created_at, deleted
              FROM checkin_record
             WHERE user_id = ?
               AND training_date = ?
               AND plan_item_id IS NULL
             ORDER BY deleted ASC, id DESC
             LIMIT 1
            """;

    /** 复活被撤销的打卡记录：deleted 置 0、finished 置 1，并覆盖本次入参；AND deleted = 1 做并发 CAS */
    private static final String REVIVE_CHECKIN_SQL = """
            UPDATE checkin_record
               SET deleted = 0,
                   finished = 1,
                   plan_id = ?,
                   duration_min = ?,
                   feeling = ?,
                   remark = ?,
                   created_at = ?
             WHERE id = ?
               AND deleted = 1
            """;

    private final CheckinRecordMapper checkinRecordMapper;
    private final TrainingPlanMapper trainingPlanMapper;
    private final TrainingPlanItemMapper trainingPlanItemMapper;
    /** 只用于绕过 @TableLogic 读/复活已撤销的打卡记录（同一个 DataSource，参与当前事务） */
    private final JdbcTemplate jdbcTemplate;

    // ------------------------------------------------------------------
    // 1. 今日任务
    // ------------------------------------------------------------------

    /**
     * 今日任务。
     *
     * <p>优先取 training_date = 今天的明细（按 sortNo 升序）；计划明细的 day_index 是"周几"，
     * 而 start_date 固定为本周周一，所以用户选的训练日不一定包含今天，此时做降级：
     * 取离今天最近的一天（优先未来最近，没有未来日期就取过去最近）返回，并在
     * hasTaskToday / taskDate / nextTrainingDate 上说明。用户没有 ACTIVE 计划、
     * 或计划里没有带日期的明细时都不抛异常，返回空任务（App 首次进入要用）。</p>
     */
    @Transactional(readOnly = true)
    public TodayTaskResponse todayTask() {
        CurrentUser currentUser = CurrentUserProvider.require();
        Long userId = currentUser.getId();
        LocalDate today = LocalDate.now();

        TrainingPlan activePlan = findActivePlan(userId);
        if (activePlan == null) {
            // 还没有 ACTIVE 计划：返回空任务让前端展示"去生成计划"引导
            log.debug("today task queried, scenario=checkin-today, userId={}, trainingDate={}, activePlan=false, hasTaskToday=false, itemCount=0, doneCount=0",
                    userId, today);
            return new TodayTaskResponse(null, null, null, today, false, null, null, 0, 0, List.of());
        }

        // 一次取回该计划全部带日期的明细，按训练日期升序（同一天再按 sortNo 排）
        List<TrainingPlanItem> planItems = trainingPlanItemMapper.selectList(new LambdaQueryWrapper<TrainingPlanItem>()
                .eq(TrainingPlanItem::getPlanId, activePlan.getId())
                .isNotNull(TrainingPlanItem::getTrainingDate)
                .orderByAsc(TrainingPlanItem::getTrainingDate)
                .orderByAsc(TrainingPlanItem::getSortNo)
                .orderByAsc(TrainingPlanItem::getId));

        // 按训练日期分组，LinkedHashMap 天然保持日期升序；没有 training_date 的脏数据跳过
        Map<LocalDate, List<TrainingPlanItem>> itemsByDate = new LinkedHashMap<>();
        for (TrainingPlanItem planItem : planItems) {
            if (planItem.getTrainingDate() == null) {
                continue;
            }
            itemsByDate.computeIfAbsent(planItem.getTrainingDate(), key -> new ArrayList<>()).add(planItem);
        }

        // 今天有安排就用今天；今天没有安排则降级到离今天最近的一天
        LocalDate taskDate = itemsByDate.containsKey(today)
                ? today
                : findNearestTrainingDate(itemsByDate.keySet(), today);
        if (taskDate == null) {
            log.warn("today task empty, scenario=checkin-today, userId={}, planId={}, trainingDate={}, reason=no-dated-plan-item",
                    userId, activePlan.getId(), today);
            return new TodayTaskResponse(activePlan.getId(), activePlan.getName(), activePlan.getLevel(),
                    today, false, null, null, 0, 0, List.of());
        }

        boolean hasTaskToday = taskDate.equals(today);
        // 今天不是训练日时，nextTrainingDate 指向本次降级返回的那一天，前端据此提示"下一个训练日"
        LocalDate nextTrainingDate = hasTaskToday ? null : taskDate;

        List<TrainingPlanItem> taskItems = itemsByDate.get(taskDate);
        // 打卡记录按 taskDate 匹配：降级到过去某天时也能把当天的打卡状态带出来
        Map<Long, CheckinRecord> checkinByItemId = loadCheckins(userId, taskDate);

        List<TodayTaskItemResponse> items = new ArrayList<>(taskItems.size());
        int doneCount = 0;
        for (TrainingPlanItem planItem : taskItems) {
            // 该明细在 taskDate 当天是否已打卡：已打卡则直接带上打卡信息，App 无需再查一次
            CheckinRecord checkin = checkinByItemId.get(planItem.getId());
            boolean checkedIn = checkin != null;
            String itemStatus = checkedIn
                    ? ITEM_STATUS_DONE
                    : (planItem.getStatus() == null ? ITEM_STATUS_PENDING : planItem.getStatus());
            if (ITEM_STATUS_DONE.equals(itemStatus)) {
                doneCount++;
            }
            items.add(new TodayTaskItemResponse(
                    planItem.getId(),
                    planItem.getExerciseId(),
                    planItem.getExerciseName(),
                    planItem.getSets(),
                    planItem.getReps(),
                    planItem.getDurationSec(),
                    planItem.getRestSec(),
                    itemStatus,
                    checkedIn,
                    checkin == null ? null : checkin.getId(),
                    checkin == null ? null : checkin.getDurationMin(),
                    checkin == null ? null : checkin.getFeeling()));
        }

        log.debug("today task queried, scenario=checkin-today, userId={}, trainingDate={}, activePlan=true, planId={}, hasTaskToday={}, taskDate={}, itemCount={}, doneCount={}",
                userId, today, activePlan.getId(), hasTaskToday, taskDate, items.size(), doneCount);
        return new TodayTaskResponse(
                activePlan.getId(),
                activePlan.getName(),
                activePlan.getLevel(),
                today,
                hasTaskToday,
                taskDate,
                nextTrainingDate,
                items.size(),
                doneCount,
                items);
    }

    // ------------------------------------------------------------------
    // 2. 打卡
    // ------------------------------------------------------------------

    /**
     * 打卡。判定顺序：
     * <ol>
     *   <li>校验 durationMin / feeling / remark（非法直接 BAD_REQUEST）；</li>
     *   <li>planItemId 非空时校验明细存在（PLAN_ITEM_NOT_FOUND）且属于当前用户（CHECKIN_NOT_BELONG_TO_USER），
     *       并解析 planId（明细所属计划优先，其次请求值，最后当前 ACTIVE 计划）；</li>
     *   <li>确定 trainingDate：有明细用明细的 training_date，自由训练用请求值（缺省今天）；</li>
     *   <li>按 (user_id, plan_item_id, training_date) 查重（含已撤销记录）：命中活记录 → CHECKIN_ALREADY_EXISTS；
     *       命中撤销记录 → 复活该行并覆盖本次入参，不新增行，同时把明细置回 DONE；</li>
     *   <li>都没有 → 事务内插入 checkin_record（finished=1）并把明细置为 DONE，数据库唯一键兜底并发，
     *       DuplicateKeyException 同样转成 CHECKIN_ALREADY_EXISTS。</li>
     * </ol>
     *
     * @param request 打卡请求，允许整体为空（等价于"按当前 ACTIVE 计划今天打卡"）
     */
    @Transactional(rollbackFor = Exception.class)
    public CheckinResponse create(CheckinSaveRequest request) {
        CurrentUser currentUser = CurrentUserProvider.require();
        Long userId = currentUser.getId();
        CheckinSaveRequest safeRequest = request == null ? new CheckinSaveRequest(null, null, null, null, null, null) : request;

        Integer durationMin = resolveDurationMin(safeRequest.durationMin(), userId);
        String feeling = resolveFeeling(safeRequest.feeling(), userId);
        String remark = resolveRemark(safeRequest.remark(), userId);

        // 明细校验：必须存在（PLAN_ITEM_NOT_FOUND）且挂在当前用户自己的计划上（CHECKIN_NOT_BELONG_TO_USER）
        TrainingPlanItem planItem = loadOwnedPlanItem(safeRequest.planItemId(), userId);
        Long planItemId = planItem == null ? null : planItem.getId();
        Long planId = resolvePlanId(userId, safeRequest.planId(), planItem);
        // 日期口径：带明细时一律以明细的 training_date 为准（忽略请求值），只有自由训练才用请求日期
        LocalDate trainingDate = resolveTrainingDate(userId, safeRequest.trainingDate(), planItem);

        // 幂等 + 复活：连已撤销（deleted=1）的记录一起查，命中活记录才算重复打卡
        CheckinRecord existing = findCheckinIncludingDeleted(userId, planItemId, trainingDate);
        if (existing != null && !isDeleted(existing)) {
            log.error("checkin create rejected, scenario=checkin-create, reason=already-exists, userId={}, planItemId={}, trainingDate={}",
                    userId, planItemId, trainingDate);
            throw new BusinessException(ErrorCode.CHECKIN_ALREADY_EXISTS, "今天已经打过卡了");
        }
        if (existing != null) {
            // 命中撤销过的记录：不能 insert（唯一键包含逻辑删除的行），改为复活该行并覆盖本次入参
            CheckinRecord revived = reviveCheckin(existing, planId, durationMin, feeling, remark);
            markPlanItemDone(planItem);
            log.info("checkin revived, scenario=checkin-create, userId={}, checkinId={}, planItemId={}, trainingDate={}",
                    userId, revived.getId(), planItemId, trainingDate);
            return CheckinResponse.from(revived);
        }

        CheckinRecord checkinRecord = new CheckinRecord();
        checkinRecord.setUserId(userId);
        checkinRecord.setPlanId(planId);
        checkinRecord.setPlanItemId(planItemId);
        checkinRecord.setTrainingDate(trainingDate);
        checkinRecord.setFinished(FINISHED_YES);
        checkinRecord.setDurationMin(durationMin);
        checkinRecord.setFeeling(feeling);
        checkinRecord.setRemark(remark);
        // created_at 库里有默认值，但 MyBatis-Plus 不会回填，这里显式赋值以便直接返回给前端
        checkinRecord.setCreatedAt(LocalDateTime.now());

        // 幂等第 2 道：数据库唯一键 uk_checkin_item_date 兜底，并发重复提交由此拦住
        try {
            checkinRecordMapper.insert(checkinRecord);
        } catch (DuplicateKeyException exception) {
            log.error("checkin create rejected, scenario=checkin-create, reason=duplicate-key, userId={}, planItemId={}, trainingDate={}",
                    userId, planItemId, trainingDate, exception);
            throw new BusinessException(ErrorCode.CHECKIN_ALREADY_EXISTS, "今天已经打过卡了");
        }

        // 打卡成功：把计划明细置为已完成（同一事务，失败一起回滚）
        markPlanItemDone(planItem);

        log.info("checkin created, scenario=checkin-create, userId={}, planItemId={}, trainingDate={}",
                userId, planItemId, trainingDate);
        return CheckinResponse.from(checkinRecord);
    }

    // ------------------------------------------------------------------
    // 3. 撤销打卡
    // ------------------------------------------------------------------

    /**
     * 撤销打卡。事务内：逻辑删除打卡记录 + 把对应明细状态回置 PENDING。
     *
     * <p>权限：非 ADMIN 时 userId 必须与当前用户相同；ADMIN 可撤销任意记录。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        CurrentUser currentUser = CurrentUserProvider.require();
        Long userId = currentUser.getId();
        if (id == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "打卡记录ID不能为空");
        }

        CheckinRecord checkinRecord = checkinRecordMapper.selectById(id);
        if (checkinRecord == null) {
            log.error("checkin cancel rejected, scenario=checkin-cancel, reason=not-found, userId={}, checkinId={}",
                    userId, id);
            throw new BusinessException(ErrorCode.CHECKIN_NOT_FOUND, "打卡记录不存在");
        }

        // 越权校验：非 ADMIN 只能撤销自己的记录
        if (!isAdmin(currentUser) && !Objects.equals(checkinRecord.getUserId(), userId)) {
            log.error("checkin cancel rejected, scenario=checkin-cancel, reason=not-belong-to-user, userId={}, checkinId={}, recordUserId={}",
                    userId, id, checkinRecord.getUserId());
            throw new BusinessException(ErrorCode.CHECKIN_NOT_BELONG_TO_USER, "该打卡记录不属于当前用户");
        }

        // @TableLogic 逻辑删除：deleted 置 1，历史查询与打卡日历自动过滤
        checkinRecordMapper.deleteById(id);
        // 明细状态回置 PENDING，用户今天可以重新打卡该动作
        resetPlanItemPending(checkinRecord.getPlanItemId());

        log.info("checkin cancelled, scenario=checkin-cancel, userId={}, checkinId={}, planItemId={}, trainingDate={}",
                userId, id, checkinRecord.getPlanItemId(), checkinRecord.getTrainingDate());
    }

    // ------------------------------------------------------------------
    // 4. 打卡日历
    // ------------------------------------------------------------------

    /**
     * 打卡日历。month 缺省为当月，返回该月每一天（无打卡的天也返回，finishedCount=0）。
     *
     * @param month 月份 yyyy-MM，可空
     */
    @Transactional(readOnly = true)
    public CheckinCalendarResponse calendar(String month) {
        Long userId = CurrentUserProvider.require().getId();
        YearMonth yearMonth = resolveMonth(month, userId);
        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();

        List<CheckinRecord> records = checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .between(CheckinRecord::getTrainingDate, firstDay, lastDay));

        // 按日期聚合打卡次数（打卡记录均为 finished=1，一条记录算一次打卡）
        Map<LocalDate, Integer> countByDate = new HashMap<>();
        for (CheckinRecord record : records) {
            if (record.getTrainingDate() != null) {
                countByDate.merge(record.getTrainingDate(), 1, Integer::sum);
            }
        }

        List<CheckinCalendarDayResponse> days = new ArrayList<>(yearMonth.lengthOfMonth());
        for (LocalDate date = firstDay; !date.isAfter(lastDay); date = date.plusDays(1)) {
            int finishedCount = countByDate.getOrDefault(date, 0);
            days.add(new CheckinCalendarDayResponse(date, finishedCount, finishedCount > 0));
        }

        int activeDays = countByDate.size();
        log.debug("checkin calendar queried, scenario=checkin-calendar, userId={}, month={}, monthFinishedCount={}, activeDays={}",
                userId, yearMonth.format(MONTH_FORMATTER), records.size(), activeDays);
        return new CheckinCalendarResponse(yearMonth.format(MONTH_FORMATTER), days, records.size(), activeDays);
    }

    // ------------------------------------------------------------------
    // 5. 连续打卡
    // ------------------------------------------------------------------

    /**
     * 连续打卡统计：当前连续天数、历史最长连续天数、有打卡的总天数。
     *
     * <p>算法（自己实现，不用 SQL 变量技巧）：先取出该用户所有打卡记录，只保留 training_date 列，
     * 放进 TreeSet 去重并按日期升序排列（同一天打卡多次只算一天）。</p>
     * <ul>
     *   <li>currentStreak：起点规则是「今天已打卡就从今天起算，今天没打卡就从昨天起算」，
     *       即今天还没练不算断签，从起点逐日往前找，只要相邻日期相差正好 1 天就继续累加，
     *       一旦某个日期不在集合中就停止。</li>
     *   <li>maxStreak：升序集合一次遍历，若当前日期 = 前一天 +1 则当前连续段 +1，否则重新开段取最大值。</li>
     *   <li>totalDays：去重后的日期集合大小。</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public CheckinStreakResponse streak() {
        Long userId = CurrentUserProvider.require().getId();
        LocalDate today = LocalDate.now();

        // 只查 training_date 一列，减少数据传输；@TableLogic 自动过滤已撤销（deleted=1）的记录
        List<CheckinRecord> records = checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .select(CheckinRecord::getTrainingDate)
                .eq(CheckinRecord::getUserId, userId));

        Set<LocalDate> checkedDates = new TreeSet<>();
        for (CheckinRecord record : records) {
            if (record.getTrainingDate() != null) {
                checkedDates.add(record.getTrainingDate());
            }
        }

        // 历史最长连续天数：升序一次遍历
        int maxStreak = 0;
        int running = 0;
        LocalDate previous = null;
        for (LocalDate date : checkedDates) {
            running = (previous != null && previous.plusDays(1).equals(date)) ? running + 1 : 1;
            maxStreak = Math.max(maxStreak, running);
            previous = date;
        }

        // 当前连续天数：今天打卡了就从今天往前数，今天没打卡就从昨天往前数（今天不算断签）
        LocalDate cursor = checkedDates.contains(today) ? today : today.minusDays(1);
        int currentStreak = 0;
        while (checkedDates.contains(cursor)) {
            currentStreak++;
            cursor = cursor.minusDays(1);
        }

        int totalDays = checkedDates.size();
        log.debug("checkin streak queried, scenario=checkin-streak, userId={}, currentStreak={}, maxStreak={}, totalDays={}",
                userId, currentStreak, maxStreak, totalDays);
        return new CheckinStreakResponse(currentStreak, maxStreak, totalDays);
    }

    // ------------------------------------------------------------------
    // 6. 打卡历史分页
    // ------------------------------------------------------------------

    /**
     * 打卡历史分页：按 trainingDate 倒序、createdAt 倒序。
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数，1-100
     */
    @Transactional(readOnly = true)
    public PageResult<CheckinResponse> page(long page, long size) {
        if (page < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "page 必须大于 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "size 必须在 1-" + MAX_PAGE_SIZE + " 之间");
        }

        Long userId = CurrentUserProvider.require().getId();
        Page<CheckinRecord> pageParam = new Page<>(page, size);
        Page<CheckinRecord> result = checkinRecordMapper.selectPage(pageParam, new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .orderByDesc(CheckinRecord::getTrainingDate)
                .orderByDesc(CheckinRecord::getCreatedAt)
                .orderByDesc(CheckinRecord::getId));

        List<CheckinResponse> items = result.getRecords().stream()
                .map(CheckinResponse::from)
                .toList();
        log.debug("checkin history queried, scenario=checkin-page, userId={}, page={}, size={}, total={}",
                userId, page, size, result.getTotal());
        return PageResult.of(items, page, size, result.getTotal());
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    /**
     * 查询当前用户的 ACTIVE 计划。表设计上"一个用户同时只有一条 ACTIVE"，
     * 这里仍按 id 倒序取最新一条，避免历史脏数据导致 selectOne 抛异常。
     */
    private TrainingPlan findActivePlan(Long userId) {
        List<TrainingPlan> plans = trainingPlanMapper.selectList(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getUserId, userId)
                .eq(TrainingPlan::getStatus, PLAN_STATUS_ACTIVE)
                .orderByDesc(TrainingPlan::getId));
        return plans.isEmpty() ? null : plans.get(0);
    }

    /**
     * 取离今天最近的训练日：优先未来最近的一天（严格晚于今天），没有未来日期时取过去最近的一天。
     *
     * <p>调用方必须按日期升序传入 dates（LinkedHashMap.keySet()）：升序遍历中第一个晚于今天的日期
     * 就是未来最近的一天；遍历结束仍未找到时，past 变量停留在最后一个早于今天的日期，即过去最近的一天。</p>
     *
     * @return 最近训练日；dates 为空时返回 null
     */
    private LocalDate findNearestTrainingDate(Set<LocalDate> dates, LocalDate today) {
        LocalDate past = null;
        for (LocalDate date : dates) {
            if (date.isAfter(today)) {
                return date;
            }
            if (date.isBefore(today)) {
                past = date;
            }
        }
        return past;
    }

    /**
     * 查询某天该用户的所有打卡记录，按 planItemId 建索引（自由训练的 planItemId 为空，不参与匹配）。
     *
     * @param trainingDate 任务所属日期（今日任务降级后可能是过去/未来的某天）
     */
    private Map<Long, CheckinRecord> loadCheckins(Long userId, LocalDate trainingDate) {
        List<CheckinRecord> checkins = checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .eq(CheckinRecord::getTrainingDate, trainingDate));
        Map<Long, CheckinRecord> checkinByItemId = new HashMap<>();
        for (CheckinRecord checkin : checkins) {
            if (checkin.getPlanItemId() != null) {
                checkinByItemId.putIfAbsent(checkin.getPlanItemId(), checkin);
            }
        }
        return checkinByItemId;
    }

    /**
     * 校验并加载计划明细：不存在抛 PLAN_ITEM_NOT_FOUND；所属计划不是当前用户的抛 CHECKIN_NOT_BELONG_TO_USER。
     */
    private TrainingPlanItem loadOwnedPlanItem(Long planItemId, Long userId) {
        if (planItemId == null) {
            // 自由训练：没有明细，不做明细校验
            return null;
        }
        TrainingPlanItem planItem = trainingPlanItemMapper.selectById(planItemId);
        if (planItem == null) {
            log.error("checkin create rejected, scenario=checkin-create, reason=plan-item-not-found, userId={}, planItemId={}",
                    userId, planItemId);
            throw new BusinessException(ErrorCode.PLAN_ITEM_NOT_FOUND, "训练明细不存在");
        }
        TrainingPlan plan = trainingPlanMapper.selectById(planItem.getPlanId());
        // 明细必须挂在自己的计划上，防止越权给别人（或已被替换删除的计划）打卡
        if (plan == null || !Objects.equals(plan.getUserId(), userId)) {
            log.error("checkin create rejected, scenario=checkin-create, reason=plan-item-not-belong-to-user, userId={}, planItemId={}, planId={}",
                    userId, planItemId, planItem.getPlanId());
            throw new BusinessException(ErrorCode.CHECKIN_NOT_BELONG_TO_USER, "该训练明细不属于当前用户");
        }
        return planItem;
    }

    /**
     * 决定打卡记录落在哪个计划上：
     * 有明细时以明细所属计划为准（入参 planId 与之一致性只做告警，避免前端冗余参数导致数据不一致）；
     * 无明细时优先用入参 planId（校验存在且属于当前用户），否则缺省取当前 ACTIVE 计划，都没有则为空（纯自由训练）。
     */
    private Long resolvePlanId(Long userId, Long requestPlanId, TrainingPlanItem planItem) {
        if (planItem != null) {
            if (requestPlanId != null && !Objects.equals(requestPlanId, planItem.getPlanId())) {
                log.warn("checkin create planId overridden by planItem, scenario=checkin-create, userId={}, requestPlanId={}, actualPlanId={}",
                        userId, requestPlanId, planItem.getPlanId());
            }
            return planItem.getPlanId();
        }
        if (requestPlanId != null) {
            TrainingPlan plan = trainingPlanMapper.selectById(requestPlanId);
            if (plan == null) {
                log.error("checkin create rejected, scenario=checkin-create, reason=plan-not-found, userId={}, planId={}",
                        userId, requestPlanId);
                throw new BusinessException(ErrorCode.PLAN_NOT_FOUND, "训练计划不存在");
            }
            if (!Objects.equals(plan.getUserId(), userId)) {
                log.error("checkin create rejected, scenario=checkin-create, reason=plan-not-belong-to-user, userId={}, planId={}",
                        userId, requestPlanId);
                throw new BusinessException(ErrorCode.CHECKIN_NOT_BELONG_TO_USER, "该训练计划不属于当前用户");
            }
            return plan.getId();
        }
        TrainingPlan activePlan = findActivePlan(userId);
        return activePlan == null ? null : activePlan.getId();
    }

    /**
     * 查同键记录，**包含已撤销（deleted=1）的行**，用于幂等判定与"撤销后复活"。
     *
     * <p>为什么要绕开 {@code @TableLogic}：MyBatis-Plus 对带逻辑删除的实体，会在 where 之后
     * 无条件追加 {@code AND deleted=0}，所以 {@code apply("deleted = 1")} 会变成
     * {@code deleted = 1 AND deleted = 0}（永远查不到），{@code last("...")} 只能追加到语句末尾、
     * 会破坏 AND/OR 优先级，都不安全；而唯一键 uk_checkin_item_date 是包含逻辑删除行的，
     * 不看到撤销行就会出现"撤销了却打不了卡"。因此这里直接用参数化 SQL 读取
     * （同一个 DataSource，走 JdbcTemplate 自动参与当前 @Transactional 事务）。</p>
     *
     * <p>自由训练（planItemId 为空）按 (user_id, training_date, plan_item_id IS NULL) 命中：
     * MySQL 唯一索引对 NULL 不去重，唯一键在这里不起作用，"自由训练一天一次"由本方法配合
     * {@link #create} 在服务层保证。同一键理论上可能存在多行时，优先取活记录（deleted=0），
     * 都是撤销记录则取最新一条复活。</p>
     *
     * @return 同键记录（优先未撤销的）；没有则 null
     */
    private CheckinRecord findCheckinIncludingDeleted(Long userId, Long planItemId, LocalDate trainingDate) {
        String sql = planItemId == null ? SELECT_EXISTING_FREE_SQL : SELECT_EXISTING_WITH_ITEM_SQL;
        Object[] args = planItemId == null
                ? new Object[]{userId, Date.valueOf(trainingDate)}
                : new Object[]{userId, Date.valueOf(trainingDate), planItemId};
        List<CheckinRecord> rows = jdbcTemplate.query(sql, (rs, rowNum) -> mapCheckinRow(rs), args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 复活撤销过的打卡记录：deleted 置回 0、finished 置 1，并按本次入参覆盖 plan_id / duration_min /
     * feeling / remark，created_at 刷新为当前时间。加 {@code AND deleted = 1} 做 CAS，
     * 并发下被别的请求先复活时返回 0 行，此时按"已存在打卡"处理，避免返回一条不属于本次操作的记录。
     */
    private CheckinRecord reviveCheckin(CheckinRecord existing, Long planId, Integer durationMin, String feeling, String remark) {
        LocalDateTime createdAt = LocalDateTime.now();
        int updated = jdbcTemplate.update(
                REVIVE_CHECKIN_SQL,
                planId,
                durationMin,
                feeling,
                remark,
                Timestamp.valueOf(createdAt),
                existing.getId());
        if (updated != 1) {
            log.warn("checkin revive skipped, scenario=checkin-create, reason=concurrent-revive-or-deleted, checkinId={}, affected={}",
                    existing.getId(), updated);
            throw new BusinessException(ErrorCode.CHECKIN_ALREADY_EXISTS, "今天已经打过卡了");
        }
        // 返回值直接用复活后的状态，避免再查一次库
        existing.setPlanId(planId);
        existing.setDurationMin(durationMin);
        existing.setFeeling(feeling);
        existing.setRemark(remark);
        existing.setCreatedAt(createdAt);
        existing.setFinished(FINISHED_YES);
        existing.setDeleted(0);
        return existing;
    }

    /** 手工映射一行 checkin_record（JdbcTemplate 不走 MyBatis 的下划线转驼峰配置） */
    private CheckinRecord mapCheckinRow(ResultSet rs) throws SQLException {
        CheckinRecord record = new CheckinRecord();
        record.setId(rs.getLong("id"));
        record.setUserId(rs.getLong("user_id"));
        long planId = rs.getLong("plan_id");
        record.setPlanId(rs.wasNull() ? null : planId);
        long planItemId = rs.getLong("plan_item_id");
        record.setPlanItemId(rs.wasNull() ? null : planItemId);
        Date trainingDate = rs.getDate("training_date");
        record.setTrainingDate(trainingDate == null ? null : trainingDate.toLocalDate());
        record.setFinished(rs.getInt("finished"));
        int durationMin = rs.getInt("duration_min");
        record.setDurationMin(rs.wasNull() ? null : durationMin);
        record.setFeeling(rs.getString("feeling"));
        record.setRemark(rs.getString("remark"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        record.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        record.setDeleted(rs.getInt("deleted"));
        return record;
    }

    /** 该记录是否已被逻辑删除（撤销） */
    private boolean isDeleted(CheckinRecord record) {
        return record.getDeleted() != null && record.getDeleted() == 1;
    }

    /** 把计划明细状态置为 DONE（已经是 DONE 就不重复写库） */
    private void markPlanItemDone(TrainingPlanItem planItem) {
        if (planItem == null || ITEM_STATUS_DONE.equals(planItem.getStatus())) {
            return;
        }
        TrainingPlanItem update = new TrainingPlanItem();
        update.setId(planItem.getId());
        update.setStatus(ITEM_STATUS_DONE);
        update.setUpdatedAt(LocalDateTime.now());
        trainingPlanItemMapper.updateById(update);
    }

    /** 把计划明细状态回置为 PENDING（明细已被物理删除时只告警，不影响撤销打卡本身） */
    private void resetPlanItemPending(Long planItemId) {
        if (planItemId == null) {
            // 自由训练没有明细，无需回置
            return;
        }
        TrainingPlanItem planItem = trainingPlanItemMapper.selectById(planItemId);
        if (planItem == null) {
            log.warn("checkin cancel skipped plan item reset, scenario=checkin-cancel, reason=plan-item-not-found, planItemId={}",
                    planItemId);
            return;
        }
        if (ITEM_STATUS_PENDING.equals(planItem.getStatus())) {
            return;
        }
        TrainingPlanItem update = new TrainingPlanItem();
        update.setId(planItemId);
        update.setStatus(ITEM_STATUS_PENDING);
        update.setUpdatedAt(LocalDateTime.now());
        trainingPlanItemMapper.updateById(update);
    }

    /** 是否管理员。roleCode 异常（如测试上下文）时按普通用户处理，避免抛 500 */
    private boolean isAdmin(CurrentUser currentUser) {
        try {
            return UserRole.ADMIN == currentUser.getRole();
        } catch (IllegalArgumentException exception) {
            log.warn("current user role unresolved, scenario=checkin-cancel, roleCode={}", currentUser.getRoleCode());
            return false;
        }
    }

    /**
     * 打卡日期：带明细时以明细的 training_date 为准（请求值只用于告警，不参与写库），
     * 避免唯一键 (user_id, plan_item_id, training_date) 被前端传来的奇怪日期污染；
     * 只有自由训练（没有明细）或明细没有 training_date 时，才用请求值，请求值为空则取今天。
     *
     * @param trainingDate 请求里的训练日期，格式 yyyy-MM-dd，可空
     * @param planItem     计划明细，自由训练时为 null
     * @return 实际写库的训练日期
     */
    private LocalDate resolveTrainingDate(Long userId, String trainingDate, TrainingPlanItem planItem) {
        if (planItem != null && planItem.getTrainingDate() != null) {
            if (trainingDate != null && !trainingDate.isBlank()
                    && !trainingDate.trim().equals(planItem.getTrainingDate().toString())) {
                log.warn("checkin create trainingDate ignored, scenario=checkin-create, userId={}, planItemId={}, requestTrainingDate={}, actualTrainingDate={}",
                        userId, planItem.getId(), trainingDate, planItem.getTrainingDate());
            }
            return planItem.getTrainingDate();
        }
        if (trainingDate == null || trainingDate.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(trainingDate.trim());
        } catch (DateTimeParseException exception) {
            log.error("checkin create rejected, scenario=checkin-create, reason=invalid-training-date, userId={}, trainingDate={}",
                    userId, trainingDate, exception);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "trainingDate 格式错误，正确格式为 yyyy-MM-dd，例如 2024-06-01");
        }
    }

    /** 训练时长：可空，不能为负数 */
    private Integer resolveDurationMin(Integer durationMin, Long userId) {
        if (durationMin == null) {
            return null;
        }
        if (durationMin < 0) {
            log.error("checkin create rejected, scenario=checkin-create, reason=invalid-duration-min, userId={}, durationMin={}",
                    userId, durationMin);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "durationMin 不能为负数");
        }
        return durationMin;
    }

    /** 训练感受：可空，只能取 EASY / NORMAL / HARD（大小写不敏感，统一存大写） */
    private String resolveFeeling(String feeling, Long userId) {
        if (feeling == null || feeling.isBlank()) {
            return null;
        }
        String normalized = feeling.trim().toUpperCase(Locale.ROOT);
        if (!FEELING_OPTIONS.contains(normalized)) {
            log.error("checkin create rejected, scenario=checkin-create, reason=invalid-feeling, userId={}, feeling={}",
                    userId, feeling);
            throw new BusinessException(ErrorCode.BAD_REQUEST, FEELING_MESSAGE);
        }
        return normalized;
    }

    /** 备注：可空，长度不超过 255（与列定义一致） */
    private String resolveRemark(String remark, Long userId) {
        if (remark == null || remark.isBlank()) {
            return null;
        }
        String trimmed = remark.trim();
        if (trimmed.length() > REMARK_MAX_LENGTH) {
            log.error("checkin create rejected, scenario=checkin-create, reason=remark-too-long, userId={}, length={}",
                    userId, trimmed.length());
            throw new BusinessException(ErrorCode.BAD_REQUEST, "remark 长度不能超过 " + REMARK_MAX_LENGTH + " 个字符");
        }
        return trimmed;
    }

    /** 月份：为空取当月；格式必须是 yyyy-MM，解析失败抛 BAD_REQUEST 并给中文提示 */
    private YearMonth resolveMonth(String month, Long userId) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month.trim(), MONTH_FORMATTER);
        } catch (DateTimeParseException exception) {
            log.error("checkin calendar rejected, scenario=checkin-calendar, reason=invalid-month, userId={}, month={}",
                    userId, month, exception);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "month 格式错误，正确格式为 yyyy-MM，例如 2024-06");
        }
    }
}
