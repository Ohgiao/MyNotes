package com.mynotes.vo;

import java.time.LocalDate;

public record SearchItemVO(
        Long id,
        String type,
        String title,
        String snippet,
        LocalDate sourceDate,
        boolean titleHit) {
}
