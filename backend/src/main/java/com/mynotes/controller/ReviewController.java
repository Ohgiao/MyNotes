package com.mynotes.controller;

import com.mynotes.dto.ReviewResultDTO;
import com.mynotes.service.ReviewService;
import com.mynotes.vo.ReviewItemVO;
import com.mynotes.vo.ReviewResultVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/today")
    public List<ReviewItemVO> today(@RequestParam(defaultValue = "20") int limit) {
        return reviewService.due(limit);
    }

    @PostMapping("/{id}/result")
    public ReviewResultVO result(@PathVariable Long id, @Valid @RequestBody ReviewResultDTO dto) {
        return reviewService.submit(id, dto.outcome());
    }
}
