package com.mynotes.service.impl;

import com.mynotes.entity.Entry;
import com.mynotes.mapper.EntryMapper;
import com.mynotes.service.TopicService;
import com.mynotes.vo.TreeNodeVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TopicServiceImpl implements TopicService {

    private final EntryMapper entryMapper;

    public TopicServiceImpl(EntryMapper entryMapper) {
        this.entryMapper = entryMapper;
    }

    /**
     * 一次取出全部笔记在内存里拼树：主题笔记量级是几百条，比递归 CTE 反复查库便宜得多。
     */
    @Override
    public List<TreeNodeVO> tree() {
        List<Entry> notes = entryMapper.findAllNotes();
        Set<Long> alive = new HashSet<>();
        notes.forEach(n -> alive.add(n.getId()));

        Map<Long, List<Entry>> byParent = new LinkedHashMap<>();
        for (Entry note : notes) {
            Long parent = note.getParentId() != null && alive.contains(note.getParentId())
                    ? note.getParentId()
                    : null;
            byParent.computeIfAbsent(parent, k -> new ArrayList<>()).add(note);
        }
        return build(byParent, null);
    }

    private List<TreeNodeVO> build(Map<Long, List<Entry>> byParent, Long parentId) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .map(note -> new TreeNodeVO(
                        note.getId(),
                        note.getTitle(),
                        note.getSourceDate(),
                        note.getReviewStatus(),
                        note.getMastery(),
                        note.getNextReviewAt(),
                        build(byParent, note.getId())))
                .toList();
    }
}
