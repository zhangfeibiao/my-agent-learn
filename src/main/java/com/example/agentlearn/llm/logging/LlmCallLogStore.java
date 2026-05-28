package com.example.agentlearn.llm.logging;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface LlmCallLogStore {
    void append(LlmCallLog log) throws IOException;

    List<LlmCallLog> listLatest(int limit) throws IOException;

    Optional<LlmCallLog> findById(String id) throws IOException;

    void clear() throws IOException;
}
