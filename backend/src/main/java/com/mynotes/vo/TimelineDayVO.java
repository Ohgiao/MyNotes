package com.mynotes.vo;

import java.time.LocalDate;
import java.util.List;

public record TimelineDayVO(LocalDate date, List<EntryListItemVO> entries) {
}
