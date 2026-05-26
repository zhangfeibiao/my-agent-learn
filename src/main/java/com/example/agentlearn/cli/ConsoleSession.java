package com.example.agentlearn.cli;

import com.example.agentlearn.config.AppConfig;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

public final class ConsoleSession {
    private final AppConfig config;
    private final InputStream input;
    private final PrintStream output;

    public ConsoleSession(AppConfig config, InputStream input, PrintStream output) {
        this.config = config;
        this.input = input;
        this.output = output;
    }

    public static ConsoleSession placeholder(AppConfig config) {
        return new ConsoleSession(config, System.in, System.out);
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
                output.println("Indexing is not wired yet. Next task will add it.");
                continue;
            }
            output.printf("Agent is not wired yet. Configured chat model: %s%n", config.chatModel());
        }
    }
}
