package com.mynotes.controller;

import com.mynotes.service.SearchService;
import com.mynotes.vo.SearchItemVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public List<SearchItemVO> search(@RequestParam String q,
                                     @RequestParam(required = false) String type,
                                     @RequestParam(defaultValue = "50") int limit) {
        return searchService.search(q, type, limit);
    }
}
