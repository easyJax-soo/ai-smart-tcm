package com.bobo.aismartcloud.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class WebSearchToolTest {

    @Value("${search-api.api-key}")
    private String searchApiKey;

    @Test
    public void testSearchWeb() {
        WebSearchTool tool = new WebSearchTool(searchApiKey);
        String query = "Minimax https://www.minimaxi.com";
        String result = tool.searchWeb(query);
        System.out.println("Result: " + result);

        assertNotNull(result);
        assertFalse(result.startsWith("Error"), () -> "搜索失败：" + result);
        assertFalse(result.startsWith("未搜索到结果"), () -> "无结果：" + result);
        assertTrue(result.length() > 10, () -> "返回结果过短：" + result);
    }
}