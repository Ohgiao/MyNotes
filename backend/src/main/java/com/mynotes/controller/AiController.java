package com.mynotes.controller;

import com.mynotes.dto.AskDTO;
import com.mynotes.service.AiService;
import com.mynotes.vo.AiStatusVO;
import com.mynotes.vo.AskResultVO;
import com.mynotes.vo.ReindexResultVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/status")
    public AiStatusVO status() {
        return aiService.status();
    }

    @PostMapping("/reindex")
    public ReindexResultVO reindex(@RequestParam(defaultValue = "200") int limit) {
        return aiService.reindex(limit);
    }

    @PostMapping("/ask")
    public AskResultVO ask(@Valid @RequestBody AskDTO dto) {
        return aiService.ask(dto.question(), dto.topK() == null ? 5 : dto.topK());
    }
}
