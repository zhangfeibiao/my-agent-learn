package com.example.agentlearn.rag;

import java.util.Comparator;
import java.util.List;

public final class CosineSimilarity {
    private CosineSimilarity() {
    }

    public static List<VectorRecord> rank(List<Double> query, List<VectorRecord> records, int topK) {
        return records.stream()
                .sorted(Comparator.comparingDouble((VectorRecord record) -> similarity(query, record.embedding())).reversed())
                .limit(topK)
                .toList();
    }

    public static double similarity(List<Double> left, List<Double> right) {
        int length = Math.min(left.size(), right.size());
        if (length == 0) {
            return 0.0;
        }

        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int i = 0; i < length; i++) {
            double l = left.get(i);
            double r = right.get(i);
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }

        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
