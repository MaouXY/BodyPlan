package com.bodyplan.module.training.support;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 逗号分隔字符串与集合的互转。建档问卷里的多选字段（重点部位、偏好类型、伤病、限制等）统一按此约定落库。
 */
public final class TagValues {

    private static final String SEPARATOR = ",";

    private TagValues() {
    }

    public static List<String> split(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(SEPARATOR))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    public static String join(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        String joined = values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(SEPARATOR));
        return StringUtils.hasText(joined) ? joined : null;
    }
}
