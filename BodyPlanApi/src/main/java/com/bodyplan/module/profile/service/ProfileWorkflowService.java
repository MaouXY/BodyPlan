package com.bodyplan.module.profile.service;

import com.bodyplan.common.security.CurrentUserProvider;
import com.bodyplan.module.fitness.service.FitnessTestService;
import com.bodyplan.module.profile.dto.ProfileCompleteRequest;
import com.bodyplan.module.training.dto.PlanGenerateRequest;
import com.bodyplan.module.training.dto.TrainingPlanResponse;
import com.bodyplan.module.training.service.TrainingPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileWorkflowService {
    private final ProfileService profileService;
    private final FitnessTestService fitnessTestService;
    private final TrainingPlanService trainingPlanService;

    // 建档和复测共用事务，生成失败时连同身体数据、体能测试和旧计划状态一起回滚。
    @Transactional(rollbackFor = Exception.class)
    public TrainingPlanResponse complete(ProfileCompleteRequest request) {
        Long userId = CurrentUserProvider.require().getId();
        try {
            profileService.saveProfile(request.profile());
            fitnessTestService.saveTest(request.fitness());
            TrainingPlanResponse plan = trainingPlanService.generate(new PlanGenerateRequest(null, true, null));
            log.info("profile workflow completed, scenario=profile-complete, userId={}, planId={}", userId, plan.id());
            return plan;
        } catch (RuntimeException exception) {
            log.error("profile workflow failed and will roll back, scenario=profile-complete, userId={}", userId, exception);
            throw exception;
        }
    }
}
