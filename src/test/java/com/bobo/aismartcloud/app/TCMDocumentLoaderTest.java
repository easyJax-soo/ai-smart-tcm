package com.bobo.aismartcloud.app;

import com.bobo.aismartcloud.rag.TCMDocumentLoader;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TCMDocumentLoaderTest {


    @Resource
    private TCMDocumentLoader tcmDocumentLoader;

    @Test
    void testChat() {
        tcmDocumentLoader.loadMarkdowns();
    }


}
