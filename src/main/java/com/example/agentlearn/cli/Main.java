package com.example.agentlearn.cli;

import com.example.agentlearn.config.AppConfig;
import com.example.agentlearn.llm.OpenAiCompatibleEmbeddingClient;
import com.example.agentlearn.rag.Indexer;
import com.example.agentlearn.rag.JsonVectorStore;
import com.example.agentlearn.rag.MarkdownDocumentLoader;
import com.example.agentlearn.rag.TextSplitter;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        AppConfig config = AppConfig.fromEnvironment();
        Indexer indexer = new Indexer(
                new MarkdownDocumentLoader(),
                new TextSplitter(1200),
                new OpenAiCompatibleEmbeddingClient(config.apiBaseUrl(), config.apiKey(), config.embeddingModel()),
                new JsonVectorStore(config.vectorStorePath())
        );
        ConsoleSession session = new ConsoleSession(config, System.in, System.out, indexer);
        session.run();
    }
}
