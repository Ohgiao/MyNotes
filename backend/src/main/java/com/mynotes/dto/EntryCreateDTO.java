package com.mynotes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;

public record EntryCreateDTO(
        @Pattern(regexp = "log|note", message = "type 只能是 log 或 note") String type,
        String title,
        @NotBlank(message = "内容不能为空") String contentMd,
        LocalDate sourceDate,
        Long parentId,
        List<String> tagNames) {
}
