package com.mynotes.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TagMapper {

    /**
     * 用 upsert 拿 id，避免"先查再插"在并发下抛唯一键冲突。
     */
    Long upsertByName(@Param("name") String name);

    List<String> findNamesByEntryId(@Param("entryId") Long entryId);

    void deleteByEntryId(@Param("entryId") Long entryId);

    void link(@Param("entryId") Long entryId, @Param("tagId") Long tagId);
}
