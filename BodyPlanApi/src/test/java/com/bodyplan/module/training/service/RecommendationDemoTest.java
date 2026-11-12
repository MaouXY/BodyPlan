package com.bodyplan.module.training.service;

import com.bodyplan.common.exception.BusinessException;
import com.bodyplan.module.fitness.entity.FitnessTest;
import com.bodyplan.module.profile.entity.UserProfile;
import com.bodyplan.module.training.entity.Exercise;
import com.bodyplan.module.training.mapper.ExerciseMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecommendationDemoTest {
    @Test
    void restrictionsExcludeIncompatibleActionsAndChangesTakeEffect() {
        var mapper = mock(ExerciseMapper.class);
        var service = new PlanRecommendationService(mapper);
        var safe = exercise(1L, "拉伸", "STRETCH", 1, null, "NONE");
        var knee = exercise(2L, "深蹲", "STRENGTH", 1, "KNEE", "NONE");
        var weights = exercise(3L, "哑铃", "STRENGTH", 2, null, "DUMBBELL");
        when(mapper.selectList(any())).thenReturn(List.of(safe, knee, weights));
        var profile = new UserProfile();
        profile.setUserId(42L);
        profile.setWeeklyDays("1,4,6");
        profile.setDailyMinutes(30);
        profile.setInjuries("KNEE");
        profile.setTrainLimits("NO_EQUIPMENT");
        var fitness = new FitnessTest();
        fitness.setLevel("L2");
        var restricted = service.recommend(profile, fitness, "BUILD_MUSCLE");
        assertEquals(3, restricted.days().size());
        assertTrue(restricted.days().stream().flatMap(d -> d.items().stream()).allMatch(i -> i.exercise().getId() == 1L));
        profile.setInjuries(null);
        profile.setTrainLimits(null);
        var updated = service.recommend(profile, fitness, "BUILD_MUSCLE");
        assertTrue(updated.days().stream().flatMap(d -> d.items().stream()).anyMatch(i -> i.exercise().getId() == 3L));
    }

    @Test
    void emptyLibraryFailsInsteadOfMakingUpPlan() {
        var mapper = mock(ExerciseMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        var profile = new UserProfile();
        profile.setUserId(42L);
        profile.setWeeklyDays("1");
        var fitness = new FitnessTest();
        fitness.setLevel("L1");
        assertThrows(BusinessException.class, () -> new PlanRecommendationService(mapper).recommend(profile, fitness, "LOSE_FAT"));
    }

    private Exercise exercise(long id, String name, String sport, int difficulty, String contra, String equipment) {
        var result = new Exercise();
        result.setId(id);
        result.setName(name);
        result.setPart("LEG");
        result.setSportType(sport);
        result.setDifficulty(difficulty);
        result.setContraFlags(contra);
        result.setEquipment(equipment);
        result.setScene("HOME");
        result.setSortNo((int) id);
        return result;
    }
}
