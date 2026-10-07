package com.mynotes.mapper;

import com.mynotes.entity.Entry;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EntryLinkMapper {

    void insert(@Param("srcId") Long srcId,
                @Param("dstId") Long dstId,
                @Param("linkType") String linkType);

    int deleteByPair(@Param("srcId") Long srcId, @Param("dstId") Long dstId);

    List<Entry> findOutgoing(@Param("srcId") Long srcId);

    List<Entry> findIncoming(@Param("dstId") Long dstId);
}
