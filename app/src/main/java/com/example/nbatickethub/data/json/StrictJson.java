package com.example.nbatickethub.data.json;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small dependency-free JSON reader so parsing is identical on Android and JVM tests. */
public final class StrictJson {
    private static final int MAX_DEPTH = 64;
    private static final int MAX_CHARACTERS = 1_048_576;
    private final Reader reader;
    private int current;
    private int offset;

    private StrictJson(Reader reader) throws IOException {
        this.reader = reader;
        advance();
    }

    public static Object read(Reader reader) throws IOException {
        StrictJson parser = new StrictJson(reader);
        Object result = parser.value(0);
        parser.whitespace();
        if (parser.current != -1) { throw parser.error("Trailing content"); }
        return result;
    }

    private Object value(int depth) throws IOException {
        if (depth > MAX_DEPTH) { throw error("JSON is too deeply nested"); }
        whitespace();
        switch (current) {
            case '{': return object(depth + 1);
            case '[': return array(depth + 1);
            case '"': return string();
            case 't': literal("true"); return Boolean.TRUE;
            case 'f': literal("false"); return Boolean.FALSE;
            case 'n': literal("null"); return null;
            default:
                if (current == '-' || (current >= '0' && current <= '9')) { return number(); }
                throw error("Expected JSON value");
        }
    }

    private Map<String, Object> object(int depth) throws IOException {
        consume('{');
        Map<String, Object> result = new LinkedHashMap<>();
        whitespace();
        if (current == '}') { advance(); return result; }
        while (true) {
            whitespace();
            if (current != '"') { throw error("Expected object key"); }
            String key = string();
            whitespace();
            consume(':');
            if (result.containsKey(key)) { throw error("Duplicate object key: " + key); }
            result.put(key, value(depth));
            whitespace();
            if (current == '}') { advance(); return result; }
            consume(',');
        }
    }

    private List<Object> array(int depth) throws IOException {
        consume('[');
        List<Object> result = new ArrayList<>();
        whitespace();
        if (current == ']') { advance(); return result; }
        while (true) {
            result.add(value(depth));
            whitespace();
            if (current == ']') { advance(); return result; }
            consume(',');
        }
    }

    private String string() throws IOException {
        consume('"');
        StringBuilder result = new StringBuilder();
        while (current != '"') {
            if (current < 32) { throw error("Unterminated string or control character"); }
            if (current == '\\') {
                advance();
                switch (current) {
                    case '"': case '\\': case '/': result.append((char) current); advance(); break;
                    case 'b': result.append('\b'); advance(); break;
                    case 'f': result.append('\f'); advance(); break;
                    case 'n': result.append('\n'); advance(); break;
                    case 'r': result.append('\r'); advance(); break;
                    case 't': result.append('\t'); advance(); break;
                    case 'u':
                        int code = 0;
                        for (int i = 0; i < 4; i++) {
                            advance();
                            int digit = current >= '0' && current <= '9' ? current - '0'
                                    : current >= 'a' && current <= 'f' ? current - 'a' + 10
                                    : current >= 'A' && current <= 'F' ? current - 'A' + 10 : -1;
                            if (digit < 0) { throw error("Invalid Unicode escape"); }
                            code = code * 16 + digit;
                        }
                        result.append((char) code);
                        advance();
                        break;
                    default: throw error("Invalid string escape");
                }
            } else {
                result.append((char) current);
                advance();
            }
        }
        advance();
        for (int i = 0; i < result.length(); i++) {
            char c = result.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i >= result.length() || !Character.isLowSurrogate(result.charAt(i))) {
                    throw error("Unpaired Unicode surrogate");
                }
            } else if (Character.isLowSurrogate(c)) { throw error("Unpaired Unicode surrogate"); }
        }
        return result.toString();
    }

    private BigDecimal number() throws IOException {
        StringBuilder token = new StringBuilder();
        while (current != -1 && current != ',' && current != ']' && current != '}'
                && current != ' ' && current != '\t' && current != '\r' && current != '\n') {
            if (token.length() >= 128) { throw error("Number is too long"); }
            token.append((char) current);
            advance();
        }
        if (!token.toString().matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")) {
            throw error("Invalid JSON number");
        }
        try { return new BigDecimal(token.toString()); }
        catch (NumberFormatException error) { throw error("Invalid numeric exponent"); }
    }

    private void literal(String text) throws IOException {
        for (int i = 0; i < text.length(); i++) { consume(text.charAt(i)); }
    }

    private void whitespace() throws IOException {
        while (current == ' ' || current == '\t' || current == '\r' || current == '\n') {
            advance();
        }
    }

    private void consume(char expected) throws IOException {
        if (current != expected) { throw error("Expected '" + expected + "'"); }
        advance();
    }

    private void advance() throws IOException {
        current = reader.read();
        if (++offset > MAX_CHARACTERS) { throw error("Manifest exceeds character budget"); }
    }

    private IOException error(String message) {
        return new IOException(message + " at character " + offset);
    }
}
