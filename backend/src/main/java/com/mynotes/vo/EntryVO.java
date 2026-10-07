package com.mynotes.vo;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record EntryVO(
        Long id,
        String type,
        String title,
        String contentMd,
        Long parentId,
        LocalDate sourceDate,
        String reviewStatus,
        int mastery,
        Instant nextReviewAt,
        int reviewCount,
        boolean pinned,
        Instant createdAt,
        Instant updatedAt,
        List<String> tags,
        EntryRelationsVO relations) {
}
