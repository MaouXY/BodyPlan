package com.bodyplan.module.profile.service;

import com.bodyplan.common.exception.BusinessException;
import com.bodyplan.common.exception.ErrorCode;
import com.bodyplan.common.security.CurrentUser;
import com.bodyplan.module.fitness.dto.FitnessTestSaveRequest;
import com.bodyplan.module.fitness.service.FitnessTestService;
import com.bodyplan.module.profile.dto.ProfileCompleteRequest;
import com.bodyplan.module.profile.dto.ProfileSaveRequest;
import com.bodyplan.module.training.dto.TrainingPlanResponse;
import com.bodyplan.module.training.service.TrainingPlanService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProfileWorkflowServiceTest {
    private final ProfileService profile = mock(ProfileService.class);
    private final FitnessTestService fitness = mock(FitnessTestService.class);
    private final TrainingPlanService plans = mock(TrainingPlanService.class);
    private final RecordingTransactions transactions = new RecordingTransactions();
    private ProfileWorkflowService service;
    private ProfileCompleteRequest request;

    @BeforeEach
    void setUp() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                CurrentUser.builder().principalId(42L).roleCode("USER").build(), null, List.of()));
        var proxy = new ProxyFactory(new ProfileWorkflowService(profile, fitness, plans));
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        service = (ProfileWorkflowService) proxy.getProxy();
        var data = new ObjectMapper().readValue("""
                {"gender":"MALE","age":28,"heightCm":175,"weightKg":78,"primaryGoal":"LOSE_FAT",
                 "weeklyDays":[1,3,5],"dailyMinutes":30}
                """, ProfileSaveRequest.class);
        request = new ProfileCompleteRequest(data, new FitnessTestSaveRequest(15, 25, 20, "SLIGHT_HARD"));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void commitsOnlyAfterProfileFitnessAndPlanSucceed() {
        var plan = mock(TrainingPlanResponse.class);
        when(plan.id()).thenReturn(100L);
        when(plans.generate(any())).thenReturn(plan);
        assertSame(plan, service.complete(request));
        var sequence = inOrder(profile, fitness, plans);
        sequence.verify(profile).saveProfile(request.profile());
        sequence.verify(fitness).saveTest(request.fitness());
        sequence.verify(plans).generate(argThat(r -> Boolean.TRUE.equals(r.force())));
        assertEquals(1, transactions.commits);
        assertEquals(0, transactions.rollbacks);
    }

    @Test
    void recommendationFailureRollsBackWholeWorkflow() {
        var failure = new BusinessException(ErrorCode.PLAN_GENERATE_FAILED, "没有候选动作");
        when(plans.generate(any())).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class, () -> service.complete(request)));
        verify(profile).saveProfile(request.profile());
        verify(fitness).saveTest(request.fitness());
        assertEquals(0, transactions.commits);
        assertEquals(1, transactions.rollbacks);
    }

    @Test
    void dependencyFailureStopsBeforePlanAndRollsBack() {
        when(fitness.saveTest(any())).thenThrow(new IllegalStateException("simulated dependency failure"));
        assertThrows(IllegalStateException.class, () -> service.complete(request));
        verifyNoInteractions(plans);
        assertEquals(1, transactions.rollbacks);
    }

    // 验证 Spring 实际事务拦截器的提交/回滚边界，不连接用户数据库。
    static class RecordingTransactions extends AbstractPlatformTransactionManager {
        int commits;
        int rollbacks;
        protected Object doGetTransaction() { return new Object(); }
        protected void doBegin(Object transaction, TransactionDefinition definition) {}
        protected void doCommit(DefaultTransactionStatus status) { commits++; }
        protected void doRollback(DefaultTransactionStatus status) { rollbacks++; }
    }
}
