package com.mynotes.vo;

import java.time.LocalDate;

public record CitationVO(
        int index,
        Long entryId,
        String type,
        String title,
        String snippet,
        LocalDate sourceDate,
        double score,
        String matchedBy) {
}
