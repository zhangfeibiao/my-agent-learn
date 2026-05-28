package com.example.agentlearn.cli;

import com.example.agentlearn.config.AppConfig;
import com.example.agentlearn.runtime.AgentRuntime;
import com.example.agentlearn.runtime.AgentRuntimeFactory;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (wantsHelp(args)) {
            System.out.print(HelpText.usage());
            return;
        }

        AppConfig config = AppConfig.fromEnvironment();
        AgentRuntime runtime = AgentRuntimeFactory.create(config);
        ConsoleSession session = new ConsoleSession(config, System.in, System.out, runtime.indexer(), runtime.createAgent(), runtime.mcpClient());
        session.run();
    }

    private static boolean wantsHelp(String[] args) {
        for (String arg : args) {
            if ("--help".equals(arg) || "-h".equals(arg)) {
                return true;
            }
        }
        return false;
    }
}
