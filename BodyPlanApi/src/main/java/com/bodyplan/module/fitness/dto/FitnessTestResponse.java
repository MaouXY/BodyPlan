package com.bodyplan.module.fitness.dto;

import java.time.LocalDateTime;

/**
 * 体能测试出参。
 *
 * @param id          记录主键
 * @param pushUp      俯卧撑个数
 * @param squat       深蹲个数
 * @param crunch      卷腹个数
 * @param stairStatus 上五楼状态
 * @param level       体能等级：L1 入门 / L2 进阶 / L3 挑战
 * @param testedAt    测试时间
 */
public record FitnessTestResponse(
        Long id,
        Integer pushUp,
        Integer squat,
        Integer crunch,
        String stairStatus,
        String level,
        LocalDateTime testedAt
) {
}
