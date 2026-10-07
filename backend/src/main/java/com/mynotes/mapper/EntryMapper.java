package com.mynotes.mapper;

import com.mynotes.entity.Entry;
import com.mynotes.entity.EntryHit;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface EntryMapper {

    void insert(Entry entry);

    Entry findById(@Param("id") Long id);

    int update(Entry entry);

    int softDelete(@Param("id") Long id);

    int restore(@Param("id") Long id);

    List<Entry> findTimeline(@Param("type") String type,
                             @Param("date") LocalDate date,
                             @Param("cursorDate") LocalDate cursorDate,
                             @Param("cursorId") Long cursorId,
                             @Param("limit") int limit);

    List<Entry> search(@Param("pattern") String pattern,
                       @Param("type") String type,
                       @Param("limit") int limit);

    List<Entry> findAllNotes();

    List<Entry> findDeleted(@Param("limit") int limit);

    List<Entry> findDueForReview(@Param("limit") int limit);

    int countDue();

    int applyReviewResult(@Param("id") Long id,
                          @Param("delta") int delta,
                          @Param("days") int days);

    /**
     * 从 newParentId 往上走，看能不能走到 childId，用来拒绝把节点挂到自己的后代下面。
     */
    int countAncestorsMatching(@Param("childId") Long childId, @Param("newParentId") Long newParentId);

    List<Entry> findPendingEmbeddings(@Param("limit") int limit);

    int countPendingEmbeddings();

    int countEmbedded();

    void saveEmbedding(@Param("id") Long id, @Param("vector") String vector);

    List<EntryHit> searchByVector(@Param("vector") String vector, @Param("limit") int limit);

    List<Entry> findByIds(@Param("ids") List<Long> ids);
}
