package com.example.agentlearn.web;

import com.example.agentlearn.agent.AgentAnswer;

@FunctionalInterface
public interface ChatService {
    AgentAnswer answer(String message) throws Exception;
}
