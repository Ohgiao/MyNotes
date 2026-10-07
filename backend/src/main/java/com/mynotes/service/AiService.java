package com.mynotes.service;

import com.mynotes.vo.AiStatusVO;
import com.mynotes.vo.AskResultVO;
import com.mynotes.vo.ReindexResultVO;

public interface AiService {

    AiStatusVO status();

    ReindexResultVO reindex(int limit);

    AskResultVO ask(String question, int topK);
}
