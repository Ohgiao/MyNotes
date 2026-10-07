package com.mynotes.vo;

import java.time.Instant;
import java.time.LocalDate;

public record ReviewItemVO(
        Long id,
        String title,
        String preview,
        int mastery,
        int reviewCount,
        Instant nextReviewAt,
        LocalDate sourceDate) {
}
