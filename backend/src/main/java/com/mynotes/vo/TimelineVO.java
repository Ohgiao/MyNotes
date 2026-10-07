package com.mynotes.vo;

import java.util.List;

public record TimelineVO(List<TimelineDayVO> days, String nextCursor) {
}
