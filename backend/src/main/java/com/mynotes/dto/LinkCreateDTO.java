package com.mynotes.dto;

import jakarta.validation.constraints.NotNull;

public record LinkCreateDTO(@NotNull(message = "需要提供目标条目 id") Long dstId) {
}
