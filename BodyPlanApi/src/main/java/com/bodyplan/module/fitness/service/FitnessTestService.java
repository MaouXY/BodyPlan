package com.bodyplan.module.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bodyplan.common.exception.BusinessException;
import com.bodyplan.common.exception.ErrorCode;
import com.bodyplan.common.security.CurrentUser;
import com.bodyplan.common.security.CurrentUserProvider;
import com.bodyplan.module.fitness.dto.FitnessTestResponse;
import com.bodyplan.module.fitness.dto.FitnessTestSaveRequest;
import com.bodyplan.module.fitness.entity.FitnessTest;
import com.bodyplan.module.fitness.mapper.FitnessTestMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 体能测试服务：按固定规则把三项成绩 + 上五楼状态折算成体能等级。
 *
 * <p>等级规则（阈值均为类常量）：
 * <pre>
 * 俯卧撑 pushUp：    ≥20 → 3 分，≥10 → 2 分，其余(含 null) → 1 分
 * 深蹲   squat：     ≥35 → 3 分，≥20 → 2 分，其余(含 null) → 1 分
 * 卷腹   crunch：    ≥30 → 3 分，≥15 → 2 分，其余(含 null) → 1 分
 * 上五楼 stairStatus：EASY → 3 分，SLIGHT_HARD → 2 分，HARD → 1 分，FAIL → 0 分，null → 1 分
 * 总分 4-12：≥10 → L3，≥6 → L2，否则 L1
 * </pre>
 * 校验：三项成绩必须为非负整数（负数由 jakarta.validation 与服务层各拦一次），上五楼状态必须是固定枚举之一。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FitnessTestService {

    // ==================== 单项计分阈值（分数） ====================

    /** 俯卧撑 ≥20 个记 3 分 */
    public static final int PUSH_UP_LEVEL3 = 20;
    /** 俯卧撑 ≥10 个记 2 分 */
    public static final int PUSH_UP_LEVEL2 = 10;
    /** 深蹲 ≥35 个记 3 分 */
    public static final int SQUAT_LEVEL3 = 35;
    /** 深蹲 ≥20 个记 2 分 */
    public static final int SQUAT_LEVEL2 = 20;
    /** 卷腹 ≥30 个记 3 分 */
    public static final int CRUNCH_LEVEL3 = 30;
    /** 卷腹 ≥15 个记 2 分 */
    public static final int CRUNCH_LEVEL2 = 15;

    /** 单个动作满分 3 分 */
    private static final int ITEM_SCORE_MAX = 3;
    /** 单个动作最低分 1 分（未填写也按 1 分计） */
    private static final int ITEM_SCORE_MIN = 1;

    /** 上五楼状态取值 */
    private static final String STAIR_EASY = "EASY";
    private static final String STAIR_SLIGHT_HARD = "SLIGHT_HARD";
    private static final String STAIR_HARD = "HARD";
    private static final String STAIR_FAIL = "FAIL";
    /** 上五楼状态为空时的默认得分 */
    private static final int STAIR_SCORE_DEFAULT = 1;

    // ==================== 总分与等级阈值 ====================

    /** 总分上下限（4-12）：下限 = 三个动作各最低 1 分 + 上五楼 1 分，上限 = 4 项各满分 */
    public static final int TOTAL_SCORE_MIN = 4;
    public static final int TOTAL_SCORE_MAX = 12;
    /** 总分 ≥10 判为 L3（挑战） */
    public static final int TOTAL_SCORE_L3 = 10;
    /** 总分 ≥6 判为 L2（进阶） */
    public static final int TOTAL_SCORE_L2 = 6;

    /** 等级编码 */
    private static final String LEVEL_L3 = "L3";
    private static final String LEVEL_L2 = "L2";
    private static final String LEVEL_L1 = "L1";

    /** 历史查询：默认条数与最大条数 */
    private static final int DEFAULT_FITNESS_LIMIT = 10;
    private static final int MAX_FITNESS_LIMIT = 100;

    private final FitnessTestMapper fitnessTestMapper;

    /**
     * 录入体能测试：按规则换算等级后落库，testedAt 取当前时间。
     */
    @Transactional(rollbackFor = Exception.class)
    public FitnessTestResponse saveTest(FitnessTestSaveRequest request) {
        CurrentUser currentUser = CurrentUserProvider.require();
        Long userId = currentUser.getId();

        try {
            validateCount(request.pushUp(), "俯卧撑个数");
            validateCount(request.squat(), "深蹲个数");
            validateCount(request.crunch(), "卷腹个数");

            String stairStatus = StringUtils.hasText(request.stairStatus()) ? request.stairStatus() : null;
            int totalScore = scoreOfPushUp(request.pushUp())
                    + scoreOfSquat(request.squat())
                    + scoreOfCrunch(request.crunch())
                    + scoreOfStair(stairStatus);
            String level = resolveLevel(totalScore);

            FitnessTest fitnessTest = new FitnessTest();
            fitnessTest.setUserId(userId);
            fitnessTest.setPushUp(request.pushUp());
            fitnessTest.setSquat(request.squat());
            fitnessTest.setCrunch(request.crunch());
            fitnessTest.setStairStatus(stairStatus);
            fitnessTest.setLevel(level);
            fitnessTest.setTestedAt(LocalDateTime.now());
            fitnessTestMapper.insert(fitnessTest);

            log.info(
                    "fitness test saved, scenario=fitness-test-save, userId={}, testId={}, totalScore={}, level={}",
                    userId,
                    fitnessTest.getId(),
                    totalScore,
                    level
            );
            return toResponse(fitnessTest);
        } catch (BusinessException exception) {
            log.error("fitness test save failed, scenario=fitness-test-save, userId={}", userId, exception);
            throw exception;
        }
    }

    /**
     * 最近一次体能测试（按 testedAt 倒序取 1 条），没有则抛 FITNESS_TEST_NOT_FOUND。
     */
    public FitnessTestResponse getLatestTest() {
        CurrentUser currentUser = CurrentUserProvider.require();
        Long userId = currentUser.getId();

        FitnessTest latest = fitnessTestMapper.selectOne(new LambdaQueryWrapper<FitnessTest>()
                .eq(FitnessTest::getUserId, userId)
                .orderByDesc(FitnessTest::getTestedAt)
                .orderByDesc(FitnessTest::getId)
                .last("LIMIT 1"));
        if (latest == null) {
            log.error("fitness test query failed, scenario=fitness-test-latest, userId={}, reason=not-found", userId);
            throw new BusinessException(ErrorCode.FITNESS_TEST_NOT_FOUND, "还没有体能测试记录，请先完成一次体能测试");
        }

        log.info("fitness test queried, scenario=fitness-test-latest, userId={}, testId={}, level={}",
                userId, latest.getId(), latest.getLevel());
        return toResponse(latest);
    }

    /**
     * 体能测试历史：按 testedAt 倒序，默认 10 条、最多 100 条。
     */
    public List<FitnessTestResponse> listTests(Integer limit) {
        CurrentUser currentUser = CurrentUserProvider.require();
        Long userId = currentUser.getId();

        int size = resolveLimit(limit);
        List<FitnessTest> tests = fitnessTestMapper.selectList(new LambdaQueryWrapper<FitnessTest>()
                .eq(FitnessTest::getUserId, userId)
                .orderByDesc(FitnessTest::getTestedAt)
                .orderByDesc(FitnessTest::getId)
                .last("LIMIT " + size));
        log.info("fitness test listed, scenario=fitness-test-list, userId={}, limit={}, size={}",
                userId, size, tests.size());
        return tests.stream().map(this::toResponse).toList();
    }

    // ==================== 计分规则实现 ====================

    /** 俯卧撑得分：≥20 → 3 分，≥10 → 2 分，其余（含 null）→ 1 分 */
    int scoreOfPushUp(Integer pushUp) {
        if (pushUp == null) {
            return ITEM_SCORE_MIN;
        }
        if (pushUp >= PUSH_UP_LEVEL3) {
            return ITEM_SCORE_MAX;
        }
        return pushUp >= PUSH_UP_LEVEL2 ? 2 : ITEM_SCORE_MIN;
    }

    /** 深蹲得分：≥35 → 3 分，≥20 → 2 分，其余（含 null）→ 1 分 */
    int scoreOfSquat(Integer squat) {
        if (squat == null) {
            return ITEM_SCORE_MIN;
        }
        if (squat >= SQUAT_LEVEL3) {
            return ITEM_SCORE_MAX;
        }
        return squat >= SQUAT_LEVEL2 ? 2 : ITEM_SCORE_MIN;
    }

    /** 卷腹得分：≥30 → 3 分，≥15 → 2 分，其余（含 null）→ 1 分 */
    int scoreOfCrunch(Integer crunch) {
        if (crunch == null) {
            return ITEM_SCORE_MIN;
        }
        if (crunch >= CRUNCH_LEVEL3) {
            return ITEM_SCORE_MAX;
        }
        return crunch >= CRUNCH_LEVEL2 ? 2 : ITEM_SCORE_MIN;
    }

    /** 上五楼状态得分：EASY → 3 分，SLIGHT_HARD → 2 分，HARD → 1 分，FAIL → 0 分，null → 1 分 */
    int scoreOfStair(String stairStatus) {
        if (!StringUtils.hasText(stairStatus)) {
            return STAIR_SCORE_DEFAULT;
        }
        return switch (stairStatus) {
            case STAIR_EASY -> 3;
            case STAIR_SLIGHT_HARD -> 2;
            case STAIR_HARD -> 1;
            case STAIR_FAIL -> 0;
            default -> STAIR_SCORE_DEFAULT;
        };
    }

    /** 总分 4-12：≥10 → L3，≥6 → L2，否则 L1 */
    String resolveLevel(int totalScore) {
        if (totalScore >= TOTAL_SCORE_L3) {
            return LEVEL_L3;
        }
        return totalScore >= TOTAL_SCORE_L2 ? LEVEL_L2 : LEVEL_L1;
    }

    // ==================== 内部工具 ====================

    /** 成绩必须为非负整数，负数抛 BAD_REQUEST + 中文提示（jakarta.validation 已拦一层，这里做服务层兜底） */
    private void validateCount(Integer value, String fieldName) {
        if (value != null && value < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, fieldName + "不能为负数");
        }
    }

    private FitnessTestResponse toResponse(FitnessTest fitnessTest) {
        return new FitnessTestResponse(
                fitnessTest.getId(),
                fitnessTest.getPushUp(),
                fitnessTest.getSquat(),
                fitnessTest.getCrunch(),
                fitnessTest.getStairStatus(),
                fitnessTest.getLevel(),
                fitnessTest.getTestedAt()
        );
    }

    /** limit 兜底：为空或 ≤0 取默认值（10），超过上限（100）取上限 */
    private int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_FITNESS_LIMIT;
        }
        return Math.min(limit, MAX_FITNESS_LIMIT);
    }
}
