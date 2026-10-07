package com.mynotes.service.impl;

import com.mynotes.common.BadRequestException;
import com.mynotes.common.NotFoundException;
import com.mynotes.common.Previews;
import com.mynotes.entity.Entry;
import com.mynotes.mapper.EntryMapper;
import com.mynotes.service.ReviewService;
import com.mynotes.vo.ReviewItemVO;
import com.mynotes.vo.ReviewResultVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final int PREVIEW_MAX = 200;
    private static final int MAX_LIMIT = 50;

    /**
     * 最小可用的复习机制：三档结果直接映射到熟练度增减和下次复习间隔，
     * 熟练度到 3 就毕业出队。刻意不做 SM-2——个人数据量下参数噪声大于收益。
     */
    private static final Map<String, int[]> OUTCOMES = Map.of(
            "known", new int[]{1, 7},
            "vague", new int[]{0, 3},
            "forgot", new int[]{-1, 1});

    private final EntryMapper entryMapper;

    public ReviewServiceImpl(EntryMapper entryMapper) {
        this.entryMapper = entryMapper;
    }

    @Override
    public List<ReviewItemVO> due(int limit) {
        return entryMapper.findDueForReview(Math.clamp(limit, 1, MAX_LIMIT)).stream()
                .map(this::toItem)
                .toList();
    }

    @Override
    @Transactional
    public ReviewResultVO submit(Long id, String outcome) {
        int[] rule = OUTCOMES.get(outcome);
        if (rule == null) {
            throw new BadRequestException("outcome 只能是 known / vague / forgot");
        }
        if (entryMapper.applyReviewResult(id, rule[0], rule[1]) == 0) {
            throw new NotFoundException("没有待复习的笔记: " + id);
        }
        Entry updated = entryMapper.findById(id);
        return new ReviewResultVO(id, updated.getReviewStatus(), updated.getMastery(),
                updated.getNextReviewAt(), entryMapper.countDue());
    }

    private ReviewItemVO toItem(Entry entry) {
        return new ReviewItemVO(
                entry.getId(),
                entry.getTitle(),
                Previews.preview(entry.getContentPlain(), PREVIEW_MAX),
                entry.getMastery(),
                entry.getReviewCount(),
                entry.getNextReviewAt(),
                entry.getSourceDate());
    }
}
