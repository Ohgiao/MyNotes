package com.mynotes.vo;

import java.time.LocalDate;

public record LinkItemVO(
        Long id,
        String type,
        String title,
        LocalDate sourceDate) {
}
