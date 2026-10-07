package com.mynotes.vo;

import java.util.List;

public record AskResultVO(String question,
                          String answer,
                          List<CitationVO> citations,
                          String mode) {
}
