package com.example.agentlearn.cli;

import com.example.agentlearn.config.AppConfig;
import com.example.agentlearn.rag.Indexer;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

public final class ConsoleSession {
    private final AppConfig config;
    private final InputStream input;
    private final PrintStream output;
    private final Indexer indexer;

    public ConsoleSession(AppConfig config, InputStream input, PrintStream output, Indexer indexer) {
        this.config = config;
        this.input = input;
        this.output = output;
        this.indexer = indexer;
    }

    public static ConsoleSession placeholder(AppConfig config) {
        return new ConsoleSession(config, System.in, System.out, null);
    }

    public void run() {
        output.println("Personal Knowledge Agent");
        output.println("Type /index to rebuild the knowledge index, /exit to quit.");
        output.println();

        Scanner scanner = new Scanner(input);
        while (true) {
            output.print("> ");
            if (!scanner.hasNextLine()) {
                output.println();
                return;
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            if ("/exit".equals(line)) {
                output.println("Bye.");
                return;
            }
            if ("/index".equals(line)) {
                rebuildIndex();
                continue;
            }
            output.printf("Agent is not wired yet. Configured chat model: %s%n", config.chatModel());
        }
    }

    private void rebuildIndex() {
        if (indexer == null) {
            output.println("Indexer is not configured.");
            return;
        }
        try {
            int count = indexer.rebuild(config.knowledgeDir());
            output.printf("Indexed %d chunks into %s%n", count, config.vectorStorePath());
        } catch (Exception e) {
            output.println("Indexing failed: " + e.getMessage());
        }
    }
}
