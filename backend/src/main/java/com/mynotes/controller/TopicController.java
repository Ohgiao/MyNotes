package com.mynotes.controller;

import com.mynotes.service.TopicService;
import com.mynotes.vo.TreeNodeVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping("/tree")
    public List<TreeNodeVO> tree() {
        return topicService.tree();
    }
}
