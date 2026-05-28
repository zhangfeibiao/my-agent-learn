package com.example.agentlearn.web;

import com.example.agentlearn.rag.IndexResult;

public interface IndexService {
    IndexResult update() throws Exception;

    IndexResult rebuild() throws Exception;
}
