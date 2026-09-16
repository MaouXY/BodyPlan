package com.bodyplan.module.admin.dto.exercise;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端动作列表 / 详情响应。
 *
 * @param id           动作ID
 * @param name         动作名称
 * @param part         目标部位
 * @param sportType    运动类型
 * @param difficulty   难度：1 入门、2 进阶、3 挑战
 * @param scene        场景
 * @param equipment    器械
 * @param contraFlags  禁忌标签数组（数据库中为逗号拼接字符串）
 * @param videoUrl     视频地址
 * @param coverUrl     封面地址
 * @param keyPoints    动作要点
 * @param cautions     注意事项
 * @param suitableFor  适用人群
 * @param status       状态：1 上架、0 下架
 * @param sortNo       排序号
 * @param createdAt    创建时间
 * @param updatedAt    更新时间
 */
public record AdminExerciseResponse(
        Long id,
        String name,
        String part,
        String sportType,
        Integer difficulty,
        String scene,
        String equipment,
        List<String> contraFlags,
        String videoUrl,
        String coverUrl,
        String keyPoints,
        String cautions,
        String suitableFor,
        Integer status,
        Integer sortNo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
