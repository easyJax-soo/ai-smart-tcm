package com.bobo.aismartcloud.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WebSearchTool {

    private static final Logger log = LoggerFactory.getLogger(WebSearchTool.class);

    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";
    private static final int MAX_RESULTS = 5;

    private final String apiKey;

    public WebSearchTool(String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool(description = "Search for information from Baidu Search Engine")
    public String searchWeb(
            @ToolParam(description = "Search query keyword") String query) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("q", query);
        paramMap.put("api_key", apiKey);
        paramMap.put("engine", "baidu");
        try {
            String response = HttpUtil.get(SEARCH_API_URL, paramMap);

            // 调试：打印 searchapi.io 原始响应
            log.info("[WebSearchTool] 原始响应: {}", response);

            JSONObject jsonObject = JSONUtil.parseObj(response);
            JSONArray organicResults = jsonObject.getJSONArray("organic_results");

            // 防御 1：字段缺失或为 null（key 失效 / engine 不支持 / 限流等）
            if (organicResults == null || organicResults.isEmpty()) {
                return "未搜索到结果。原始响应：" + jsonObject.toString();
            }

            // 防御 2：结果数 < 5 时 subList 会抛 IndexOutOfBoundsException
            int take = Math.min(MAX_RESULTS, organicResults.size());
            String result = IntStream.range(0, take)
                    .mapToObj(organicResults::get)
                    .map(obj -> ((JSONObject) obj).toString())
                    .collect(Collectors.joining(","));
            return result;
        } catch (Exception e) {
            log.error("[WebSearchTool] 搜索失败", e);
            return "Error searching Baidu: " + e.getMessage();
        }
    }
}
