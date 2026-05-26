package com.example.agentlearn.prompt;

import com.example.agentlearn.TestSupport;

import java.util.Map;

public final class PromptRendererTest {
    public static void main(String[] args) {
        replacesKnownPlaceholdersAndKeepsUnknownOnesVisible();
    }

    private static void replacesKnownPlaceholdersAndKeepsUnknownOnesVisible() {
        PromptRenderer renderer = new PromptRenderer();

        String rendered = renderer.render(
                "Hello {{name}}\nContext: {{context}}\nMissing: {{missing}}",
                Map.of("name", "Agent", "context", "RAG chunks")
        );

        TestSupport.assertContains(rendered, "Hello Agent");
        TestSupport.assertContains(rendered, "Context: RAG chunks");
        TestSupport.assertContains(rendered, "{{missing}}");
    }
}
