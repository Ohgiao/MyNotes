package com.mynotes.common;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;

public final class PlainTextExtractor {

    private static final Parser PARSER = Parser.builder().build();
    private static final TextContentRenderer RENDERER = TextContentRenderer.builder().build();

    private PlainTextExtractor() {
    }

    /**
     * 检索只作用于抽取出的纯文本：markdown 的 # ` []() 会让中文分词和 ILIKE 命中错误的形状。
     */
    public static String extract(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        Node document = PARSER.parse(markdown);
        String text = RENDERER.render(document);
        return text.replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                   .replaceAll("\\n{3,}", "\n\n")
                   .strip();
    }
}
