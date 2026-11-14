package com.bodyplan.module.activity;

import com.bodyplan.common.result.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sport-sessions")
@RequiredArgsConstructor
public class SportController {
    private final SportService service;
    @GetMapping public ApiResponse<List<SportModels.Summary>> history() { return ApiResponse.success(service.history()); }
    @GetMapping("/{id}") public ApiResponse<SportModels.Detail> detail(@PathVariable long id) { return ApiResponse.success(service.detail(id)); }
    @PostMapping public ApiResponse<SportModels.Summary> save(@Valid @RequestBody SportModels.Save request) {
        return ApiResponse.success(service.save(request));
    }
}
