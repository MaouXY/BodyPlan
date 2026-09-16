package com.bodyplan.module.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bodyplan.common.exception.BusinessException;
import com.bodyplan.common.exception.ErrorCode;
import com.bodyplan.module.fitness.entity.FitnessTest;
import com.bodyplan.module.profile.entity.UserProfile;
import com.bodyplan.module.training.entity.Exercise;
import com.bodyplan.module.training.mapper.ExerciseMapper;
import com.bodyplan.module.training.support.TagValues;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 计划推荐引擎：按“目标 + 体能等级 + 重点部位 + 伤病禁忌 + 场地器械”五要素，
 * 用三步排除法从动作库筛选动作，再用固定的组数/时长规则组装成一周计划。
 *
 * <p>三步排除法：
 * <ol>
 *   <li>伤病排除：动作 contra_flags 与用户 injuries 有交集则排除；</li>
 *   <li>场地器械排除：无器械只保留 equipment=NONE，宿舍/小空间排除户外动作，不能跳/怕吵排除跳跃类；</li>
 *   <li>难度排除：动作 difficulty 超过体能等级允许的上限则排除。</li>
 * </ol>
 * 剩余动作按“重点部位命中 &gt; 目标偏好的运动类型 &gt; 难度 &gt; 排序号”排序后轮转选入每一天，
 * 并保证每天有一个拉伸收操动作。所有阈值都是本类常量，便于答辩时解释规则来源。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanRecommendationService {

    /** 目标 → 运动类型偏好顺序（靠前者优先入选） */
    private static final Map<String, List<String>> GOAL_SPORT_PREFERENCE = Map.of(
            "LOSE_FAT", List.of("CARDIO", "HIIT", "STRENGTH", "STRETCH"),
            "BUILD_MUSCLE", List.of("STRENGTH", "HIIT", "STRETCH", "CARDIO"),
            "CARDIO", List.of("CARDIO", "RUNNING", "HIIT", "STRETCH"),
            "POSTURE", List.of("STRETCH", "YOGA", "STRENGTH", "CARDIO"),
            "LOCAL_STRENGTH", List.of("STRENGTH", "HIIT", "STRETCH"),
            "DAILY_FITNESS", List.of("STRETCH", "CARDIO", "STRENGTH", "YOGA")
    );
    private static final List<String> DEFAULT_SPORT_PREFERENCE = List.of("STRETCH", "CARDIO", "STRENGTH", "YOGA");

    /** 体能等级 → 允许的动作难度上限 */
    private static final Map<String, Integer> LEVEL_MAX_DIFFICULTY = Map.of("L1", 1, "L2", 2, "L3", 3);

    /** 每日时长换算动作数：每个动作（含组间休息）约 8 分钟，最少 3 个、最多 5 个 */
    private static final int MINUTES_PER_ACTION = 8;
    private static final int MIN_ACTIONS_PER_DAY = 3;
    private static final int MAX_ACTIONS_PER_DAY = 5;

    /** 计划周期：4 周 */
    public static final int PLAN_WEEKS = 4;

    private static final Set<String> STRETCH_TYPES = Set.of("STRETCH", "YOGA");
    private static final Set<String> DORM_ALLOWED_SCENES = Set.of("HOME", "DORM");
    /**
     * “无器械”允许的器械取值：瑜伽垫属于铺垫类辅助物，不算器械，因此 NONE 与 MAT 都放行；
     * DUMBBELL / BAND 才是需要排除的器械。若产品口径变化，只需调整这个集合。
     */
    private static final Set<String> NO_EQUIPMENT_ALLOWED = Set.of("NONE", "MAT");
    private static final List<String> JUMP_NAME_KEYWORDS = List.of("跳");
    private static final List<Integer> DEFAULT_WEEKLY_DAYS = List.of(1, 3, 5);

    private final ExerciseMapper exerciseMapper;

    public RecommendationPlan recommend(UserProfile profile, FitnessTest fitnessTest, String goal) {
        String level = StringUtils.hasText(fitnessTest.getLevel()) ? fitnessTest.getLevel() : "L1";
        int maxDifficulty = LEVEL_MAX_DIFFICULTY.getOrDefault(level, 1);

        Set<String> injuries = new HashSet<>(TagValues.split(profile.getInjuries()));
        injuries.remove("NONE");
        Set<String> limits = new LinkedHashSet<>(TagValues.split(profile.getTrainLimits()));
        Set<String> focusParts = new LinkedHashSet<>(TagValues.split(profile.getFocusParts()));
        List<Integer> weeklyDays = parseWeeklyDays(profile.getWeeklyDays());
        int dailyMinutes = profile.getDailyMinutes() == null || profile.getDailyMinutes() <= 0 ? 30 : profile.getDailyMinutes();
        List<String> preferredSports = GOAL_SPORT_PREFERENCE.getOrDefault(goal, DEFAULT_SPORT_PREFERENCE);

        List<Exercise> all = exerciseMapper.selectList(new LambdaQueryWrapper<Exercise>()
                .eq(Exercise::getStatus, 1)
                .orderByAsc(Exercise::getSortNo)
                .orderByAsc(Exercise::getId));

        // 第一步：伤病排除
        List<Exercise> injuryExcluded = all.stream()
                .filter(exercise -> intersects(TagValues.split(exercise.getContraFlags()), injuries))
                .toList();
        Set<Long> injuryExcludedIds = ids(injuryExcluded);
        List<Exercise> afterInjury = all.stream().filter(exercise -> !injuryExcludedIds.contains(exercise.getId())).toList();

        // 第二步：场地与器械限制排除
        List<Exercise> limitExcluded = afterInjury.stream()
                .filter(exercise -> !allowedByLimits(exercise, limits))
                .toList();
        Set<Long> limitExcludedIds = ids(limitExcluded);
        List<Exercise> afterLimits = afterInjury.stream().filter(exercise -> !limitExcludedIds.contains(exercise.getId())).toList();

        // 第三步：难度超限排除
        List<Exercise> difficultyExcluded = afterLimits.stream()
                .filter(exercise -> exercise.getDifficulty() != null && exercise.getDifficulty() > maxDifficulty)
                .toList();
        Set<Long> difficultyExcludedIds = ids(difficultyExcluded);
        List<Exercise> candidates = afterLimits.stream()
                .filter(exercise -> !difficultyExcludedIds.contains(exercise.getId()))
                .toList();

        if (candidates.isEmpty()) {
            log.warn("plan recommend rejected, scenario=plan-recommend, reason=no-candidate, userId={}, goal={}, level={}, injuries={}, limits={}",
                    profile.getUserId(), goal, level, injuries, limits);
            throw new BusinessException(ErrorCode.PLAN_GENERATE_FAILED,
                    "根据你的伤病与场地限制，暂时没有可推荐的动作，请调整限制条件或联系管理员补充动作库");
        }

        List<Exercise> ordered = candidates.stream()
                .sorted(Comparator
                        .comparingInt((Exercise exercise) -> focusParts.contains(exercise.getPart()) ? 0 : 1)
                        .thenComparingInt(exercise -> preferenceIndex(exercise.getSportType(), preferredSports))
                        .thenComparingInt(exercise -> exercise.getDifficulty() == null ? 1 : exercise.getDifficulty())
                        .thenComparingInt(exercise -> exercise.getSortNo() == null ? 0 : exercise.getSortNo())
                        .thenComparingLong(Exercise::getId))
                .toList();

        int actionsPerDay = Math.min(Math.max(Math.round((float) dailyMinutes / MINUTES_PER_ACTION), MIN_ACTIONS_PER_DAY), MAX_ACTIONS_PER_DAY);
        Exercise stretchCandidate = ordered.stream()
                .filter(exercise -> STRETCH_TYPES.contains(exercise.getSportType()))
                .findFirst()
                .orElse(null);

        List<RecommendedDay> days = new ArrayList<>();
        int offset = 0;
        for (Integer dayIndex : weeklyDays) {
            List<Exercise> picked = pickForDay(ordered, offset, actionsPerDay);
            offset += actionsPerDay;
            List<RecommendedItem> items = new ArrayList<>(picked.stream().map(this::toItem).toList());
            boolean hasStretch = items.stream().anyMatch(item -> STRETCH_TYPES.contains(item.exercise().getSportType()));
            if (!hasStretch && stretchCandidate != null) {
                // 保证每天都有拉伸收操动作
                items.add(toItem(stretchCandidate));
            }
            days.add(new RecommendedDay(dayIndex, items));
        }

        int excludedCount = injuryExcluded.size() + limitExcluded.size() + difficultyExcluded.size();
        String matchedRule = buildMatchedRule(goal, level, focusParts);
        String reason = buildReason(goal, level, maxDifficulty, focusParts, preferredSports, injuries, limits,
                injuryExcluded.size(), limitExcluded.size(), difficultyExcluded.size(), candidates.size(),
                ordered.size(), days.size(), actionsPerDay);

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("goal", goal);
        snapshot.put("level", level);
        snapshot.put("maxDifficulty", maxDifficulty);
        snapshot.put("focusParts", focusParts);
        snapshot.put("preferredSportTypes", preferredSports);
        snapshot.put("injuries", injuries);
        snapshot.put("trainLimits", limits);
        snapshot.put("weeklyDays", weeklyDays);
        snapshot.put("dailyMinutes", dailyMinutes);
        snapshot.put("actionsPerDay", actionsPerDay);
        snapshot.put("exerciseTotal", all.size());
        snapshot.put("injuryExcluded", injuryExcluded.size());
        snapshot.put("limitExcluded", limitExcluded.size());
        snapshot.put("difficultyExcluded", difficultyExcluded.size());
        snapshot.put("candidateTotal", candidates.size());

        log.info("plan recommended, scenario=plan-recommend, userId={}, goal={}, level={}, candidateTotal={}, injuryExcluded={}, limitExcluded={}, difficultyExcluded={}, dayCount={}, actionsPerDay={}",
                profile.getUserId(), goal, level, candidates.size(), injuryExcluded.size(), limitExcluded.size(),
                difficultyExcluded.size(), days.size(), actionsPerDay);

        return new RecommendationPlan(days, matchedRule, reason, excludedCount, snapshot);
    }

    private List<Exercise> pickForDay(List<Exercise> ordered, int offset, int count) {
        List<Exercise> picked = new ArrayList<>();
        Set<Long> pickedIds = new HashSet<>();
        int size = ordered.size();
        // 优先在整个候选池里轮转挑选不重复的动作
        for (int cursor = 0; cursor < size && picked.size() < count; cursor++) {
            Exercise candidate = ordered.get((offset + cursor) % size);
            if (pickedIds.add(candidate.getId())) {
                picked.add(candidate);
            }
        }
        // 候选动作本身不足时允许重复，保证每天动作数达标
        for (int cursor = 0; picked.size() < count; cursor++) {
            picked.add(ordered.get((offset + cursor) % size));
        }
        return picked;
    }

    private RecommendedItem toItem(Exercise exercise) {
        String sportType = StringUtils.hasText(exercise.getSportType()) ? exercise.getSportType() : "STRENGTH";
        int difficulty = exercise.getDifficulty() == null ? 1 : exercise.getDifficulty();
        return switch (sportType) {
            case "CARDIO" -> new RecommendedItem(exercise, 3, null, 45, 30);
            case "HIIT" -> new RecommendedItem(exercise, 4, null, 30, 30);
            case "RUNNING" -> new RecommendedItem(exercise, 1, null, 600, 0);
            case "STRETCH" -> new RecommendedItem(exercise, 2, null, 30, 15);
            case "YOGA" -> new RecommendedItem(exercise, 2, null, 45, 20);
            default -> new RecommendedItem(exercise, 3, difficulty <= 1 ? 12 : 10, null, 45);
        };
    }

    private boolean allowedByLimits(Exercise exercise, Set<String> limits) {
        if (limits.contains("NO_EQUIPMENT") && !NO_EQUIPMENT_ALLOWED.contains(exercise.getEquipment())) {
            return false;
        }
        if (limits.contains("DORM") && !DORM_ALLOWED_SCENES.contains(exercise.getScene())) {
            return false;
        }
        if (limits.contains("SMALL_SPACE") && "OUTDOOR".equals(exercise.getScene())) {
            return false;
        }
        return !(limits.contains("NO_JUMP") || limits.contains("NO_NOISE")) || !isJumpLike(exercise);
    }

    /**
     * 动作表没有单独的“跳跃”标签，这里用运动类型与名称做近似判断（HIIT 类与名称含“跳”的动作）。
     */
    private boolean isJumpLike(Exercise exercise) {
        if ("HIIT".equals(exercise.getSportType())) {
            return true;
        }
        String name = exercise.getName();
        return name != null && JUMP_NAME_KEYWORDS.stream().anyMatch(name::contains);
    }

    private boolean intersects(List<String> contraFlags, Set<String> injuries) {
        if (contraFlags.isEmpty() || injuries.isEmpty()) {
            return false;
        }
        return contraFlags.stream().anyMatch(injuries::contains);
    }

    private int preferenceIndex(String sportType, List<String> preferredSports) {
        int index = preferredSports.indexOf(sportType);
        return index < 0 ? preferredSports.size() : index;
    }

    private Set<Long> ids(List<Exercise> exercises) {
        Set<Long> result = new HashSet<>();
        exercises.forEach(exercise -> result.add(exercise.getId()));
        return result;
    }

    private List<Integer> parseWeeklyDays(String weeklyDays) {
        List<Integer> parsed = TagValues.split(weeklyDays).stream()
                .map(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException exception) {
                        return null;
                    }
                })
                .filter(value -> value != null && value >= 1 && value <= 7)
                .distinct()
                .sorted()
                .toList();
        return parsed.isEmpty() ? DEFAULT_WEEKLY_DAYS : parsed;
    }

    private String buildMatchedRule(String goal, String level, Set<String> focusParts) {
        StringBuilder builder = new StringBuilder("GOAL_").append(goal).append("+LEVEL_").append(level);
        if (!focusParts.isEmpty()) {
            builder.append("+FOCUS_").append(String.join("_", focusParts));
        }
        return builder.toString();
    }

    private String buildReason(String goal,
                               String level,
                               int maxDifficulty,
                               Set<String> focusParts,
                               List<String> preferredSports,
                               Set<String> injuries,
                               Set<String> limits,
                               int injuryExcluded,
                               int limitExcluded,
                               int difficultyExcluded,
                               int candidateTotal,
                               int orderedTotal,
                               int dayCount,
                               int actionsPerDay) {
        return "目标为 %s，体能等级 %s（动作难度上限 %d 级）；重点部位 %s，优先匹配 %s 类动作。"
                .formatted(goal, level, maxDifficulty,
                        focusParts.isEmpty() ? "未指定" : String.join("、", focusParts),
                        String.join(" > ", preferredSports))
                + "按三步排除法：伤病（%s）排除 %d 个动作，场地与器械限制（%s）排除 %d 个，难度超限排除 %d 个，"
                .formatted(injuries.isEmpty() ? "无" : String.join("、", injuries), injuryExcluded,
                        limits.isEmpty() ? "无" : String.join("、", limits), limitExcluded, difficultyExcluded)
                + "最终从 %d 个可用动作中选出 %d 个组成每周 %d 天、每天 %d 个动作的计划（每天含拉伸收操）。"
                .formatted(orderedTotal, candidateTotal, dayCount, actionsPerDay);
    }

    public record RecommendationPlan(
            List<RecommendedDay> days,
            String matchedRule,
            String reason,
            int excludedCount,
            Map<String, Object> inputSnapshot
    ) {
    }

    public record RecommendedDay(
            int dayIndex,
            List<RecommendedItem> items
    ) {
    }

    public record RecommendedItem(
            Exercise exercise,
            int sets,
            Integer reps,
            Integer durationSec,
            int restSec
    ) {
    }
}
