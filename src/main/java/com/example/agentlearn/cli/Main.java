package com.example.agentlearn.cli;

import com.example.agentlearn.config.AppConfig;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        ConsoleSession session = ConsoleSession.placeholder(AppConfig.fromEnvironment());
        session.run();
    }
}
