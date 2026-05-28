package com.example.agentlearn.llm.logging;

import java.util.UUID;

public final class LlmCallContext {
    private static final ThreadLocal<String> SESSION_ID = new ThreadLocal<>();

    private LlmCallContext() {
    }

    public static String sessionId() {
        String sessionId = SESSION_ID.get();
        return sessionId == null || sessionId.isBlank() ? UUID.randomUUID().toString() : sessionId;
    }

    public static void withNewSession(ThrowingRunnable action) throws Exception {
        withSession(UUID.randomUUID().toString(), action);
    }

    public static void withSession(String sessionId, ThrowingRunnable action) throws Exception {
        String previous = SESSION_ID.get();
        SESSION_ID.set(sessionId);
        try {
            action.run();
        } finally {
            restore(previous);
        }
    }

    public static <T> T withNewSession(ThrowingSupplier<T> action) throws Exception {
        return withSession(UUID.randomUUID().toString(), action);
    }

    public static <T> T withSession(String sessionId, ThrowingSupplier<T> action) throws Exception {
        String previous = SESSION_ID.get();
        SESSION_ID.set(sessionId);
        try {
            return action.get();
        } finally {
            restore(previous);
        }
    }

    private static void restore(String previous) {
        if (previous == null) {
            SESSION_ID.remove();
        } else {
            SESSION_ID.set(previous);
        }
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
