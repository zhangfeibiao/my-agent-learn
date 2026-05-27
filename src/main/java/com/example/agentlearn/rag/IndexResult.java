package com.example.agentlearn.rag;

public record IndexResult(
        int totalChunks,
        int reusedChunks,
        int embeddedChunks,
        int updatedFiles,
        int deletedFiles
) {
}
