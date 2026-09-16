package com.bodyplan.module.admin.dto.exercise;

/**
 * 管理端动作标签响应。
 *
 * @param id      标签ID
 * @param tagType 标签类型：PART/SPORT_TYPE/SCENE/EQUIPMENT/DIFFICULTY
 * @param tagCode 标签编码
 * @param tagName 标签名称
 * @param sortNo  排序号
 * @param status  状态：1 启用、0 停用
 */
public record AdminExerciseTagResponse(
        Long id,
        String tagType,
        String tagCode,
        String tagName,
        Integer sortNo,
        Integer status
) {
}
