package com.mynotes.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ReviewResultDTO(
        @NotNull @Pattern(regexp = "known|vague|forgot", message = "outcome 只能是 known / vague / forgot")
        String outcome) {
}
