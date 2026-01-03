package com.sanctuary.core.data;

import com.sanctuary.core.model.StatData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JsonDataLoader 단위 테스트
 */
public class JsonDataLoaderTest {

    private File tempFolder;
    private JsonDataLoader loader;
    private Logger logger = Logger.getLogger("TestDataLoader");

    @BeforeEach
    public void setUp() throws IOException {
        tempFolder = new File("target/test-data");
        if (tempFolder.exists()) {
            deleteFolder(tempFolder);
        }
        tempFolder.mkdirs();

        // 데이터 폴더 구조 생성
        new File(tempFolder, "data").mkdirs();

        loader = new JsonDataLoader(tempFolder, logger);
    }

    @Test
    public void testLoadStats() throws IOException {
        // 더미 JSON 파일 생성
        File statFile = new File(tempFolder, "data/stats.json");
        try (FileWriter writer = new FileWriter(statFile)) {
            writer.write("[\n" +
                    "  {\n" +
                    "    \"id\": \"TEST_STR\",\n" +
                    "    \"name\": \"테스트 힘\",\n" +
                    "    \"type\": \"CORE\"\n" +
                    "  }\n" +
                    "]");
        }

        loader.reload();

        StatData stat = loader.getStat("TEST_STR");
        assertNotNull(stat, "스탯 데이터가 로드되어야 합니다.");
        assertEquals("테스트 힘", stat.getName());
        assertEquals("CORE", stat.getType());
    }

    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory())
                    deleteFolder(f);
                else
                    f.delete();
            }
        }
        folder.delete();
    }
}
