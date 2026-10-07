package com.mynotes.vo;

import java.time.Instant;

public record ReviewResultVO(
        Long id,
        String reviewStatus,
        int mastery,
        Instant nextReviewAt,
        int remainingDue) {
}
