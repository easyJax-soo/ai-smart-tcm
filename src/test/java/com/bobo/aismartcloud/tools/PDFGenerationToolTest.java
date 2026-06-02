package com.bobo.aismartcloud.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class PDFGenerationToolTest {

    @Test
    public void testGeneratePDF() {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "测试生成.pdf";
        String content = "Minimax https://www.minimaxi.com";
        String result = tool.generatePDF(fileName, content);
        assertNotNull(result);
    }
}
   