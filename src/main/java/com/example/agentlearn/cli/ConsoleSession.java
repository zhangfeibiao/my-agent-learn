package com.example.agentlearn.cli;

import com.example.agentlearn.agent.Agent;
import com.example.agentlearn.agent.AgentAnswer;
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
    private final Agent agent;

    public ConsoleSession(AppConfig config, InputStream input, PrintStream output, Indexer indexer, Agent agent) {
        this.config = config;
        this.input = input;
        this.output = output;
        this.indexer = indexer;
        this.agent = agent;
    }

    public static ConsoleSession placeholder(AppConfig config) {
        return new ConsoleSession(config, System.in, System.out, null, null);
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
            answer(line);
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

    private void answer(String line) {
        if (agent == null) {
            output.printf("Agent is not configured. Chat model: %s%n", config.chatModel());
            return;
        }
        try {
            AgentAnswer answer = agent.answer(line);
            output.println();
            output.println("Agent:");
            output.println(answer.answer());
            if (!answer.sources().isEmpty()) {
                output.println();
                output.println("Sources:");
                for (String source : answer.sources()) {
                    output.println("- " + source);
                }
            }
        } catch (Exception e) {
            output.println("Agent failed: " + e.getMessage());
        }
    }
}
