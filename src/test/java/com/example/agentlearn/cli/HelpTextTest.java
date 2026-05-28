package com.example.agentlearn.cli;

import com.example.agentlearn.TestSupport;

public final class HelpTextTest {
    public static void main(String[] args) {
        includesUsageAndCommands();
    }

    private static void includesUsageAndCommands() {
        String text = HelpText.usage();

        TestSupport.assertContains(text, "Usage:");
        TestSupport.assertContains(text, "./scripts/run.sh");
        TestSupport.assertContains(text, "/index");
        TestSupport.assertContains(text, "/reindex");
        TestSupport.assertContains(text, "/mcp-tools");
        TestSupport.assertContains(text, "/help");
        TestSupport.assertContains(text, "/exit");
    }
}
