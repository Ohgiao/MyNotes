package com.mynotes.service.impl;

import com.mynotes.client.OpenAiCompatibleClient;
import com.mynotes.common.BadRequestException;
import com.mynotes.common.Previews;
import com.mynotes.config.AiProperties;
import com.mynotes.entity.Entry;
import com.mynotes.entity.EntryHit;
import com.mynotes.mapper.EntryMapper;
import com.mynotes.service.AiService;
import com.mynotes.service.SearchService;
import com.mynotes.vo.AiStatusVO;
import com.mynotes.vo.AskResultVO;
import com.mynotes.vo.CitationVO;
import com.mynotes.vo.ReindexResultVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiServiceImpl implements AiService {

    private static final int KEYWORD_POOL = 20;
    private static final int EMBED_BATCH = 16;
    private static final int PASSAGE_MAX = 600;
    private static final int CITATION_MAX = 200;
    private static final int RRF_K = 60;

    private static final String SYSTEM_PROMPT = """
            你是我个人学习笔记的问答助手。
            规则：只根据提供的笔记片段回答；每个论断后面标注来源编号，形如 [1] 或 [2][3]；
            笔记里没有覆盖到的部分要明确说"你的笔记里没有相关内容"，不要用自己的知识补全，不要编造。
            用中文回答，简洁。""";

    /**
     * 提问里的疑问词和虚词，用来把"连接池大小怎么定"切成能命中的词项。
     */
    private static final List<String> QUESTION_NOISE = List.of(
            "为什么", "怎么样", "怎样", "怎么", "如何", "是否", "是不是", "有没有", "哪些", "哪个", "什么",
            "请问", "我之前", "以前", "现在", "一下", "多少", "可以", "需要", "关于", "的", "了", "吗", "呢",
            "是", "有", "和", "与", "在", "就", "要", "会", "我", "它", "这", "那");
    private static final int MAX_TERMS = 8;

    private final AiProperties properties;
    private final EntryMapper entryMapper;
    private final SearchService searchService;
    private final OpenAiCompatibleClient client;

    public AiServiceImpl(AiProperties properties,
                         EntryMapper entryMapper,
                         SearchService searchService,
                         OpenAiCompatibleClient client) {
        this.properties = properties;
        this.entryMapper = entryMapper;
        this.searchService = searchService;
        this.client = client;
    }

    @Override
    public AiStatusVO status() {
        return new AiStatusVO(properties.configured(),
                properties.configured() ? "rag" : "keyword",
                entryMapper.countPendingEmbeddings(),
                entryMapper.countEmbedded());
    }

    @Override
    public ReindexResultVO reindex(int limit) {
        requireConfigured();
        List<Entry> pending = entryMapper.findPendingEmbeddings(Math.clamp(limit, 1, 200));
        int embedded = 0;
        for (int from = 0; from < pending.size(); from += EMBED_BATCH) {
            List<Entry> batch = pending.subList(from, Math.min(from + EMBED_BATCH, pending.size()));
            List<float[]> vectors = client.embed(batch.stream().map(Entry::getContentPlain).toList());
            for (int i = 0; i < batch.size(); i++) {
                entryMapper.saveEmbedding(batch.get(i).getId(), toLiteral(vectors.get(i)));
                embedded++;
            }
        }
        return new ReindexResultVO(embedded, entryMapper.countPendingEmbeddings());
    }

    @Override
    public AskResultVO ask(String question, int topK) {
        String q = question == null ? "" : question.strip();
        if (q.isEmpty()) {
            throw new BadRequestException("问题不能为空");
        }
        int k = Math.clamp(topK, 1, 10);

        List<Long> keywordIds = keywordCandidates(q);
        List<EntryHit> vector = List.of();
        if (properties.configured()) {
            float[] queryVector = client.embed(List.of(q)).get(0);
            vector = entryMapper.searchByVector(toLiteral(queryVector), Math.min(k * 3, 50));
        }

        List<Ranked> ranked = fuse(keywordIds, vector, k);
        if (ranked.isEmpty()) {
            return new AskResultVO(q, properties.configured() ? "你的笔记里没有相关内容。" : "", List.of(),
                    properties.configured() ? "rag" : "keyword");
        }

        Map<Long, Entry> loaded = new HashMap<>();
        entryMapper.findByIds(ranked.stream().map(Ranked::id).toList())
                .forEach(entry -> loaded.put(entry.getId(), entry));

        List<CitationVO> citations = new ArrayList<>();
        StringBuilder passages = new StringBuilder();
        for (int i = 0; i < ranked.size(); i++) {
            Ranked item = ranked.get(i);
            Entry entry = loaded.get(item.id());
            if (entry == null) {
                continue;
            }
            int index = citations.size() + 1;
            String label = "log".equals(entry.getType()) ? "日志" : "笔记";
            citations.add(new CitationVO(index, entry.getId(), entry.getType(), entry.getTitle(),
                    Previews.preview(entry.getContentPlain(), CITATION_MAX), entry.getSourceDate(),
                    item.score(), item.matchedBy()));
            passages.append('[').append(index).append("]（")
                    .append(entry.getSourceDate()).append('·').append(label).append('）')
                    .append(entry.getTitle()).append('\n')
                    .append(Previews.preview(entry.getContentPlain(), PASSAGE_MAX)).append("\n\n");
        }

        if (!properties.configured()) {
            return new AskResultVO(q, "", citations, "keyword");
        }
        String answer = client.chat(SYSTEM_PROMPT, "问题：" + q + "\n\n笔记片段：\n" + passages);
        return new AskResultVO(q, answer, citations, "rag");
    }

    /**
     * 整句 ILIKE 命中权重更高，命中不到时再用切出来的词项兜底——
     * 自然语言提问（"连接池大小怎么定"）几乎不可能原样出现在笔记里。
     */
    private List<Long> keywordCandidates(String question) {
        Map<Long, Double> hits = new LinkedHashMap<>();
        searchService.search(question, null, KEYWORD_POOL)
                .forEach(item -> hits.merge(item.id(), 3.0, Double::sum));
        for (String term : terms(question)) {
            searchService.search(term, null, 10)
                    .forEach(item -> hits.merge(item.id(), 1.0, Double::sum));
        }
        return hits.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(KEYWORD_POOL)
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<String> terms(String question) {
        List<String> tokens = new ArrayList<>();
        for (String raw : question.split("[\\s,，。.、!！?？:：;；()（）\"'“”]+")) {
            String token = raw;
            for (String noise : QUESTION_NOISE) {
                token = token.replace(noise, "");
            }
            token = token.strip();
            if (token.length() < 2) {
                continue;
            }
            if (token.length() <= 4) {
                tokens.add(token);
            } else {
                for (int i = 0; i + 2 <= token.length() && tokens.size() < MAX_TERMS; i++) {
                    tokens.add(token.substring(i, i + 2));
                }
            }
            if (tokens.size() >= MAX_TERMS) {
                break;
            }
        }
        return tokens.stream().distinct().limit(MAX_TERMS).toList();
    }

    /**
     * 余弦相似度和 ILIKE 命中没有共同量纲，所以按排名融合（RRF）而不是把两种分数相加。
     */
    private List<Ranked> fuse(List<Long> keywordIds, List<EntryHit> vector, int topK) {
        Map<Long, Double> scores = new LinkedHashMap<>();
        Map<Long, String> matchedBy = new HashMap<>();
        accumulate(scores, matchedBy, keywordIds, "keyword");
        accumulate(scores, matchedBy, vector.stream().map(EntryHit::getId).toList(), "vector");

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> new Ranked(e.getKey(), e.getValue(), matchedBy.getOrDefault(e.getKey(), "keyword")))
                .toList();
    }

    private void accumulate(Map<Long, Double> scores, Map<Long, String> matchedBy, List<Long> ids, String source) {
        for (int rank = 0; rank < ids.size(); rank++) {
            Long id = ids.get(rank);
            scores.merge(id, 1.0 / (RRF_K + rank + 1), Double::sum);
            String previous = matchedBy.get(id);
            matchedBy.put(id, previous == null || previous.equals(source) ? source : "both");
        }
    }

    private String toLiteral(float[] vector) {
        StringBuilder builder = new StringBuilder(vector.length * 8 + 2).append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(vector[i]);
        }
        return builder.append(']').toString();
    }

    private void requireConfigured() {
        if (!properties.configured()) {
            throw new BadRequestException("未配置 mynotes.ai 的 api-key / base-url / 模型名，无法建立向量索引");
        }
    }

    private record Ranked(Long id, double score, String matchedBy) {
    }
}
