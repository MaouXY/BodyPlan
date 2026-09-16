package com.bodyplan.module.profile.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 身体数据复测入参。
 *
 * <p>heightCm 可为空：为空时取档案中的身高；档案也没有身高则抛 BODY_DATA_INVALID。
 */
public record BodyDataSaveRequest(

        /** 身高（cm）：100-250，可空 */
        @DecimalMin(value = "100.0", message = "身高必须大于等于 100cm")
        @DecimalMax(value = "250.0", message = "身高必须小于等于 250cm")
        Double heightCm,

        /** 体重（kg）：25-250，必填 */
        @NotNull(message = "体重不能为空")
        @DecimalMin(value = "25.0", message = "体重必须大于等于 25kg")
        @DecimalMax(value = "250.0", message = "体重必须小于等于 250kg")
        Double weightKg,

        /** 身体类型：STRAIGHT / PEAR / HOURGLASS / APPLE，可空 */
        @Pattern(regexp = "STRAIGHT|PEAR|HOURGLASS|APPLE", message = "身体类型只能是 STRAIGHT、PEAR、HOURGLASS 或 APPLE")
        String bodyType,

        /** 备注，如“两周后复测”，可空 */
        @Size(max = 255, message = "备注不能超过 255 个字符")
        String remark
) {
}
