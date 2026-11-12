package com.bodyplan.module.profile.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bodyplan.common.exception.BusinessException;
import com.bodyplan.common.exception.ErrorCode;
import com.bodyplan.common.security.CurrentUser;
import com.bodyplan.common.security.CurrentUserProvider;
import com.bodyplan.module.profile.dto.BodyDataResponse;
import com.bodyplan.module.profile.dto.BodyDataSaveRequest;
import com.bodyplan.module.profile.dto.GoalSuggestionResponse;
import com.bodyplan.module.profile.dto.ProfileResponse;
import com.bodyplan.module.profile.dto.ProfileSaveRequest;
import com.bodyplan.module.profile.entity.BodyData;
import com.bodyplan.module.profile.entity.UserProfile;
import com.bodyplan.module.profile.mapper.BodyDataMapper;
import com.bodyplan.module.profile.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户档案与身体数据服务。
 *
 * <p>固定规则一览（阈值均为类常量，便于调参与答辩解释）：
 * <ul>
 *   <li>BMI 分级：&lt;18.5 偏瘦、18.5-24 正常、24-28 超重、≥28 肥胖；</li>
 *   <li>身体评分 score（0-100）= BMI 分项（满分 {@link #SCORE_BMI_MAX}）+ 年龄分项（满分 {@link #SCORE_AGE_MAX}），
 *       BMI 越接近正常区间、年龄越小得分越高，详见 {@link #scoreOf(BigDecimal, Integer)}；</li>
 *   <li>目标推荐按 R1 → R2 → R3/R3b/R3c → R4b → R4 → R5 的固定优先级命中，
 *       说明见 {@link #resolveGoalSuggestion(UserProfile)}；</li>
 *   <li>建档问卷支持分步保存（只覆盖请求里带上的字段），见 {@link #saveProfile(ProfileSaveRequest)}。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    // ==================== 身体评分 / BMI 规则常量 ====================

    /** 身体评分满分 = BMI 分项满分 + 年龄分项满分 = 70 + 30 */
    public static final int SCORE_FULL = 100;
    /** BMI 分项满分：体重区间正常是身体评分的主导因素 */
    public static final int SCORE_BMI_MAX = 70;
    /** 年龄分项满分 */
    public static final int SCORE_AGE_MAX = 30;
    /** BMI 正常区间下界（含） */
    public static final double BMI_NORMAL_MIN = 18.5;
    /** BMI 正常区间上界（含） */
    public static final double BMI_NORMAL_MAX = 24.0;
    /** BMI 超重区间上界：BMI ≥ 28 视为肥胖 */
    public static final double BMI_OVERWEIGHT_MAX = 28.0;
    /** BMI 每偏离正常区间 1 个单位扣 7 分（即每个 BMI 单位扣掉 10%，最大扣 70 分） */
    public static final double SCORE_BMI_PENALTY_PER_UNIT = 7.0;
    /** BMI 每偏离正常区间 1 个单位扣 3 分（越偏离惩罚越轻，避免分数断崖） */
    public static final double SCORE_BMI_PENALTY_PER_UNIT_FAR = 3.0;

    /**
     * BMI 偏离正常区间的“分段计罚”分界点：
     * 偏离不超过 2 个单位（BMI 16.5-18.5 或 24-26）时按每单位 7 分扣，
     * 超过 2 个单位的部分按每单位 3 分扣，保证分值连续且越远离正常区间分数越低。
     */
    public static final double SCORE_BMI_NEAR_DEVIATION = 2.0;

    /** 年龄分项：基准分（20 岁及以下拿满分 30 分，随年龄增长按下述速率递减） */
    public static final double SCORE_AGE_BASE = SCORE_AGE_MAX;
    /** 年龄分项：20-29 岁每大一岁扣 0.5 分 */
    public static final double SCORE_AGE_DECAY_STEP_1 = 0.5;
    /** 年龄分项：30 岁及以上每大一岁扣 1.0 分 */
    public static final double SCORE_AGE_DECAY_STEP_2 = 1.0;

    // ==================== 档案兜底默认值 ====================

    /** 未填写性别时的兜底值 */
    private static final String DEFAULT_GENDER = "UNKNOWN";
    /** 未完成建档 */
    private static final int PROFILE_INCOMPLETED = 0;
    /** 建档完成 */
    private static final int PROFILE_COMPLETED = 1;
    /** 逻辑删除标记：0 正常、1 已删除（与 @TableLogic / application.yaml 配置一致） */
    private static final int DELETED_NO = 0;
    private static final int DELETED_YES = 1;
    /** 身高/体重/BMI 统一保留的小数位数（与库中 DECIMAL(5,1) / DECIMAL(4,1) 对齐） */
    private static final int BODY_VALUE_SCALE = 1;

    // ==================== 目标推荐规则常量（阈值 + 规则编号） ====================

    /** 规则 R1（最高优先，健康类）：BMI ≥ 28（肥胖）→ LOSE_FAT */
    public static final String RULE_BMI_OBESE = "R1-BMI≥28肥胖";
    /** 规则 R2（次高优先，健康类）：BMI < 18.5（偏瘦）→ BUILD_MUSCLE */
    public static final String RULE_BMI_UNDERWEIGHT = "R2-BMI<18.5偏瘦";
    /** 规则 R3：24 ≤ BMI < 28（超重）且身体类型为苹果型 → LOSE_FAT */
    public static final String RULE_BMI_OVERWEIGHT_APPLE = "R3-24≤BMI<28且苹果型";
    /** 规则 R3b：24 ≤ BMI < 28 且主目标已是增肌 → 保留用户目标 BUILD_MUSCLE */
    public static final String RULE_BMI_OVERWEIGHT_KEEP_MUSCLE = "R3b-超重且已选增肌";
    /** 规则 R3c：24 ≤ BMI < 28 且非苹果型 → DAILY_FITNESS */
    public static final String RULE_BMI_OVERWEIGHT_DAILY = "R3c-超重偏高日常健身";
    /** 规则 R4b：18.5 ≤ BMI < 28 且档案里已有 primaryGoal → 直接推荐用户已选目标（不覆盖用户选择） */
    public static final String RULE_RESPECT_USER_GOAL = "R4b-尊重用户已选目标";
    /** 规则 R4：正常区间且体型为直筒型、梨型，或重点部位含肩颈 → POSTURE */
    public static final String RULE_SHAPE_POSTURE = "R4-体型或肩颈重点改善体态";
    /** 规则 R5：其它情况 → DAILY_FITNESS（无身体数据、无主目标时同样落到此规则） */
    public static final String RULE_DEFAULT_DAILY = "R5-默认日常健身";

    /** 目标编码常量 */
    private static final String GOAL_LOSE_FAT = "LOSE_FAT";
    private static final String GOAL_BUILD_MUSCLE = "BUILD_MUSCLE";
    private static final String GOAL_POSTURE = "POSTURE";
    private static final String GOAL_DAILY_FITNESS = "DAILY_FITNESS";
    private static final String GOAL_CARDIO = "CARDIO";

    /** 体型常量 */
    private static final String BODY_TYPE_STRAIGHT = "STRAIGHT";
    private static final String BODY_TYPE_PEAR = "PEAR";
    private static final String BODY_TYPE_APPLE = "APPLE";
    /** 重点部位：肩颈 */
    private static final String FOCUS_PART_SHOULDER_NECK = "SHOULDER_NECK";

    /** 身体数据历史查询：默认条数与最大条数 */
    private static final int DEFAULT_BODY_DATA_LIMIT = 10;
    private static final int MAX_BODY_DATA_LIMIT = 100;

    /** 身体数据备注文案 */
    private static final String REMARK_FIRST_PROFILE = "建档数据";
    private static final String REMARK_PROFILE_UPDATE = "档案更新";

    /** 建档必填项校验的中文提示（越界统一抛 BODY_DATA_INVALID） */
    private static final String MSG_HEIGHT_RANGE = "身高必须在 100-250cm 之间";
    private static final String MSG_WEIGHT_RANGE = "体重必须在 25-250kg 之间";
    private static final String MSG_AGE_RANGE = "年龄必须在 10-100 岁之间";

    /** 身高/体重/年龄取值范围常量（与建表注释一致） */
    private static final double MIN_HEIGHT_CM = 100.0;
    private static final double MAX_HEIGHT_CM = 250.0;
    private static final double MIN_WEIGHT_KG = 25.0;
    private static final double MAX_WEIGHT_KG = 250.0;
    private static final int MIN_AGE = 10;
    private static final int MAX_AGE = 100;

    private final UserProfileMapper userProfileMapper;
    private final BodyDataMapper bodyDataMapper;

    // ==================== 1. GET /api/v1/profile ====================

    /**
     * 查询当前用户档案；不存在则惰性创建一条默认档案（profileCompleted=0），不抛 404。
     * 若库中存在该用户 id 与唯一键 uk_user_profile_user 冲突的历史档案，见
     * {@link #createProfileOrRecover(Long)} 的恢复说明。
     */
    @Transactional(rollbackFor = Exception.class)
    public ProfileResponse getMyProfile() {
        CurrentUser currentUser = CurrentUserProvider.require();
        Long userId = currentUser.getId();

        UserProfile profile = findProfile(userId);
        if (profile == null) {
            profile = createProfileOrRecover(userId);
        }
        return buildProfileResponse(userId, profile);
    }

    // ==================== 2. PUT /api/v1/profile ====================

    /**
     * 建档问卷保存（支持分步保存，只更新请求里带上的字段）。
     *
     * <p><b>合并语义（与 ProfileSaveRequest 的注释一致）</b>：
     * <ul>
     *   <li>普通字段为 null → 保留库中原值；非 null → 覆盖；</li>
     *   <li>List 字段为 null → 保留库中原值；空数组 [] → 清空该字段；非空数组 → 覆盖；</li>
     *   <li><b>remindTime 约定：显式传 null 表示清空提醒时间</b>（提醒时间只在表单里整体提交，
     *       不做“null 保留旧值”处理；若只想改开关而不动时间，把原 remindTime 回传即可）；</li>
     *   <li>heightCm 与 weightKg 合并后都不为空才写入一条 body_data（remark：首次为「建档数据」，
     *       否则「档案更新」）；两者只有其一或都为空时跳过 body_data，但档案照常保存；</li>
     *   <li>BMI/score 用合并后的值计算；合并后缺身高或体重则 bmi=null，score 仍返回最近一条 body_data 的 score；</li>
     *   <li>profileCompleted 按合并后的完整状态判定：gender、age、heightCm、weightKg、primaryGoal、
     *       weeklyDays（非空）、dailyMinutes&gt;0 全部就绪才为 1。</li>
     * </ul>
     */
    @Transactional(rollbackFor = Exception.class)
    public ProfileResponse saveProfile(ProfileSaveRequest request) {
        CurrentUser currentUser = CurrentUserProvider.require();
        Long userId = currentUser.getId();

        try {
            // 有值才校验，空值放行（分步保存）
            validateBodyValue(request.heightCm(), request.weightKg(), request.age());

            UserProfile existing = findProfile(userId);
            boolean firstProfile = existing == null;
            // 新建档时由 createProfileOrRecover 内部完成 insert（含唯一键冲突恢复），因此这里不能再插一次
            UserProfile profile = firstProfile ? createProfileOrRecover(userId) : existing;

            applyRequest(profile, request);
            profile.setProfileCompleted(isProfileComplete(profile) ? PROFILE_COMPLETED : PROFILE_INCOMPLETED);

            // 两种路径都必须落库更新：首次建档时 createProfileOrRecover 只插入了默认行，
            // 合并后的字段（身体数据/目标/偏好等）必须靠这次 UPDATE 写入，否则会丢失。
            userProfileMapper.updateById(profile);

            // 合并后的身高体重都齐全才写 body_data，否则跳过（只保存档案）
            BodyData bodyData = saveBodyData(
                    userId,
                    profile.getHeightCm(),
                    profile.getWeightKg(),
                    profile.getBodyType(),
                    profile.getAge(),
                    firstProfile ? REMARK_FIRST_PROFILE : REMARK_PROFILE_UPDATE
            );

            log.info(
                    "profile saved, scenario=profile-save, userId={}, profileId={}, profileCompleted={}, bodyDataId={}, bmi={}, score={}",
                    userId,
                    profile.getId(),
                    profile.getProfileCompleted(),
                    bodyData == null ? null : bodyData.getId(),
                    bodyData == null ? null : bodyData.getBmi(),
                    bodyData == null ? null : bodyData.getScore()
            );
            return buildProfileResponse(userId, profile);
        } catch (BusinessException exception) {
            log.error("profile save failed, scenario=profile-save, userId={}", userId, exception);
            throw exception;
        }
    }

    // ==================== 3. GET /api/v1/profile/goal-suggestion ====================

    /**
     * 目标推荐：完全基于固定规则，无档案时抛 PROFILE_NOT_FOUND。
     * 规则优先级：R1 肥胖减脂 → R2 偏瘦增肌 → R3 超重苹果型减脂 / R3b 超重已选增肌保留 / R3c 超重日常
     * → R4b 尊重用户已选目标 → R4 体态 → R5 日常健身。备选目标固定为 3 个。
     */
    public GoalSuggestionResponse suggestGoal() {
        CurrentUser currentUser = CurrentUserProvider.require();
        Long userId = currentUser.getId();

        UserProfile profile = findProfile(userId);
        if (profile == null) {
            log.error("goal suggestion failed, scenario=profile-goal-suggestion, userId={}, reason=profile-not-found", userId);
            throw new BusinessException(ErrorCode.PROFILE_NOT_FOUND, "请先完成建档问卷，再获取训练目标建议");
        }

        return goalSuggestion(profile, userId);
    }

    public GoalSuggestionResponse previewGoal(ProfileSaveRequest request) {
        Long userId = CurrentUserProvider.require().getId();
        if (request.heightCm() == null || request.weightKg() == null || request.age() == null) {
            log.warn("goal preview rejected, scenario=profile-goal-preview, userId={}, reason=missing-body-data", userId);
            throw new BusinessException(ErrorCode.BODY_DATA_INVALID, "请先填写年龄、身高和体重");
        }
        validateBodyValue(request.heightCm(), request.weightKg(), request.age());
        UserProfile draft = new UserProfile();
        draft.setUserId(userId);
        draft.setGender(request.gender());
        draft.setAge(request.age());
        draft.setHeightCm(toScale(request.heightCm()));
        draft.setWeightKg(toScale(request.weightKg()));
        draft.setBodyType(request.bodyType());
        return goalSuggestion(draft, userId);
    }

    private GoalSuggestionResponse goalSuggestion(UserProfile profile, Long userId) {
        BigDecimal bmi = calculateBmi(profile.getHeightCm(), profile.getWeightKg());
        GoalRecommendation recommendation = resolveGoalSuggestion(profile);
        GoalSuggestionResponse response = new GoalSuggestionResponse(
                recommendation.goal(),
                recommendation.reason(),
                toDouble(bmi),
                resolveBmiLevel(bmi),
                buildAlternatives(recommendation.goal())
        );
        log.info(
                "goal suggestion generated, scenario=profile-goal-suggestion, userId={}, bmi={}, recommendedGoal={}, rule={}",
                userId,
                response.bmi(),
                response.recommendedGoal(),
                recommendation.rule()
        );
        return response;
    }

    // ==================== 4. GET /api/v1/profile/body-data ====================

    /**
     * 身体数据历史：按 recordedAt 倒序，默认 10 条、最多 100 条。
     */
    public List<BodyDataResponse> listBodyData(Integer limit) {
        CurrentUser currentUser = CurrentUserProvider.require();
        Long userId = currentUser.getId();

        int size = resolveLimit(limit, DEFAULT_BODY_DATA_LIMIT, MAX_BODY_DATA_LIMIT);
        LambdaQueryWrapper<BodyData> wrapper = new LambdaQueryWrapper<BodyData>()
                .eq(BodyData::getUserId, userId)
                .orderByDesc(BodyData::getRecordedAt)
                .orderByDesc(BodyData::getId)
                .last("LIMIT " + size);
        List<BodyData> records = bodyDataMapper.selectList(wrapper);
        log.info("body data listed, scenario=body-data-list, userId={}, limit={}, size={}", userId, size, records.size());
        return records.stream().map(this::toBodyDataResponse).toList();
    }

    // ==================== 5. POST /api/v1/profile/body-data ====================

    /**
     * 复测录入：身高可空（为空取档案身高），BMI/评分重新计算，并同步回写档案的身高体重体型。
     */
    @Transactional(rollbackFor = Exception.class)
    public BodyDataResponse saveBodyData(BodyDataSaveRequest request) {
        CurrentUser currentUser = CurrentUserProvider.require();
        Long userId = currentUser.getId();

        try {
            UserProfile profile = findProfile(userId);
            if (profile == null) {
                log.error("body data save failed, scenario=body-data-save, userId={}, reason=profile-not-found", userId);
                throw new BusinessException(ErrorCode.PROFILE_NOT_FOUND, "请先完善个人档案，再进行身体数据复测");
            }

            BigDecimal heightCm = request.heightCm() != null ? toScale(request.heightCm()) : profile.getHeightCm();
            BigDecimal weightKg = toScale(request.weightKg());
            if (heightCm == null) {
                log.error("body data save failed, scenario=body-data-save, userId={}, reason=height-missing", userId);
                throw new BusinessException(ErrorCode.BODY_DATA_INVALID, "缺少身高，请在复测时填写身高或在档案中补充身高");
            }
            validateBodyValue(heightCm.doubleValue(), weightKg.doubleValue(), null);

            String bodyType = StringUtils.hasText(request.bodyType()) ? request.bodyType() : profile.getBodyType();
            String remark = StringUtils.hasText(request.remark()) ? request.remark() : "复测数据";
            BodyData bodyData = saveBodyData(userId, heightCm, weightKg, bodyType, profile.getAge(), remark);

            // 同步档案中的身高体重体型，保证后续推荐使用最新数据
            profile.setHeightCm(heightCm);
            profile.setWeightKg(weightKg);
            profile.setBodyType(bodyType);
            userProfileMapper.updateById(profile);

            log.info(
                    "body data saved, scenario=body-data-save, userId={}, bodyDataId={}, bmi={}, score={}",
                    userId,
                    bodyData.getId(),
                    bodyData.getBmi(),
                    bodyData.getScore()
            );
            return toBodyDataResponse(bodyData);
        } catch (BusinessException exception) {
            log.error("body data save failed, scenario=body-data-save, userId={}", userId, exception);
            throw exception;
        }
    }

    // ==================== 目标推荐规则实现 ====================

    /**
     * 固定规则目标推荐。
     *
     * <p>优先级顺序（互斥，先命中先返回；阈值均为类常量）：
     * <ol>
     *   <li>{@link #RULE_BMI_OBESE} R1：BMI ≥ {@link #BMI_OVERWEIGHT_MAX}(28) 判为肥胖 → 减脂（健康类规则最高优先）；</li>
     *   <li>{@link #RULE_BMI_UNDERWEIGHT} R2：BMI &lt; {@link #BMI_NORMAL_MIN}(18.5) 判为偏瘦 → 增肌（健康类规则次高优先）；</li>
     *   <li>{@link #RULE_BMI_OVERWEIGHT_APPLE} R3：{@link #BMI_NORMAL_MAX}(24) ≤ BMI &lt; 28 且体型为苹果型 → 减脂；</li>
     *   <li>{@link #RULE_BMI_OVERWEIGHT_KEEP_MUSCLE} R3b：24 ≤ BMI &lt; 28 且主目标已是增肌 → 保留增肌；</li>
     *   <li>{@link #RULE_BMI_OVERWEIGHT_DAILY} R3c：24 ≤ BMI &lt; 28 且非苹果型 → 日常健身；</li>
     *   <li>{@link #RULE_RESPECT_USER_GOAL} R4b：18.5 ≤ BMI &lt; 28 且档案里已有 primaryGoal → 直接推荐该目标
     *       （建档问卷已确认过目标，不覆盖用户选择）；</li>
     *   <li>{@link #RULE_SHAPE_POSTURE} R4：体型为直筒型/梨型，或重点部位含肩颈 → 体态改善；</li>
     *   <li>{@link #RULE_DEFAULT_DAILY} R5：其余情况（含档案无身高体重）→ 日常健身。</li>
     * </ol>
     * 同样的档案输入永远得到同样的推荐（无随机、无时间因素）。
     */
    private GoalRecommendation resolveGoalSuggestion(UserProfile profile) {
        BigDecimal bmi = calculateBmi(profile.getHeightCm(), profile.getWeightKg());
        List<String> focusParts = split(profile.getFocusParts());

        // 无身体数据（身高体重缺失）时不做 BMI 判断，直接落到日常健身规则
        if (bmi == null) {
            log.warn("goal suggestion defaulted, scenario=profile-goal-suggestion, userId={}, reason=missing-body-data",
                    profile.getUserId());
            return new GoalRecommendation(GOAL_DAILY_FITNESS, RULE_DEFAULT_DAILY, "档案中还没有身高体重数据，先按日常健身打基础，补充身高体重后可获得更精准的目标建议。");
        }

        double bmiValue = bmi.doubleValue();
        String shapeDesc = describeShape(profile.getBodyType());

        // R1 肥胖：健康类规则最高优先
        if (bmiValue >= BMI_OVERWEIGHT_MAX) {
            return new GoalRecommendation(
                    GOAL_LOSE_FAT,
                    RULE_BMI_OBESE,
                    "你的 BMI 为 " + formatBmi(bmi) + "，已达到肥胖区间（≥28，"
                            + shapeDesc + "），减脂是当前收益最高的方向，建议以有氧 + 饮食控制为主。"
            );
        }

        // R2 偏瘦：健康类规则次高优先，先于“尊重用户已选目标”
        if (bmiValue < BMI_NORMAL_MIN) {
            return new GoalRecommendation(
                    GOAL_BUILD_MUSCLE,
                    RULE_BMI_UNDERWEIGHT,
                    "你的 BMI 为 " + formatBmi(bmi) + "，低于正常区间下界 18.5，属于偏瘦，"
                            + "建议以力量训练增肌配合加餐增重为主，避免过度有氧。"
            );
        }

        // R3 / R3b / R3c 超重区间（24 ≤ BMI < 28）
        if (bmiValue >= BMI_NORMAL_MAX) {
            if (BODY_TYPE_APPLE.equals(profile.getBodyType())) {
                return new GoalRecommendation(
                        GOAL_LOSE_FAT,
                        RULE_BMI_OVERWEIGHT_APPLE,
                        "你的 BMI 为 " + formatBmi(bmi) + "（超重区间 24-28）且体型偏苹果型，"
                                + "脂肪容易堆积在腰腹，建议优先减脂，再逐步增加力量训练。"
                );
            }
            if (GOAL_BUILD_MUSCLE.equals(profile.getPrimaryGoal())) {
                return new GoalRecommendation(
                        GOAL_BUILD_MUSCLE,
                        RULE_BMI_OVERWEIGHT_KEEP_MUSCLE,
                        "你的 BMI 为 " + formatBmi(bmi) + "（超重区间 24-28，但已接近正常区间），"
                                + "且你已选择增肌目标，建议保留增肌，同时每周安排 2-3 次有氧控制体脂。"
                );
            }
            return new GoalRecommendation(
                    GOAL_DAILY_FITNESS,
                    RULE_BMI_OVERWEIGHT_DAILY,
                    "你的 BMI 为 " + formatBmi(bmi) + "（超重区间 24-28），"
                            + "暂不需要专项减脂，建议先用规律的低强度有氧 + 全身力量养成运动习惯。"
            );
        }

        // R4b 尊重用户已在建档问卷里确认过的目标（此时 BMI 已在 18.5-28，健康类规则都不冲突）
        if (StringUtils.hasText(profile.getPrimaryGoal())) {
            return new GoalRecommendation(
                    profile.getPrimaryGoal(),
                    RULE_RESPECT_USER_GOAL,
                    "与你已选择的目标一致，当前身体指标未与该目标冲突（BMI 为 " + formatBmi(bmi)
                            + "，处于正常偏低至略超重区间，可按该目标安排训练）。"
            );
        }

        // R4 体态问题：体型偏直筒/梨型或重点部位含肩颈
        boolean postureShape = BODY_TYPE_STRAIGHT.equals(profile.getBodyType())
                || BODY_TYPE_PEAR.equals(profile.getBodyType());
        boolean postureFocus = focusParts.contains(FOCUS_PART_SHOULDER_NECK);
        if (postureShape || postureFocus) {
            String trigger = postureFocus
                    ? "你把肩颈列为了重点部位"
                    : "你的体型为" + shapeDesc;
            return new GoalRecommendation(
                    GOAL_POSTURE,
                    RULE_SHAPE_POSTURE,
                    "你的 BMI 为 " + formatBmi(bmi) + "，处于正常区间（18.5-24），"
                            + trigger + "，建议优先做体态改善，圆肩驼背与骨盆前倾调整后训练效率更高。"
            );
        }

        // R5 默认：日常健身
        return new GoalRecommendation(
                GOAL_DAILY_FITNESS,
                RULE_DEFAULT_DAILY,
                "你的 BMI 为 " + formatBmi(bmi) + "，处于正常区间（18.5-24）且无明显体态问题，"
                        + "建议以日常健身保持心肺与肌肉量，可按兴趣选择有氧或力量训练。"
        );
    }

    /**
     * 备选目标：返回除推荐目标外的 3 个候选，顺序固定，保证同一推荐结果下输出稳定。
     */
    private List<String> buildAlternatives(String recommendedGoal) {
        List<String> candidates = List.of(GOAL_DAILY_FITNESS, GOAL_BUILD_MUSCLE, GOAL_LOSE_FAT, GOAL_CARDIO, GOAL_POSTURE);
        return candidates.stream()
                .filter(goal -> !goal.equals(recommendedGoal))
                .limit(3)
                .toList();
    }

    /**
     * BMI 中文分级：&lt;18.5 偏瘦、18.5-24 正常、24-28 超重、≥28 肥胖；无数据返回“未知”。
     */
    private String resolveBmiLevel(BigDecimal bmi) {
        if (bmi == null) {
            return "未知";
        }
        double value = bmi.doubleValue();
        if (value < BMI_NORMAL_MIN) {
            return "偏瘦";
        }
        if (value < BMI_NORMAL_MAX) {
            return "正常";
        }
        if (value < BMI_OVERWEIGHT_MAX) {
            return "超重";
        }
        return "肥胖";
    }

    /** 体型中文描述，用于拼装可读的推荐理由 */
    private String describeShape(String bodyType) {
        if (bodyType == null) {
            return "体型未填写";
        }
        return switch (bodyType) {
            case BODY_TYPE_STRAIGHT -> "直筒型";
            case BODY_TYPE_PEAR -> "梨型";
            case "HOURGLASS" -> "沙漏型";
            case BODY_TYPE_APPLE -> "苹果型";
            default -> "体型未填写";
        };
    }

    /** 推荐结果内部载体：目标编码 + 命中规则编号 + 理由 */
    private record GoalRecommendation(String goal, String rule, String reason) {
    }

    // ==================== BMI / 身体评分 ====================

    /** 身高或体重缺失/非法时返回 null */
    private BigDecimal calculateBmi(BigDecimal heightCm, BigDecimal weightKg) {
        if (heightCm == null || weightKg == null
                || heightCm.compareTo(BigDecimal.ZERO) <= 0 || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        double heightM = heightCm.doubleValue() / 100.0;
        double bmi = weightKg.doubleValue() / (heightM * heightM);
        return BigDecimal.valueOf(bmi).setScale(BODY_VALUE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 身体评分（0-100），规则透明可解释：
     * <pre>
     * score = BMI 分项（满分 70） + 年龄分项（满分 30）
     * BMI 分项：BMI 落在正常区间 18.5-24 得满分 70；
     *          偏离 ≤2 个单位按每单位 7 分扣，超出部分按每单位 3 分扣，最低 0 分；
     *          （例：BMI 25 → 70 - 7 = 63；BMI 30 → 70 - 7*2 - 3*4 = 44）
     * 年龄分项：≤20 岁得满分 30；20-29 岁每大一岁扣 0.5 分；30 岁及以上每大一岁扣 1.0 分，最低 0 分；
     *          （例：24 岁 → 28 分；28 岁 → 26 分）；<b>age 为空时按满分 30 计</b>（视为青壮年，不扣年龄分），
     *          因此分步保存只填了身高体重也能得到一个完整的评分。
     * </pre>
     * 结论：BMI 越接近 18.5-24、年龄越小，得分越高；结果四舍五入后强制夹在 0-100 之间。
     */
    private int scoreOf(BigDecimal bmi, Integer age) {
        double bmiScore = SCORE_BMI_MAX;
        if (bmi != null) {
            double value = bmi.doubleValue();
            double deviation = value < BMI_NORMAL_MIN
                    ? BMI_NORMAL_MIN - value
                    : Math.max(0.0, value - BMI_NORMAL_MAX);
            if (deviation > 0) {
                double near = Math.min(deviation, SCORE_BMI_NEAR_DEVIATION);
                double far = Math.max(0.0, deviation - SCORE_BMI_NEAR_DEVIATION);
                bmiScore = SCORE_BMI_MAX
                        - near * SCORE_BMI_PENALTY_PER_UNIT
                        - far * SCORE_BMI_PENALTY_PER_UNIT_FAR;
            }
        }

        double ageScore = SCORE_AGE_MAX;
        if (age != null && age > 20) {
            int step1Years = Math.min(age, 30) - 20;
            int step2Years = Math.max(0, age - 30);
            ageScore = SCORE_AGE_BASE
                    - step1Years * SCORE_AGE_DECAY_STEP_1
                    - step2Years * SCORE_AGE_DECAY_STEP_2;
        }

        double total = Math.max(0.0, bmiScore) + Math.max(0.0, ageScore);
        return (int) Math.round(Math.min(SCORE_FULL, total));
    }

    // ==================== 内部工具 ====================

    /** 按用户查询档案（逻辑删除由 @TableLogic 自动过滤） */
    private UserProfile findProfile(Long userId) {
        return userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
    }

    /**
     * 取档案，没有就惰性创建（GET 与 PUT 共用）；处理唯一键 uk_user_profile_user 与逻辑删除的冲突。
     *
     * <p>背景：唯一键只约束 user_id 列，逻辑删除的历史档案仍占用该 user_id，直接 insert 会抛
     * DuplicateKeyException（出现过“删号重建”就会触发）。处理策略：
     * <ul>
     *   <li>先按正常流程插入，成功即返回；</li>
     *   <li>插入冲突 → 再用 selectOne 查一次（例如并发下已被其它请求建好，此时能直接查到）；</li>
     *   <li>仍查不到说明冲突来自 deleted=1 的历史档案：把该行 deleted 置回 0 复活复用
     *       （@TableLogic 的 where 条件固定为“字段=未删除值”，所以要显式 set(deleted, 1) 才能定位到那行）；</li>
     *   <li>连历史行都没有则抛 CONFLICT + 中文提示，不抛 500。</li>
     * </ul>
     * 方法内部已完成插入，返回的实体已带主键。
     */
    private UserProfile createProfileOrRecover(Long userId) {
        UserProfile candidate = newDefaultProfile(userId);
        try {
            userProfileMapper.insert(candidate);
            log.info("profile lazily created, scenario=profile-get, userId={}, profileId={}", userId, candidate.getId());
            return candidate;
        } catch (DuplicateKeyException exception) {
            log.warn("profile insert conflict, scenario=profile-get, userId={}", userId, exception);
            UserProfile created = findProfile(userId);
            if (created != null) {
                log.warn("profile insert conflicted, scenario=profile-get, userId={}, profileId={}, reason=concurrent-created",
                        userId, created.getId());
                return created;
            }

            UserProfile deleted = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                    .eq(UserProfile::getUserId, userId)
                    .eq(UserProfile::getDeleted, DELETED_YES));
            if (deleted != null) {
                userProfileMapper.update(null, new LambdaUpdateWrapper<UserProfile>()
                        .eq(UserProfile::getId, deleted.getId())
                        .eq(UserProfile::getDeleted, DELETED_YES)
                        .set(UserProfile::getDeleted, DELETED_NO));
                log.warn("profile restored from logically deleted row, scenario=profile-get, userId={}, profileId={}",
                        userId, deleted.getId());
                deleted.setDeleted(DELETED_NO);
                return deleted;
            }

            log.error("profile create failed, scenario=profile-get, userId={}, reason=duplicate-key-without-row", userId);
            throw new BusinessException(ErrorCode.CONFLICT, "该账号存在遗留的历史档案数据，请清理 user_profile 后重试");
        }
    }

    /** 构造一条默认档案（未完成建档，逻辑删除值显式补 0，便于查询条件直接使用） */
    private UserProfile newDefaultProfile(Long userId) {
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setGender(DEFAULT_GENDER);
        profile.setRemindEnabled(0);
        profile.setProfileCompleted(PROFILE_INCOMPLETED);
        profile.setDeleted(DELETED_NO);
        return profile;
    }

    /**
     * 分步保存的核心：只把请求里带上的字段合并进档案，未传字段保留库中原值。
     * List 字段：null 不处理、空数组清空、非空数组覆盖。
     */
    private void applyRequest(UserProfile profile, ProfileSaveRequest request) {
        if (StringUtils.hasText(request.gender())) {
            profile.setGender(request.gender());
        } else if (!StringUtils.hasText(profile.getGender())) {
            // 新建档案且未填性别时兜底 UNKNOWN
            profile.setGender(DEFAULT_GENDER);
        }
        if (request.age() != null) {
            profile.setAge(request.age());
        }
        if (request.heightCm() != null) {
            profile.setHeightCm(toScale(request.heightCm()));
        }
        if (request.weightKg() != null) {
            profile.setWeightKg(toScale(request.weightKg()));
        }
        if (StringUtils.hasText(request.bodyType())) {
            profile.setBodyType(request.bodyType());
        }
        if (StringUtils.hasText(request.primaryGoal())) {
            profile.setPrimaryGoal(request.primaryGoal());
        }
        if (request.subGoals() != null) {
            profile.setSubGoals(join(cleanList(request.subGoals())));
        }
        if (request.focusParts() != null) {
            profile.setFocusParts(join(cleanList(request.focusParts())));
        }
        if (request.sportTypes() != null) {
            profile.setSportTypes(join(cleanList(request.sportTypes())));
        }
        if (request.weeklyDays() != null) {
            profile.setWeeklyDays(join(cleanWeeklyDays(request.weeklyDays())));
        }
        if (request.dailyMinutes() != null) {
            profile.setDailyMinutes(request.dailyMinutes());
        }
        if (request.remindEnabled() != null) {
            profile.setRemindEnabled(request.remindEnabled() >= 1 ? 1 : 0);
        }
        // 提醒时间：null 即清空（约定见 saveProfile 注释）
        profile.setRemindTime(StringUtils.hasText(request.remindTime()) ? request.remindTime() : null);
        if (request.injuries() != null) {
            profile.setInjuries(join(cleanList(request.injuries())));
        }
        if (request.trainLimits() != null) {
            profile.setTrainLimits(join(cleanList(request.trainLimits())));
        }
    }

    /** 身高/体重/年龄范围校验，只校验有值的情况；越界抛 BODY_DATA_INVALID + 中文提示 */
    private void validateBodyValue(Double heightCm, Double weightKg, Integer age) {
        if (heightCm != null && (heightCm < MIN_HEIGHT_CM || heightCm > MAX_HEIGHT_CM)) {
            throw new BusinessException(ErrorCode.BODY_DATA_INVALID, MSG_HEIGHT_RANGE);
        }
        if (weightKg != null && (weightKg < MIN_WEIGHT_KG || weightKg > MAX_WEIGHT_KG)) {
            throw new BusinessException(ErrorCode.BODY_DATA_INVALID, MSG_WEIGHT_RANGE);
        }
        if (age != null && (age < MIN_AGE || age > MAX_AGE)) {
            throw new BusinessException(ErrorCode.BODY_DATA_INVALID, MSG_AGE_RANGE);
        }
    }

    /**
     * 建档完成判定（按合并后的档案状态）：gender、age、heightCm、weightKg、primaryGoal 齐全，
     * weeklyDays 非空，且 dailyMinutes &gt; 0。缺 age 时不算完成（允许分步保存）。
     */
    private boolean isProfileComplete(UserProfile profile) {
        return StringUtils.hasText(profile.getGender())
                && profile.getAge() != null
                && profile.getHeightCm() != null
                && profile.getWeightKg() != null
                && StringUtils.hasText(profile.getPrimaryGoal())
                && !CollectionUtils.isEmpty(splitInt(profile.getWeeklyDays()))
                && profile.getDailyMinutes() != null
                && profile.getDailyMinutes() > 0;
    }

    /**
     * 写入一条 body_data（bmi、score 重新计算），recordedAt 取当前时间。
     * 身高或体重缺失时返回 null，不写入（调用方已做校验，这里只做兜底）。
     * 评分中的年龄取档案年龄，年龄为空时视为青壮年（不扣年龄分）。
     */
    private BodyData saveBodyData(Long userId, BigDecimal heightCm, BigDecimal weightKg, String bodyType,
                                  Integer age, String remark) {
        if (heightCm == null || weightKg == null) {
            return null;
        }
        BigDecimal bmi = calculateBmi(heightCm, weightKg);
        BodyData bodyData = new BodyData();
        bodyData.setUserId(userId);
        bodyData.setHeightCm(heightCm);
        bodyData.setWeightKg(weightKg);
        bodyData.setBmi(bmi);
        bodyData.setBodyType(bodyType);
        bodyData.setScore(scoreOf(bmi, age));
        bodyData.setRemark(remark);
        bodyData.setRecordedAt(LocalDateTime.now());
        bodyDataMapper.insert(bodyData);
        return bodyData;
    }

    /** 组装档案出参（含 BMI 中文分级与最近一次身体评分；合并后缺身高或体重时 bmi=null） */
    private ProfileResponse buildProfileResponse(Long userId, UserProfile profile) {
        BigDecimal bmi = calculateBmi(profile.getHeightCm(), profile.getWeightKg());
        return new ProfileResponse(
                userId,
                profile.getGender(),
                profile.getAge(),
                toDouble(profile.getHeightCm()),
                toDouble(profile.getWeightKg()),
                toDouble(bmi),
                resolveBmiLevel(bmi),
                findLatestScore(userId),
                profile.getBodyType(),
                profile.getPrimaryGoal(),
                split(profile.getSubGoals()),
                split(profile.getFocusParts()),
                split(profile.getSportTypes()),
                splitInt(profile.getWeeklyDays()),
                profile.getDailyMinutes(),
                profile.getRemindEnabled(),
                profile.getRemindTime(),
                split(profile.getInjuries()),
                split(profile.getTrainLimits()),
                profile.getProfileCompleted()
        );
    }

    /** 最近一条身体数据的评分，用于档案页展示 */
    private Integer findLatestScore(Long userId) {
        BodyData latest = bodyDataMapper.selectOne(new LambdaQueryWrapper<BodyData>()
                .eq(BodyData::getUserId, userId)
                .orderByDesc(BodyData::getRecordedAt)
                .orderByDesc(BodyData::getId)
                .last("LIMIT 1"));
        return latest == null ? null : latest.getScore();
    }

    private BodyDataResponse toBodyDataResponse(BodyData bodyData) {
        return new BodyDataResponse(
                bodyData.getId(),
                toDouble(bodyData.getHeightCm()),
                toDouble(bodyData.getWeightKg()),
                toDouble(bodyData.getBmi()),
                bodyData.getBodyType(),
                bodyData.getScore(),
                bodyData.getRemark(),
                bodyData.getRecordedAt()
        );
    }

    /** 训练日：去重 + 升序排序，保持 "1,3,5" 这类可读存储形态 */
    private List<Integer> cleanWeeklyDays(List<Integer> values) {
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(day -> Math.max(1, Math.min(7, day)))
                .distinct()
                .sorted()
                .toList();
    }

    /** 字符串列表：去空、去首尾空格、去重并保持原顺序 */
    private List<String> cleanList(List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    /** 列表 → 逗号分隔字符串；空列表存 null */
    private String join(List<?> values) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        return values.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    /** 逗号分隔字符串 → 列表；空值返回空列表（Jackson 序列化为 []） */
    private List<String> split(String value) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    /** 逗号分隔字符串 → 整数列表，非法片段直接跳过 */
    private List<Integer> splitInt(String value) {
        List<String> parts = split(value);
        if (parts.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> result = new ArrayList<>(parts.size());
        for (String part : parts) {
            try {
                result.add(Integer.valueOf(part));
            } catch (NumberFormatException exception) {
                log.warn("weekly days parse skipped, scenario=profile-read, userId={}, value={}",
                        CurrentUserProvider.require().getId(), part, exception);
            }
        }
        return result;
    }

    /** 保留 1 位小数，四舍五入（与库中 DECIMAL(5,1) 对齐） */
    private BigDecimal toScale(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(BODY_VALUE_SCALE, RoundingMode.HALF_UP);
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private String formatBmi(BigDecimal bmi) {
        return bmi.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    /** limit 兜底：为空或 ≤0 取默认值（10），超过上限（100）取上限 */
    private int resolveLimit(Integer limit, int defaultLimit, int maxLimit) {
        if (limit == null || limit <= 0) {
            return defaultLimit;
        }
        return Math.min(limit, maxLimit);
    }
}
