package com.bodyplan.module.nutrition;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class NutritionModels {
    private NutritionModels() {}
    public record Food(String id, String name, String category, BigDecimal kcal, BigDecimal protein, BigDecimal fat,
                       BigDecimal carbohydrate, BigDecimal fiber, String source, boolean enabled) {}
    public record FoodSave(@NotBlank @Size(max=80) String name, @NotBlank @Size(max=30) String category,
                           @NotNull @DecimalMin("0") @DecimalMax("1000") BigDecimal kcal,
                           @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal protein,
                           @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal fat,
                           @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal carbohydrate,
                           @DecimalMin("0") @DecimalMax("100") BigDecimal fiber,
                           @NotBlank @Size(max=200) String source, boolean enabled) {}
    public record Entry(String id, LocalDate entryDate, String meal, String foodId, String foodName, BigDecimal grams,
                        BigDecimal kcal, BigDecimal protein, BigDecimal fat, BigDecimal carbohydrate,
                        BigDecimal fiber, String source) {}
    public record EntrySave(@NotNull LocalDate entryDate, @NotBlank String meal, @NotNull Long foodId,
                            @NotNull @DecimalMin("1") @DecimalMax("3000") BigDecimal grams) {}
    public record Target(@NotNull LocalDate targetDate, @NotBlank @Pattern(regexp="LOSE_FAT|BUILD_MUSCLE|MAINTAIN") String goal,
                         @Min(500) @Max(10000) int kcal, @Min(0) @Max(1000) int protein,
                         @Min(0) @Max(1000) int fat, @Min(0) @Max(2000) int carbohydrate, @Min(0) @Max(200) int fiber) {}
    public record Totals(BigDecimal kcal, BigDecimal protein, BigDecimal fat, BigDecimal carbohydrate,
                         BigDecimal fiber, boolean fiberComplete) {}
    public record Day(LocalDate date, List<Entry> entries, Totals totals, Target target) {}
}
