package com.mynotes.service;

import com.mynotes.dto.EntryCreateDTO;
import com.mynotes.dto.EntryUpdateDTO;
import com.mynotes.vo.EntryListItemVO;
import com.mynotes.vo.EntryVO;
import com.mynotes.vo.TimelineVO;

import java.util.List;

public interface EntryService {

    EntryVO create(EntryCreateDTO dto);

    EntryVO get(Long id);

    EntryVO update(Long id, EntryUpdateDTO dto);

    void delete(Long id);

    void restore(Long id);

    TimelineVO timeline(String type, String cursor, int size);

    List<EntryListItemVO> trash(int limit);
}
