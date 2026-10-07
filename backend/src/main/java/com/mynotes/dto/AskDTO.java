package com.mynotes.dto;

import jakarta.validation.constraints.NotBlank;

public record AskDTO(@NotBlank(message = "问题不能为空") String question, Integer topK) {
}
