package com.navya.ai_ops_copilot.rag;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    public record SearchHit(String source, String section, Double score, String content) {}

    // NEW: what /api/rag/ask sends back: the answer + which cards were used
    public record RagAnswer(String answer, List<String> sources) {}

    private final VectorStore vectorStore;
    private final ChatClient chatClient;                     // NEW

    // NEW: Spring now passes in two things: the vector store and a ChatClient.Builder
    public RagController(VectorStore vectorStore, ChatClient.Builder builder) {
        this.vectorStore = vectorStore;
        this.chatClient = builder
                .defaultSystem("""
                        You are an SRE assistant.
                        Answer using ONLY the runbook excerpts provided.
                        If the excerpts do not cover the question, say you don't have a runbook for it.
                        Be concise and mention which runbook you used.
                        """)
                .build();
    }

    // Step 3: search only (unchanged)
    @GetMapping("/search")
    public List<SearchHit> search(@RequestParam String q,
                                  @RequestParam(defaultValue = "3") int k) {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder().query(q).topK(k).build());

        return results.stream()
                .map(doc -> new SearchHit(
                        (String) doc.getMetadata().get("source"),
                        (String) doc.getMetadata().get("section"),
                        doc.getScore(),
                        doc.getText()))
                .toList();
    }

    // NEW: Step 4: search, then ask the AI with the results
    @PostMapping("/ask")
    public RagAnswer ask(@RequestBody String question) {

        // 1. Find the 3 closest runbook cards
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder().query(question).topK(4).build());

        // 2. Join their text into one block, separated by ---
        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        // 3. Build the prompt: runbook excerpts + the question
                String prompt = """
                Runbook excerpts:
                %s

                Question: %s

                If none of the excerpts are relevant, reply only: "No runbook covers this."
                Otherwise, answer in exactly this format, using details from the excerpts:

                Runbook used: <runbook name>
                Immediate action: <first remediation step>
                If it happens again: <the other remediation steps, including any specific numbers>
                Escalation: <the escalation rule, including any time limits>
                """.formatted(context, question);

        // 4. Ask the AI
        String answer = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        // 5. List which cards were used, e.g. "memory-leak.md / Remediation"
        List<String> sources = docs.stream()
                .map(doc -> doc.getMetadata().get("source") + " / " + doc.getMetadata().get("section"))
                .toList();

        return new RagAnswer(answer, sources);
    }
}