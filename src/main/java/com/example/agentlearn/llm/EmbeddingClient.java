package com.example.agentlearn.llm;

import java.util.List;

@FunctionalInterface
public interface EmbeddingClient {
    List<Double> embed(String text) throws Exception;
}
