package com.example.nbatickethub.model;

final class ModelChecks {
    private ModelChecks() { }

    static String text(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    static String id(String value) {
        text(value, "id");
        if (!value.matches("[A-Za-z0-9][A-Za-z0-9_.:-]*")) {
            throw new IllegalArgumentException("Invalid ID: " + value);
        }
        return value;
    }

    static float finite(float value, String field) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(field + " must be finite");
        }
        return value;
    }

    static int positive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }
}

