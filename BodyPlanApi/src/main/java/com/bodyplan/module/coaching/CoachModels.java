package com.bodyplan.module.coaching;

import jakarta.validation.constraints.*;

public final class CoachModels {
    private CoachModels() {}
    public record CreateCoach(@NotBlank @Pattern(regexp="[a-zA-Z0-9_]{3,32}") String username,
                              @NotBlank @Size(min=6,max=72) String password,
                              @NotBlank @Size(max=64) String nickname) {}
    public record Assign(@NotNull Long studentId, @NotNull Long coachId) {}
    public record Remind(@Size(max=300) String content) {}
}
