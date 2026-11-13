package com.bodyplan.module.record.dto;

/**
 * 打卡请求体。
 *
 * <p>旧打卡接口保留可空字段兼容；新自选运动接口要求 exerciseId 与 1~300 分钟时长。</p>
 *
 * <p>训练日期与感受这里用 String 接收而不是 LocalDate，是为了把「格式错误」在 Service 里
 * 转成带中文提示的 BAD_REQUEST，避免 Jackson 反序列化失败被全局异常处理器统一成「请求体格式错误」。</p>
 *
 * @param planItemId   计划明细ID，自由训练可为空
 * @param planId       计划ID，为空时：有明细取明细所属计划，否则取当前 ACTIVE 计划，都没有则为空（自由训练）
 * @param trainingDate 训练日期，格式 yyyy-MM-dd；带 planItemId 时该值被忽略（以明细的 training_date 为准），
 *                     只有自由训练才使用，为空时取今天
 * @param durationMin  本次训练时长（分钟），可空且不能为负数
 * @param feeling      训练感受：EASY / NORMAL / HARD，可空
 * @param remark       备注，可空且长度不超过 255
 * @param exerciseId   自选运动ID；计划打卡由计划明细确定，不接受替换成其他动作
 */
public record CheckinSaveRequest(
        Long planItemId,
        Long planId,
        String trainingDate,
        Integer durationMin,
        String feeling,
        String remark,
        Long exerciseId
) {
    /** 兼容原有计划打卡调用；自由运动选择使用 exerciseId。 */
    public CheckinSaveRequest(Long planItemId, Long planId, String trainingDate,
                              Integer durationMin, String feeling, String remark) {
        this(planItemId, planId, trainingDate, durationMin, feeling, remark, null);
    }
}
