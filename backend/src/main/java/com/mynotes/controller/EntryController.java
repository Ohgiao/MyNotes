package com.mynotes.controller;

import com.mynotes.dto.EntryCreateDTO;
import com.mynotes.dto.EntryUpdateDTO;
import com.mynotes.dto.LinkCreateDTO;
import com.mynotes.service.EntryService;
import com.mynotes.service.LinkService;
import com.mynotes.vo.EntryListItemVO;
import com.mynotes.vo.EntryVO;
import com.mynotes.vo.TimelineVO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class EntryController {

    private final EntryService entryService;
    private final LinkService linkService;

    public EntryController(EntryService entryService, LinkService linkService) {
        this.entryService = entryService;
        this.linkService = linkService;
    }

    @PostMapping("/entries")
    public ResponseEntity<EntryVO> create(@Valid @RequestBody EntryCreateDTO dto) {
        EntryVO created = entryService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/entries/" + created.id()))
                .body(created);
    }

    @GetMapping("/entries/{id}")
    public EntryVO get(@PathVariable Long id) {
        return entryService.get(id);
    }

    @PutMapping("/entries/{id}")
    public EntryVO update(@PathVariable Long id, @Valid @RequestBody EntryUpdateDTO dto) {
        return entryService.update(id, dto);
    }

    @DeleteMapping("/entries/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        entryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/entries/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable Long id) {
        entryService.restore(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/entries/{id}/links")
    public ResponseEntity<Void> addLink(@PathVariable Long id, @Valid @RequestBody LinkCreateDTO dto) {
        linkService.add(id, dto.dstId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/entries/{id}/links/{dstId}")
    public ResponseEntity<Void> removeLink(@PathVariable Long id, @PathVariable Long dstId) {
        linkService.remove(id, dstId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trash")
    public List<EntryListItemVO> trash(@RequestParam(defaultValue = "100") int limit) {
        return entryService.trash(limit);
    }

    @GetMapping("/timeline")
    public TimelineVO timeline(@RequestParam(required = false) String type,
                               @RequestParam(required = false) String cursor,
                               @RequestParam(defaultValue = "30") int size) {
        return entryService.timeline(type, cursor, size);
    }
}
