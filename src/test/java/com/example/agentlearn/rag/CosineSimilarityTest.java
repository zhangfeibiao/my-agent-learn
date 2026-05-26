package com.example.agentlearn.rag;

import com.example.agentlearn.TestSupport;

import java.nio.file.Path;
import java.util.List;

public final class CosineSimilarityTest {
    public static void main(String[] args) {
        ranksClosestVectorFirst();
    }

    private static void ranksClosestVectorFirst() {
        List<VectorRecord> records = List.of(
                new VectorRecord("a", Path.of("a.md"), "A", List.of(0.0, 1.0)),
                new VectorRecord("b", Path.of("b.md"), "B", List.of(1.0, 0.0))
        );

        List<VectorRecord> ranked = CosineSimilarity.rank(List.of(1.0, 0.0), records, 2);

        TestSupport.assertEquals(ranked.get(0).id(), "b");
        TestSupport.assertEquals(ranked.get(1).id(), "a");
    }
}
