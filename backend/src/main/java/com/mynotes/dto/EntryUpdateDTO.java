package com.mynotes.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record EntryUpdateDTO(
        String title,
        @NotBlank(message = "内容不能为空") String contentMd,
        LocalDate sourceDate,
        Long parentId,
        Boolean pinned,
        List<String> tagNames) {
}
