package com.example.agentlearn.llm;

import java.util.List;

@FunctionalInterface
public interface ChatClient {
    String complete(List<ChatMessage> messages) throws Exception;
}
