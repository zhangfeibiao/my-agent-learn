package com.example.agentlearn.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private Json() {
    }

    public static Object parse(String text) {
        return new Parser(text).parse();
    }

    public static String stringify(Object value) {
        StringBuilder builder = new StringBuilder();
        write(value, builder);
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    private static void write(Object value, StringBuilder builder) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof String string) {
            writeString(string, builder);
        } else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else if (value instanceof Map<?, ?> map) {
            builder.append('{');
            boolean first = true;
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) map).entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                writeString(String.valueOf(entry.getKey()), builder);
                builder.append(':');
                write(entry.getValue(), builder);
            }
            builder.append('}');
        } else if (value instanceof Iterable<?> iterable) {
            builder.append('[');
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                write(item, builder);
            }
            builder.append(']');
        } else {
            writeString(String.valueOf(value), builder);
        }
    }

    private static void writeString(String value, StringBuilder builder) {
        builder.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(c);
            }
        }
        builder.append('"');
    }

    private static final class Parser {
        private final String text;
        private int index;

        private Parser(String text) {
            this.text = text;
        }

        private Object parse() {
            Object value = parseValue();
            skipWhitespace();
            if (index != text.length()) {
                throw error("Unexpected trailing content");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= text.length()) {
                throw error("Unexpected end of JSON");
            }
            char c = text.charAt(index);
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                expect('}');
                return object;
            }
            while (true) {
                String key = parseString();
                skipWhitespace();
                expect(':');
                object.put(key, parseValue());
                skipWhitespace();
                if (peek('}')) {
                    expect('}');
                    return object;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> array = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                expect(']');
                return array;
            }
            while (true) {
                array.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    expect(']');
                    return array;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < text.length()) {
                char c = text.charAt(index++);
                if (c == '"') {
                    return builder.toString();
                }
                if (c == '\\') {
                    if (index >= text.length()) {
                        throw error("Unfinished escape sequence");
                    }
                    char escaped = text.charAt(index++);
                    switch (escaped) {
                        case '"' -> builder.append('"');
                        case '\\' -> builder.append('\\');
                        case '/' -> builder.append('/');
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case 'u' -> builder.append(parseUnicode());
                        default -> throw error("Unsupported escape sequence");
                    }
                } else {
                    builder.append(c);
                }
            }
            throw error("Unterminated string");
        }

        private char parseUnicode() {
            if (index + 4 > text.length()) {
                throw error("Invalid unicode escape");
            }
            String hex = text.substring(index, index + 4);
            index += 4;
            return (char) Integer.parseInt(hex, 16);
        }

        private Object parseLiteral(String literal, Object value) {
            if (!text.startsWith(literal, index)) {
                throw error("Expected " + literal);
            }
            index += literal.length();
            return value;
        }

        private Number parseNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            while (index < text.length() && Character.isDigit(text.charAt(index))) {
                index++;
            }
            if (peek('.')) {
                index++;
                while (index < text.length() && Character.isDigit(text.charAt(index))) {
                    index++;
                }
            }
            if (peek('e') || peek('E')) {
                index++;
                if (peek('+') || peek('-')) {
                    index++;
                }
                while (index < text.length() && Character.isDigit(text.charAt(index))) {
                    index++;
                }
            }
            if (start == index) {
                throw error("Expected number");
            }
            return Double.parseDouble(text.substring(start, index));
        }

        private void expect(char expected) {
            skipWhitespace();
            if (index >= text.length() || text.charAt(index) != expected) {
                throw error("Expected '" + expected + "'");
            }
            index++;
        }

        private boolean peek(char expected) {
            return index < text.length() && text.charAt(index) == expected;
        }

        private void skipWhitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at index " + index);
        }
    }
}
