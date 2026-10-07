package com.mynotes.service;

import com.mynotes.vo.ReviewItemVO;
import com.mynotes.vo.ReviewResultVO;

import java.util.List;

public interface ReviewService {

    List<ReviewItemVO> due(int limit);

    ReviewResultVO submit(Long id, String outcome);
}
