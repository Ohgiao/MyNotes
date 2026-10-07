package com.mynotes.service;

import com.mynotes.vo.SearchItemVO;

import java.util.List;

public interface SearchService {

    List<SearchItemVO> search(String query, String type, int limit);
}
