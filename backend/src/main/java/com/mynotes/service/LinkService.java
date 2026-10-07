package com.mynotes.service;

public interface LinkService {

    void add(Long srcId, Long dstId);

    void remove(Long srcId, Long dstId);
}
