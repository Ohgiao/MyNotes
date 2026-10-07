package com.mynotes.vo;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TreeNodeVO(
        Long id,
        String title,
        LocalDate sourceDate,
        String reviewStatus,
        int mastery,
        Instant nextReviewAt,
        List<TreeNodeVO> children) {
}
