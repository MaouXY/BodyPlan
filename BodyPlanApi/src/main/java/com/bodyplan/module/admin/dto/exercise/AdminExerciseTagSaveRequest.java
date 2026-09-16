package com.bodyplan.module.admin.dto.exercise;

import jakarta.validation.constraints.NotBlank;

/**
 * 管理端新增 / 更新动作标签请求。
 *
 * @param tagType 标签类型：PART/SPORT_TYPE/SCENE/EQUIPMENT/DIFFICULTY
 * @param tagCode 标签编码，同类型内唯一
 * @param tagName 标签名称
 * @param sortNo  排序号
 * @param status  状态：1 启用、0 停用，默认 1
 */
public record AdminExerciseTagSaveRequest(
        @NotBlank(message = "标签类型不能为空")
        String tagType,
        @NotBlank(message = "标签编码不能为空")
        String tagCode,
        @NotBlank(message = "标签名称不能为空")
        String tagName,
        Integer sortNo,
        Integer status
) {
}
