package com.mynotes.vo;

import java.time.Instant;
import java.time.LocalDate;

public record EntryListItemVO(
        Long id,
        String type,
        String title,
        String preview,
        LocalDate sourceDate,
        boolean pinned,
        Instant updatedAt) {
}
