package com.mynotes.service.impl;

import com.mynotes.common.BadRequestException;
import com.mynotes.common.NotFoundException;
import com.mynotes.common.PlainTextExtractor;
import com.mynotes.common.Previews;
import com.mynotes.dto.EntryCreateDTO;
import com.mynotes.dto.EntryUpdateDTO;
import com.mynotes.entity.Entry;
import com.mynotes.mapper.EntryLinkMapper;
import com.mynotes.mapper.EntryMapper;
import com.mynotes.mapper.TagMapper;
import com.mynotes.service.EntryService;
import com.mynotes.vo.EntryListItemVO;
import com.mynotes.vo.EntryRelationsVO;
import com.mynotes.vo.EntryVO;
import com.mynotes.vo.LinkItemVO;
import com.mynotes.vo.TimelineDayVO;
import com.mynotes.vo.TimelineVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class EntryServiceImpl implements EntryService {

    private static final int TITLE_MAX = 40;
    private static final int PREVIEW_MAX = 140;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_TRASH_SIZE = 200;

    private final EntryMapper entryMapper;
    private final TagMapper tagMapper;
    private final EntryLinkMapper entryLinkMapper;

    public EntryServiceImpl(EntryMapper entryMapper,
                            TagMapper tagMapper,
                            EntryLinkMapper entryLinkMapper) {
        this.entryMapper = entryMapper;
        this.tagMapper = tagMapper;
        this.entryLinkMapper = entryLinkMapper;
    }

    @Override
    @Transactional
    public EntryVO create(EntryCreateDTO dto) {
        String type = dto.type() == null || dto.type().isBlank() ? "log" : dto.type();
        String plain = PlainTextExtractor.extract(dto.contentMd());

        Entry entry = new Entry();
        entry.setType(type);
        entry.setTitle(deriveTitle(dto.title(), plain));
        entry.setContentMd(dto.contentMd());
        entry.setContentPlain(plain);
        entry.setParentId("note".equals(type) ? dto.parentId() : null);
        entry.setSourceDate(dto.sourceDate() == null ? LocalDate.now() : dto.sourceDate());
        entry.setPinned(false);
        entry.setMastery((short) 0);
        if ("note".equals(type)) {
            entry.setReviewStatus("learning");
            entry.setNextReviewAt(Instant.now().plus(1, ChronoUnit.DAYS));
        } else {
            entry.setReviewStatus("none");
        }
        if (entry.getParentId() != null) {
            requireParentAllowed(null, entry.getParentId());
        }

        entryMapper.insert(entry);
        replaceTags(entry.getId(), dto.tagNames());
        return toVO(requireById(entry.getId()));
    }

    @Override
    public EntryVO get(Long id) {
        return toVO(requireById(id));
    }

    @Override
    @Transactional
    public EntryVO update(Long id, EntryUpdateDTO dto) {
        Entry entry = requireById(id);
        String plain = PlainTextExtractor.extract(dto.contentMd());

        entry.setContentMd(dto.contentMd());
        entry.setContentPlain(plain);
        entry.setTitle(dto.title() == null || dto.title().isBlank()
                ? Previews.firstLine(plain, TITLE_MAX)
                : dto.title().strip());
        if (dto.sourceDate() != null) {
            entry.setSourceDate(dto.sourceDate());
        }
        if (dto.parentId() != null) {
            if ("log".equals(entry.getType())) {
                throw new BadRequestException("只有主题笔记可以挂在主题树下");
            }
            requireParentAllowed(id, dto.parentId());
            entry.setParentId(dto.parentId());
        }
        if (dto.pinned() != null) {
            entry.setPinned(dto.pinned());
        }

        if (entryMapper.update(entry) == 0) {
            throw new NotFoundException("条目不存在: " + id);
        }
        if (dto.tagNames() != null) {
            replaceTags(id, dto.tagNames());
        }
        return toVO(requireById(id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (entryMapper.softDelete(id) == 0) {
            throw new NotFoundException("条目不存在: " + id);
        }
    }

    @Override
    @Transactional
    public void restore(Long id) {
        if (entryMapper.restore(id) == 0) {
            throw new NotFoundException("条目不存在: " + id);
        }
    }

    @Override
    public TimelineVO timeline(String type, String cursor, int size) {
        int limit = Math.clamp(size, 1, MAX_PAGE_SIZE);
        LocalDate cursorDate = null;
        Long cursorId = null;
        if (cursor != null && !cursor.isBlank()) {
            String[] parts = cursor.split(":");
            if (parts.length != 2) {
                throw new BadRequestException("cursor 格式应为 sourceDate:id");
            }
            cursorDate = LocalDate.parse(parts[0]);
            cursorId = Long.valueOf(parts[1]);
        }

        List<Entry> rows = entryMapper.findTimeline(type, null, cursorDate, cursorId, limit);
        Map<LocalDate, List<EntryListItemVO>> byDay = new LinkedHashMap<>();
        for (Entry row : rows) {
            byDay.computeIfAbsent(row.getSourceDate(), d -> new ArrayList<>())
                 .add(toListItem(row));
        }

        List<TimelineDayVO> days = byDay.entrySet().stream()
                .map(e -> new TimelineDayVO(e.getKey(), e.getValue()))
                .toList();
        String nextCursor = rows.size() == limit
                ? rows.get(rows.size() - 1).getSourceDate() + ":" + rows.get(rows.size() - 1).getId()
                : null;
        return new TimelineVO(days, nextCursor);
    }

    @Override
    public List<EntryListItemVO> trash(int limit) {
        return entryMapper.findDeleted(Math.clamp(limit, 1, MAX_TRASH_SIZE)).stream()
                .map(this::toListItem)
                .toList();
    }

    private Entry requireById(Long id) {        Entry entry = entryMapper.findById(id);
        if (entry == null) {
            throw new NotFoundException("条目不存在: " + id);
        }
        return entry;
    }

    /**
     * 快速记录常常不填标题；从正文首行补一个，否则时间线上全是空白卡片。
     */
    private String deriveTitle(String requestedTitle, String plain) {
        if (requestedTitle != null && !requestedTitle.isBlank()) {
            return requestedTitle.strip();
        }
        return Previews.firstLine(plain, TITLE_MAX);
    }

    private EntryListItemVO toListItem(Entry entry) {
        return new EntryListItemVO(
                entry.getId(),
                entry.getType(),
                entry.getTitle(),
                Previews.preview(entry.getContentPlain(), PREVIEW_MAX),
                entry.getSourceDate(),
                entry.isPinned(),
                entry.getUpdatedAt());
    }

    private EntryVO toVO(Entry entry) {
        return new EntryVO(
                entry.getId(),
                entry.getType(),
                entry.getTitle(),
                entry.getContentMd(),
                entry.getParentId(),
                entry.getSourceDate(),
                entry.getReviewStatus(),
                entry.getMastery(),
                entry.getNextReviewAt(),
                entry.getReviewCount(),
                entry.isPinned(),
                entry.getCreatedAt(),
                entry.getUpdatedAt(),
                tagMapper.findNamesByEntryId(entry.getId()),
                relations(entry.getId()));
    }

    private EntryRelationsVO relations(Long entryId) {
        return new EntryRelationsVO(
                entryLinkMapper.findOutgoing(entryId).stream().map(this::toLinkItem).toList(),
                entryLinkMapper.findIncoming(entryId).stream().map(this::toLinkItem).toList());
    }

    private LinkItemVO toLinkItem(Entry entry) {
        return new LinkItemVO(entry.getId(), entry.getType(), entry.getTitle(), entry.getSourceDate());
    }

    /**
     * 父节点必须存在、必须是笔记，且不能是自己的后代——否则主题树会拼出环，节点会从视图里消失。
     */
    private void requireParentAllowed(Long childId, Long parentId) {
        Entry parent = requireById(parentId);
        if (!"note".equals(parent.getType())) {
            throw new BadRequestException("主题树的父节点必须是主题笔记");
        }
        if (childId != null && entryMapper.countAncestorsMatching(childId, parentId) > 0) {
            throw new BadRequestException("不能把主题挂到自己的下级");
        }
    }

    private void replaceTags(Long entryId, List<String> tagNames) {
        tagMapper.deleteByEntryId(entryId);
        if (tagNames == null) {
            return;
        }
        new LinkedHashSet<>(tagNames).stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.strip())
                .limit(20)
                .forEach(name -> tagMapper.link(entryId, tagMapper.upsertByName(name)));
    }
}
