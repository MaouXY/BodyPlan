package com.bodyplan.module.admin.dto.user;

import java.time.LocalDateTime;

/**
 * 管理端用户详情中的最近一次体能测试。
 *
 * @param id          测试ID
 * @param pushUp      一次可完成俯卧撑个数
 * @param squat       一次可完成深蹲个数
 * @param crunch      一次可完成卷腹个数
 * @param stairStatus 上五楼状态
 * @param level       体能等级：L1 / L2 / L3
 * @param testedAt    测试时间
 */
public record AdminFitnessTestBrief(
        Long id,
        Integer pushUp,
        Integer squat,
        Integer crunch,
        String stairStatus,
        String level,
        LocalDateTime testedAt
) {
}
