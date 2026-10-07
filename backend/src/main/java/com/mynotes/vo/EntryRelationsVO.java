package com.mynotes.vo;

import java.util.List;

public record EntryRelationsVO(List<LinkItemVO> outgoing, List<LinkItemVO> incoming) {

    public static EntryRelationsVO empty() {
        return new EntryRelationsVO(List.of(), List.of());
    }
}
