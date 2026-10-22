package com.navya.ai_ops_copilot.rag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class RunbookIngestion implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RunbookIngestion.class);

    private final VectorStore vectorStore;

    // Spring passes in the VectorStore it created from application.yml (pgvector)
    public RunbookIngestion(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    // Runs automatically once the app has started
    @Override
    public void run(ApplicationArguments args) throws IOException {
        // 1. Find every .md file in src/main/resources/runbooks/
        Resource[] files = new PathMatchingResourcePatternResolver()
                .getResources("classpath:runbooks/*.md");

        // 2. Read each file and split it into sections
        List<Document> chunks = new ArrayList<>();
        for (Resource file : files) {
            String text = file.getContentAsString(StandardCharsets.UTF_8);
            chunks.addAll(splitIntoSections(file.getFilename(), text));
        }

        // 3 + 4. Embed every chunk and save it in pgvector (Spring AI does both)
        vectorStore.add(chunks);

        log.info("Loaded {} runbook chunks from {} files", chunks.size(), files.length);
    }

    // Split one runbook into one Document per "## " section
    private List<Document> splitIntoSections(String fileName, String text) {
        // First line is the title, e.g. "# Runbook: Memory Leak / OutOfMemoryError"
        String title = text.lines().findFirst().orElse("").replace("#", "").trim();

        List<Document> docs = new ArrayList<>();
        String[] sections = text.split("\n## ");

        // sections[0] is the title part, so start at 1
        for (int i = 1; i < sections.length; i++) {
            String section = sections[i];
            int newline = section.indexOf('\n');
            String sectionName = section.substring(0, newline).trim();   // e.g. "Remediation"
            String body = section.substring(newline + 1).trim();         // the section's text

            // Put the title in every chunk so each one makes sense on its own
            String content = title + " - " + sectionName + "\n" + body;

            // Same file + section always gets the same id, so restarts update instead of duplicating
            String id = UUID.nameUUIDFromBytes(
                    (fileName + "#" + sectionName).getBytes(StandardCharsets.UTF_8)).toString();

            docs.add(new Document(id, content,
                    Map.of("source", fileName, "section", sectionName)));
        }
        return docs;
    }
}
