package com.bobo.aismartcloud.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;
/**
 *
 * 自定义基于Token的切词器
 * 相比于结合AI来做切分的话，该Bean显得会没那么好用
 */
@Component
class MyTokenTextSplitter {
    public List<Document> splitDocuments(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter();
        return splitter.apply(documents);
    }

    public List<Document> splitCustomized(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter(
                200,      // chunkSize
                100,      // minChunkSizeChars
                10,        // minChunkLengthToEmbed
                5000,    // maxNumChunks
                true,     // keepSeparator
                List.of('.', '?', '!', '\n', '，', '。', '！', '？', '\n')  // 中英文标点
        );
        return splitter.apply(documents);
    }
}