package com.bodyplan.module.coaching;

import com.bodyplan.common.exception.BusinessException;
import com.bodyplan.common.security.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class CoachServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final CoachService service = new CoachService(jdbc, mock(PasswordEncoder.class));
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    private void login(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                CurrentUser.builder().principalId(88L).roleCode(role).build(), null, List.of()));
    }
    @Test void coachCannotReadUnassignedStudent() {
        login("COACH");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(42L), eq(88L))).thenReturn(0);
        assertThrows(BusinessException.class, () -> service.student(42, java.time.LocalDate.now()));
        verify(jdbc, never()).queryForMap(anyString(), any(Object[].class));
    }
    @Test void coachCannotReadAnotherUsersMessage() {
        login("USER");
        when(jdbc.queryForObject(anyString(),eq(Integer.class),eq(9L),eq(88L))).thenReturn(0);
        assertThrows(BusinessException.class, () -> service.readMessage(9));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
}
