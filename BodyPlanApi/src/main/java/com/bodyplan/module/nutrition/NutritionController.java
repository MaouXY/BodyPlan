package com.bodyplan.module.nutrition;

import com.bodyplan.common.result.ApiResponse;
import com.bodyplan.common.result.PageResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import static com.bodyplan.module.nutrition.NutritionModels.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NutritionController {
    private final NutritionService service;
    @GetMapping("/nutrition/foods")
    public ApiResponse<PageResult<Food>> foods(@RequestParam(defaultValue="") String keyword, @RequestParam(defaultValue="1") int page) {
        return ApiResponse.success(service.foods(keyword, page, false));
    }
    @GetMapping("/nutrition/day")
    public ApiResponse<Day> day(@RequestParam LocalDate date) { return ApiResponse.success(service.day(date)); }
    @PostMapping("/nutrition/entries")
    public ApiResponse<String> add(@Valid @RequestBody EntrySave request) { return ApiResponse.success(service.saveEntry(null, request)); }
    @PutMapping("/nutrition/entries/{id}")
    public ApiResponse<String> edit(@PathVariable long id, @Valid @RequestBody EntrySave request) { return ApiResponse.success(service.saveEntry(id, request)); }
    @DeleteMapping("/nutrition/entries/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) { service.deleteEntry(id); return ApiResponse.success(); }
    @PutMapping("/nutrition/target")
    public ApiResponse<Void> target(@Valid @RequestBody Target request) { service.saveTarget(request); return ApiResponse.success(); }
    @GetMapping("/admin/foods") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResult<Food>> adminFoods(@RequestParam(defaultValue="") String keyword, @RequestParam(defaultValue="1") int page) {
        return ApiResponse.success(service.foods(keyword, page, true));
    }
    @PostMapping("/admin/foods") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> addFood(@Valid @RequestBody FoodSave request) { return ApiResponse.success(service.saveFood(null, request)); }
    @PutMapping("/admin/foods/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> editFood(@PathVariable long id, @Valid @RequestBody FoodSave request) {
        return ApiResponse.success(service.saveFood(id, request));
    }
}
