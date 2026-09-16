package com.bodyplan.module.fitness.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * 体能测试录入入参。
 *
 * <p>三项成绩都可以为空（表示“做不了”），服务层按固定规则折算分数：
 * <ul>
 *   <li>俯卧撑 pushUp：≥20 → 3 分，≥10 → 2 分，其余（含 null）→ 1 分；</li>
 *   <li>深蹲 squat：≥35 → 3 分，≥20 → 2 分，其余（含 null）→ 1 分；</li>
 *   <li>卷腹 crunch：≥30 → 3 分，≥15 → 2 分，其余（含 null）→ 1 分；</li>
 *   <li>上五楼状态 stairStatus：EASY → 3 分，SLIGHT_HARD → 2 分，HARD → 1 分，FAIL → 0 分（null → 1 分）。</li>
 * </ul>
 * 总分区间 4-12，≥10 为 L3、≥6 为 L2，否则 L1。
 */
public record FitnessTestSaveRequest(

        /** 一次可完成俯卧撑个数，可空，非负 */
        @Min(value = 0, message = "俯卧撑个数不能为负数")
        @Max(value = 500, message = "俯卧撑个数不能超过 500 个")
        Integer pushUp,

        /** 一次可完成深蹲个数，可空，非负 */
        @Min(value = 0, message = "深蹲个数不能为负数")
        @Max(value = 500, message = "深蹲个数不能超过 500 个")
        Integer squat,

        /** 一次可完成卷腹个数，可空，非负 */
        @Min(value = 0, message = "卷腹个数不能为负数")
        @Max(value = 500, message = "卷腹个数不能超过 500 个")
        Integer crunch,

        /** 上五楼状态：EASY / SLIGHT_HARD / HARD / FAIL，可空 */
        @Pattern(regexp = "EASY|SLIGHT_HARD|HARD|FAIL", message = "上五楼状态只能是 EASY、SLIGHT_HARD、HARD 或 FAIL")
        String stairStatus
) {
}
