package com.example.agentlearn.prompt;

import com.example.agentlearn.TestSupport;

import java.util.List;

public final class PromptComposerTest {
    public static void main(String[] args) {
        instructsKnowledgeAnswersToBeDirectWithoutInlineSourcePhrases();
        instructsFallbackToModelKnowledgeWhenRetrievedContextCannotAnswer();
    }

    private static void instructsKnowledgeAnswersToBeDirectWithoutInlineSourcePhrases() {
        String prompt = new PromptComposer("system").compose(List.of(), List.of(), "我是谁？", "", "");

        TestSupport.assertContains(prompt, "If the answer comes from retrieved knowledge context, answer directly");
        TestSupport.assertContains(prompt, "You may summarize or synthesize retrieved chunks");
        TestSupport.assertContains(prompt, "The JSON answer field must contain only the answer itself");
        TestSupport.assertContains(prompt, "Do not include sources, citations, source filenames, source labels, or chunk ids");
        TestSupport.assertContains(prompt, "Do not prefix the answer with phrases like");
        TestSupport.assertContains(prompt, "Do not include source filenames or source labels inside the answer text");
    }

    private static void instructsFallbackToModelKnowledgeWhenRetrievedContextCannotAnswer() {
        String prompt = new PromptComposer("system").compose(List.of(), List.of(), "解释 JVM？", "", "");

        TestSupport.assertContains(prompt, "If retrieved knowledge context cannot answer the question");
        TestSupport.assertContains(prompt, "use the model's own general knowledge to answer");
        TestSupport.assertContains(prompt, "Do not claim that fallback knowledge came from retrieved context");
    }
}
