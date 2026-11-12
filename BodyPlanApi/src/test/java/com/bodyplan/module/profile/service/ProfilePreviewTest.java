package com.bodyplan.module.profile.service;

import com.bodyplan.common.exception.BusinessException;
import com.bodyplan.common.security.CurrentUser;
import com.bodyplan.module.profile.dto.ProfileSaveRequest;
import com.bodyplan.module.profile.entity.UserProfile;
import com.bodyplan.module.profile.mapper.BodyDataMapper;
import com.bodyplan.module.profile.mapper.UserProfileMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProfilePreviewTest {
    private final UserProfileMapper profiles = mock(UserProfileMapper.class);
    private final BodyDataMapper bodyData = mock(BodyDataMapper.class);
    private final ProfileService service = new ProfileService(profiles, bodyData);
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                CurrentUser.builder().principalId(42L).roleCode("USER").build(), null, List.of()));
    }

    @AfterEach
    void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test
    void previewUsesUnsavedBodyDataWithoutWritingAnything() throws Exception {
        var request = json.readValue("""
                {"gender":"MALE","age":28,"heightCm":175,"weightKg":95,"bodyType":"APPLE"}
                """, ProfileSaveRequest.class);
        var result = service.previewGoal(request);
        assertEquals("LOSE_FAT", result.recommendedGoal());
        assertEquals(31.0, result.bmi());
        verifyNoInteractions(profiles, bodyData);
    }

    @Test
    void changingDraftChangesRecommendationWithoutExistingProfile() throws Exception {
        var request = json.readValue("""
                {"gender":"FEMALE","age":24,"heightCm":163,"weightKg":45,"bodyType":"PEAR"}
                """, ProfileSaveRequest.class);
        assertEquals("BUILD_MUSCLE", service.previewGoal(request).recommendedGoal());
        verifyNoInteractions(profiles, bodyData);
    }

    @Test
    void missingBodyDataIsRejectedRatherThanDefaulted() throws Exception {
        assertThrows(BusinessException.class,
                () -> service.previewGoal(json.readValue("{}", ProfileSaveRequest.class)));
        verifyNoInteractions(profiles, bodyData);
    }

    @Test
    void existingProfileReadDoesNotAttemptDuplicateInsert() {
        var existing = new UserProfile();
        existing.setUserId(42L);
        existing.setProfileCompleted(0);
        when(profiles.selectOne(any())).thenReturn(existing);
        assertEquals(42L, service.getMyProfile().userId());
        verify(profiles, never()).insert(any(UserProfile.class));
    }
}
