package com.bodyplan.module.coaching;

import com.bodyplan.common.result.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CoachController {
    private final CoachService service;
    @GetMapping("/admin/coaches") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Map<String,Object>>> coaches() { return ApiResponse.success(service.coaches()); }
    @PostMapping("/admin/coaches") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> create(@Valid @RequestBody CoachModels.CreateCoach request) { return ApiResponse.success(service.createCoach(request)); }
    @GetMapping("/admin/coach-candidates") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Map<String,Object>>> candidates(@RequestParam(defaultValue="") String keyword) { return ApiResponse.success(service.candidates(keyword)); }
    @PutMapping("/admin/coach-assignment") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> assign(@Valid @RequestBody CoachModels.Assign request) { service.assign(request); return ApiResponse.success(); }
    @DeleteMapping("/admin/coach-assignment/{studentId}") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> unassign(@PathVariable long studentId) { service.unassign(studentId); return ApiResponse.success(); }
    @GetMapping("/coach/students") @PreAuthorize("hasAnyRole('ADMIN','COACH')")
    public ApiResponse<List<Map<String,Object>>> students(@RequestParam LocalDate date) { return ApiResponse.success(service.students(date)); }
    @GetMapping("/coach/students/{studentId}") @PreAuthorize("hasAnyRole('ADMIN','COACH')")
    public ApiResponse<Map<String,Object>> student(@PathVariable long studentId, @RequestParam LocalDate date) {
        return ApiResponse.success(service.student(studentId, date));
    }
    @PostMapping("/coach/students/{studentId}/reminders") @PreAuthorize("hasAnyRole('ADMIN','COACH')")
    public ApiResponse<String> remind(@PathVariable long studentId, @Valid @RequestBody CoachModels.Remind request) {
        return ApiResponse.success(service.remind(studentId, request.content()));
    }
    @GetMapping("/messages")
    public ApiResponse<Map<String,Object>> messages() { return ApiResponse.success(service.messages()); }
    @PutMapping("/messages/{id}/read")
    public ApiResponse<Void> read(@PathVariable long id) { service.readMessage(id); return ApiResponse.success(); }
}
