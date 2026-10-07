package com.mynotes.service.impl;

import com.mynotes.common.Previews;
import com.mynotes.entity.Entry;
import com.mynotes.mapper.EntryMapper;
import com.mynotes.service.SearchService;
import com.mynotes.vo.SearchItemVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class SearchServiceImpl implements SearchService {

    private static final int SNIPPET_RADIUS = 60;
    private static final int MAX_LIMIT = 100;

    private final EntryMapper entryMapper;

    public SearchServiceImpl(EntryMapper entryMapper) {
        this.entryMapper = entryMapper;
    }

    @Override
    public List<SearchItemVO> search(String query, String type, int limit) {
        String q = query == null ? "" : query.strip();
        if (q.isEmpty()) {
            return List.of();
        }
        String needle = q.toLowerCase(Locale.ROOT);
        return entryMapper.search(toIlikePattern(q), type, Math.clamp(limit, 1, MAX_LIMIT))
                .stream()
                .map(entry -> toItem(entry, q, needle))
                .toList();
    }

    /**
     * 用户输入的 % 和 _ 是字面量而不是通配符，所以先转义再两侧加通配符。
     */
    private String toIlikePattern(String query) {
        String escaped = query.replace("\\", "\\\\")
                              .replace("%", "\\%")
                              .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private SearchItemVO toItem(Entry entry, String query, String needle) {
        return new SearchItemVO(
                entry.getId(),
                entry.getType(),
                entry.getTitle(),
                Previews.snippet(entry.getContentPlain(), query, SNIPPET_RADIUS),
                entry.getSourceDate(),
                entry.getTitle().toLowerCase(Locale.ROOT).contains(needle));
    }
}
