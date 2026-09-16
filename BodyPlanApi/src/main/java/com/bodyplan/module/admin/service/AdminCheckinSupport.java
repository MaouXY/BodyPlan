package com.bodyplan.module.admin.service;

import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 管理端打卡记录公共换算工具。
 */
public final class AdminCheckinSupport {

    private AdminCheckinSupport() {
    }

    /**
     * 把逗号分隔的字符串还原为数组，空值返回空数组。
     * 会剔除空白项并保持原顺序。
     */
    public static List<String> splitToArray(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String item : raw.split(",")) {
            if (StringUtils.hasText(item)) {
                values.add(item.trim());
            }
        }
        return values;
    }

    /**
     * 把数组拼为逗号分隔字符串，空集合返回 null（不写空串）。
     */
    public static String joinToList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> cleaned = values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        return cleaned.isEmpty() ? null : String.join(",", cleaned);
    }

    /**
     * 计算当前连续打卡天数。
     * 口径：以日期去重后自最新日期向前逐日判断；若最新打卡日早于昨天，连续天数记为 0
     * （即“断卡”后不再累计历史连续天数）。
     *
     * @param distinctDates 该用户的打卡日期集合（顺序不限）
     * @param today         参考日期（一般为 LocalDate.now()）
     */
    public static int currentStreak(Set<LocalDate> distinctDates, LocalDate today) {
        if (distinctDates == null || distinctDates.isEmpty()) {
            return 0;
        }
        TreeSet<LocalDate> sortedDesc = new TreeSet<>(Collections.reverseOrder());
        sortedDesc.addAll(distinctDates);
        LocalDate latest = sortedDesc.first();
        if (latest.isBefore(today.minusDays(1))) {
            return 0;
        }
        int streak = 1;
        LocalDate cursor = latest;
        for (LocalDate date : sortedDesc) {
            if (date.equals(cursor)) {
                continue;
            }
            if (date.equals(cursor.minusDays(1))) {
                streak++;
                cursor = date;
            } else {
                break;
            }
        }
        return streak;
    }

    /**
     * 计算当前连续打卡天数（入参为任意顺序的日期列表）。
     */
    public static int currentStreak(List<LocalDate> dates, LocalDate today) {
        if (dates == null || dates.isEmpty()) {
            return 0;
        }
        return currentStreak(new TreeSet<>(dates), today);
    }
}
