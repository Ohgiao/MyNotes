package com.mynotes.service.impl;

import com.mynotes.common.BadRequestException;
import com.mynotes.common.NotFoundException;
import com.mynotes.mapper.EntryLinkMapper;
import com.mynotes.mapper.EntryMapper;
import com.mynotes.service.LinkService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LinkServiceImpl implements LinkService {

    private static final String LINK_TYPE = "ref";

    private final EntryMapper entryMapper;
    private final EntryLinkMapper entryLinkMapper;

    public LinkServiceImpl(EntryMapper entryMapper, EntryLinkMapper entryLinkMapper) {
        this.entryMapper = entryMapper;
        this.entryLinkMapper = entryLinkMapper;
    }

    @Override
    @Transactional
    public void add(Long srcId, Long dstId) {
        if (srcId.equals(dstId)) {
            throw new BadRequestException("不能引用自己");
        }
        requireExists(srcId);
        requireExists(dstId);
        entryLinkMapper.insert(srcId, dstId, LINK_TYPE);
    }

    @Override
    @Transactional
    public void remove(Long srcId, Long dstId) {
        entryLinkMapper.deleteByPair(srcId, dstId);
    }

    private void requireExists(Long id) {
        if (entryMapper.findById(id) == null) {
            throw new NotFoundException("条目不存在: " + id);
        }
    }
}
