package com.bodyplan.module.record.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 今日任务响应。
 *
 * <p>用户没有 ACTIVE 计划时不做异常处理：planId / planName / planLevel 为空、items 为空数组、
 * totalCount=0，App 首次进入据此展示「去生成计划」引导。</p>
 *
 * <p>降级规则（计划明细的 day_index 是"周几"，training_date = start_date + (day_index - 1)，
 * 用户选的训练日不一定包含今天）：</p>
 * <ul>
 *   <li>今天有安排：hasTaskToday=true、taskDate=今天、items=今天的明细；</li>
 *   <li>今天没有安排：取离今天最近的一天（优先未来最近，没有未来日期就取过去最近），
 *       items=那天的明细、hasTaskToday=false、taskDate=那天、nextTrainingDate=那天；</li>
 *   <li>计划里一条带日期的明细都没有：items=[]、taskDate=null、nextTrainingDate=null、hasTaskToday=false。</li>
 * </ul>
 *
 * @param planId           当前 ACTIVE 计划ID，无计划时为空
 * @param planName         计划名称，无计划时为空
 * @param planLevel        计划难度 L1/L2/L3，无计划时为空
 * @param trainingDate     客户端口径的"今天"（固定为服务器当天），便于前端直接展示日期
 * @param hasTaskToday     今天是否确有训练安排
 * @param taskDate         本次返回的明细所属日期（=今天，或降级后的最近训练日）；无明细时为空
 * @param nextTrainingDate 下一个训练日：今天不是训练日时等于 taskDate；今天有安排或没有计划明细时为空
 * @param totalCount       本次返回的动作总数
 * @param doneCount        本次返回中已完成动作数（明细状态为 DONE 的数量）
 * @param items            本次返回的明细，按 sortNo 升序
 */
public record TodayTaskResponse(
        Long planId,
        String planName,
        String planLevel,
        LocalDate trainingDate,
        Boolean hasTaskToday,
        LocalDate taskDate,
        LocalDate nextTrainingDate,
        Integer totalCount,
        Integer doneCount,
        List<TodayTaskItemResponse> items
) {
}
