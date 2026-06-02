package com.bobo.aismartcloud.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileOperationToolTest {

    @Test
    void readFile() {
        FileOperationTool tool = new FileOperationTool();
        String fileName = "测试读取文件.txt";
        String result = tool.readFile(fileName);
        assertNotNull(result);
    }

    @Test
    void writeFile() {
        FileOperationTool tool = new FileOperationTool();
        String fileName = "测试写入文件.txt";
        String content = "========== 测试写入文件内容==========";
        String result = tool.writeFile(fileName, content);
        assertNotNull(result);
    }
}