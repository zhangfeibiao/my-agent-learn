package com.example.agentlearn;

import java.util.Collection;
import java.util.Objects;

public final class TestSupport {
    private TestSupport() {
    }

    public static void assertEquals(Object actual, Object expected) {
        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("Expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

    public static void assertContains(String actual, String expectedPart) {
        if (actual == null || !actual.contains(expectedPart)) {
            throw new AssertionError("Expected text to contain <" + expectedPart + "> but was <" + actual + ">");
        }
    }

    public static void assertContains(Collection<String> actual, String expected) {
        if (!actual.contains(expected)) {
            throw new AssertionError("Expected collection to contain <" + expected + "> but was <" + actual + ">");
        }
    }
}
